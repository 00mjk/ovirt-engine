package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.scheduling.SchedulingManager;
import org.ovirt.engine.core.bll.scheduling.VdsFreeMemoryChecker;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.validator.DiskImagesValidator;
import org.ovirt.engine.core.bll.validator.LocalizedVmStatus;
import org.ovirt.engine.core.bll.validator.VmValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.action.MigrateVmParameters;
import org.ovirt.engine.core.common.businessentities.MigrationMethod;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.network.InterfaceStatus;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.vdscommands.MigrateStatusVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.MigrateVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.NetworkUtils;

@LockIdNameAttribute(isReleaseAtEndOfExecute = false)
public class MigrateVmCommand<T extends MigrateVmParameters> extends RunVmCommandBase<T> {

    private Guid vdsDestinationId;
    protected boolean forcedMigrationForNonMigratableVM;

    /** Used to log the migration error. */
    private VdcBllErrors migrationErrorCode;

    public MigrateVmCommand(T parameters) {
        super(parameters);
        forcedMigrationForNonMigratableVM = parameters.isForceMigrationForNonMigratableVM();
    }

    /**
     * this property is used for audit log events
     */
    public String getVdsDestination() {
        return getDestinationVds() != null ? getDestinationVds().getName() : null;
    }

    /**
     * @return Migration error text which is used in audit log message, if the migration status was queried from VDSM.
     */
    public String getDueToMigrationError() {
        if (migrationErrorCode == null) {
            return " ";
        }

        return " due to Error: " + Backend.getInstance()
                .getVdsErrorsTranslator()
                .TranslateErrorTextSingle(migrationErrorCode.name(), true);
    }

    @Override
    protected VDS getDestinationVds() {
        if (_destinationVds == null && vdsDestinationId != null) {
            _destinationVds = DbFacade.getInstance().getVdsDao().get(vdsDestinationId);
        }
        return _destinationVds;
    }

    @Override
    protected void failedToRunVm() {
        // In case the migration failed and the VM turned back to Up in the
        // source, we don't need to handle it as a VM that failed to run
        if (getVm().getStatus() != VMStatus.Up) {
            super.failedToRunVm();
        }
    }

    protected void failedToMigrate() {
        try {
            determineMigrationFailureForAuditLog();
            _isRerun = false;
            setSucceeded(false);
            log();
            failedToRunVm();
        }
        finally {
            freeLock();
        }
    }

    protected void initVdss() {
        setVdsIdRef(getVm().getRunOnVds());
        VDS destVds = getDestinationVds();
        Guid vdsToRunOn =
                SchedulingManager.getInstance().schedule(getVdsGroup(),
                        getVm(),
                        getVdsBlackList(),
                        getVdsWhiteList(),
                        destVds == null ? null : destVds.getId(),
                        new ArrayList<String>(),
                        new VdsFreeMemoryChecker(this),
                        getCorrelationId());
        setVdsDestinationId(vdsToRunOn);
        if (vdsToRunOn != null && !Guid.Empty.equals(vdsToRunOn)) {
            getRunVdssList().add(vdsToRunOn);
        }
        VmHandler.updateVmGuestAgentVersion(getVm());
        // make _destinationVds null in order to refresh it from db in case it
        // changed.
        _destinationVds = null;
        if (vdsDestinationId != null && vdsDestinationId.equals(Guid.Empty)) {
            throw new VdcBLLException(VdcBllErrors.RESOURCE_MANAGER_CANT_ALLOC_VDS_MIGRATION);
        }

        if (getDestinationVds() == null || getVds() == null) {
            throw new VdcBLLException(VdcBllErrors.RESOURCE_MANAGER_VDS_NOT_FOUND);
        }
    }

    @Override
    protected void executeVmCommand() {
        initVdss();
        perform();
        setSucceeded(true);
    }

    private void perform() {
        getVm().setMigratingToVds(vdsDestinationId);

        getParameters().setStartTime(new Date());

        // Starting migration at src VDS
        boolean connectToLunDiskSuccess = connectLunDisks(vdsDestinationId);
        if (connectToLunDiskSuccess) {
            setActionReturnValue(Backend
                    .getInstance()
                    .getResourceManager()
                    .RunAsyncVdsCommand(
                            VDSCommandType.Migrate,
                            createMigrateVDSCommandParameters(),
                            this)
                    .getReturnValue());
        }
        if (!connectToLunDiskSuccess || getActionReturnValue() != VMStatus.MigratingFrom) {
            getVm().setMigreatingToPort(0);
            getVm().setMigreatingFromPort(0);
            getVm().setMigratingToVds(null);
            throw new VdcBLLException(VdcBllErrors.RESOURCE_MANAGER_MIGRATION_FAILED_AT_DST);
        }
        ExecutionHandler.setAsyncJob(getExecutionContext(), true);
    }

