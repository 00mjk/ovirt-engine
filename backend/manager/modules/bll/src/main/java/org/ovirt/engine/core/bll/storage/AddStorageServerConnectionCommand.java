package org.ovirt.engine.core.bll.storage;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.InternalCommandAttribute;
import org.ovirt.engine.core.common.action.StorageServerConnectionParametersBase;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.businessentities.storage_server_connections;
import org.ovirt.engine.core.common.validation.NfsMountPointConstraint;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

@SuppressWarnings("serial")
@InternalCommandAttribute
public class AddStorageServerConnectionCommand<T extends StorageServerConnectionParametersBase> extends
        ConnectStorageToVdsCommand<T> {
    public AddStorageServerConnectionCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void executeCommand() {
        storage_server_connections currConnection = getConnection();
        boolean isValidConnection = true;
        isValidConnection = Connect(getVds().getId()).getFirst();

        // Add storage Connection to the database.
        if (isValidConnection && (StringUtils.isNotEmpty(currConnection.getid())
                || getDbFacade().getStorageServerConnectionDAO().get(currConnection.getid()) == null)) {
            currConnection.setid(Guid.NewGuid().toString());
            getDbFacade().getStorageServerConnectionDAO().save(currConnection);
        }
       getReturnValue().setActionReturnValue(getConnection().getid());
        setSucceeded(true);
    }

    @Override
    protected storage_server_connections getConnection() {
        if (StringUtils.isEmpty(getParameters().getStorageServerConnection().getid())) {
            List<storage_server_connections> connections;
            if ((connections = DbFacade.getInstance().getStorageServerConnectionDAO().getAllForStorage(
                    getParameters().getStorageServerConnection().getconnection())).size() != 0) {
                getParameters().setStorageServerConnection(connections.get(0));
            }
        }
        return (getParameters()).getStorageServerConnection();
    }

    @Override
    protected boolean canDoAction() {
        boolean returnValue = true;
        storage_server_connections paramConnection = getParameters().getStorageServerConnection();
        if (returnValue && paramConnection.getstorage_type() == StorageType.NFS
                && !new NfsMountPointConstraint().isValid(paramConnection.getconnection(), null)) {
            returnValue = false;
            addCanDoActionMessage("VALIDATION.STORAGE.CONNECTION.INVALID");
        }

        if (getParameters().getVdsId().equals(Guid.Empty)) {
            returnValue = InitializeVds();
        } else if (getVds() == null) {
            addCanDoActionMessage(VdcBllMessages.VDS_INVALID_SERVER_ID);
            returnValue = false;
        } else if (getVds().getstatus() != VDSStatus.Up) {
            addCanDoActionMessage(VdcBllMessages.VDS_ADD_STORAGE_SERVER_STATUS_MUST_BE_UP);
            returnValue = false;
        }

        if (returnValue) {
            if (paramConnection.getstorage_type() == StorageType.LOCALFS) {
                storage_pool storagePool = DbFacade.getInstance().getStoragePoolDAO().getForVds(getVds().getId());
                if (storagePool == null || storagePool.getstorage_pool_type() != StorageType.LOCALFS) {
                    returnValue = false;
                    addCanDoActionMessage(VdcBllMessages.VDS_CANNOT_ADD_LOCAL_STORAGE_TO_NON_LOCAL_HOST);
                }
            }
            if (returnValue) {
                IStorageHelper storageHelper = StorageHelperDirector.getInstance().getItem(
                        paramConnection.getstorage_type());
                if (!storageHelper.ValidateStoragePoolConnectionsInHost(
                        getVds(),
                        new java.util.ArrayList<storage_server_connections>(java.util.Arrays
                                .asList(new storage_server_connections[] { paramConnection })), Guid.Empty))

                {
                    returnValue = false;
                    addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_CONNECTION);
                }
            }
        }
        return returnValue;
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(CreateEntity.class);
        return super.getValidationGroups();
    }
}
