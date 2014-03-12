package org.ovirt.engine.core.bll.storage;

import java.util.ArrayList;
import java.util.Collections;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.CommandBase;
import org.ovirt.engine.core.bll.SortedMultipleActionsRunnerBase;
import org.ovirt.engine.core.common.action.StorageDomainPoolParametersBase;
import org.ovirt.engine.core.common.action.StoragePoolWithStoragesParameter;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;

public class AttachStorageDomainsMultipleActionRunner extends SortedMultipleActionsRunnerBase {
    public AttachStorageDomainsMultipleActionRunner(VdcActionType actionType,
            ArrayList<VdcActionParametersBase> parameters, boolean isInternal) {
        super(actionType, parameters, isInternal);
    }

    @Override
    public ArrayList<VdcReturnValueBase> execute() {
        if (getParameters().size() > 0) {
            StoragePool pool = DbFacade.getInstance().getStoragePoolDao().get(
                    ((StorageDomainPoolParametersBase) getParameters().get(0)).getStoragePoolId());
            if (pool.getStatus() == StoragePoolStatus.Uninitialized) {
                ArrayList<Guid> storageDomainIds = new ArrayList<Guid>();
                for (VdcActionParametersBase param : getParameters()) {
                    storageDomainIds.add(((StorageDomainPoolParametersBase) param).getStorageDomainId());
                }
                ArrayList<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();
                parameters.add(new StoragePoolWithStoragesParameter(pool, storageDomainIds, getParameters().get(0)
                        .getSessionId()));
                if (isInternal) {
                    return Backend.getInstance().runInternalMultipleActions(VdcActionType.AddStoragePoolWithStorages,
                            parameters);
                } else {
                    return Backend.getInstance().runMultipleActions(VdcActionType.AddStoragePoolWithStorages,
                            parameters, false);
                }
            } else {
                return super.execute();
            }
        } else {
            return super.execute();
        }
    }

    @Override
    protected void sortCommands() {
        Collections.sort(getCommands(), new StorageDomainsByTypeComparer());
    }

    @Override
    protected void runCommands() {
        sortCommands();

        for (final CommandBase<?> command : getCommands()) {
            if (command.getReturnValue().getCanDoAction()) {
                ThreadPoolUtil.execute(new Runnable() {

                    @Override
                    public void run() {
                        executeValidatedCommand(command);
                    }
                });
            }
        }
    }
}