    private MigrateVDSCommandParameters createMigrateVDSCommandParameters() {
        String srcVdsHost = getVds().getHostName();
        String dstVdsHost = String.format("%1$s:%2$s",
                getDestinationVds().getHostName(),
                getDestinationVds().getPort());

        return new MigrateVDSCommandParameters(getVdsId(), getVmId(), srcVdsHost, vdsDestinationId,
                dstVdsHost, MigrationMethod.ONLINE, isTunnelMigrationUsed(), getMigrationNetworkIp(), getVds().getVdsGroupCompatibilityVersion());
    }

    private boolean isTunnelMigrationUsed() {
        if (!FeatureSupported.tunnelMigration(getVm().getVdsGroupCompatibilityVersion())) {
            return false;
        }
        // if vm has no override for tunnel migration (its null),
        // use cluster's setting
        return getVm().getTunnelMigration() != null ?
                getVm().getTunnelMigration()
                : getVdsGroup().isTunnelMigration();
    }

    private String getMigrationNetworkIp() {

        if (!FeatureSupported.migrationNetwork(getVm().getVdsGroupCompatibilityVersion())) {
            return null;
        }

        Network migrationNetwork = null;

        // Find migrationNetworkCluster
        List<Network> allNetworksInCluster =
                DbFacade.getInstance().getNetworkDao().getAllForCluster(getVm().getVdsGroupId());

        for (Network tempNetwork : allNetworksInCluster) {
            if (tempNetwork.getCluster().isMigration()) {
                migrationNetwork = tempNetwork;
                break;
            }
        }

        if (migrationNetwork != null) {

            // assure migration network is active on source host
            if (getMigrationNetworkAddress(getVds().getId(), migrationNetwork.getName()) == null) {
                return null;
            }

            // find migration IP address on destination host
            return getMigrationNetworkAddress(getDestinationVds().getId(), migrationNetwork.getName());
        }

        return null;
    }

    private String getMigrationNetworkAddress(Guid hostId, String migrationNetworkName) {
        final List<VdsNetworkInterface> nics =
                getDbFacade().getInterfaceDao().getAllInterfacesForVds(hostId);

        for (VdsNetworkInterface nic : nics) {
            if (migrationNetworkName.equals(nic.getNetworkName()) && migrationInterfaceUp(nic, nics)) {
                return nic.getAddress();
            }
        }

        return null;
    }

    protected boolean migrationInterfaceUp(VdsNetworkInterface nic, List<VdsNetworkInterface> nics) {
        if (NetworkUtils.isVlan(nic)) {
            String physicalNic = NetworkUtils.stripVlan(nic.getName());
            for (VdsNetworkInterface iface : nics) {
                if (iface.getName().equals(physicalNic)) {
                    return iface.getStatistics().getStatus() == InterfaceStatus.UP;
                }
            }
        }

        return nic.getStatistics().getStatus() == InterfaceStatus.UP;
    }

