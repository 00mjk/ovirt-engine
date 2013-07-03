package org.ovirt.engine.core.bll.lsm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.CommandBase;
import org.ovirt.engine.core.bll.ImagesHandler;
import org.ovirt.engine.core.bll.InternalCommandAttribute;
import org.ovirt.engine.core.bll.LockIdNameAttribute;
import org.ovirt.engine.core.bll.LockMessagesMatchUtil;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageDependent;
import org.ovirt.engine.core.bll.tasks.SPMAsyncTaskHandler;
import org.ovirt.engine.core.bll.tasks.TaskHandlerCommand;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.validator.DiskImagesValidator;
import org.ovirt.engine.core.bll.validator.StorageDomainValidator;
import org.ovirt.engine.core.bll.validator.VmValidator;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.LiveMigrateDiskParameters;
import org.ovirt.engine.core.common.action.LiveMigrateVmDisksParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskCreationInfo;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.DiskDao;
import org.ovirt.engine.core.dao.DiskImageDAO;
import org.ovirt.engine.core.dao.StorageDomainDAO;
import org.ovirt.engine.core.utils.collections.MultiValueMapUtils;

@LockIdNameAttribute
@NonTransactiveCommandAttribute
@InternalCommandAttribute
public class LiveMigrateVmDisksCommand<T extends LiveMigrateVmDisksParameters> extends CommandBase<T>
        implements TaskHandlerCommand<LiveMigrateVmDisksParameters>, QuotaStorageDependent {

    private Map<Guid, DiskImage> diskImagesMap = new HashMap<Guid, DiskImage>();
    private Map<Guid, StorageDomain> storageDomainsMap = new HashMap<Guid, StorageDomain>();

    public LiveMigrateVmDisksCommand(T parameters) {
        super(parameters);

        getParameters().setCommandType(getActionType());
        setVmId(getParameters().getVmId());
    }

    @Override
    protected List<SPMAsyncTaskHandler> initTaskHandlers() {
        return Arrays.<SPMAsyncTaskHandler> asList(
                new LiveSnapshotTaskHandler(this),
                new LiveMigrateDisksTaskHandler(this)
                );
    }

    /* Overridden stubs declared as public in order to implement ITaskHandlerCommand */

    @Override
    public T getParameters() {
        return super.getParameters();
    }

    @Override
    public Guid createTask(
            Guid taskId,
            AsyncTaskCreationInfo asyncTaskCreationInfo,
            VdcActionType parentCommand,
            VdcObjectType entityType,
            Guid... entityIds) {
        return super.createTask(taskId, asyncTaskCreationInfo, parentCommand, entityType, entityIds);
    }

    @Override
    public Guid persistAsyncTaskPlaceHolder(VdcActionType parentCommand) {
        return super.persistAsyncTaskPlaceHolder(parentCommand);
    }

    @Override
    public VdcActionType getActionType() {
        return super.getActionType();
    }

    @Override
    public void preventRollback() {
        getParameters().setExecutionIndex(0);
    }

    @Override
    public Guid createTask(Guid taskId, AsyncTaskCreationInfo asyncTaskCreationInfo, VdcActionType parentCommand) {
        return super.createTask(taskId, asyncTaskCreationInfo, parentCommand);
    }

    @Override
    public ArrayList<Guid> getTaskIdList() {
        return super.getTaskIdList();
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = new ArrayList<PermissionSubject>();

        for (LiveMigrateDiskParameters parameters : getParameters().getParametersList()) {
            permissionList.add(new PermissionSubject(parameters.getImageId(),
                    VdcObjectType.Disk,
                    ActionGroup.CONFIGURE_DISK_STORAGE));
            permissionList.add(new PermissionSubject(parameters.getStorageDomainId(),
                    VdcObjectType.Storage,
                    ActionGroup.CREATE_DISK));
        }

        return permissionList;
    }

    @Override
    protected void executeCommand() {
        setSucceeded(true);
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        return Collections.singletonMap(getVmId().toString(),
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.VM, VdcBllMessages.ACTION_TYPE_FAILED_OBJECT_LOCKED));
    }

    @Override
    public VM getVm() {
        VM vm = super.getVm();
        if (vm != null) {
            setVm(vm);
        }

        return vm;
    }

    protected DiskDao getDiskDao() {
        return getDbFacade().getDiskDao();
    }

    protected DiskImageDAO getDiskImageDao() {
        return getDbFacade().getDiskImageDao();
    }

    protected StorageDomainDAO getStorageDomainDao() {
        return getDbFacade().getStorageDomainDao();
    }

    private DiskImage getDiskImageById(Guid imageId) {
        if (diskImagesMap.containsKey(imageId)) {
            return diskImagesMap.get(imageId);
        }

        DiskImage diskImage = getDiskImageDao().get(imageId);
        diskImagesMap.put(imageId, diskImage);

        return diskImage;
    }

    private StorageDomain getStorageDomainById(Guid storageDomainId, Guid storagePoolId) {
        if (storageDomainsMap.containsKey(storageDomainId)) {
            return storageDomainsMap.get(storageDomainId);
        }

        StorageDomain storageDomain = getStorageDomainDao().getForStoragePool(storageDomainId, storagePoolId);
        storageDomainsMap.put(storageDomainId, storageDomain);

        return storageDomain;
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__MOVE);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM_DISK);
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaStorageConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<QuotaConsumptionParameter>();

        for (LiveMigrateDiskParameters parameters : getParameters().getParametersList()) {
            DiskImage diskImage = getDiskImageById(parameters.getImageId());

            list.add(new QuotaStorageConsumptionParameter(
                    parameters.getQuotaId(),
                    null,
                    QuotaConsumptionParameter.QuotaAction.CONSUME,
                    parameters.getStorageDomainId(),
                    (double) diskImage.getSizeInGigabytes()));

            if (diskImage.getQuotaId() != null && !Guid.Empty.equals(diskImage.getQuotaId())) {
                list.add(new QuotaStorageConsumptionParameter(
                        diskImage.getQuotaId(),
                        null,
                        QuotaConsumptionParameter.QuotaAction.RELEASE,
                        parameters.getSourceDomainId(),
                        (double) diskImage.getSizeInGigabytes()));
            }
        }
        return list;
    }

    @Override
    protected boolean canDoAction() {
        setStoragePoolId(getVm().getStoragePoolId());

        if (!isValidParametersList() || !checkImagesStatus() || !isValidSpaceRequirements()
                || !isVmNotRunningStateless()) {
            return false;
        }

        for (LiveMigrateDiskParameters parameters : getParameters().getParametersList()) {
            getReturnValue().setCanDoAction(isDiskNotShareable(parameters.getImageId())
                    && isTemplateInDestStorageDomain(parameters.getImageId(), parameters.getStorageDomainId())
                    && validateSourceStorageDomain(parameters.getImageId())
                    && validateDestStorage(parameters.getImageId(), parameters.getStorageDomainId()));

            if (!getReturnValue().getCanDoAction()) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidParametersList() {
        if (getParameters().getParametersList().isEmpty()) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_NO_DISKS_SPECIFIED);
        }

        return true;
    }

    protected boolean checkImagesStatus() {
        List<DiskImage> disksToCheck = ImagesHandler.filterImageDisks(getDiskDao().getAllForVm(getVmId()), true, false);
        DiskImagesValidator diskImagesValidator = new DiskImagesValidator(disksToCheck);
        return validate(diskImagesValidator.diskImagesNotLocked());
    }

    private boolean isDiskNotShareable(Guid imageId) {
        DiskImage diskImage = getDiskImageById(imageId);

        if (diskImage.isShareable()) {
            addCanDoActionMessage(String.format("$%1$s %2$s", "diskAliases", diskImage.getDiskAlias()));
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_SHAREABLE_DISK_NOT_SUPPORTED);
        }

        return true;
    }

    private boolean isTemplateInDestStorageDomain(Guid imageId, Guid sourceDomainId) {
        Guid templateId = getDiskImageById(imageId).getImageTemplateId();

        if (!Guid.Empty.equals(templateId)) {
            DiskImage templateImage = getDiskImageDao().get(templateId);
            if (!templateImage.getStorageIds().contains(sourceDomainId)) {
                return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_NOT_FOUND_ON_DESTINATION_DOMAIN);
            }
        }

        return true;
    }

    private boolean validateSourceStorageDomain(Guid imageId) {
        DiskImage diskImage = getDiskImageById(imageId);
        Guid domainId = diskImage.getStorageIds().get(0);
        StorageDomainValidator validator = getValidator(domainId, getStoragePoolId());

        return validate(validator.isDomainExistAndActive());
    }

    private boolean validateDestStorage(Guid imageId, Guid destDomainId) {
        StorageDomainValidator validator = getValidator(destDomainId, getStoragePoolId());

        return validate(validator.isDomainExistAndActive()) && validate(validator.domainIsValidDestination());
    }

    private StorageDomainValidator getValidator(Guid domainId, Guid storagePoolId) {
        return new StorageDomainValidator(getStorageDomainById(domainId, storagePoolId));
    }

    protected boolean isValidSpaceRequirements() {
        Map<Guid, List<DiskImage>> storageDomainsImagesMap = new HashMap<Guid, List<DiskImage>>();

        for (LiveMigrateDiskParameters parameters : getParameters().getParametersList()) {
            MultiValueMapUtils.addToMap(parameters.getStorageDomainId(),
                    getDiskImageById(parameters.getImageId()),
                    storageDomainsImagesMap);
        }

        for (Map.Entry<Guid, List<DiskImage>> entry : storageDomainsImagesMap.entrySet()) {
            Guid destDomainId = entry.getKey();
            List<DiskImage> disksList = entry.getValue();
            Guid storagePoolId = disksList.get(0).getStoragePoolId();
            StorageDomain destDomain = getStorageDomainById(destDomainId, storagePoolId);

            if (!isStorageDomainWithinThresholds(destDomain)) {
                return false;
            }

            long totalImagesSize = 0;
            for (DiskImage diskImage : disksList) {
                Guid templateId = diskImage.getImageTemplateId();
                List<DiskImage> allImageSnapshots =
                        ImagesHandler.getAllImageSnapshots(diskImage.getImageId(), templateId);

                diskImage.getSnapshots().addAll(allImageSnapshots);
                totalImagesSize += Math.round(diskImage.getActualDiskWithSnapshotsSize());
            }

            if (!doesStorageDomainhaveSpaceForRequest(destDomain, totalImagesSize)) {
                return false;
            }
        }

        return true;
    }

    protected boolean isStorageDomainWithinThresholds(StorageDomain storageDomain) {
        return validate(new StorageDomainValidator(storageDomain).isDomainWithinThresholds());
    }

    protected boolean doesStorageDomainhaveSpaceForRequest(StorageDomain storageDomain, long totalImagesSize) {
        return validate(new StorageDomainValidator(storageDomain).isDomainHasSpaceForRequest(totalImagesSize));
    }

    private boolean isVmNotRunningStateless() {
        return validate(createVmValidator().vmNotRunningStateless());
    }

    protected VmValidator createVmValidator() {
        return new VmValidator(getVm());
    }
}
