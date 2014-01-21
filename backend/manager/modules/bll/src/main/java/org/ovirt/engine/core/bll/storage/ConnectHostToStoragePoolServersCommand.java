package org.ovirt.engine.core.bll.storage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.InternalCommandAttribute;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.HostStoragePoolParametersBase;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.vdscommands.StorageServerConnectionManagementVDSParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;

/**
 * Connect host to all Storage server connections in Storage pool. We
 * considering that connection failed only if data domains failed to connect. If
 * Iso/Export domains failed to connect - only log it.
 */
@NonTransactiveCommandAttribute
@InternalCommandAttribute
public class ConnectHostToStoragePoolServersCommand extends
        ConnectHostToStoragePoolServerCommandBase<HostStoragePoolParametersBase> {

    public ConnectHostToStoragePoolServersCommand(HostStoragePoolParametersBase parameters) {
        super(parameters);
        setStoragePool(parameters.getStoragePool());
        setVds(parameters.getVds());
    }

    @Override
    protected void executeCommand() {
        initConnectionList();
        setSucceeded(connectStorageServer(getStoragePool().getStorageType(), getConnections()));

        if (getNeedToConnectIso()) {
            if (!connectStorageServer(getIsoType(), getIsoConnections())) {
                log.infoFormat("Failed to connect host {0} to StoragePool {1} Iso domain/s connections", getVds()
                        .getName(), getStoragePool().getName());
            }
        }
        if (getNeedToConnectExport()) {
            if (!connectStorageServer(getExportType(), getExportConnections())) {
                log.infoFormat("Failed to connect host {0} to StoragePool {1} Export domain/s connections", getVds()
                        .getName(), getStoragePool().getName());
            }
        }

        if (!getSucceeded()) {
           AuditLogDirector.log(this, AuditLogType.CONNECT_STORAGE_SERVERS_FAILED);
        }
    }

    private boolean connectStorageServer(StorageType type, List<StorageServerConnections> connections) {
        boolean connectSucceeded = true;
        if (connections != null && connections.size() > 0) {
            if (type == StorageType.ISCSI) {
                connections = ISCSIStorageHelper.updateIfaces(connections, getVds().getId());
            }

            Map<String, String> retValues = (HashMap<String, String>) Backend
                    .getInstance()
                    .getResourceManager()
                    .RunVdsCommand(
                            VDSCommandType.ConnectStorageServer,
                            new StorageServerConnectionManagementVDSParameters(getVds().getId(),
                                    getStoragePool().getId(), type, connections)).getReturnValue();
            connectSucceeded =
                    StorageHelperDirector.getInstance().getItem(type).isConnectSucceeded(retValues, connections);
            log.infoFormat("Host {0} storage connection was {1} ", getVds().getName(),
                    connectSucceeded ? "succeeded" : "failed");
        }
        return connectSucceeded;
    }
}
