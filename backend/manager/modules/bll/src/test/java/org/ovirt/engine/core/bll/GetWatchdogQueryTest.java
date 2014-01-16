package org.ovirt.engine.core.bll;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.VmDeviceDAO;

@RunWith(MockitoJUnitRunner.class)
public class GetWatchdogQueryTest extends AbstractQueryTest<IdQueryParameters, GetWatchdogQuery<IdQueryParameters>> {

    @Mock
    VmDeviceDAO vmDeviceDAO;

    @Test
    public void executeQueryCommandWithNull() {
        Mockito.when(getDbFacadeMockInstance().getVmDeviceDao()).thenReturn(vmDeviceDAO);
        Mockito.when(getQueryParameters().getId()).thenReturn(new Guid("ee655a4d-effc-4aab-be2b-2f80ff40cd1c"));
        getQuery().executeQueryCommand();
        Assert.assertNull(getQuery().getReturnValue());
    }

    @Test
    @Ignore
    public void executeQueryCommandWithWatchdog() {
        final Guid vmId = new Guid("ee655a4d-effc-4aab-be2b-2f80ff40cd1c");
        VmDeviceDAO vmDeviceDaoMock = Mockito.mock(VmDeviceDAO.class);
        HashMap<String, Object> watchdogSpecParams = new HashMap<String, Object>();
        watchdogSpecParams.put("model", "i6300esb");
        watchdogSpecParams.put("action", "reset");
        VmDevice vmDevice = new VmDevice(new VmDeviceId(new Guid("6f86b8a4-e721-4149-b2df-056eb621b16a"),
                vmId), VmDeviceGeneralType.WATCHDOG, VmDeviceType.WATCHDOG.getName(), "", 1, watchdogSpecParams, true,
                true, true, "", null, null);
        Mockito.when(vmDeviceDaoMock.getVmDeviceByVmIdAndType(vmId, VmDeviceGeneralType.WATCHDOG))
                .thenReturn(Arrays.asList(vmDevice));
        GetWatchdogQuery<IdQueryParameters> query =
                new GetWatchdogQuery<IdQueryParameters>(new IdQueryParameters(vmId));
        query = Mockito.spy(query);
        Mockito.when(query.getVmDeviceDAO()).thenReturn(vmDeviceDaoMock);
        IdQueryParameters params = new IdQueryParameters(vmId);
        Mockito.when(query.getParameters())
                .thenReturn(params);
        Mockito.doCallRealMethod().when(query).executeQueryCommand();
        Mockito.doCallRealMethod().when(query).getReturnValue();
        Mockito.doCallRealMethod().when(query).setReturnValue(Mockito.any(Object.class));

        query.executeQueryCommand();
        Assert.assertNotNull(query.getReturnValue());
        Assert.assertEquals((((VmWatchdog)query.getReturnValue()).getAction()).name().toLowerCase(), "reset");
        Assert.assertEquals((((VmWatchdog)query.getReturnValue()).getModel()).name().toLowerCase(), "i6300esb");
    }

}
