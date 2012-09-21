package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.quota.QuotaManager;
import org.ovirt.engine.core.bll.quota.Quotable;
import org.ovirt.engine.core.bll.snapshots.SnapshotsManager;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.RemoveAllVmImagesParameters;
import org.ovirt.engine.core.common.action.RemoveVmParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.LunDisk;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@LockIdNameAttribute
@NonTransactiveCommandAttribute(forceCompensation = true)
public class RemoveVmCommand<T extends RemoveVmParameters> extends VmCommand<T> implements Quotable{

    private static final long serialVersionUID = -3202434016040084728L;
    private boolean hasImages;
    private List<String> disksLeftInVm = new ArrayList<String>();

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected RemoveVmCommand(Guid commandId) {
        super(commandId);
    }

    public RemoveVmCommand(T parameters) {
        super(parameters);
        parameters.setEntityId(getVmId());
        if (getVm() != null) {
            setStoragePoolId(getVm().getstorage_pool_id());
        }
    }

    @Override
    protected void executeVmCommand() {
        if (getVm().getstatus() != VMStatus.ImageLocked) {
            VmHandler.LockVm(getVm().getDynamicData(), getCompensationContext());
        }
        freeLock();
        setSucceeded(removeVm());
    }

    private boolean removeVm() {
        VM vm = getVm();
        Guid vmId = getVmId();
        hasImages = vm.getDiskList().size() > 0;

        removeVmInSpm(vm.getstorage_pool_id(), vmId);
        if (hasImages && !removeVmImages(null)) {
            return false;
        }

        if (!hasImages) {
            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
                @Override
                public Void runInTransaction() {
                    removeVmFromDb();
                    return null;
                }
            });
        }
        return true;
    }

    @Override
    protected boolean canDoAction() {
        boolean retVal = true;

        if (getVm() == null) {
            retVal = false;
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        } else {
            retVal = super.canDoAction() && canRemoveVm();
            setDescription(getVmName());
        }
        return retVal;
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__REMOVE);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM);
    }

    public static boolean IsVmRunning(Guid vmId) {
        VM vm = DbFacade.getInstance().getVmDao().get(vmId);
        if (vm != null) {
            return VM.isStatusUpOrPaused(vm.getstatus()) || vm.getstatus() == VMStatus.Unknown;
        }
        return false;
    }

    private boolean isVmInPool(Guid vmId) {
        return getVm().getVmPoolId() != null;
    }

    private boolean canRemoveVm() {
        boolean returnValue = true;
        List<String> messages = getReturnValue().getCanDoActionMessages();
        if (getVm() == null) {
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_EXIST.toString());
            returnValue = false;
        } else if (IsVmRunning(getVmId()) || (getVm().getstatus() == VMStatus.NotResponding)) {
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_IS_RUNNING.toString());
            returnValue = false;
        } else if (getVm().getstatus() == VMStatus.Suspended) {
            messages.add(VdcBllMessages.VM_CANNOT_REMOVE_VM_WHEN_STATUS_IS_NOT_DOWN.toString());
            returnValue = false;
        } else if (isVmInPool(getVmId())) {
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_ATTACHED_TO_POOL.toString());
            returnValue = false;
        }
        // enable to remove vms without images
        else {
            ValidationResult vmDuringSnapshotResult =
                    new SnapshotsValidator().vmNotDuringSnapshot(getVm().getId());
            if (!vmDuringSnapshotResult.isValid()) {
                messages.add(vmDuringSnapshotResult.getMessage().name());
                returnValue = false;
            } else {
                VmHandler.updateDisksFromDb(getVm());
                if (!ImagesHandler.PerformImagesChecks(getVm(), messages, getVm().getstorage_pool_id(), Guid.Empty,
                                false, !getParameters().getForce(), false, false,
                                getParameters().getForce(), false, !getVm().getDiskMap().values().isEmpty(),
                                true, getVm().getDiskMap().values())) {
                    returnValue = false;
                }
            }
        }
        // we cannot force remove if there is running task
        if (returnValue && getParameters().getForce() && getVm().getstatus() == VMStatus.ImageLocked
                && AsyncTaskManager.getInstance().HasTasksByStoragePoolId(getVm().getstorage_pool_id())) {
            messages.add(VdcBllMessages.VM_CANNOT_REMOVE_HAS_RUNNING_TASKS.toString());
            returnValue = false;
        }
        return returnValue;
    }

    protected boolean removeVmImages(List<DiskImage> images) {
        RemoveAllVmImagesParameters tempVar = new RemoveAllVmImagesParameters(getVmId(), images);
        tempVar.setParentCommand(getActionType());
        tempVar.setEntityId(getParameters().getEntityId());
        tempVar.setParentParameters(getParameters());
        VdcReturnValueBase vdcRetValue =
                Backend.getInstance().runInternalAction(VdcActionType.RemoveAllVmImages,
                        tempVar,
                        ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));

        if (vdcRetValue.getSucceeded()) {
            getReturnValue().getTaskIdList().addAll(vdcRetValue.getInternalTaskIdList());
        }

        return vdcRetValue.getSucceeded();
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            if (hasImages) {
                return getSucceeded() ? AuditLogType.USER_REMOVE_VM : AuditLogType.USER_FAILED_REMOVE_VM;
            } else {
                return getSucceeded() ? AuditLogType.USER_REMOVE_VM_FINISHED : AuditLogType.USER_FAILED_REMOVE_VM;
            }
        case END_FAILURE:
        case END_SUCCESS:
        default:
            return disksLeftInVm.isEmpty() ? AuditLogType.USER_REMOVE_VM_FINISHED
                    : AuditLogType.USER_REMOVE_VM_FINISHED_WITH_ILLEGAL_DISKS;
        }
    }

    @Override
    protected Map<String, String> getExclusiveLocks() {
        return Collections.singletonMap(getVmId().toString(), LockingGroup.VM.name());
    }

    protected void removeVmFromDb() {
        removeLunDisks();
        removeVmUsers();
        removeVmNetwork();
        new SnapshotsManager().removeSnapshots(getVmId());
        removeVmStatic();
    }

    /**
     * The following method will perform a removing of all lunDisks from vm.
     * These is only DB operation
     */
    private void removeLunDisks() {
        List<LunDisk> lunDisks =
                ImagesHandler.filterDiskBasedOnLuns(getVm().getDiskMap().values());
        for (LunDisk lunDisk : lunDisks) {
            ImagesHandler.removeLunDisk(lunDisk);
        }
    }

    @Override
    protected void endVmCommand() {
        try {
            if (acquireLock()) {
                // Ensures the lock on the VM guid can be acquired. This prevents a race
                // between executeVmCommand (for example, of a first multiple VMs removal that includes VM A,
                // and a second multiple VMs removal that include the same VM).
                setVm(DbFacade.getInstance().getVmDao().get(getVmId()));
                if (getVm() != null) {
                    updateDisksAfterVmRemoved();

                    // Remove VM from DB.
                    removeVmFromDb();
                }
            }
            setSucceeded(true);
        } finally {
            freeLock();
        }
    }

    /**
     * Update disks for VM after disks were removed.
     */
    private void updateDisksAfterVmRemoved() {
        VmHandler.updateDisksFromDb(getVm());

        // Get all disk images for VM (VM should not have any image disk associated with it).
        List<DiskImage> diskImages = ImagesHandler.filterImageDisks(getVm().getDiskMap().values(),
                true,
                false);

        // If the VM still has disk images related to it, change their status to Illegal.
        if (!diskImages.isEmpty()) {
            for (DiskImage diskImage : diskImages) {
                disksLeftInVm.add(diskImage.getDiskAlias());
            }
            AddCustomValue("DisksNames", StringUtils.join(disksLeftInVm, ","));
        }
    }

    @Override
    public boolean validateAndSetQuota() {
        this.rollbackQuota();
        return true;
    }

    @Override
    public void rollbackQuota() {
        if (getStoragePool() == null) {
            setStoragePool(getStoragePoolDAO().getForVdsGroup(getVm().getvds_group_id()));
        }
        Guid quotaId = getVm().getQuotaId();
        Set<Guid> quotaIdsForRollback = new HashSet<Guid>();
        if (quotaId != null) {
            // Uncache the details of this quota. next time the quota will be called, a new calculation
            // would be done base on the DB.
            quotaIdsForRollback.add(quotaId);
        }
        VmHandler.updateDisksFromDb(getVm());
        for (DiskImage image : getVm().getDiskList()){
            quotaId = image.getQuotaId();

            if (quotaId != null) {
                quotaIdsForRollback.add(quotaId);
            }
        }
        QuotaManager.getInstance().rollbackQuota(getStoragePool(), new ArrayList<Guid>(quotaIdsForRollback));
    }

    @Override
    public Guid getQuotaId() {
        return null;
    }

    @Override
    public void addQuotaPermissionSubject(List<PermissionSubject> quotaPermissionList) {
        //
    }
}
