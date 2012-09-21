package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.storage.StorageHandlingCommandBase;
import org.ovirt.engine.core.bll.storage.StoragePoolStatusHandler;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.SetNonOperationalVdsParameters;
import org.ovirt.engine.core.common.action.StoragePoolParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.FenceActionType;
import org.ovirt.engine.core.common.businessentities.FenceStatusReturnValue;
import org.ovirt.engine.core.common.businessentities.NonOperationalReason;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.VdsSpmStatus;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServerInfo;
import org.ovirt.engine.core.common.vdscommands.ConnectStoragePoolVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterHostAddVDSParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AlertDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.InterfaceDAO;

/**
 * Initialize Vds on its loading. For storages: First connect all storage
 * servers to VDS. Second connect Vds to storage Pool.
 *
 * After server initialized - its will be moved to Up status.
 */
@NonTransactiveCommandAttribute
public class InitVdsOnUpCommand<T extends StoragePoolParametersBase> extends StorageHandlingCommandBase<T> {
    private boolean _fencingSucceeded = true;
    private boolean _vdsProxyFound;
    private boolean _connectStorageSucceeded, _connectPoolSucceeded;
    private boolean _glusterPeerListSucceeded, _glusterPeerProbeSucceeded;
    private FenceStatusReturnValue _fenceStatusReturnValue;

    public InitVdsOnUpCommand(T parameters) {
        super(parameters);
        setVdsId(parameters.getVdsId());
    }

    @Override
    protected void executeCommand() {
        VDSGroup vdsGroup = getVdsGroup();

        if (vdsGroup.supportsVirtService()) {
            initVirtResources();
        }

        if (vdsGroup.supportsGlusterService()) {
            initGlusterPeerProcess();
        }
        setSucceeded(true);
    }

    private void initVirtResources() {
        if (InitializeStorage()) {
            processFencing();
            processStoragePoolStatus();
        } else {
            setNonOperational(NonOperationalReason.STORAGE_DOMAIN_UNREACHABLE);
        }
    }

    private void processFencing() {
        FencingExecutor executor = new FencingExecutor(getVds(), FenceActionType.Status);
        // check first if we have any VDS to act as the proxy for fencing
        // actions.
        if (getVds().getpm_enabled() && executor.FindVdsToFence()) {
            VDSReturnValue returnValue = executor.Fence();
            _fencingSucceeded = returnValue.getSucceeded();
            _fenceStatusReturnValue = (FenceStatusReturnValue) returnValue.getReturnValue();
            _vdsProxyFound = true;
        }
    }

    private void processStoragePoolStatus() {
        if (getVds().getspm_status() != VdsSpmStatus.None) {
            storage_pool pool = DbFacade.getInstance().getStoragePoolDao().get(getVds().getstorage_pool_id());
            if (pool != null && pool.getstatus() == StoragePoolStatus.NotOperational) {
                pool.setstatus(StoragePoolStatus.Problematic);
                DbFacade.getInstance().getStoragePoolDao().updateStatus(pool.getId(), pool.getstatus());
                StoragePoolStatusHandler.PoolStatusChanged(pool.getId(), pool.getstatus());
            }
        }
    }

    private void setNonOperational(NonOperationalReason reason) {
        SetNonOperationalVdsParameters tempVar = new SetNonOperationalVdsParameters(getVds().getId(), reason);
        tempVar.setSaveToDb(true);
        Backend.getInstance().runInternalAction(VdcActionType.SetNonOperationalVds, tempVar,  ExecutionHandler.createInternalJobContext());
    }

