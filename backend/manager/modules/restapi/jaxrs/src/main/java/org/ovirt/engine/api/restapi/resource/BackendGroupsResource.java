package org.ovirt.engine.api.restapi.resource;

import java.text.MessageFormat;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.api.common.util.QueryHelper;
import org.ovirt.engine.api.model.BaseResource;
import org.ovirt.engine.api.model.Group;
import org.ovirt.engine.api.model.Groups;
import org.ovirt.engine.api.resource.GroupResource;
import org.ovirt.engine.api.resource.GroupsResource;
import org.ovirt.engine.core.authentication.DirectoryGroup;
import org.ovirt.engine.core.common.action.DirectoryIdParameters;
import org.ovirt.engine.core.common.action.IdParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.DbGroup;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.DirectoryIdQueryParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.ExternalId;
import org.ovirt.engine.core.compat.Guid;

/**
 * This resource corresponds to groups that have been looked up in some directory accessible to the engine and then
 * added to the engine database. Groups can be added and removed from the collection, and this will add or remove them
 * from the database (not from the underlying directory).
 */
public class BackendGroupsResource
        extends AbstractBackendCollectionResource<Group, DbGroup>
        implements GroupsResource {

    static final String[] SUB_COLLECTIONS = { "permissions", "roles", "tags" };

    private static final String GROUPS_SEARCH_PATTERN = "grpname != \"\"";
    private static final String AND_SEARCH_PATTERN = " and ";

    /**
     * This search pattern is used when searching for the directory group that will be added to the database when the
     * {@code add} operation is performed.
     */
    private static final String DIRECTORY_GROUP_SEARCH_TEMPLATE = "ADGROUP@{0}: ";

    public BackendGroupsResource() {
        super(Group.class, DbGroup.class, SUB_COLLECTIONS);
    }

    /**
     * This method calculates the search pattern that will be used to perform the search of database groups during the
     * execution of the {@code list} operation.
     */
    private String getSearchPattern() {
        String userProvidedPattern = QueryHelper.getConstraint(getUriInfo(), "",  modelType);
        return userProvidedPattern.equals("Groups : ") ?
               userProvidedPattern + GROUPS_SEARCH_PATTERN
               :
               userProvidedPattern + AND_SEARCH_PATTERN + GROUPS_SEARCH_PATTERN;
    }

    /**
     * Determine what is the name of the directory that corresponds to the given group model. It may contained in the
     * model directly, or it can be embedded in the name.
     *
     * @param group the model of the group
     * @return the name of the directory or {@code null} if the group can't be determined
     */
    private String getDirectoryName(Group group) {
        if (group.isSetDomain() && group.getDomain().isSetName()) {
            return group.getDomain().getName();
        }
        else if (group.isSetDomain() && group.getDomain().isSetId()) {
            List<String> domains = getBackendCollection(
                String.class,
                VdcQueryType.GetDomainList,
                new VdcQueryParametersBase()
            );
            for (String domain :domains) {
                Guid domainId = new Guid(domain.getBytes(), true);
                if (domainId.toString().equals(group.getDomain().getId())) {
                   return domain;
                }
            }
            throw new WebFaultException(
                null,
                "Domain: '" + group.getDomain().getId().toString() + "' does not exist.",
                Response.Status.BAD_REQUEST
            );
        }
        else if (group.isSetName() && group.getName().contains("/")) {
            return group.getName().substring(0, group.getName().indexOf("/"));
        }
        return null;
    }

    /**
     * This method calculates the search pattern used to search for the directory group that will be added to the
     * database when performing the {@code add} operation.
     *
     * @param groupname the name of the user that will be searched in the
     *     directory
     * @param domain the name of the directory where the search will be
     *     performed
     */
    private String getDirectoryGroupSearchPattern(String groupname, String domain) {
        String constraint = QueryHelper.getConstraint(getUriInfo(), DbGroup.class, false);
        final StringBuilder sb = new StringBuilder(128);

        sb.append(MessageFormat.format(DIRECTORY_GROUP_SEARCH_TEMPLATE, domain));

        sb.append(StringUtils.isEmpty(constraint) ?
                        "allnames=" + groupname
                        :
                        constraint);

        return sb.toString();
    }

    private Groups mapDbGroupCollection(List<DbGroup> entities) {
        Groups collection = new Groups();
        for (DbGroup entity : entities) {
            Group group = map(entity);
            group = populate(group, entity);
            group = addLinks(group, BaseResource.class);
            collection.getGroups().add(group);
        }
        return collection;
    }

    @Override
    protected Group doPopulate(Group model, DbGroup entity) {
        return model;
    }

    @Override
    @SingleEntityResource
    public GroupResource getGroupSubResource(String id) {
        return inject(new BackendGroupResource(id, this));
    }

    @Override
    public Groups list() {
        if (isFiltered()) {
            return mapDbGroupCollection(getBackendCollection(VdcQueryType.GetAllDbGroups, new VdcQueryParametersBase()));
        }
        else {
            return mapDbGroupCollection(getBackendCollection(SearchType.DBGroup, getSearchPattern()));
        }
    }

    @Override
    public Response add(Group group) {
        validateParameters(group, "name");
        if (!isNameContainsDomain(group)) {
            validateParameters(group, "domain.id|name");
        }
        String directoryName = getDirectoryName(group);
        DirectoryGroup directoryGroup = findDirectoryGroup(directoryName, group);
        if (directoryGroup == null) {
            return Response.status(Status.BAD_REQUEST)
                .entity("No such group: " + group.getName() + " in directory " + directoryName)
                .build();
        }
        DirectoryIdParameters parameters = new DirectoryIdParameters();
        parameters.setDirectory(directoryName);
        parameters.setId(directoryGroup.getId());
        QueryIdResolver<Guid> resolver = new QueryIdResolver<>(VdcQueryType.GetDbGroupById, IdQueryParameters.class);
        return performCreate(VdcActionType.AddGroup, parameters, resolver, BaseResource.class);
    }

    /**
     * Find the directory user that corresponds to the given model.
     *
     * @param directoryName the name of the directory where to perform the search
     * @param groupModel the group model
     * @return the requested directory group or {@code null} if no such group exists
     */
    private DirectoryGroup findDirectoryGroup(String directoryName, Group groupModel) {
        // Try to find a group that matches the identifier contained in the model:
        if (groupModel.isSetId()) {
            String groupId = groupModel.getId();
            return getEntity(
                DirectoryGroup.class,
                VdcQueryType.GetDirectoryGroupById,
                new DirectoryIdQueryParameters(directoryName, ExternalId.fromHex(groupId)),
                groupId,
                true
            );
        }

        // Try to find a group that matches the name contained in the model:
        if (groupModel.isSetName()) {
            return getEntity(
                DirectoryGroup.class,
                SearchType.DirectoryGroup,
                getDirectoryGroupSearchPattern(groupModel.getName(), directoryName)
            );
        }

        return null;
    }

    @Override
    public Response performRemove(String id) {
        return performAction(VdcActionType.RemoveGroup, new IdParameters(asGuid(id)));
    }

    private boolean isNameContainsDomain(Group group) {
        String name = group.getName();
        return name.contains("/") && name.indexOf('/') != name.length() - 1;
    }

}
