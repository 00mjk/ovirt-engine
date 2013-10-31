package org.ovirt.engine.core.bll;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.ovirt.engine.core.bll.attestationbroker.AttestThread;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.storage.StorageHandlingCommandBase;
import org.ovirt.engine.core.bll.storage.StoragePoolStatusHandler;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.HostStoragePoolParametersBase;
import org.ovirt.engine.core.common.action.SetNonOperationalVdsParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.AttestationResultEnum;
import org.ovirt.engine.core.common.businessentities.FenceActionType;
import org.ovirt.engine.core.common.businessentities.FenceStatusReturnValue;
import org.ovirt.engine.core.common.businessentities.NonOperationalReason;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VdsSpmStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServer;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServerInfo;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.eventqueue.Event;
import org.ovirt.engine.core.common.eventqueue.EventQueue;
import org.ovirt.engine.core.common.eventqueue.EventResult;
import org.ovirt.engine.core.common.eventqueue.EventType;
import org.ovirt.engine.core.common.gluster.GlusterFeatureSupported;
import org.ovirt.engine.core.common.vdscommands.ConnectStoragePoolVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.MomPolicyVDSParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsIdAndVdsVDSCommandParametersBase;
import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.common.vdscommands.gluster.AddGlusterServerVDSParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AlertDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.gluster.GlusterServerDao;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.utils.ejb.BeanProxyType;
import org.ovirt.engine.core.utils.ejb.BeanType;
import org.ovirt.engine.core.utils.ejb.EjbUtils;
import org.ovirt.engine.core.vdsbroker.attestation.AttestationService;
import org.ovirt.engine.core.vdsbroker.attestation.AttestationValue;
import org.ovirt.engine.core.vdsbroker.irsbroker.IrsBrokerCommand;

/**
 * Initialize Vds on its loading. For storages: First connect all storage
 * servers to VDS. Second connect Vds to storage Pool.
 *
 * After server initialized - its will be moved to Up status.
 */
@NonTransactiveCommandAttribute
public class InitVdsOnUpCommand extends StorageHandlingCommandBase<HostStoragePoolParametersBase> {
    private boolean fenceSucceeded = true;
    private boolean vdsProxyFound;
    private boolean connectPoolSucceeded;
    private boolean glusterHostUuidFound, glusterPeerListSucceeded, glusterPeerProbeSucceeded;
    private FenceStatusReturnValue fenceStatusReturnValue;

    public InitVdsOnUpCommand(HostStoragePoolParametersBase parameters) {
        super(parameters);
        setVds(parameters.getVds());
    }

    private boolean initTrustedService() {
        List <String> hosts = new ArrayList<String> ();

        if (AttestThread.isTrustedVds(getVds().getId())) {
            return true;
        }

        hosts.add(getVds().getHostName());
        List<AttestationValue> value = new ArrayList<AttestationValue> ();
        try {
            value = AttestationService.getInstance().attestHosts(hosts);
        } catch (Exception e) {
            log.errorFormat("Encounter an exception while attesting host's trustworthiness for Host {0}, Error is {1}",
                    hosts,
                    e);
        }
        if (value.size() > 0 && value.get(0).getTrustLevel() == AttestationResultEnum.TRUSTED) {
            return true;
        } else {
            setNonOperational(NonOperationalReason.UNTRUSTED, null);
            return false;
        }
    }

    @Override
    protected void executeCommand() {
        VDSGroup vdsGroup = getVdsGroup();

        boolean initSucceeded = true;

        if (vdsGroup.supportsTrustedService()) {
            initSucceeded = initTrustedService();
        }

        if (initSucceeded && vdsGroup.supportsVirtService()) {
            initSucceeded = initVirtResources();
        }

        if (initSucceeded && vdsGroup.supportsGlusterService()) {
            initSucceeded = initGlusterHost();
        }

        setSucceeded(initSucceeded);
    }

    private boolean initVirtResources() {
        if (InitializeStorage()) {
            processFence();
            processStoragePoolStatus();
            runUpdateMomPolicy(getVdsGroup(), getVds());
        } else {
            Map<String, String> customLogValues = Collections.singletonMap("StoragePoolName", getStoragePoolName());
            setNonOperational(NonOperationalReason.STORAGE_DOMAIN_UNREACHABLE, customLogValues);
            return false;
        }
        return true;
    }