    private boolean InitializeStorage() {
        boolean returnValue = false;
        setStoragePoolId(getVds().getstorage_pool_id());

        // if no pool or pool is uninitialized or in maintenance mode no need to
        // connect any storage
        if (getStoragePool() == null || StoragePoolStatus.Uninitialized == getStoragePool().getstatus()
                || StoragePoolStatus.Maintanance == getStoragePool().getstatus()) {
            returnValue = true;
            _connectStorageSucceeded = true;
            _connectPoolSucceeded = true;
        } else {
            boolean suppressCheck = getAllRunningVdssInPool().size() == 0;
            StoragePoolParametersBase tempStorageBaseParams =
                    new StoragePoolParametersBase(getVds().getstorage_pool_id());
            tempStorageBaseParams.setVdsId(getVds().getId());
            tempStorageBaseParams.setSuppressCheck(suppressCheck);
            tempStorageBaseParams.setTransactionScopeOption(TransactionScopeOption.Suppress);
            if (Backend.getInstance()
                    .runInternalAction(VdcActionType.ConnectHostToStoragePoolServers, tempStorageBaseParams)
                    .getSucceeded()
                    || suppressCheck) {
                _connectStorageSucceeded = true;
                try {
                    setStoragePool(null);
                    returnValue = _connectPoolSucceeded = Backend
                            .getInstance()
                            .getResourceManager()
                            .RunVdsCommand(
                                    VDSCommandType.ConnectStoragePool,
                                    new ConnectStoragePoolVDSCommandParameters(getVds().getId(), getVds()
                                            .getstorage_pool_id(), getVds().getvds_spm_id(), getMasterDomainIdFromDb(),
                                            getStoragePool().getmaster_domain_version())).getSucceeded();
                } catch (RuntimeException exp) {
                    log.errorFormat("Could not connect host {0} to pool {1}", getVds().getvds_name(), getStoragePool()
                            .getname());
                    returnValue = false;
                }
                // if couldn't connect check if this is the only vds
                // return true if connect succeeded or it's the only vds
                if (!returnValue && suppressCheck) {
                    AuditLogDirector.log(new AuditLogableBase(getVdsId()),
                            AuditLogType.VDS_STORAGE_CONNECTION_FAILED_BUT_LAST_VDS);
                    returnValue = true;
                }
            }
        }
        return returnValue;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        AuditLogType type = AuditLogType.UNASSIGNED;

        if(!getVdsGroup().supportsVirtService()) {
            if (getVdsGroup().supportsGlusterService()) {
                if (!_glusterPeerListSucceeded) {
                    type = AuditLogType.GLUSTER_SERVERS_LIST_FAILED;
                } else if (!_glusterPeerProbeSucceeded) {
                    type = AuditLogType.GLUSTER_HOST_ADD_FAILED;
                }
            }
            return type;
        }

        if (!_connectStorageSucceeded) {
            type = AuditLogType.CONNECT_STORAGE_SERVERS_FAILED;
        } else if (!_connectPoolSucceeded) {
            type = AuditLogType.CONNECT_STORAGE_POOL_FAILED;
        } else if (getVds().getpm_enabled() && _fencingSucceeded) {
            type = AuditLogType.VDS_FENCE_STATUS;
        } else if (getVds().getpm_enabled() && !_fencingSucceeded) {
            type = AuditLogType.VDS_FENCE_STATUS_FAILED;
        }

        // PM alerts
        AuditLogableBase logable = new AuditLogableBase(getVds().getId());
        if (getVds().getpm_enabled()) {
            if (!_vdsProxyFound) {
                logable.AddCustomValue("Reason",
                        AuditLogDirector.GetMessage(AuditLogType.VDS_ALERT_FENCING_NO_PROXY_HOST));
                AlertDirector.Alert(logable, AuditLogType.VDS_ALERT_FENCING_TEST_FAILED);
            } else if (!_fenceStatusReturnValue.getIsSucceeded()) {
                logable.AddCustomValue("Reason", _fenceStatusReturnValue.getMessage());
                AlertDirector.Alert(logable, AuditLogType.VDS_ALERT_FENCING_TEST_FAILED);
            }
        } else {
            AlertDirector.Alert(logable, AuditLogType.VDS_ALERT_FENCING_IS_NOT_CONFIGURED);
        }
        return type;
    }

