package org.ovirt.engine.api.restapi.resource;

import org.junit.Test;
import org.ovirt.engine.api.model.Cluster;
import org.ovirt.engine.api.model.Template;
import org.ovirt.engine.api.model.VmPool;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmPoolParametersBase;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.VmPoolType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.GetVmTemplateParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

import static org.easymock.EasyMock.expect;

public class BackendVmPoolsResourceTest extends
        AbstractBackendCollectionResourceTest<VmPool, org.ovirt.engine.core.common.businessentities.VmPool, BackendVmPoolsResource> {

    public BackendVmPoolsResourceTest() {
        super(new BackendVmPoolsResource(), SearchType.VmPools, "Pools : ");
    }

    @Override
    protected List<VmPool> getCollection() {
        return collection.list().getVmPools();
    }

    @Override
    protected org.ovirt.engine.core.common.businessentities.VmPool getEntity(int index) {
        org.ovirt.engine.core.common.businessentities.VmPool entity = control.createMock(org.ovirt.engine.core.common.businessentities.VmPool.class);
        expect(entity.getVmPoolId()).andReturn(GUIDS[index]).anyTimes();
        expect(entity.getName()).andReturn(NAMES[index]).anyTimes();
        expect(entity.getVmPoolDescription()).andReturn(DESCRIPTIONS[index]).anyTimes();
        expect(entity.getVmPoolType()).andReturn(VmPoolType.Automatic).anyTimes();
        expect(entity.getVdsGroupId()).andReturn(GUIDS[2]).anyTimes();
        return entity;
    }

    protected org.ovirt.engine.core.common.businessentities.VmTemplate getTemplateEntity() {
        VmTemplate entity = control.createMock(VmTemplate.class);
        expect(entity.getId()).andReturn(GUIDS[1]).anyTimes();
        expect(entity.getVdsGroupId()).andReturn(GUIDS[2]).anyTimes();
        expect(entity.getName()).andReturn(NAMES[1]).anyTimes();
        expect(entity.getDescription()).andReturn(DESCRIPTIONS[1]).anyTimes();
        expect(entity.getNumOfCpus()).andReturn(8).anyTimes();
        expect(entity.getNumOfSockets()).andReturn(2).anyTimes();
        expect(entity.getDefaultDisplayType()).andReturn(DisplayType.vnc).anyTimes();
        expect(entity.getNumOfMonitors()).andReturn(2).anyTimes();
        expect(entity.getVmType()).andReturn(VmType.Server).anyTimes();
        return entity;
    }

    private VmPool getModel(int index) {
        VmPool model = new VmPool();
        model.setName(NAMES[index]);
        model.setDescription(DESCRIPTIONS[index]);
        model.setId(GUIDS[index].toString());
        model.setCluster(new Cluster());
        model.getCluster().setId(GUIDS[2].toString());
        model.setTemplate(new Template());
        model.getTemplate().setId(GUIDS[1].toString());
        return model;
    }

    @Test
    public void add() throws Exception {
        setUriInfo(setUpBasicUriExpectations());

        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                GetVmTemplateParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[1] },
                getTemplateEntity());

        setUpGetConsoleExpectations(new int[]{1});

        setUpCreationExpectations(VdcActionType.AddVmPoolWithVms,
             VmPoolParametersBase.class,
             new String[] { "StorageDomainId" },
             new Object[] { GUIDS[0] },
             true,
             true,
             GUIDS[0],
             VdcQueryType.GetVmPoolById,
             IdQueryParameters.class,
             new String[] { "Id" },
             new Object[] { GUIDS[0] },
             getEntity(0));

        Response response = collection.add(getModel(0));
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof VmPool);
        verifyModelTemplate((VmPool) response.getEntity());
    }

    @Test
    @Override
    public void testList() throws Exception {
        UriInfo uriInfo = setUpUriExpectations(null);

        setUpQueryExpectations("");
        collection.setUriInfo(uriInfo);
        verifyCollection(getCollection());
    }

    @Test
    @Override
    public void testQuery() throws Exception {
        UriInfo uriInfo = setUpUriExpectations(QUERY);

        setUpQueryExpectations(QUERY);
        collection.setUriInfo(uriInfo);
        verifyCollection(getCollection());
    }

    protected void verifyModelTemplate(VmPool model) {
        super.verifyModel(model, 0);
        verifyModelSpecific(model);
    }

    static void verifyModelSpecific(VmPool model) {
        assertNotNull(model.getCluster());
        assertEquals(GUIDS[2].toString(), model.getCluster().getId());
    }
}