    private void processFence() {
        FenceExecutor executor = new FenceExecutor(getVds(), FenceActionType.Status);
        // check first if we have any VDS to act as the proxy for fence
        // actions.
        if (getVds().getpm_enabled() && executor.findProxyHost()) {
            VDSReturnValue returnValue = executor.fence();
            fenceSucceeded = returnValue.getSucceeded();
            fenceStatusReturnValue = (FenceStatusReturnValue) returnValue.getReturnValue();
            vdsProxyFound = true;
        }
    }

    private void processStoragePoolStatus() {
        if (getVds().getSpmStatus() != VdsSpmStatus.None) {
            StoragePool pool = DbFacade.getInstance().getStoragePoolDao().get(getVds().getStoragePoolId());
            if (pool != null && pool.getStatus() == StoragePoolStatus.NotOperational) {
                pool.setStatus(StoragePoolStatus.NonResponsive);
                DbFacade.getInstance().getStoragePoolDao().updateStatus(pool.getId(), pool.getStatus());
                StoragePoolStatusHandler.poolStatusChanged(pool.getId(), pool.getStatus());
            }
        }
    }

    private void setNonOperational(NonOperationalReason reason, Map<String, String> customLogValues) {
        SetNonOperationalVdsParameters tempVar =
                new SetNonOperationalVdsParameters(getVds().getId(), reason, customLogValues);
        tempVar.setSaveToDb(true);
        Backend.getInstance().runInternalAction(VdcActionType.SetNonOperationalVds, tempVar,  ExecutionHandler.createInternalJobContext());
    }

    private boolean InitializeStorage() {
        boolean returnValue = false;

        // if no pool or pool is uninitialized or in maintenance mode no need to
        // connect any storage
        if (getStoragePool() == null || StoragePoolStatus.Uninitialized == getStoragePool().getStatus()
                || StoragePoolStatus.Maintenance == getStoragePool().getStatus()) {
            returnValue = true;
            connectPoolSucceeded = true;
        } else {
            HostStoragePoolParametersBase params = new HostStoragePoolParametersBase(getStoragePool(), getVds());
            Backend.getInstance().runInternalAction(VdcActionType.ConnectHostToStoragePoolServers, params);
            returnValue = connectHostToPool();
            connectPoolSucceeded = returnValue;
        }
        return returnValue;
    }

    /**
     * The following method should connect host to pool
     * The method will perform a connect storage pool operation,
     * if operation will wail on StoragePoolWrongMaster or StoragePoolMasterNotFound errors
     * we will try to run reconstruct
     * @return
     */
    private boolean connectHostToPool() {
        final VDS vds = getVds();
        EventResult result =
                ((EventQueue) EjbUtils.findBean(BeanType.EVENTQUEUE_MANAGER, BeanProxyType.LOCAL)).submitEventSync(new Event(getStoragePool().getId(),
                        null, vds.getId(), EventType.VDSCONNECTTOPOOL, "Trying to connect host " + vds.getHostName() + " with id " + vds.getId() + " to the pool " + getStoragePool().getId()),
                        new Callable<EventResult>() {
                            @Override
                            public EventResult call() {
                                return runConnectHostToPoolEvent(getStoragePool().getId(), vds);
                            }
                        });
        if (result != null) {
            return result.isSuccess();
        }
        return false;
    }

    private EventResult runConnectHostToPoolEvent(final Guid storagePoolId, final VDS vds) {
        EventResult result = new EventResult(true, EventType.VDSCONNECTTOPOOL);
        StoragePool storagePool = getStoragePoolDAO().get(storagePoolId);
        StorageDomain masterDomain = getStorageDomainDAO().getStorageDomainByTypeAndPool(storagePoolId, StorageDomainType.Master);
        boolean masterDomainInactiveOrUnknown = masterDomain.getStatus() == StorageDomainStatus.InActive
                || masterDomain.getStatus() == StorageDomainStatus.Unknown;
        try {
            runVdsCommand(VDSCommandType.ConnectStoragePool,
                    new ConnectStoragePoolVDSCommandParameters(vds.getId(), storagePoolId,
                            vds.getVdsSpmId(), masterDomain.getId(),
                            storagePool.getmaster_domain_version()));
        } catch (VdcBLLException e) {
            if (masterDomainInactiveOrUnknown) {
                log.infoFormat("Could not connect host {0} to pool {1}, as the master domain is in inactive/unknown status - not failing the operation",
                        vds.getName(),
                        storagePool
                                .getName());
            } else {
                log.errorFormat("Could not connect host {0} to pool {1}", vds.getName(), storagePool
                        .getName());
                result.setSuccess(false);
            }
        }

        if (result.isSuccess()) {
            result.setSuccess(proceedVdsStats(!masterDomainInactiveOrUnknown));
            if (!result.isSuccess()) {
                AuditLogDirector.log(new AuditLogableBase(getVdsId()),
                        AuditLogType.VDS_STORAGE_VDS_STATS_FAILED);
            }
        }
        return result;
    }

