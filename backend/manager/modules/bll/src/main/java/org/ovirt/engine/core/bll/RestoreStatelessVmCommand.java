package org.ovirt.engine.core.bll;

import java.util.List;

import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.common.action.RestoreAllSnapshotsParameters;
import org.ovirt.engine.core.common.action.UpdateVmVersionParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmOperationParameterBase;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.SnapshotActionEnum;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

@InternalCommandAttribute
public class RestoreStatelessVmCommand<T extends VmOperationParameterBase> extends VmCommand<T> {

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected RestoreStatelessVmCommand(Guid commandId) {
        super(commandId);
    }

    public RestoreStatelessVmCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void executeCommand() {
        VdcReturnValueBase result =
                getBackend().runInternalAction(VdcActionType.UpdateVmVersion,
                        new UpdateVmVersionParameters(getVmId()),
                        ExecutionHandler.createDefaultContexForTasks(getExecutionContext(), getLock()));

        // if it fail because of canDoAction, its safe to restore the snapshot
        // and the vm will still be usable with previous version
        if (!result.getSucceeded() && !result.getCanDoAction()) {
            log.warnFormat("Couldn't update VM {0} ({1}) version from it's template, continue with restoring stateless snapshot.",
                    getVm().getName(),
                    getVmId());

            boolean returnVal = true;
            Guid snapshotId = DbFacade.getInstance().getSnapshotDao().getId(getVmId(), SnapshotType.STATELESS);
            List<DiskImage> imagesList = null;

            if (snapshotId != null) {
                imagesList = DbFacade.getInstance().getDiskImageDao().getAllSnapshotsForVmSnapshot(snapshotId);
            }

            if (imagesList != null && imagesList.size() > 0) {
                /**
                 * restore all snapshots
                 */
                RestoreAllSnapshotsParameters restoreParameters = new RestoreAllSnapshotsParameters(getVm().getId(), SnapshotActionEnum.RESTORE_STATELESS);
                restoreParameters.setShouldBeLogged(false);
                restoreParameters.setImages(imagesList);
                VdcReturnValueBase vdcReturn =
                        Backend.getInstance().runInternalAction(VdcActionType.RestoreAllSnapshots,
                                restoreParameters,
                                ExecutionHandler.createDefaultContexForTasks(getExecutionContext(), getLock()));
                returnVal = vdcReturn.getSucceeded();
            }
            setSucceeded(returnVal);
        }
    }
}
