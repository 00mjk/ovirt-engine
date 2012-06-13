package org.ovirt.engine.core.bll.adbroker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.session.SessionDataContainer;
import org.ovirt.engine.core.common.businessentities.AdUser;
import org.ovirt.engine.core.common.businessentities.ad_groups;
import org.ovirt.engine.core.common.interfaces.IVdcUser;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public abstract class LdapBrokerCommandBase extends BrokerCommandBase {
    @Override
    protected String getPROTOCOL() {
        return "LDAP://";
    }

    protected LdapBrokerCommandBase(LdapUserPasswordBaseParameters parameters) {
        super(parameters);
        setAuthenticationDomain(getDomain());
    }

    protected LdapBrokerCommandBase(LdapBrokerBaseParameters parameters) {
        super(parameters);
        initCredentials(parameters.getDomain());
    }

    protected void initCredentials(String domain) {
        IVdcUser curUser;
        if (StringHelper.isNullOrEmpty(getParameters().getSessionId())) {
            curUser = SessionDataContainer.getInstance().getUser(false);
        } else {
            curUser = SessionDataContainer.getInstance().getUser(getParameters().getSessionId(), false);
        }
        // verify that in auto login mode , user is not taken from session.
        if (curUser != null && !StringHelper.isNullOrEmpty(curUser.getPassword())) {
            setLoginName(curUser.getUserName());
            setPassword(curUser.getPassword());
            setAuthenticationDomain(curUser.getDomainControler());
        } else {
            Domain domainObject = UsersDomainsCacheManagerService.getInstance().getDomain(domain);
            if (domainObject != null) {
                setLoginName(domainObject.getUserName());
                setPassword(domainObject.getPassword());
                if (getLoginName().contains("@")) {
                    String userDomain = getLoginName().split("@")[1].toLowerCase();
                    setAuthenticationDomain(userDomain);
                } else {
                    setAuthenticationDomain(domain);
                }
            }
        }
    }

    @Override
    public LdapReturnValueBase Execute() {
        try {
            log.debugFormat("Running LDAP command: {0}", getClass().getName());
            String loginNameForKerberos =
                    LdapBrokerUtils.modifyLoginNameForKerberos(getLoginName(), getAuthenticationDomain());
            LdapCredentials ldapCredentials = new LdapCredentials(loginNameForKerberos, getPassword());
            DirectorySearcher directorySearcher = new DirectorySearcher(ldapCredentials);
            executeQuery(directorySearcher);
        } catch (RuntimeException e) {
            log.errorFormat(
                    "Failed to run command {0}. Domain is {1}. User is {2}.}",
                    getClass().getSimpleName(), getDomain(), getLoginName());
        }
        return _ldapReturnValue;
    }

    protected void handleRootDSEFailure(DirectorySearcher directorySearcher) {
        // Supposed to handle rootDSEFailure - default implementation does nothing. Subclasses may override this
        // behavior
    }

    protected abstract void executeQuery(DirectorySearcher directorySearcher);

    protected AdUser populateUserData(AdUser user, String domain) {
        if (user == null) {
            return null;
        }
        user.setDomainControler(domain);

        // Getting the groups
        java.util.HashMap<String, ad_groups> groupsDict = new java.util.HashMap<String, ad_groups>();

        GroupsDNQueryGenerator generator = new GroupsDNQueryGenerator();
        proceedGroupsSearchResult(user.getMemberof(), groupsDict, generator);

        user.setGroups(groupsDict);

        return user;
    }

    protected void PopulateGroup(LdapQueryData queryData,
            String domain,
            java.util.Map<String, ad_groups> groupsDict,
            String loginName,
            String password) {
        try {
            GroupsDNQueryGenerator generator = new GroupsDNQueryGenerator();
            List<GroupSearchResult> searchResultCollection =
                    LdapBrokerUtils.performGroupQuery(loginName, password, domain, queryData);
            if (searchResultCollection != null) {
                for (GroupSearchResult searchResult : searchResultCollection) {
                    ProceedGroupsSearchResult(searchResult, groupsDict, generator);
                }
            }
            // If generator has results, it means there are parent groups
            if (generator.getHasValues()) {
                List<LdapQueryData> partialQueries = generator.getLdapQueriesData();
                for (LdapQueryData partialQuery : partialQueries) {
                    PopulateGroup(partialQuery, domain, groupsDict, loginName, password);
                }
            }
        } catch (RuntimeException e) {
            log.infoFormat("PopulateGroup failed. Exception: {0}", e);
        }
    }

    private void ProceedGroupsSearchResult(GroupSearchResult groupsResult,
            java.util.Map<String, ad_groups> groupsDict, GroupsDNQueryGenerator generator) {
        List<String> groupsList = groupsResult.getMemberOf();
        proceedGroupsSearchResult(groupsList, groupsDict, generator);
    }

    private void proceedGroupsSearchResult(List<String> groupDNList,
            Map<String, ad_groups> groupsDict, GroupsDNQueryGenerator generator) {
        if (groupDNList == null) {
            return;
        }
        for (String groupDN : groupDNList) {
            String groupName = LdapBrokerUtils.generateGroupDisplayValue(groupDN);
            if (!groupsDict.containsKey(groupName)) {
                ad_groups group = DbFacade.getInstance().getAdGroupDAO().getByName(groupName);
                if (group == null) {
                    group = new ad_groups();
                    group.setname(groupName);
                }
                group.setDistinguishedName(groupDN);
                groupsDict.put(groupName, group);
                generator.add(groupDN);
            }
        }
    }

    protected GroupsDNQueryGenerator createGroupsGeneratorForUser(AdUser user) {
        List<String> dnsList = new ArrayList<String>();
        for (ad_groups adGroup : user.getGroups().values()) {
            dnsList.add(adGroup.getDistinguishedName());
        }
        GroupsDNQueryGenerator generator = new GroupsDNQueryGenerator(new HashSet<String>(dnsList));
        return generator;
    }

    private static Log log = LogFactory.getLog(LdapBrokerCommandBase.class);
}
