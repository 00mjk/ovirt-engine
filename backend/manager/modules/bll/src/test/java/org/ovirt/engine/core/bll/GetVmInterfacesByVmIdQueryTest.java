package org.ovirt.engine.core.bll;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.ovirt.engine.core.common.queries.GetVmByVmIdParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.VmNetworkInterfaceDAO;

/** A test case for {@link GetVmInterfacesByVmIdQuery} */
public class GetVmInterfacesByVmIdQueryTest extends AbstractUserQueryTest<GetVmByVmIdParameters, GetVmInterfacesByVmIdQuery<GetVmByVmIdParameters> > {

    /** A test that checked that all the parameters are passed properly to the DAO */
    @Test
    public void testExectueQuery() {
        DbFacade dbFacadeMock = getDbFacadeMockInstance();

        VmNetworkInterfaceDAO daoMock = mock(VmNetworkInterfaceDAO.class);
        when(dbFacadeMock.getVmNetworkInterfaceDAO()).thenReturn(daoMock);

        Guid guid   = new Guid();

        GetVmByVmIdParameters params = getQueryParameters();
        when(params.getId()).thenReturn(guid);

        GetVmInterfacesByVmIdQuery<?> query = getQuery();

        query.executeQueryCommand();

        verify(daoMock).getAllForVm(guid, getUser().getUserId(), getQueryParameters().isFiltered());
    }
}