    /**
     * command succeeded and VM is up => migration done
     * command succeeded and VM is not up => migration started
     * command failed and rerun flag is set => rerun migration was initiated
     * command failed and rerun flag is not set => migration failed
     */
    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ?
                getActionReturnValue() == VMStatus.Up ?
                        AuditLogType.VM_MIGRATION_DONE
                        : getAuditLogForMigrationStarted()
                : _isRerun ?
                        AuditLogType.VM_MIGRATION_TRYING_RERUN
                        : getAuditLogForMigrationFailure();
    }

    private AuditLogType getAuditLogForMigrationStarted() {
        return isInternalExecution() ?
                AuditLogType.VM_MIGRATION_START_SYSTEM_INITIATED
                : AuditLogType.VM_MIGRATION_START;
    }

    private AuditLogType getAuditLogForMigrationFailure() {
        if (getVds().getStatus() == VDSStatus.PreparingForMaintenance) {
            return AuditLogType.VM_MIGRATION_FAILED_DURING_MOVE_TO_MAINTENANCE;
        }

        if (getDestinationVds() == null) {
            return AuditLogType.VM_MIGRATION_FAILED_NO_VDS_TO_RUN_ON;
        }

        return AuditLogType.VM_MIGRATION_FAILED;
    }

    protected Guid getVdsDestinationId() {
        return vdsDestinationId;
    }

    protected void setVdsDestinationId(Guid value) {
        vdsDestinationId = value;
    }

    @Override
    protected boolean canDoAction() {
        final VM vm = getVm();

        if (vm == null) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        }

        if (!canRunActionOnNonManagedVm()) {
            return false;
        }

        // If VM is pinned to host, no migration can occur
        if (vm.getMigrationSupport() == MigrationSupport.PINNED_TO_HOST) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_VM_IS_PINNED_TO_HOST);
        }

        if (vm.getMigrationSupport() == MigrationSupport.IMPLICITLY_NON_MIGRATABLE
                && !forcedMigrationForNonMigratableVM) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_VM_IS_NON_MIGRTABLE_AND_IS_NOT_FORCED_BY_USER_TO_MIGRATE);
        }

        if (vm.getStatus() == VMStatus.MigratingFrom) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_MIGRATION_IN_PROGRESS);
        }

        if (vm.getStatus() == VMStatus.NotResponding) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_VM_STATUS_ILLEGAL, LocalizedVmStatus.from(vm.getStatus()));
        }

        if (vm.getStatus() == VMStatus.Paused) {
            return failCanDoAction(VdcBllMessages.MIGRATE_PAUSED_VM_IS_UNSUPPORTED);
        }

        if (!vm.isQualifyToMigrate()) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_VM_IS_NOT_RUNNING);
        }

        VmValidator vmValidator = new VmValidator(vm);

        if (!validate(vmValidator.vmNotHavingPluggedDiskSnapshots(VdcBllMessages.ACTION_TYPE_FAILED_VM_HAS_PLUGGED_DISK_SNAPSHOT))) {
            return false;
        }

        if (getDestinationVds() != null && getDestinationVds().getStatus() != VDSStatus.Up) {
            addCanDoActionMessage(VdcBllMessages.VAR__HOST_STATUS__UP);
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_VDS_STATUS_ILLEGAL);
        }

        return validate(new SnapshotsValidator().vmNotDuringSnapshot(vm.getId()))
                // This check was added to prevent migration of VM while its disks are being migrated
                // TODO: replace it with a better solution
                && validate(new DiskImagesValidator(ImagesHandler.getPluggedActiveImagesForVm(vm.getId())).diskImagesNotLocked())
                && SchedulingManager.getInstance().canSchedule(getVdsGroup(),
                        getVm(),
                        getVdsBlackList(),
                        getParameters().getInitialHosts(),
                        getVdsDestinationId(),
                        getReturnValue().getCanDoActionMessages());
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__MIGRATE);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM);
    }

    @Override
    public void rerun() {
         // make Vm property to null in order to refresh it from db
        setVm(null);

        // if vm is up and rerun is called then it got up on the source, try to rerun
        if (getVm() != null && getVm().getStatus() == VMStatus.Up) {
            determineMigrationFailureForAuditLog();
            setVdsDestinationId(null);
            super.rerun();
        } else {
            // vm went down on the destination and source, migration failed.
            failedToMigrate();
            // signal the caller that a rerun was made so that it won't log
            // the failure message again
            _isRerun = true;
        }
    }

    @Override
    public void runningSucceded() {
        try {
            super.runningSucceded();
        }
        finally {
            freeLock();
        }
    }

    @Override
    public void reportCompleted() {
        try {
            super.reportCompleted();
        }
        finally {
            /*
             * Decrement the pending counters here as this is the only place which
             * is called consistently regardless of the migration result.
             */
            decreasePendingVms(getVdsDestinationId());

            freeLock();
        }
    }

    /**
     * Log that the migration had failed with the error code that is in the VDS and needs to be retrieved.
     */
    protected void determineMigrationFailureForAuditLog() {
        if (getVm() != null && getVm().getStatus() == VMStatus.Up) {
            try {
                Backend.getInstance().getResourceManager().RunVdsCommand(VDSCommandType.MigrateStatus,
                        new MigrateStatusVDSCommandParameters(getVdsId(), getVmId()));
            } catch (VdcBLLException e) {
                migrationErrorCode = e.getErrorCode();
            }
        }
    }

    @Override
    protected Guid getCurrentVdsId() {
        return getVdsDestinationId() != null ? getVdsDestinationId() : super.getCurrentVdsId();
    }

    public String getDuration() {
        // return time in seconds
        return String.valueOf((new Date().getTime() - getParameters().getStartTime().getTime()) / 1000 % 60);
    }

    @Override
    protected String getLockMessage() {
        StringBuilder builder = new StringBuilder(VdcBllMessages.ACTION_TYPE_FAILED_VM_IS_BEING_MIGRATED.name());
        builder.append(String.format("$VmName %1$s", getVmName()));
        return builder.toString();
    }

    // hosts that cannot be selected for scheduling (failed hosts + VM source host)
    private List<Guid> getVdsBlackList() {
        List<Guid> blackList = new ArrayList<Guid>(getRunVdssList());
        if (getVdsId() != null) {
            blackList.add(getVdsId());
        }
        return blackList;
    }

    // initial hosts list picked for scheduling, currently
    // passed by load balancing process.
    protected List<Guid> getVdsWhiteList() {
        return getParameters().getInitialHosts();
    }
}