    private VDSReturnValue runUpdateMomPolicy(final VDSGroup cluster, final VDS vds) {
        VDSReturnValue returnValue = new VDSReturnValue();
        if (cluster.getcompatibility_version().compareTo(Version.v3_3) >= 0) {
            try {
                returnValue = runVdsCommand(VDSCommandType.SetMOMPolicyParameters,
                        new MomPolicyVDSParameters(vds, cluster.isEnableBallooning()));
            } catch (VdcBLLException e) {
                log.errorFormat("Could not update MoM policy on host {0}", vds.getName());
                returnValue.setSucceeded(false);
            }
        }

        return returnValue;
    }

    protected boolean proceedVdsStats(boolean shouldCheckReportedDomains) {
        boolean returnValue = true;
        try {
            runVdsCommand(VDSCommandType.GetStats, new VdsIdAndVdsVDSCommandParametersBase(getVds()));
            if (shouldCheckReportedDomains) {
                List<Guid> problematicDomainsIds =
                        IrsBrokerCommand.fetchDomainsReportedAsProblematic(getVds().getStoragePoolId(),
                                getVds().getDomains());
                for (Guid domainId : problematicDomainsIds) {
                    StorageDomainStatic domainInfo = getStorageDomainStaticDAO().get(domainId);
                    log.errorFormat("Storage Domain {0} of pool {1} is in problem in host {2}",
                            domainInfo != null ? domainInfo.getStorageName() : domainId,
                            getStoragePool().getName(),
                            getVds().getName());
                    if (domainInfo == null || domainInfo.getStorageDomainType().isDataDomain()) {
                        returnValue = false;
                    }
                }
            }
        } catch (VdcBLLException e) {
            log.errorFormat("Could not get Host statistics for Host {0}, Error is {1}",
                    getVds().getName(),
                    e);
            returnValue = false;
        }
        return returnValue;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        AuditLogType type = AuditLogType.UNASSIGNED;

        if (getVdsGroup().supportsVirtService()) {
            if (!connectPoolSucceeded) {
                type = AuditLogType.CONNECT_STORAGE_POOL_FAILED;
            } else if (getVds().getpm_enabled() && fenceSucceeded) {
                type = AuditLogType.VDS_FENCE_STATUS;
            } else if (getVds().getpm_enabled() && !fenceSucceeded) {
                type = AuditLogType.VDS_FENCE_STATUS_FAILED;
            }

            // PM alerts
            AuditLogableBase logable = new AuditLogableBase(getVds().getId());
            if (getVds().getpm_enabled()) {
                if (!vdsProxyFound) {
                    logable.addCustomValue("Reason",
                            AuditLogDirector.getMessage(AuditLogType.VDS_ALERT_FENCE_NO_PROXY_HOST));
                    AlertDirector.Alert(logable, AuditLogType.VDS_ALERT_FENCE_TEST_FAILED);
                } else if (!fenceStatusReturnValue.getIsSucceeded()) {
                    logable.addCustomValue("Reason", fenceStatusReturnValue.getMessage());
                    AlertDirector.Alert(logable, AuditLogType.VDS_ALERT_FENCE_TEST_FAILED);
                }
            } else {
                AlertDirector.Alert(logable, AuditLogType.VDS_ALERT_FENCE_IS_NOT_CONFIGURED);
            }
        }

        if (type == AuditLogType.UNASSIGNED && getVdsGroup().supportsGlusterService()) {
            if (!glusterHostUuidFound) {
                type = AuditLogType.GLUSTER_HOST_UUID_NOT_FOUND;
            } else if (!glusterPeerListSucceeded) {
                type = AuditLogType.GLUSTER_SERVERS_LIST_FAILED;
            } else if (!glusterPeerProbeSucceeded) {
                type = AuditLogType.GLUSTER_SERVER_ADD_FAILED;
            }
        }

        return type;
    }

    private boolean initGlusterHost() {
        glusterHostUuidFound = true;
        if (GlusterFeatureSupported.glusterHostUuidSupported(getVdsGroup().getcompatibility_version())) {
            if (!saveGlusterHostUuid()) {
                glusterHostUuidFound = false;
                setNonOperational(NonOperationalReason.GLUSTER_HOST_UUID_NOT_FOUND, null);
            }
        }
        return glusterHostUuidFound && initGlusterPeerProcess();
    }

