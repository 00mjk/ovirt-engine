package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.utils.MockConfigRule.mockConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.validator.RunVmValidator;
import org.ovirt.engine.core.common.action.RunVmParams;
import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.IVdsAsyncCommand;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmPayload;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsAndVmIDVDSParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.DiskDao;
import org.ovirt.engine.core.dao.StorageDomainDAO;
import org.ovirt.engine.core.dao.StoragePoolDAO;
import org.ovirt.engine.core.dao.VmDAO;
import org.ovirt.engine.core.utils.MockConfigRule;

@RunWith(MockitoJUnitRunner.class)
public class RunVmCommandTest {

    @ClassRule
    public static MockConfigRule mcr = new MockConfigRule(
            mockConfig(ConfigValues.GuestToolsSetupIsoPrefix, "General", "")
            );

    /**
     * The command under test.
     */
    private RunVmCommand<RunVmParams> command;

    @Mock
    private VDSBrokerFrontend vdsBrokerFrontend;

    @Mock
    private VmDAO vmDAO;

    @Mock
    private StoragePoolDAO spDao;

    @Mock
    private BackendInternal backend;

    @Mock
    private IsoDomainListSyncronizer isoDomainListSyncronizer;

    @Mock
    OsRepository osRepository;

    private static final String ACTIVE_ISO_PREFIX =
            "/rhev/data-center/mnt/some_computer/f6bccab4-e2f5-4e02-bba0-5748a7bc07b6/images/11111111-1111-1111-1111-111111111111";
    private static final String INACTIVE_ISO_PREFIX = "";

    public void mockBackend() {
        doReturn(backend).when(command).getBackend();

        VDSReturnValue vdsReturnValue = new VDSReturnValue();
        vdsReturnValue.setReturnValue(true);
        when(vdsBrokerFrontend.RunVdsCommand(any(VDSCommandType.class), any(VDSParametersBase.class))).thenReturn(vdsReturnValue);
        when(backend.getResourceManager()).thenReturn(vdsBrokerFrontend);

        // Set Valid Iso Prefix
        setIsoPrefixVDSMethod(ACTIVE_ISO_PREFIX);

        // Set create Vm.
        setCreateVmVDSMethod();
    }

    /**
     * Set create VM to return VM with status Up.
     */
    private void setCreateVmVDSMethod() {
        VDSReturnValue returnValue = new VDSReturnValue();
        returnValue.setReturnValue(VMStatus.Up);
        when(backend.getResourceManager().RunAsyncVdsCommand(eq(VDSCommandType.CreateVm),
                any(VdsAndVmIDVDSParametersBase.class),
                any(IVdsAsyncCommand.class))).thenReturn(returnValue);
    }

    private static DiskImage createImage() {
        final DiskImage diskImage = new DiskImage();
        diskImage.setId(Guid.newGuid());
        diskImage.setStorageIds(new ArrayList<Guid>(Arrays.asList(Guid.newGuid())));
        return diskImage;
    }

