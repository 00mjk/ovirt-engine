package org.ovirt.engine.core.common.businessentities;

import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.eventqueue.EventResult;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;

public interface IVdsEventListener {
    void vdsNotResponding(VDS vds, boolean executeSshSoftFencing); // BLL

    void vdsNonOperational(Guid vdsId, NonOperationalReason type, boolean logCommand, boolean saveToDb,
            Guid domainId); // BLL

    void vdsNonOperational(Guid vdsId, NonOperationalReason type, boolean logCommand, boolean saveToDb,
            Guid domainId,
            Map<String, String> customLogValues); // BLL

    void vdsMovedToMaintenance(VDS vds); // BLL

    EventResult storageDomainNotOperational(Guid storageDomainId, Guid storagePoolId); // BLL

    EventResult masterDomainNotOperational(Guid storageDomainId, Guid storagePoolId, boolean isReconstructToInactiveDomains, boolean canReconstructToCurrentMaster); // BLL

    void processOnVmStop(Guid vmId);

    boolean vdsUpEvent(VDS vds);

    void processOnClientIpChange(VDS vds, Guid vmId);

    void processOnCpuFlagsChange(Guid vdsId);

    void processOnVmPoweringUp(Guid vds_id, Guid vmid, String display_ip, int display_port);

    void handleVdsVersion(Guid vdsId);

    void rerun(Guid vmId);

    void runningSucceded(Guid vmId);

    void removeAsyncRunningCommand(Guid vmId);

    void storagePoolUpEvent(StoragePool storagePool);


    void storagePoolStatusChange(Guid storagePoolId, StoragePoolStatus status, AuditLogType auditLogType,
            VdcBllErrors error);

    void storagePoolStatusChange(Guid storagePoolId, StoragePoolStatus status, AuditLogType auditLogType,
            VdcBllErrors error, TransactionScopeOption transactionScopeOption);

    void storagePoolStatusChanged(Guid storagePoolId, StoragePoolStatus status);

    void runFailedAutoStartVM(Guid vmId);

    void addExternallyManagedVms(List<VmStatic> externalVmList);

    void handleVdsMaintenanceTimeout(VDS vds);

    /**
     * update host's scheduling related properties
     *
     * @param vds
     */
    void updateSchedulingStats(VDS vds); // BLL
}