    private void initGlusterPeerProcess() {
        _glusterPeerListSucceeded = true;
        _glusterPeerProbeSucceeded = true;
        List<VDS> vdsList = getVdsDAO().getAllForVdsGroupWithStatus(getVdsGroupId(), VDSStatus.Up);
        // If the cluster already having Gluster servers, get an up server
        if (vdsList != null && vdsList.size() > 0) {
            VDS upServer = null;
            for (VDS vds : vdsList) {
                if (!getVdsId().equals(vds.getId())) {
                    upServer = vds;
                    break;
                }
            }

            // If new server is not part of the existing gluster peers, add into peer group
            if (upServer != null) {
                List<GlusterServerInfo> glusterServers = getGlusterPeers(upServer.getId());
                if (glusterServers.size() == 0) {
                    setNonOperational(NonOperationalReason.GLUSTER_PEER_LIST_FAILED);
                } else if (!hostExists(glusterServers, getVds())) {
                    if (!glusterPeerProbe(upServer.getId(), getVds().gethost_name())) {
                        setNonOperational(NonOperationalReason.GLUSTER_PEER_PROBE_FAILED);
                    }
                }
            }
        }
    }

    private boolean hostExists(List<GlusterServerInfo> glusterServers, VDS server) {
        for (GlusterServerInfo glusterServer : glusterServers) {
            if (glusterServer.getHostnameOrIp().equals(server.gethost_name())) {
                return true;
            }
            for (VdsNetworkInterface vdsNwInterface : getVdsInterfaces(server.getId())) {
                if (glusterServer.getHostnameOrIp().equals(vdsNwInterface.getAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    public InterfaceDAO getInterfaceDAO() {
        return getDbFacade().getInterfaceDao();
    }

    private List<VdsNetworkInterface> getVdsInterfaces(Guid vdsId) {
        List<VdsNetworkInterface> interfaces = getInterfaceDAO().getAllInterfacesForVds(vdsId);
        return (interfaces == null) ? new ArrayList<VdsNetworkInterface>() : interfaces;
    }

    @SuppressWarnings("unchecked")
    private List<GlusterServerInfo> getGlusterPeers(Guid upServerId) {
        List<GlusterServerInfo> glusterServers = new ArrayList<GlusterServerInfo>();
        VDSReturnValue returnValue = runVdsCommand(VDSCommandType.GlusterServersList,
                        new VdsIdVDSCommandParametersBase(upServerId));
        if (!returnValue.getSucceeded()) {
            getReturnValue().getFault().setError(returnValue.getVdsError().getCode());
            getReturnValue().getFault().setMessage(returnValue.getVdsError().getMessage());
            AuditLogDirector.log(new AuditLogableBase(upServerId), AuditLogType.GLUSTER_SERVERS_LIST_FAILED);
            _glusterPeerListSucceeded = false;
        } else {
            glusterServers = (List<GlusterServerInfo>) returnValue.getReturnValue();
        }
        return glusterServers;
    }

    private boolean glusterPeerProbe(Guid upServerId, String newServerName) {
        try {
            VDSReturnValue returnValue = runVdsCommand(VDSCommandType.GlusterHostAdd,
                    new GlusterHostAddVDSParameters(upServerId, newServerName));
            if (!returnValue.getSucceeded()) {
                getReturnValue().getFault().setError(returnValue.getVdsError().getCode());
                getReturnValue().getFault().setMessage(returnValue.getVdsError().getMessage());
                AuditLogDirector.log(new AuditLogableBase(getVdsId()), AuditLogType.GLUSTER_HOST_ADD_FAILED);
                _glusterPeerProbeSucceeded = false;
            }
            return returnValue.getSucceeded();
        } catch (Exception e) {
            log.errorFormat("Could not peer probe the gluster server {0}. Error: {1}",
                    getVds().gethost_name(),
                    e.getMessage());
            _glusterPeerProbeSucceeded = false;
            return false;
        }
    }

}