    /**
     * Set the Iso prefix.
     *
     * @param isoPrefix
     *            - Valid Iso patch or blank (when the Iso is not active.
     */
    private void setIsoPrefixVDSMethod(final String isoPrefix) {
        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return ImagesHandler.cdPathWindowsToLinux(invocation.getArguments()[0].toString(), isoPrefix);
            }

        }).when(command).cdPathWindowsToLinux(anyString());
    }

    @Test
    public void validateSimpleInitrdAndKernelName() throws Exception {
        String Initrd = "/boot/initrd.initrd";
        String Kernel = "/boot/kernel.image";
        VM vm = createVmForTesting(Initrd, Kernel);
        assertEquals(vm.getInitrdUrl(), Initrd);
        assertEquals(vm.getKernelUrl(), Kernel);
    }

    @Test
    public void validateIsoPrefix() throws Exception {
        String initrd = "initrd";
        String kernel = "kernel";
        VM vm = createVmForTesting(RunVmCommand.ISO_PREFIX + initrd, RunVmCommand.ISO_PREFIX + kernel);
        assertEquals(vm.getInitrdUrl(), ACTIVE_ISO_PREFIX + "/" + initrd);
        assertEquals(vm.getKernelUrl(), ACTIVE_ISO_PREFIX + "/" + kernel);
    }

    @Test
    public void validateIsoPrefixForKernelAndNoPrefixForInitrd() throws Exception {
        String initrd = "initrd";
        String kernel = "kernel";
        VM vm = createVmForTesting(initrd, RunVmCommand.ISO_PREFIX + kernel);
        assertEquals(vm.getInitrdUrl(), initrd);
        assertEquals(vm.getKernelUrl(), ACTIVE_ISO_PREFIX + "/" + kernel);
    }

    @Test
    public void validateIsoPrefixForInitrdAndNoPrefixForKernel() throws Exception {
        String initrd = "initrd";
        String kernel = "kernel";
        VM vm = createVmForTesting(RunVmCommand.ISO_PREFIX + initrd, kernel);
        assertEquals(vm.getInitrdUrl(), ACTIVE_ISO_PREFIX + "/" + initrd);
        assertEquals(vm.getKernelUrl(), kernel);
    }

    @Test
    public void validateIsoPrefixNameForKernelAndNullForInitrd() throws Exception {
        String kernel = "kernel";
        VM vm = createVmForTesting(null, RunVmCommand.ISO_PREFIX + kernel);
        assertEquals(vm.getInitrdUrl(), null);
        assertEquals(vm.getKernelUrl(), ACTIVE_ISO_PREFIX + "/" + kernel);
    }

    @Test
    public void validateIsoPrefixCaseSensitive() throws Exception {
        String initrd = "ISO://";
        VM vm = createVmForTesting(initrd, null);
        assertEquals(vm.getInitrdUrl(), "");
    }

    @Test
    public void validateIsoPrefixForOnlyIsoPrefixInKernelAndInitrd() throws Exception {
        String initrd = RunVmCommand.ISO_PREFIX;
        String kernelUrl = RunVmCommand.ISO_PREFIX;
        VM vm = createVmForTesting(initrd, kernelUrl);
        assertEquals(vm.getInitrdUrl(), "");
        assertEquals(vm.getKernelUrl(), "");
    }

    @Test
    public void checkIsoPrefixForNastyCharacters() throws Exception {
        String initrd = "@#$!";
        String kernelUrl = "    ";
        VM vm = createVmForTesting(initrd, kernelUrl);
        assertEquals(vm.getInitrdUrl(), initrd);
        assertEquals(vm.getKernelUrl(), kernelUrl);
    }

    @Test
    public void validateIsoPrefixNameForInitrdAndNullForKernel() throws Exception {
        String initrd = "initrd";
        VM vm = createVmForTesting(RunVmCommand.ISO_PREFIX + initrd, null);
        assertEquals(vm.getInitrdUrl(), ACTIVE_ISO_PREFIX + "/" + initrd);
        assertEquals(vm.getKernelUrl(), null);
    }

    @Test
    public void validateIsoPrefixWhenNoActiveIso() throws Exception {
        // Set Valid Iso Prefix
        setIsoPrefixVDSMethod(INACTIVE_ISO_PREFIX);

        String initrd = "initrd";
        VM vm = createVmForTesting(RunVmCommand.ISO_PREFIX + initrd, null);
        assertEquals(vm.getInitrdUrl(), INACTIVE_ISO_PREFIX + "/" + initrd);
    }

    @Test
    public void validateIsoPrefixWithTrippleSlash() throws Exception {
        String initrd = RunVmCommand.ISO_PREFIX + "/";
        VM vm = createVmForTesting(initrd, null);
        assertEquals(vm.getInitrdUrl(), ACTIVE_ISO_PREFIX + "/");
    }

    @Test
    public void validateIsoPrefixInTheMiddleOfTheInitrdAndKerenelName() throws Exception {
        String initrd = "initrd " + RunVmCommand.ISO_PREFIX;
        String kernelUrl = "kernelUrl " + RunVmCommand.ISO_PREFIX;
        VM vm = createVmForTesting(initrd, kernelUrl);
        assertEquals(vm.getInitrdUrl(), initrd);
        assertEquals(vm.getKernelUrl(), kernelUrl);
    }

    @Test
    public void validateInitrdWithSlashOnly() throws Exception {
        String initrd = "/";
        VM vm = createVmForTesting(initrd, null);
        assertEquals(vm.getInitrdUrl(), "/");
    }

    @Test
    public void validateIsoPrefixWithBackSlash() throws Exception {
        String initrd = "iso:\\";
        VM vm = createVmForTesting(initrd, null);
        assertEquals(vm.getInitrdUrl(), "iso:\\");
    }

    @Test
    public void validateBootPrefixForInitrdAndKernelImage() throws Exception {
        String initrd = "/boot";
        String kernelImage = "/boot";
        VM vm = createVmForTesting(initrd, kernelImage);
        assertEquals(vm.getInitrdUrl(), initrd);
        assertEquals(vm.getKernelUrl(), kernelImage);
    }

    @Test
    public void validateInitrdAndKernelImageWithOneCharacter() throws Exception {
        String initrd = "i";
        String kernelImage = "k";
        VM vm = createVmForTesting(initrd, kernelImage);
        assertEquals(vm.getInitrdUrl(), "i");
        assertEquals(vm.getKernelUrl(), "k");
    }

    private VM createVmForTesting(String initrd, String kernel) {
        mockVm(command);

        // Set parameter
        command.getVm().setInitrdUrl(initrd);
        command.getVm().setKernelUrl(kernel);
        command.createVm();

        // Check Vm
        VM vm = vmDAO.get(command.getParameters().getVmId());
        return vm;
    }

    /**
     * Mock a VM.
     */
    private VM mockVm(RunVmCommand<RunVmParams> spyVmCommand) {
        VM vm = new VM();
        vm.setStatus(VMStatus.Down);
        doReturn(vmDAO).when(command).getVmDAO();
        when(vmDAO.get(command.getParameters().getVmId())).thenReturn(vm);
        doReturn(new VDSGroup()).when(command).getVdsGroup();
        return vm;
    }

    @Before
    public void createCommand() {
        when(osRepository.isWindows(Mockito.anyInt())).thenReturn(false);
        SimpleDependecyInjector.getInstance().bind(OsRepository.class, osRepository);

        RunVmParams param = new RunVmParams(Guid.newGuid());
        command = spy(new RunVmCommand<RunVmParams>(param));
        mockIsoDomainListSyncronizer();
        mockSuccessfulRunVmValidator();
        doNothing().when(command).initParametersForExternalNetworks();
        mockSuccessfulSnapshotValidator();
        mockBackend();
    }

    private void mockIsoDomainListSyncronizer() {
        doNothing().when(isoDomainListSyncronizer).init();
        doReturn(isoDomainListSyncronizer).when(command).getIsoDomainListSyncronizer();
    }

    @Test
    public void testCanDoAction() {
        final ArrayList<Disk> disks = new ArrayList<Disk>();
        final DiskImage diskImage = createImage();
        disks.add(diskImage);
        initDAOMocks(disks);
        final VM vm = new VM();
        vm.setStatus(VMStatus.Down);
        doReturn(new StoragePool()).when(command).getStoragePool();
        doReturn(vm).when(command).getVm();
        doReturn(true).when(command).validateNetworkInterfaces();
        doReturn(true).when(command).checkPayload(any(VmPayload.class), anyString());
        doReturn(new VDSGroup()).when(command).getVdsGroup();
        assertTrue(command.canDoAction());
        assertTrue(command.getReturnValue().getCanDoActionMessages().isEmpty());
    }

    /**
     * @param disks
     * @param guid
     */
    protected void initDAOMocks(final List<Disk> disks) {
        final DiskDao diskDao = mock(DiskDao.class);
        when(diskDao.getAllForVm(Guid.Empty, true)).thenReturn(disks);
        doReturn(diskDao).when(command).getDiskDao();

        final StorageDomainDAO storageDomainDAO = mock(StorageDomainDAO.class);
        when(storageDomainDAO.getAllForStoragePool(Guid.Empty))
                .thenReturn(new ArrayList<StorageDomain>());
        doReturn(storageDomainDAO).when(command).getStorageDomainDAO();
    }

    private SnapshotsValidator mockSuccessfulSnapshotValidator() {
        SnapshotsValidator snapshotsValidator = mock(SnapshotsValidator.class);
        when(snapshotsValidator.vmNotDuringSnapshot(any(Guid.class))).thenReturn(ValidationResult.VALID);
        when(snapshotsValidator.vmNotInPreview(any(Guid.class))).thenReturn(ValidationResult.VALID);
        doReturn(snapshotsValidator).when(command).getSnapshotsValidator();
        return snapshotsValidator;
    }

    private RunVmValidator mockSuccessfulRunVmValidator() {
        RunVmValidator runVmValidator = mock(RunVmValidator.class);
        when(runVmValidator.canRunVm(any(VM.class),
                Matchers.anyListOf(String.class),
                Matchers.anyListOf(Disk.class),
                any(BootSequence.class),
                any(StoragePool.class),
                anyBoolean(),
                anyString(),
                anyString(),
                anyBoolean(),
                Matchers.anyListOf(Guid.class),
                Matchers.anyListOf(Guid.class),
                any(Guid.class),
                any(VDSGroup.class))).thenReturn(true);
        doReturn(runVmValidator).when(command).getRunVmValidator();
        return runVmValidator;
    }
}
