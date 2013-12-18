package org.ovirt.engine.core.bll;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.utils.MockConfigRule.mockConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dao.DiskDao;
import org.ovirt.engine.core.dao.VdsDAO;
import org.ovirt.engine.core.dao.VmDAO;
import org.ovirt.engine.core.utils.MockConfigRule;
import org.ovirt.engine.core.utils.customprop.ValidationError;

/** A test case for the {@link UpdateVmCommand}. */
@RunWith(MockitoJUnitRunner.class)
public class UpdateVmCommandTest {

    private VM vm;
    private VmStatic vmStatic;
    private UpdateVmCommand<VmManagementParametersBase> command;
    private VDSGroup group;

    @Mock
    private VmDAO vmDAO;
    @Mock
    private VdsDAO vdsDAO;
    @Mock
    private DiskDao diskDAO;

    @Mock
    OsRepository osRepository;

    @ClassRule
    public static MockConfigRule mcr = new MockConfigRule(
            mockConfig(ConfigValues.MaxVmNameLengthWindows, 15),
            mockConfig(ConfigValues.MaxVmNameLengthNonWindows, 64),
            mockConfig(ConfigValues.SupportedClusterLevels,
                    new HashSet<Version>(Arrays.asList(Version.v2_2, Version.v3_0, Version.v3_1))),
            mockConfig(ConfigValues.VMMinMemorySizeInMB, 256),
            mockConfig(ConfigValues.VM32BitMaxMemorySizeInMB, 20480),
            mockConfig(ConfigValues.PredefinedVMProperties, "3.1", ""),
            mockConfig(ConfigValues.UserDefinedVMProperties, "3.1", ""),
            mockConfig(ConfigValues.PredefinedVMProperties, "3.0", ""),
            mockConfig(ConfigValues.UserDefinedVMProperties, "3.0", ""),
            mockConfig(ConfigValues.ValidNumOfMonitors, "1,2,4"),
            mockConfig(ConfigValues.VmPriorityMaxValue, 100),
            mockConfig(ConfigValues.MaxNumOfVmCpus, "3.0", 16),
            mockConfig(ConfigValues.MaxNumOfVmSockets, "3.0", 16),
            mockConfig(ConfigValues.MaxNumOfCpuPerSocket, "3.0", 16),
            mockConfig(ConfigValues.VirtIoScsiEnabled, Version.v3_3.toString(), true)
            );

    @Before
    public void setUp() {
        SimpleDependecyInjector.getInstance().bind(OsRepository.class, osRepository);
        when(osRepository.getMinimumRam(0, Version.v3_0)).thenReturn(0);
        when(osRepository.getMinimumRam(0, null)).thenReturn(0);
        when(osRepository.getMaximumRam(0, Version.v3_0)).thenReturn(256);
        when(osRepository.getMaximumRam(0, null)).thenReturn(256);
        when(osRepository.isWindows(0)).thenReturn(false);

        VmHandler.init();
        vm = new VM();
        vmStatic = new VmStatic();
        group = new VDSGroup();
        group.setId(Guid.newGuid());
        group.setcompatibility_version(Version.v3_0);

        vm.setVdsGroupId(group.getId());
        vmStatic.setVdsGroupId(group.getId());

        VmManagementParametersBase params = new VmManagementParametersBase();
        params.setCommandType(VdcActionType.UpdateVm);
        params.setVmStaticData(vmStatic);

        command = spy(new UpdateVmCommand<VmManagementParametersBase>(params) {
            @Override
            public VDSGroup getVdsGroup() {
                return group;
            }
        });
        doReturn(vm).when(command).getVm();
    }

