package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.utils.MockConfigRule.mockConfig;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.action.HotPlugDiskToVmParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmOsType;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBaseMockUtils;
import org.ovirt.engine.core.dao.DiskDao;
import org.ovirt.engine.core.dao.VdsDAO;
import org.ovirt.engine.core.dao.VmDAO;
import org.ovirt.engine.core.dao.VmDeviceDAO;
import org.ovirt.engine.core.dao.VmNetworkInterfaceDAO;
import org.ovirt.engine.core.utils.MockConfigRule;

@RunWith(MockitoJUnitRunner.class)
public class HotPlugDiskToVmCommandTest {

    protected Guid diskImageGuid = Guid.NewGuid();
    protected Guid vmId = Guid.NewGuid();

    @Rule
    public static final MockConfigRule mcr = new MockConfigRule(
            mockConfig
            (ConfigValues.HotPlugUnsupportedOsList,
                    "RHEL3x64"),
            mockConfig(ConfigValues.HotPlugEnabled, "3.1", true)
            );

    @Mock
    private VmDAO vmDAO;
    @Mock
    private VdsDAO vdsDao;
    @Mock
    protected DiskDao diskDao;
    @Mock
    private VmDeviceDAO vmDeviceDAO;

    /**
     * The command under test.
     */
    protected HotPlugDiskToVmCommand<HotPlugDiskToVmParameters> command;

    @Test
    public void canDoActionFailedVMNotFound() throws Exception {
        mockNullVm();
        assertFalse(command.canDoAction());
        assertTrue(command.getReturnValue()
                .getCanDoActionMessages()
                .contains(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_FOUND.toString()));
    }

    @Test
    public void canDoActionFailedVMHasNotDisk() throws Exception {
        mockVmStatusUp();
        doReturn(diskDao).when(command).getDiskDao();
        when(diskDao.get(diskImageGuid)).thenReturn(null);
        assertFalse(command.canDoAction());
        assertTrue(command.getReturnValue()
                .getCanDoActionMessages()
                .contains(VdcBllMessages.ACTION_TYPE_FAILED_DISK_NOT_EXIST.toString()));
    }

    @Test
    public void canDoActionFailedVirtIODisk() throws Exception {
        mockVmStatusUp();
        createNotVirtIODisk();
        assertFalse(command.canDoAction());
        assertTrue(command.getReturnValue()
                .getCanDoActionMessages()
                .contains(VdcBllMessages.HOT_PLUG_DISK_IS_NOT_VIRTIO.toString()));
    }

    @Test
    public void canDoActionFailedWrongPlugStatus() throws Exception {
        mockVmStatusUp();
        cretaeDiskWrongPlug(true);
        assertFalse(command.canDoAction());
        assertTrue(command.getReturnValue()
                .getCanDoActionMessages()
                .contains(VdcBllMessages.HOT_PLUG_DISK_IS_NOT_UNPLUGGED.toString()));
    }

    @Test
    public void canDoActionFailedGuestOsIsNotSupported() {
        VM vm = mockVmStatusUp();
        vm.setvm_os(VmOsType.RHEL3x64);
        cretaeVirtIODisk();
        assertFalse(command.canDoAction());
        assertTrue(command.getReturnValue()
                .getCanDoActionMessages()
                .contains(VdcBllMessages.ACTION_TYPE_FAILED_GUEST_OS_VERSION_IS_NOT_SUPPORTED.toString()));
    }

    @Test
    public void canDoActionSuccess() {
        mockVmStatusUp();
        cretaeVirtIODisk();
        assertTrue(command.canDoAction());
        assertTrue(command.getReturnValue().getCanDoActionMessages().isEmpty());
    }

    @Before
    public void initializeCommand() {
        command = spy(createCommand());
        mockVds();
        when(command.getActionType()).thenReturn(getCommandActionType());
        doReturn(mock(VmNetworkInterfaceDAO.class)).when(command).getVmNetworkInterfaceDAO();
    }

    protected HotPlugDiskToVmCommand<HotPlugDiskToVmParameters> createCommand() {
        return new HotPlugDiskToVmCommand<HotPlugDiskToVmParameters>(createParameters());
    }

    protected VdcActionType getCommandActionType() {
        return VdcActionType.HotPlugDiskToVm;
    }

    private void mockNullVm() {
        AuditLogableBaseMockUtils.mockVmDao(command, vmDAO);
        when(vmDAO.get(command.getParameters().getVmId())).thenReturn(null);
        cretaeVirtIODisk();
    }

    /**
     * Mock a VM in status Up
     */
    protected VM mockVmStatusUp() {
        VM vm = new VM();
        vm.setstatus(VMStatus.Up);
        vm.setvm_os(VmOsType.RHEL6);
        vm.setId(vmId);
        vm.setrun_on_vds(Guid.NewGuid());
        AuditLogableBaseMockUtils.mockVmDao(command, vmDAO);
        mockVMDAO(vm);
        return vm;
    }

    private void mockVMDAO(VM vm) {
        when(vmDAO.get(command.getParameters().getVmId())).thenReturn(vm);
        List<VM> vmList = new ArrayList<VM>();
        VM vm1 = new VM();
        vm1.setId(command.getParameters().getVmId());
        VM vm2 = new VM();
        vm2.setId(Guid.NewGuid());
        vmList.add(vm1);
        vmList.add(vm2);
        when(vmDAO.getVmsListForDisk(any(Guid.class))).thenReturn(vmList);
    }

    /**
     * Mock VDS
     */
    protected void mockVds() {
        VDS vds = new VDS();
        vds.setvds_group_compatibility_version(new Version("3.1"));
        doReturn(vdsDao).when(command).getVdsDAO();
        when(vdsDao.get(Mockito.any(Guid.class))).thenReturn(vds);
    }

    /**
     * @return Valid parameters for the command.
     */
    protected HotPlugDiskToVmParameters createParameters() {
        return new HotPlugDiskToVmParameters(vmId, diskImageGuid);
    }

    /**
     * The following method will create a disk which is not VirtIO
     * @return
     */
    private DiskImage createNotVirtIODisk() {
        DiskImage disk = new DiskImage();
        disk.setImageId(diskImageGuid);
        disk.setDiskInterface(DiskInterface.IDE);
        disk.setactive(true);
        disk.setvm_guid(vmId);
        doReturn(diskDao).when(command).getDiskDao();
        when(diskDao.get(diskImageGuid)).thenReturn(disk);
        return disk;
    }

    /**
     * The following method will create a VirtIO disk , which is marked as unplugged
     * @return
     */
    protected void cretaeVirtIODisk() {
        DiskImage disk = new DiskImage();
        disk.setImageId(diskImageGuid);
        disk.setDiskInterface(DiskInterface.VirtIO);
        disk.setactive(true);
        disk.setvm_guid(vmId);
        doReturn(diskDao).when(command).getDiskDao();
        when(diskDao.get(diskImageGuid)).thenReturn(disk);
        mockVmDevice(false);
    }

    /**
      * The following method will create a VirtIO disk with provided plug option
      * @param plugged - the value which will be set to plug field
      * @return
      */
    protected void cretaeDiskWrongPlug(boolean plugged) {
        cretaeVirtIODisk();
        mockVmDevice(plugged);
    }

    protected void mockVmDevice(boolean plugged) {
        VmDevice vmDevice = new VmDevice();
        vmDevice.setId(new VmDeviceId());
        vmDevice.setIsPlugged(plugged);
        doReturn(vmDeviceDAO).when(command).getVmDeviceDao();
        when(vmDeviceDAO.get(Mockito.any(VmDeviceId.class))).thenReturn(vmDevice);
    }

}
