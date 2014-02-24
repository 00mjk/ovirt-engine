package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import org.ovirt.engine.api.model.Domain;
import org.ovirt.engine.api.model.Fault;
import org.ovirt.engine.api.model.Group;
import org.ovirt.engine.core.aaa.DirectoryGroup;
import org.ovirt.engine.core.aaa.DirectoryStub;
import org.ovirt.engine.core.common.action.DirectoryIdParameters;
import org.ovirt.engine.core.common.action.IdParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.DbGroup;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.DirectoryIdQueryParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendGroupsResourceTest
    extends AbstractBackendCollectionResourceTest<Group, DbGroup, BackendGroupsResource> {

    /**
     * This is the query that will be used when the user didn't provide any query explicitly.
     */
    private static final String QUERY = "grpname != \"\"";

    /**
     * This is the query that will be used when the user provided a query explicitly in the parameters.
     */
    private static final String SEARCH_QUERY =
        "name=s* AND id=*0 and grpname != \"\"";

    /**
     * These are the names that will be used to build the directory group objects returned by mocked directory group
     * searches, thus then must have the same format that we generate when searching in directories.
     */
    private static final String[] GROUP_NAMES;

    static {
        GROUP_NAMES = new String[NAMES.length];
        for (int i = 0; i < NAMES.length; i++) {
            GROUP_NAMES[i] = DOMAIN + "/Groups/" + NAMES[i];
        }
    }

    public BackendGroupsResourceTest() {
        super(new BackendGroupsResource(), SearchType.DBGroup, "Groups : ");
    }

    @Test
    public void testList() throws Exception {
        UriInfo uriInfo = setUpUriExpectations(null);
        setUpQueryExpectations(QUERY);
        collection.setUriInfo(uriInfo);
        verifyCollection(getCollection());
    }

    @Test
    public void testListFailure() throws Exception {
        UriInfo uriInfo = setUpUriExpectations(null);
        setUpQueryExpectations(QUERY, FAILURE);
        collection.setUriInfo(uriInfo);
        try {
            getCollection();
            fail("expected WebApplicationException");
        }
        catch (WebApplicationException wae) {
            assertTrue(wae.getResponse().getEntity() instanceof Fault);
            assertEquals(mockl10n(FAILURE), ((Fault) wae.getResponse().getEntity()).getDetail());
        }
    }

    @Test
    public void testListCrash() throws Exception {
        UriInfo uriInfo = setUpUriExpectations(null);
        Throwable t = new RuntimeException(FAILURE);
        setUpQueryExpectations(QUERY, t);
        collection.setUriInfo(uriInfo);
        try {
            getCollection();
            fail("expected WebApplicationException");
        }
        catch (WebApplicationException wae) {
            verifyFault(wae, BACKEND_FAILED_SERVER_LOCALE, t);
        }
    }

    @Test
    public void testListCrashClientLocale() throws Exception {
        UriInfo uriInfo = setUpUriExpectations(null);
        locales.add(CLIENT_LOCALE);
        Throwable t = new RuntimeException(FAILURE);
        setUpQueryExpectations(QUERY, t);
        collection.setUriInfo(uriInfo);
        try {
            getCollection();
            fail("expected WebApplicationException");
        }
        catch (WebApplicationException wae) {
            verifyFault(wae, BACKEND_FAILED_CLIENT_LOCALE, t);
        }
        finally {
            locales.clear();
        }
    }

    @Test
    public void testQuery() throws Exception {
        UriInfo uriInfo = setUpUriExpectations(SEARCH_QUERY);
        setUpQueryExpectations(SEARCH_QUERY);
        collection.setUriInfo(uriInfo);
        verifyCollection(getCollection());
    }

    @Test
    public void testRemove() throws Exception {
        setUpGetEntityExpectations();
        setUriInfo(
            setUpActionExpectations(
                VdcActionType.RemoveGroup,
                IdParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[0] },
                true,
                true
            )
        );
        verifyRemove(collection.remove(GUIDS[0].toString()));
    }

    @Test
    public void testRemoveNonExistant() throws Exception{
        setUpGetEntityExpectations(NON_EXISTANT_GUID, true);
        control.replay();
        try {
            collection.remove(NON_EXISTANT_GUID.toString());
            fail("expected WebApplicationException");
        }
        catch (WebApplicationException wae) {
            assertNotNull(wae.getResponse());
            assertEquals(404, wae.getResponse().getStatus());
        }
    }

    private void setUpGetEntityExpectations() throws Exception {
        setUpGetEntityExpectations(GUIDS[0], false);
    }

    private void setUpGetEntityExpectations(Guid entityId, boolean returnNull) throws Exception {
        setUpGetEntityExpectations(
            VdcQueryType.GetDbGroupById,
            IdQueryParameters.class,
            new String[] { "Id" },
            new Object[] { entityId },
            returnNull? null : getEntity(0)
        );
    }

    @Test
    public void testRemoveCantDo() throws Exception {
        doTestBadRemove(false, true, CANT_DO);
    }

    @Test
    public void testRemoveFailed() throws Exception {
        doTestBadRemove(true, false, FAILURE);
    }

    private void doTestBadRemove(boolean canDo, boolean success, String detail) throws Exception {
        setUpGetEntityExpectations();
        setUriInfo(
            setUpActionExpectations(
                VdcActionType.RemoveGroup,
                IdParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[0] },
                canDo,
                success
            )
        );
        try {
            collection.remove(GUIDS[0].toString());
            fail("expected WebApplicationException");
        }
        catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    /**
     * Test that a group can be added when the user provides explicitly the name of the directory, so there is no need
     * to extract it from the name of the group.
     */
    @Test
    public void testAddGroupWithExplicitDirectoryName() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations(
            "ADGROUP@" + DOMAIN + ": allnames=" + NAMES[0],
            SearchType.DirectoryGroup,
            getDirectoryGroup(0)
        );
        setUpCreationExpectations(
            VdcActionType.AddGroup,
            DirectoryIdParameters.class,
            new String[] { "Directory", "Id" },
            new Object[] { DOMAIN, EXTERNAL_IDS[0] },
            true,
            true,
            GUIDS[0],
            VdcQueryType.GetDbGroupById,
            IdQueryParameters.class,
            new String[] { "Id" },
            new Object[] { GUIDS[0] },
            getEntity(0)
        );

        Domain domain = new Domain();
        domain.setName(DOMAIN);
        Group model = new Group();
        model.setName(NAMES[0]);
        model.setDomain(domain);

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Group);
        verifyModel((Group) response.getEntity(), 0);
    }

    /**
     * Test that a group can be added when the user doesn't explicitly provide the name of the directory, but provides
     * it as part of the group name.
     */
    @Test
    public void testAddGroupWithImplicitDirectoryName() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations(
            "ADGROUP@" + DOMAIN + ": allnames=" + NAMES[0],
            SearchType.DirectoryGroup,
            getDirectoryGroup(0)
        );
        setUpCreationExpectations(
            VdcActionType.AddGroup,
            DirectoryIdParameters.class,
            new String[] { "Directory", "Id" },
            new Object[] { DOMAIN, EXTERNAL_IDS[0] },
            true,
            true,
            GUIDS[0],
            VdcQueryType.GetDbGroupById,
            IdQueryParameters.class,
            new String[] { "Id" },
            new Object[] { GUIDS[0] },
            getEntity(0)
        );

        Group model = new Group();
        model.setName(GROUP_NAMES[0]);

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Group);
        verifyModel((Group) response.getEntity(), 0);
    }

    /**
     * Test that a group can't be added if the directory name isn't provider explicitly or as part of the group name.
     */
    @Test
    public void testAddGroupWithoutDirectoryName() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        control.replay();

        Group model = new Group();
        model.setName(NAMES[0]);

        try {
           collection.add(model);
           fail("expected WebApplicationException");
        }
        catch (WebApplicationException wae) {
            assertNotNull(wae.getResponse());
            assertEquals(400, wae.getResponse().getStatus());
        }
    }

    /**
     * Test that if the group identifier is provided it is used to search in the directory instead of the name.
     */
    @Test
    public void testAddGroupById() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations(
            VdcQueryType.GetDirectoryGroupById,
            DirectoryIdQueryParameters.class,
            new String[] { "Domain", "Id" },
            new Object[] { DOMAIN, EXTERNAL_IDS[0] },
            getDirectoryGroup(0)
        );
        setUpCreationExpectations(
            VdcActionType.AddGroup,
            DirectoryIdParameters.class,
            new String[] { "Directory", "Id" },
            new Object[] { DOMAIN, EXTERNAL_IDS[0] },
            true,
            true,
            GUIDS[0],
            VdcQueryType.GetDbGroupById,
            IdQueryParameters.class,
            new String[] { "Id" },
            new Object[] { GUIDS[0] },
            getEntity(0)
        );

        Group model = new Group();
        model.setName(GROUP_NAMES[0]);
        model.setId(EXTERNAL_IDS[0].toHex());

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Group);
        verifyModel((Group) response.getEntity(), 0);
    }

    /**
     * Test that if the provided directory identifier doesn't correspond to any existing directory user the user isn't
     * added.
     */
    @Test
    public void testAddGroupByIdFailure() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations(
            VdcQueryType.GetDirectoryGroupById,
            DirectoryIdQueryParameters.class,
            new String[] { "Domain", "Id" },
            new Object[] { DOMAIN, NON_EXISTANT_EXTERNAL_ID },
            null
        );
        control.replay();
        Group model = new Group();
        model.setName(GROUP_NAMES[0]);
        model.setId(NON_EXISTANT_EXTERNAL_ID.toHex());

        try {
            collection.add(model);
            fail("expected WebApplicationException");
        }
        catch (WebApplicationException wae) {
            assertNotNull(wae.getResponse());
            assertEquals(404, wae.getResponse().getStatus());
        }
    }

    @Override
    protected List<Group> getCollection() {
        return collection.list().getGroups();
    }

    @Override
    protected DbGroup getEntity(int index) {
        DbGroup entity = new DbGroup();
        entity.setId(GUIDS[index]);
        entity.setExternalId(EXTERNAL_IDS[index]);
        entity.setName(GROUP_NAMES[index]);
        entity.setDomain(DOMAIN);
        return entity;
    }

    private DirectoryGroup getDirectoryGroup(int index) {
        return new DirectoryGroup(new DirectoryStub(DOMAIN), EXTERNAL_IDS[index], GROUP_NAMES[index]);
    }

    @Override
    protected void verifyModel(Group model, int index) {
        assertEquals(GUIDS[index].toString(), model.getId());
        assertEquals(EXTERNAL_IDS[index].toHex(), model.getExternalId());
        assertEquals(GROUP_NAMES[index], model.getName());
        assertNotNull(model.getDomain());
        assertEquals(new Guid(DOMAIN.getBytes(), true).toString(), model.getDomain().getId());
        verifyLinks(model);
    }
}