    private boolean initGlusterPeerProcess() {
        glusterPeerListSucceeded = true;
        glusterPeerProbeSucceeded = true;
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
                Map<String, String> customLogValues = new HashMap<String, String>();
                customLogValues.put("Server", upServer.getHostName());
                if (glusterServers.size() == 0) {
                    customLogValues.put("Command", "gluster peer status");
                    setNonOperational(NonOperationalReason.GLUSTER_COMMAND_FAILED, customLogValues);
                    return false;
                } else if (!hostExists(glusterServers, getVds())) {
                    if (!glusterPeerProbe(upServer.getId(), getVds().getHostName())) {
                        customLogValues.put("Command", "gluster peer probe " + getVds().getHostName());
                        setNonOperational(NonOperationalReason.GLUSTER_COMMAND_FAILED, customLogValues);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean hostExists(List<GlusterServerInfo> glusterServers, VDS server) {
        if (GlusterFeatureSupported.glusterHostUuidSupported(getVdsGroup().getcompatibility_version())) {
            GlusterServer glusterServer = DbFacade.getInstance().getGlusterServerDao().getByServerId(server.getId());
            if (glusterServer != null) {
                for (GlusterServerInfo glusterServerInfo : glusterServers) {
                    if (glusterServerInfo.getUuid().equals(glusterServer.getGlusterServerUuid())) {
                        return true;
                    }
                }
            }
        }
        else {
            for (GlusterServerInfo glusterServer : glusterServers) {
                if (glusterServer.getHostnameOrIp().equals(server.getHostName())) {
                    return true;
                }
                try {
                    String glusterHostAddr = InetAddress.getByName(glusterServer.getHostnameOrIp()).getHostAddress();
                    for (VdsNetworkInterface vdsNwInterface : getVdsInterfaces(server.getId())) {
                        if (glusterHostAddr.equals(vdsNwInterface.getAddress())) {
                            return true;
                        }
                    }
                } catch (UnknownHostException e) {
                    log.errorFormat("Could not resole IP address of the host {0}. Error: {1}",
                            glusterServer.getHostnameOrIp(),
                            e.getMessage());
                }
            }
        }
        return false;
    }

    private boolean saveGlusterHostUuid() {
        GlusterServerDao glusterServerDao = DbFacade.getInstance().getGlusterServerDao();
        GlusterServer glusterServer = glusterServerDao.getByServerId(getVds().getId());
        if (glusterServer == null) {
            VDSReturnValue returnValue = runVdsCommand(VDSCommandType.GetGlusterHostUUID,
                    new VdsIdVDSCommandParametersBase(getVds().getId()));
            if (returnValue.getSucceeded() && returnValue.getReturnValue() != null) {
                glusterServer = new GlusterServer();
                glusterServer.setId(getVds().getId());
                glusterServer.setGlusterServerUuid(Guid.createGuidFromString((String) returnValue.getReturnValue()));
                glusterServerDao.save(glusterServer);
            }
            else {
                return false;
            }
        }
        return true;
    }

    public InterfaceDao getInterfaceDAO() {
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
            AuditLogableBase logable = new AuditLogableBase(upServerId);
            logable.updateCallStackFromThrowable(returnValue.getExceptionObject());
            AuditLogDirector.log(logable, AuditLogType.GLUSTER_SERVERS_LIST_FAILED);
            glusterPeerListSucceeded = false;
        } else {
            glusterServers = (List<GlusterServerInfo>) returnValue.getReturnValue();
        }
        return glusterServers;
    }

    private boolean glusterPeerProbe(Guid upServerId, String newServerName) {
        try {
            VDSReturnValue returnValue = runVdsCommand(VDSCommandType.AddGlusterServer,
                    new AddGlusterServerVDSParameters(upServerId, newServerName));
            if (!returnValue.getSucceeded()) {
                getReturnValue().getFault().setError(returnValue.getVdsError().getCode());
                getReturnValue().getFault().setMessage(returnValue.getVdsError().getMessage());
                AuditLogableBase logable = new AuditLogableBase(getVdsId());
                logable.updateCallStackFromThrowable(returnValue.getExceptionObject());
                AuditLogDirector.log(logable, AuditLogType.GLUSTER_SERVER_ADD_FAILED);
                glusterPeerProbeSucceeded = false;
            }
            return returnValue.getSucceeded();
        } catch (Exception e) {
            log.errorFormat("Could not peer probe the gluster server {0}. Error: {1}",
                    getVds().getHostName(),
                    e.getMessage());
            glusterPeerProbeSucceeded = false;
            return false;
        }
    }

}
