package org.ovirt.engine.core.bll.storage;

import java.text.MessageFormat;
import java.util.List;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.VmCommand;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.ReconstructMasterParameters;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.storage_pool_iso_map;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.vdscommands.ConnectStoragePoolVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.DisconnectStoragePoolVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.MarkPoolInReconstructModeVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.ReconstructMarkAction;
import org.ovirt.engine.core.common.vdscommands.ReconstructMasterVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.RefreshStoragePoolVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.ResetIrsVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;

@SuppressWarnings("serial")
@NonTransactiveCommandAttribute(forceCompensation = true)
public class ReconstructMasterDomainCommand<T extends ReconstructMasterParameters> extends
        DeactivateStorageDomainCommand<T> {

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected ReconstructMasterDomainCommand(Guid commandId) {
        super(commandId);
    }

    public ReconstructMasterDomainCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected boolean canDoAction() {
        List<storage_pool_iso_map> poolDomains = DbFacade.getInstance()
                .getStoragePoolIsoMapDao().getAllForStoragePool(getStoragePool().getId());
        for (storage_pool_iso_map poolDomain : poolDomains) {
            if (poolDomain.getstatus() == StorageDomainStatus.Locked) {
                addInvalidSDStatusMessage(poolDomain.getstatus());
                return false;
            }
        }

        return InitializeVds();
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__RECONSTRUCT_MASTER);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__STORAGE__DOMAIN);
    }

    protected void addInvalidSDStatusMessage(StorageDomainStatus status) {
        addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_DOMAIN_STATUS_ILLEGAL2);
        addCanDoActionMessage(String.format("$status %1$s", status));
    }


    protected boolean reconstructMaster() {
        ProceedStorageDomainTreatmentByDomainType(true);

        // To issue a reconstructMaster you need to set the domain inactive
        if (getParameters().isInactive()) {
            executeInNewTransaction(new TransactionMethod<Void>() {
                public Void runInTransaction() {
                    SetStorageDomainStatus(StorageDomainStatus.InActive, getCompensationContext());
                    calcStoragePoolStatusByDomainsStatus();
                    getCompensationContext().stateChanged();
                    return null;
                }
            });
        }

        if (_isLastMaster) {
            return stopSpm();
        }

        // Pause the timers for the domain error handling
        runVdsCommand(VDSCommandType.MarkPoolInReconstructMode,
                new MarkPoolInReconstructModeVDSCommandParameters(
                        getStoragePoolId().getValue(), ReconstructMarkAction.ClearJobs));
        boolean commandSucceeded = stopSpm();

        final List<String> disconnectPoolFormats = Config.<List<String>> GetValue(
                ConfigValues.DisconnectPoolOnReconstruct);

        if (commandSucceeded && disconnectPoolFormats.contains(getNewMaster(true).getStorageFormat().getValue())) {
            commandSucceeded = runVdsCommand(
                    VDSCommandType.DisconnectStoragePool,
                    new DisconnectStoragePoolVDSCommandParameters(getVds().getId(),
                            getStoragePool().getId(), getVds().getvds_spm_id())
                    ).getSucceeded();
        }

        if (!commandSucceeded) {
            return false;
        }

        List<storage_pool_iso_map> domains = getStoragePoolIsoMapDAO()
                .getAllForStoragePool(getStoragePool().getId());

        return runVdsCommand(VDSCommandType.ReconstructMaster,
                new ReconstructMasterVDSCommandParameters(getVds().getId(),
                        getVds().getvds_spm_id(), getStoragePool().getId(),
                        getStoragePool().getname(), _newMasterStorageDomainId, domains,
                        getStoragePool().getmaster_domain_version())).getSucceeded();

    }

    @Override
    protected void executeCommand() {
        try {
            boolean commandSucceeded = reconstructMaster();
            connectAndRefreshAllUpHosts(commandSucceeded);

            if (!_isLastMaster && commandSucceeded) {
                SearchParameters p = new SearchParameters(
                    MessageFormat.format(DesktopsInStoragePoolQuery,
                        getStoragePool().getname()), SearchType.VM);

                p.setMaxCount(Integer.MAX_VALUE);

                @SuppressWarnings("unchecked")
                List<VM> vmsInPool = (List<VM>) Backend.getInstance()
                     .runInternalQuery(VdcQueryType.Search, p).getReturnValue();

                VmCommand.updateVmInSpm(getStoragePool().getId(), vmsInPool);
            }

            setSucceeded(commandSucceeded);
        } finally {
            // reset cache and mark reconstruct for pool as finished
            Backend.getInstance()
                    .getResourceManager()
                    .RunVdsCommand(
                            VDSCommandType.MarkPoolInReconstructMode,
                            new MarkPoolInReconstructModeVDSCommandParameters(getStoragePoolId()
                                    .getValue(), ReconstructMarkAction.ClearCache));
        }
    }

    protected boolean stopSpm() {
        boolean commandSucceeded = true;
        if (getStoragePool().getspm_vds_id() != null) {
            // if spm host id is different from selected host get the spm
            // in order to try and perform stop spm
            VDS spm = null;
            if (getStoragePool().getspm_vds_id().equals(getVds().getId())) {
                spm = getVds();
            } else {
                spm = DbFacade.getInstance()
                        .getVdsDao()
                        .get(getStoragePool().getspm_vds_id());
            }
            if (spm != null) {
                ResetIrsVDSCommandParameters tempVar2 = new ResetIrsVDSCommandParameters(
                        getStoragePool().getId(), spm.gethost_name(), spm.getId());
                tempVar2.setIgnoreStopFailed(true);
                commandSucceeded = Backend.getInstance().getResourceManager()
                            .RunVdsCommand(VDSCommandType.ResetIrs, tempVar2).getSucceeded();

                // if spm host is up switch to use it in the following logic
                if (spm.getstatus() == VDSStatus.Up) {
                    setVdsId(spm.getId());
                    setVds(spm);
                }
            }
        }
        return commandSucceeded;
    }

    private void connectAndRefreshAllUpHosts(final boolean commandSucceeded) {
        for (VDS vds : getAllRunningVdssInPool()) {
            try {
                if (!_isLastMaster && commandSucceeded) {
                    VDSReturnValue returnValue = Backend.getInstance()
                            .getResourceManager()
                            .RunVdsCommand(
                                    VDSCommandType.ConnectStoragePool,
                                    new ConnectStoragePoolVDSCommandParameters(vds.getId(),
                                            getStoragePool().getId(), vds.getvds_spm_id(),
                                            _newMasterStorageDomainId, getStoragePool()
                                                    .getmaster_domain_version()));
                    if (returnValue.getSucceeded()) {
                        Backend.getInstance()
                                .getResourceManager()
                                .RunVdsCommand(
                                        VDSCommandType.RefreshStoragePool,
                                        new RefreshStoragePoolVDSCommandParameters(vds.getId(),
                                                getStoragePool().getId(),
                                                _newMasterStorageDomainId,
                                                getStoragePool().getmaster_domain_version()));
                    } else {
                        log.errorFormat("Post reconstruct actions (connectPool) did not complete on host {0} in the pool. error {1}",
                                vds.getId(),
                                returnValue.getVdsError().getMessage());
                    }
                }
                // only if we deactivate the storage domain we want to disconnect from it.
                if (!getParameters().isInactive()) {
                    StorageHelperDirector.getInstance()
                            .getItem(getStorageDomain().getstorage_type())
                            .DisconnectStorageFromDomainByVdsId(getStorageDomain(), vds.getId());
                }

            } catch (Exception e) {
                log.errorFormat("Post reconstruct actions (connectPool,refreshPool,disconnect storage)"
                        + " did not complete on host {0} in the pool. error {1}",
                        vds.getId(),
                        e.getMessage());
            }
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? _isLastMaster ? AuditLogType.RECONSTRUCT_MASTER_FAILED_NO_MASTER
                : AuditLogType.RECONSTRUCT_MASTER_DONE : AuditLogType.RECONSTRUCT_MASTER_FAILED;
    }
}
