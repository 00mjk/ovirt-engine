package org.ovirt.engine.extensions.aaa.builtin.kerberosldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.api.extensions.aaa.Authn;
import org.ovirt.engine.core.common.businessentities.LdapGroup;
import org.ovirt.engine.core.common.businessentities.LdapUser;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.utils.kerberos.AuthenticationResult;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public abstract class LdapBrokerCommandBase extends BrokerCommandBase {

    protected static final Map<AuthenticationResult, Integer> resultsMap = new HashMap<>();

    static {
        resultsMap.put(AuthenticationResult.CANNOT_FIND_LDAP_SERVER_FOR_DOMAIN,
                Authn.AuthResult.REMOTE_UNAVAILABLE);
        resultsMap.put(AuthenticationResult.CLIENT_NOT_FOUND_IN_KERBEROS_DATABASE,
                Authn.AuthResult.CREDENTIALS_INCORRECT);
        resultsMap.put(AuthenticationResult.CLOCK_SKEW_TOO_GREAT,
                Authn.AuthResult.GENERAL_ERROR);
        resultsMap.put(AuthenticationResult.CONNECTION_ERROR,
                Authn.AuthResult.REMOTE_UNAVAILABLE);
        resultsMap.put(AuthenticationResult.CONNECTION_TIMED_OUT,
                Authn.AuthResult.TIMED_OUT);
        resultsMap.put(AuthenticationResult.DNS_COMMUNICATION_ERROR,
                Authn.AuthResult.REMOTE_UNAVAILABLE);
        resultsMap.put(AuthenticationResult.DNS_ERROR,
                Authn.AuthResult.REMOTE_UNAVAILABLE);
        resultsMap.put(AuthenticationResult.INTERNAL_KERBEROS_ERROR,
                Authn.AuthResult.GENERAL_ERROR);
        resultsMap.put(AuthenticationResult.INVALID_CREDENTIALS,
                Authn.AuthResult.CREDENTIALS_INVALID);
        resultsMap.put(AuthenticationResult.NO_KDCS_FOUND,
                Authn.AuthResult.REMOTE_UNAVAILABLE);
        resultsMap.put(AuthenticationResult.NO_USER_INFORMATION_WAS_FOUND_FOR_USER,
                Authn.AuthResult.CREDENTIALS_INCORRECT);
        resultsMap.put(AuthenticationResult.OTHER,
                Authn.AuthResult.GENERAL_ERROR);
        resultsMap.put(AuthenticationResult.PASSWORD_EXPIRED,
                Authn.AuthResult.CREDENTIALS_EXPIRED);
        resultsMap.put(AuthenticationResult.USER_ACCOUNT_DISABLED_OR_LOCKED,
                Authn.AuthResult.ACCOUNT_LOCKED);
        resultsMap.put(AuthenticationResult.WRONG_REALM,
                Authn.AuthResult.CREDENTIALS_INCORRECT);
    }

    private Map<String, LdapGroup> globalGroupsDict = new HashMap<>();

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
        setUserDomainCredentials(domain);
    }

    protected void setUserDomainCredentials(String domain) {
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

    @Override
    public LdapReturnValueBase execute() {
        boolean exceptionOccurred = true;
        try {
            log.debugFormat("Running LDAP command: {0}", getClass().getName());
            String loginNameForKerberos =
                    LdapBrokerUtils.modifyLoginNameForKerberos(getLoginName(), getAuthenticationDomain());
            LdapCredentials ldapCredentials = new LdapCredentials(loginNameForKerberos, getPassword());
            DirectorySearcher directorySearcher = new DirectorySearcher(ldapCredentials);
            executeQuery(directorySearcher);
            exceptionOccurred = directorySearcher.getException() != null;
        }
 finally {
            if (exceptionOccurred) {
                log.error(String.format("Failed to run command %s. Domain is %s. User is %s.",
                        getClass().getSimpleName(), getDomain(), getLoginName()));
                _ldapReturnValue.setExceptionString(VdcBllMessages.FAILED_TO_RUN_LDAP_QUERY.name());
                _ldapReturnValue.setSucceeded(false);
            }
        }
        return _ldapReturnValue;
    }

    protected void handleRootDSEFailure(DirectorySearcher directorySearcher) {
        // Supposed to handle rootDSEFailure - default implementation does nothing. Subclasses may override this
        // behavior
    }

    protected abstract void executeQuery(DirectorySearcher directorySearcher);

    protected LdapUser populateUserData(LdapUser user, String domain) {
        return populateUserData(user, domain, true);
    }

    protected LdapUser populateUserData(LdapUser user, String domain, boolean populateGroups) {
        if (user == null) {
            return null;
        }
        user.setDomainControler(domain);

        // Getting the groups
        HashMap<String, LdapGroup> groupsDict = new HashMap<String, LdapGroup>();

        GroupsDNQueryGenerator generator = new GroupsDNQueryGenerator();
        proceedGroupsSearchResult(null, user.getMemberof(), groupsDict, generator);
        user.setGroups(groupsDict);
        if (user.getUserName() != null && !user.getUserName().contains("@")) {
            user.setUserName(user.getUserName() + "@" + user.getDomainControler());
        }

        if (populateGroups) {
            if (generator.getHasValues()) {
                List<LdapQueryData> partialQueries = generator.getLdapQueriesData();
                for (LdapQueryData currQueryData : partialQueries) {
                    populateGroup(currQueryData,
                            getAuthenticationDomain(),
                            groupsDict,
                            getLoginName(),
                            getPassword());
                }
            }
        }
        user.setGroups(groupsDict);
        return user;
    }

    protected void populateGroup(LdapQueryData queryData,
                                 String domain,
                                 Map<String, LdapGroup> groupsDict,
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
                    populateGroup(partialQuery, domain, groupsDict, loginName, password);
                }
            }
        } catch (RuntimeException e) {
            log.infoFormat("populateGroup failed. Exception: {0}", e);
        }
    }

    private void ProceedGroupsSearchResult(GroupSearchResult groupsResult,
            Map<String, LdapGroup> groupsDict, GroupsDNQueryGenerator generator) {
        List<String> groupsList = groupsResult.getMemberOf();
        LdapGroup group = new LdapGroup();
        group.setid(groupsResult.getId());
        group.setname(LdapBrokerUtils.generateGroupDisplayValue(groupsResult.getDistinguishedName()));
        group.setMemberOf(groupsResult.getMemberOf());
        group.setDistinguishedName(groupsResult.getDistinguishedName());
        groupsDict.put(group.getname(), group);
        globalGroupsDict.put(group.getname(), group);
        proceedGroupsSearchResult(groupsResult.getId(), groupsList, groupsDict, generator);
    }

    private void proceedGroupsSearchResult(String groupId, List<String> groupDNList,
            Map<String, LdapGroup> groupsDict, GroupsDNQueryGenerator generator) {
        if (groupDNList == null) {
            return;
        }
        for (String groupDN : groupDNList) {
            String groupName = LdapBrokerUtils.generateGroupDisplayValue(groupDN);
            if (!groupsDict.containsKey(groupName)) {
                LdapGroup group = globalGroupsDict.get(groupDN);
                if (group == null) {
                    generator.add(groupDN);
                } else {
                    groupsDict.put(groupName, group);
                }
            }
        }
    }

    protected GroupsDNQueryGenerator createGroupsGeneratorForUser(LdapUser user) {
        List<String> dnsList = new ArrayList<String>();
        for (LdapGroup adGroup : user.getGroups().values()) {
            dnsList.add(adGroup.getDistinguishedName());
        }
        GroupsDNQueryGenerator generator = new GroupsDNQueryGenerator(new HashSet<String>(dnsList));
        return generator;
    }

    private static final Log log = LogFactory.getLog(LdapBrokerCommandBase.class);
}