    @Test
    public void testLongName() {
        vmStatic.setName("this_should_be_very_long_vm_name_so_it will_fail_can_do_action_validation");
        assertFalse("canDoAction should fail for too long vm name.", command.canDoAction());
        assertCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_NAME_LENGTH_IS_TOO_LONG);
    }

    @Test
    public void testValidName() {
        prepareVmToPassCanDoAction();

        boolean c = command.canDoAction();
        assertTrue("canDoAction should have passed.", c);
    }

    @Test
    public void testChangeToExistingName() {
        prepareVmToPassCanDoAction();
        mockSameNameQuery(true);

        assertFalse("canDoAction should have failed with vm name already in use.", command.canDoAction());
        assertCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_NAME_ALREADY_USED);
    }

    @Test
    public void testNameNotChanged() {
        prepareVmToPassCanDoAction();
        vm.setName("vm1");
        mockSameNameQuery(true);

        assertTrue("canDoAction should have passed.", command.canDoAction());
    }

    @Test
    public void testDedicatedHostNotExist() {
        prepareVmToPassCanDoAction();

        // this will cause null to return when getting vds from vdsDAO
        doReturn(vdsDAO).when(command).getVdsDAO();

        vmStatic.setDedicatedVmForVds(Guid.newGuid());

        assertFalse("canDoAction should have failed with invalid dedicated host.", command.canDoAction());
        assertCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_DEDICATED_VDS_NOT_IN_SAME_CLUSTER);
    }

    @Test
    public void testDedicatedHostNotInSameCluster() {
        prepareVmToPassCanDoAction();

        VDS vds = new VDS();
        vds.setVdsGroupId(Guid.newGuid());
        doReturn(vdsDAO).when(command).getVdsDAO();
        when(vdsDAO.get(any(Guid.class))).thenReturn(vds);
        vmStatic.setDedicatedVmForVds(Guid.newGuid());

        assertFalse("canDoAction should have failed with invalid dedicated host.", command.canDoAction());
        assertCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_DEDICATED_VDS_NOT_IN_SAME_CLUSTER);
    }

    @Test
    public void testValidDedicatedHost() {
        prepareVmToPassCanDoAction();

        VDS vds = new VDS();
        vds.setVdsGroupId(group.getId());
        doReturn(vdsDAO).when(command).getVdsDAO();
        when(vdsDAO.get(any(Guid.class))).thenReturn(vds);
        vmStatic.setDedicatedVmForVds(Guid.newGuid());

        assertTrue("canDoAction should have passed.", command.canDoAction());
    }

    @Test
    public void testInvalidNumberOfMonitors() {
        prepareVmToPassCanDoAction();
        vmStatic.setNumOfMonitors(99);

        assertFalse("canDoAction should have failed with invalid number of monitors.", command.canDoAction());
        assertCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_ILLEGAL_NUM_OF_MONITORS);
    }

    @Test
    public void testUpdateFieldsQuotaEnforcementType() {
        vm.setQuotaEnforcementType(QuotaEnforcementTypeEnum.DISABLED);
        vmStatic.setQuotaEnforcementType(QuotaEnforcementTypeEnum.SOFT_ENFORCEMENT);

        assertTrue("Quota enforcement type should be updatable", command.areUpdatedFieldsLegal());
    }

    @Test
    public void testUpdateFieldsQutoaDefault() {
        vm.setIsQuotaDefault(true);
        vmStatic.setQuotaDefault(false);

        assertTrue("Quota default should be updatable", command.areUpdatedFieldsLegal());
    }

    @Test
    public void testChangeClusterForbidden() {
        prepareVmToPassCanDoAction();
        vmStatic.setVdsGroupId(Guid.newGuid());

        assertFalse("canDoAction should have failed with cant change cluster.", command.canDoAction());
        assertCanDoActionMessage(VdcBllMessages.VM_CANNOT_UPDATE_CLUSTER);
    }

    @Test
    public void testCannotDisableVirtioScsi() {
        prepareVmToPassCanDoAction();
        command.getParameters().setVirtioScsiEnabled(false);

        Disk disk = new DiskImage();
        disk.setDiskInterface(DiskInterface.VirtIO_SCSI);
        disk.setPlugged(true);

        mockDiskDaoGetAllForVm(Collections.singletonList(disk), true);

        CanDoActionTestUtils.runAndAssertCanDoActionFailure(command,
                VdcBllMessages.CANNOT_DISABLE_VIRTIO_SCSI_PLUGGED_DISKS);
    }

    private void prepareVmToPassCanDoAction() {
        vmStatic.setName("vm1");
        vmStatic.setMemSizeMb(256);
        vmStatic.setSingleQxlPci(false);
        mockVmDaoGetVm();
        mockSameNameQuery(false);
        mockValidateCustomProperties();
        mockValidatePciAndIdeLimit();
    }

    private void assertCanDoActionMessage(VdcBllMessages msg) {
        assertTrue("canDoAction failed for the wrong reason",
                command.getReturnValue()
                        .getCanDoActionMessages()
                        .contains(msg.name()));
    }

    private void mockDiskDaoGetAllForVm(List<Disk> disks, boolean onlyPluggedDisks) {
        doReturn(diskDAO).when(command).getDiskDao();
        doReturn(disks).when(diskDAO).getAllForVm(vm.getId(), onlyPluggedDisks);
    }

    private void mockVmDaoGetVm() {
        doReturn(vmDAO).when(command).getVmDAO();
        when(vmDAO.get(any(Guid.class))).thenReturn(vm);
    }

    private void mockValidateCustomProperties() {
        doReturn(Collections.<ValidationError> emptyList()).when(command).validateCustomProperties(any(VmStatic.class));
    }

    private void mockValidatePciAndIdeLimit() {
        doReturn(true).when(command).isValidPciAndIdeLimit(any(VM.class));
    }

    private void mockSameNameQuery(boolean result) {
        doReturn(result).when(command).isVmWithSameNameExists(anyString());
    }
}
