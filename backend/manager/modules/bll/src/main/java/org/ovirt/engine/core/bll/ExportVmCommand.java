package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.storage.StoragePoolValidator;
import org.ovirt.engine.core.bll.utils.ClusterUtils;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.DiskImagesValidator;
import org.ovirt.engine.core.bll.validator.MultipleStorageDomainsValidator;
import org.ovirt.engine.core.bll.validator.StorageDomainValidator;
import org.ovirt.engine.core.bll.validator.VmValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.MoveOrCopyImageGroupParameters;
import org.ovirt.engine.core.common.action.MoveVmParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.CopyVolumeType;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotType;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMapId;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.queries.GetAllFromExportDomainQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.vdscommands.GetImageInfoVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.UpdateVMVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.KeyValuePairCompat;
import org.ovirt.engine.core.utils.GuidUtils;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;
import org.ovirt.engine.core.utils.ovf.OvfManager;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@DisableInPrepareMode
@LockIdNameAttribute
@NonTransactiveCommandAttribute(forceCompensation = true)
public class ExportVmCommand<T extends MoveVmParameters> extends MoveOrCopyTemplateCommand<T> {

    private List<DiskImage> disksImages;
    private Collection<Snapshot> snapshotsWithMemory;

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected ExportVmCommand(Guid commandId) {
        super(commandId);
    }

    public ExportVmCommand(T parameters) {
        super(parameters);
        setVmId(parameters.getContainerId());
        parameters.setEntityInfo(new EntityInfo(VdcObjectType.VM, getVmId()));
    }

    @Override
    protected boolean canDoAction() {
        if (getVm() == null) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        }
        setDescription(getVmName());
        setStoragePoolId(getVm().getStoragePoolId());

        // check that target domain exists
        StorageDomainValidator targetstorageDomainValidator = new StorageDomainValidator(getStorageDomain());
        if (!validate(targetstorageDomainValidator.isDomainExistAndActive())) {
            return false;
        }

        // load the disks of vm from database
        VmHandler.updateDisksFromDb(getVm());

        DiskImagesValidator diskImagesValidator = new DiskImagesValidator(getDisksBasedOnImage());
        if (!validate(diskImagesValidator.diskImagesNotIllegal()) ||
                !validate(diskImagesValidator.diskImagesNotLocked())) {
            return false;
        }

        // update vm snapshots for storage free space check
        ImagesHandler.fillImagesBySnapshots(getVm());

        // check that the target and source domain are in the same storage_pool
        if (getDbFacade().getStoragePoolIsoMapDao()
                .get(new StoragePoolIsoMapId(getStorageDomain().getId(),
                        getVm().getStoragePoolId())) == null) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_POOL_NOT_MATCH);
            return false;
        }

        // check if template exists only if asked for
        if (getParameters().getTemplateMustExists()) {
            if (!CheckTemplateInStorageDomain(getVm().getStoragePoolId(), getParameters().getStorageDomainId(),
                    getVm().getVmtGuid())) {
                return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_NOT_FOUND_ON_EXPORT_DOMAIN,
                        String.format("$TemplateName %1$s", getVm().getVmtName()));
            }
        }

        Map<Guid, ? extends Disk> images = getVm().getDiskMap();
        // check that the images requested format are valid (COW+Sparse)
        if (!ImagesHandler.checkImagesConfiguration(getParameters().getStorageDomainId(),
                new ArrayList<Disk>(images.values()),
                getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        if (getParameters().getCopyCollapse()) {
            for (DiskImage img : getDisksBasedOnImage()) {
                if (images.containsKey(img.getId())) {
                    // check that no RAW format exists (we are in collapse mode)
                    if (((DiskImage) images.get(img.getId())).getVolumeFormat() == VolumeFormat.RAW
                            && img.getVolumeFormat() != VolumeFormat.RAW) {
                        addCanDoActionMessage(VdcBllMessages.VM_CANNOT_EXPORT_RAW_FORMAT);
                        return false;
                    }
                }
            }
        }

        // check destination storage is Export domain
        if (getStorageDomain().getStorageDomainType() != StorageDomainType.ImportExport) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_SPECIFY_DOMAIN_IS_NOT_EXPORT_DOMAIN,
                    String.format("$storageDomainName %1$s", getStorageDomainName()));
        }

        // get the snapshot that are going to be exported and have memory
        snapshotsWithMemory = getSnapshotsToBeExportedWithMemory();

        // check destination storage have free space
        int sizeInGB = (int) getVm().getActualDiskWithSnapshotsSize() + getTotalMemoryStatesSizeGb();
        if (!doesStorageDomainhaveSpaceForRequest(getStorageDomain(), sizeInGB)) {
            return false;
        }

        SnapshotsValidator snapshotValidator = new SnapshotsValidator();
        if (!(checkVmInStorageDomain()
                && validate(new StoragePoolValidator(getStoragePool()).isUp())
                && validate(snapshotValidator.vmNotDuringSnapshot(getVmId()))
                && validate(snapshotValidator.vmNotInPreview(getVmId()))
                && validate(new VmValidator(getVm()).vmDown())
                && validate(new MultipleStorageDomainsValidator(getVm().getStoragePoolId(),
                ImagesHandler.getAllStorageIdsForImageIds(getDisksBasedOnImage())).allDomainsExistAndActive()))) {
            return false;
        }

        return true;
    }

    private Collection<Snapshot> getSnapshotsToBeExportedWithMemory() {
        if (getParameters().getCopyCollapse()) {
            Snapshot activeSnapshot = getSnapshotDao().get(getVmId(), SnapshotType.ACTIVE);
            return !activeSnapshot.getMemoryVolume().isEmpty() ?
                    Collections.<Snapshot>singleton(activeSnapshot) : Collections.<Snapshot>emptyList();
        }
        else {
            Map<String, Snapshot> memory2snapshot = new HashMap<String, Snapshot>();
            for (Snapshot snapshot : getSnapshotDao().getAll(getVmId())) {
                memory2snapshot.put(snapshot.getMemoryVolume(), snapshot);
            }
            memory2snapshot.remove(StringUtils.EMPTY);
            return memory2snapshot.values();
        }
    }

    private int getTotalMemoryStatesSizeGb() {
        long sizeInBytes = 0;
        for (Snapshot snapshot : snapshotsWithMemory) {
            VM vm = getVmFromSnapshot(snapshot);
            sizeInBytes += vm.getTotalMemorySizeInBytes() + HibernateVmCommand.META_DATA_SIZE_IN_BYTES;
        }
        return (int) Math.ceil(1.0 * sizeInBytes / BYTES_IN_GB);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__EXPORT);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM);
    }

    @Override
    protected void executeCommand() {
        VmHandler.LockVm(getVm().getDynamicData(), getCompensationContext());
        freeLock();

        // Means that there are no asynchronous tasks to execute - so we can end the command
        // immediately after the execution of the previous steps
        if (!hasSnappableDisks()) {
            endSuccessfullySynchronous();
        } else {
            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

                @Override
                public Void runInTransaction() {
                    moveOrCopyAllImageGroups();
                    return null;
                }
            });

            if (!getReturnValue().getVdsmTaskIdList().isEmpty()) {
                setSucceeded(true);
            }
        }
    }

    private boolean hasSnappableDisks() {
        return !getDisksBasedOnImage().isEmpty();
    }

    public boolean updateCopyVmInSpm(Guid storagePoolId, VM vm, Guid storageDomainId) {
        HashMap<Guid, KeyValuePairCompat<String, List<Guid>>> vmsAndMetaDictionary =
                new HashMap<Guid, KeyValuePairCompat<String, List<Guid>>>();
        OvfManager ovfManager = new OvfManager();
        ArrayList<DiskImage> AllVmImages = new ArrayList<DiskImage>();
        VmHandler.updateDisksFromDb(vm);
        List<VmNetworkInterface> interfaces = vm.getInterfaces();
        if (interfaces != null) {
            // TODO remove this when the API changes
            interfaces.clear();
            interfaces.addAll(getDbFacade().getVmNetworkInterfaceDao().getAllForVm(vm.getId()));
        }
        for (Disk disk : vm.getDiskMap().values()) {
            if (DiskStorageType.IMAGE == disk.getDiskStorageType() && !disk.isShareable()) {
                DiskImage diskImage = (DiskImage) disk;
                diskImage.setParentId(VmTemplateHandler.BlankVmTemplateId);
                diskImage.setImageTemplateId(VmTemplateHandler.BlankVmTemplateId);
                diskImage.setStorageIds(new ArrayList<Guid>(Arrays.asList(storageDomainId)));
                DiskImage diskForVolumeInfo = getDiskForVolumeInfo(diskImage);
                diskImage.setvolumeFormat(diskForVolumeInfo.getVolumeFormat());
                diskImage.setVolumeType(diskForVolumeInfo.getVolumeType());
                VDSReturnValue vdsReturnValue = Backend
                            .getInstance()
                            .getResourceManager()
                            .RunVdsCommand(
                                    VDSCommandType.GetImageInfo,
                                    new GetImageInfoVDSCommandParameters(storagePoolId, storageDomainId, diskImage
                                            .getId(), diskImage.getImageId()));
                if (vdsReturnValue != null && vdsReturnValue.getSucceeded()) {
                    DiskImage fromVdsm = (DiskImage) vdsReturnValue.getReturnValue();
                    diskImage.setActualSizeInBytes(fromVdsm.getActualSizeInBytes());
                }
                AllVmImages.add(diskImage);
            }
        }
        if (StringUtils.isEmpty(vm.getVmtName())) {
            VmTemplate t = getDbFacade().getVmTemplateDao().get(vm.getVmtGuid());
            vm.setVmtName(t.getName());
        }
        getVm().setVmtGuid(VmTemplateHandler.BlankVmTemplateId);
        String vmMeta = ovfManager.ExportVm(vm, AllVmImages, ClusterUtils.getCompatibilityVersion(vm));
        List<Guid> imageGroupIds = new ArrayList<Guid>();
        for(Disk disk : vm.getDiskMap().values()) {
            if(disk.getDiskStorageType() == DiskStorageType.IMAGE) {
                imageGroupIds.add(disk.getId());
            }
        }
        vmsAndMetaDictionary
                    .put(vm.getId(), new KeyValuePairCompat<String, List<Guid>>(vmMeta, imageGroupIds));
        UpdateVMVDSCommandParameters tempVar = new UpdateVMVDSCommandParameters(storagePoolId, vmsAndMetaDictionary);
        tempVar.setStorageDomainId(storageDomainId);
        return Backend.getInstance().getResourceManager().RunVdsCommand(VDSCommandType.UpdateVM, tempVar)
                .getSucceeded();
    }

    @Override
    protected void moveOrCopyAllImageGroups() {
        // Disks
        moveOrCopyAllImageGroups(getVm().getId(), getDisksBasedOnImage());
        // Memory volumes
        copyAllMemoryImages(getVm().getId());
    }

    private List<DiskImage> getDisksBasedOnImage() {
        if (disksImages == null) {
            disksImages = ImagesHandler.filterImageDisks(getVm().getDiskMap().values(), true, false);
        }
        return disksImages;
    }

    protected void copyAllMemoryImages(Guid containerID) {
        for (Snapshot snapshot : snapshotsWithMemory) {
            List<Guid> guids = GuidUtils.getGuidListFromString(snapshot.getMemoryVolume());

            // copy the memory dump image
            VdcReturnValueBase vdcRetValue = Backend.getInstance().runInternalAction(
                    VdcActionType.CopyImageGroup,
                    buildMoveOrCopyImageGroupParametersForMemoryDumpImage(
                            containerID, guids.get(0), guids.get(2), guids.get(3)),
                    ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));
            if (!vdcRetValue.getSucceeded()) {
                throw new VdcBLLException(vdcRetValue.getFault().getError(), "Failed during ExportVmCommand");
            }
            getReturnValue().getVdsmTaskIdList().addAll(vdcRetValue.getVdsmTaskIdList());

            // copy the memory configuration (of the VM) image
            vdcRetValue = Backend.getInstance().runInternalAction(
                    VdcActionType.CopyImageGroup,
                    buildMoveOrCopyImageGroupParametersForMemoryConfImage(
                            containerID, guids.get(0), guids.get(4), guids.get(5)),
                    ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));
            if (!vdcRetValue.getSucceeded()) {
                throw new VdcBLLException(vdcRetValue.getFault().getError(), "Failed during ExportVmCommand");
            }
            getReturnValue().getVdsmTaskIdList().addAll(vdcRetValue.getVdsmTaskIdList());
        }
    }

    private MoveOrCopyImageGroupParameters buildMoveOrCopyImageGroupParametersForMemoryDumpImage(
            Guid containerID, Guid storageDomainId, Guid imageId, Guid volumeId) {
        MoveOrCopyImageGroupParameters params = new MoveOrCopyImageGroupParameters(containerID, imageId,
                volumeId, getParameters().getStorageDomainId(), getMoveOrCopyImageOperation());
        params.setParentCommand(getActionType());
        params.setCopyVolumeType(CopyVolumeType.LeafVol);
        params.setForceOverride(getParameters().getForceOverride());
        params.setSourceDomainId(storageDomainId);
        params.setEntityInfo(getParameters().getEntityInfo());
        params.setParentParameters(getParameters());

        // if the data domains are block based storage, the memory volume type is preallocated
        // so we need to use copy collapse in order to convert it to be sparsed in the export domain
        if (getStoragePool().getStorageType().isBlockDomain()) {
            params.setUseCopyCollapse(true);
            params.setVolumeType(VolumeType.Sparse);
            params.setVolumeFormat(VolumeFormat.RAW);
        }

        return params;
    }

    private MoveOrCopyImageGroupParameters buildMoveOrCopyImageGroupParametersForMemoryConfImage(
            Guid containerID, Guid storageDomainId, Guid imageId, Guid volumeId) {
        MoveOrCopyImageGroupParameters params = new MoveOrCopyImageGroupParameters(containerID, imageId,
                volumeId, getParameters().getStorageDomainId(), getMoveOrCopyImageOperation());
        params.setParentCommand(getActionType());
        // This volume is always of type 'sparse' and format 'cow' so no need to convert,
        // and there're no snapshots for it so no reason to use copy collapse
        params.setUseCopyCollapse(false);
        params.setEntityInfo(getParameters().getEntityInfo());
        params.setCopyVolumeType(CopyVolumeType.LeafVol);
        params.setForceOverride(getParameters().getForceOverride());
        params.setParentParameters(getParameters());
        params.setSourceDomainId(storageDomainId);
        return params;
    }

    @Override
    protected void moveOrCopyAllImageGroups(Guid containerID, Iterable<DiskImage> disks) {
        for (DiskImage disk : disks) {
            VdcReturnValueBase vdcRetValue = Backend.getInstance().runInternalAction(
                    VdcActionType.CopyImageGroup,
                    buildMoveOrCopyImageGroupParametersForDisk(containerID, disk),
                    ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));
            if (!vdcRetValue.getSucceeded()) {
                throw new VdcBLLException(vdcRetValue.getFault().getError(), "Failed during ExportVmCommand");
            }

            getReturnValue().getVdsmTaskIdList().addAll(vdcRetValue.getInternalVdsmTaskIdList());
        }
    }

    private MoveOrCopyImageGroupParameters buildMoveOrCopyImageGroupParametersForDisk(Guid containerID, DiskImage disk) {
        MoveOrCopyImageGroupParameters params = new MoveOrCopyImageGroupParameters(containerID, disk.getId(),
                disk.getImageId(), getParameters().getStorageDomainId(), getMoveOrCopyImageOperation());
        params.setParentCommand(getActionType());
        params.setEntityInfo(getParameters().getEntityInfo());
        params.setUseCopyCollapse(getParameters().getCopyCollapse());
        DiskImage diskForVolumeInfo = getDiskForVolumeInfo(disk);
        params.setVolumeFormat(diskForVolumeInfo.getVolumeFormat());
        params.setVolumeType(diskForVolumeInfo.getVolumeType());
        params.setCopyVolumeType(CopyVolumeType.LeafVol);
        params.setForceOverride(getParameters().getForceOverride());
        params.setParentParameters(getParameters());
        return params;
    }

    /**
     * Return the correct disk to get the volume info (type & allocation) from. For copy collapse it's the ancestral
     * disk of the given disk, and otherwise it's the disk itself.
     *
     * @param disk
     *            The disk for which to get the disk with the info.
     * @return The disk with the correct volume info.
     */
    private DiskImage getDiskForVolumeInfo(DiskImage disk) {
        if (getParameters().getCopyCollapse()) {
            DiskImage ancestor = getDiskImageDao().getAncestor(disk.getImageId());
            if (ancestor == null) {
                log.warnFormat("Can't find ancestor of Disk with ID {0}, using original disk for volume info.",
                        disk.getImageId());
                ancestor = disk;
            }

            return ancestor;
        } else {
            return disk;
        }
    }

    /**
     * Check that vm is in export domain
     * @return
     */
    protected boolean checkVmInStorageDomain() {
        boolean retVal = true;
        GetAllFromExportDomainQueryParameters tempVar = new GetAllFromExportDomainQueryParameters(getVm()
                .getStoragePoolId(), getParameters().getStorageDomainId());
        VdcQueryReturnValue qretVal = Backend.getInstance().runInternalQuery(VdcQueryType.GetVmsFromExportDomain,
                tempVar);

        if (qretVal.getSucceeded()) {
            ArrayList<VM> vms = qretVal.getReturnValue();
            for (VM vm : vms) {
                if (vm.getId().equals(getVm().getId())) {
                    if (!getParameters().getForceOverride()) {
                        addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_GUID_ALREADY_EXIST);
                        retVal = false;
                        break;
                    }
                } else if (vm.getName().equals(getVm().getName())) {
                    addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_NAME_ALREADY_USED);
                    retVal = false;
                    break;
                }
            }
        }
        return retVal;
    }

    public static boolean CheckTemplateInStorageDomain(Guid storagePoolId, Guid storageDomainId, final Guid tmplId) {
        boolean retVal = false;
        GetAllFromExportDomainQueryParameters tempVar = new GetAllFromExportDomainQueryParameters(storagePoolId,
                storageDomainId);
        VdcQueryReturnValue qretVal = Backend.getInstance().runInternalQuery(VdcQueryType.GetTemplatesFromExportDomain,
                tempVar);

        if (qretVal.getSucceeded()) {
            if (!VmTemplateHandler.BlankVmTemplateId.equals(tmplId)) {
                Map<VmTemplate, List<DiskImage>> templates = qretVal.getReturnValue();
                VmTemplate tmpl = LinqUtils.firstOrNull(templates.keySet(), new Predicate<VmTemplate>() {
                    @Override
                    public boolean eval(VmTemplate vmTemplate) {
                        return vmTemplate.getId().equals(tmplId);
                    }
                });

                retVal = tmpl != null;
            } else {
                retVal = true;
            }
        }
        return retVal;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            return getSucceeded() ? AuditLogType.IMPORTEXPORT_STARTING_EXPORT_VM
                    : AuditLogType.IMPORTEXPORT_EXPORT_VM_FAILED;

        case END_SUCCESS:
            return getSucceeded() ? AuditLogType.IMPORTEXPORT_EXPORT_VM : AuditLogType.IMPORTEXPORT_EXPORT_VM_FAILED;

        case END_FAILURE:
            return AuditLogType.IMPORTEXPORT_EXPORT_VM_FAILED;
        }
        return super.getAuditLogTypeValue();
    }

    protected boolean updateVmImSpm() {
        Map<Guid, KeyValuePairCompat<String, List<Guid>>> metaDictionary =
                new HashMap<Guid, KeyValuePairCompat<String, List<Guid>>>();
        OvfDataUpdater.getInstance().loadVmData(getVm());
        VmHandler.updateDisksFromDb(getVm());
        OvfDataUpdater.getInstance().buildMetadataDictionaryForVm(getVm(), metaDictionary);
        return OvfDataUpdater.getInstance().executeUpdateVmInSpmCommand(getVm().getStoragePoolId(),
                metaDictionary, getParameters().getStorageDomainId());
    }

    @Override
    protected void endSuccessfully() {
        endActionOnAllImageGroups();
        VM vm = getVm();
        VmHandler.UnLockVm(vm);
        endDiskRelatedActions(vm);
        if (getParameters().getCopyCollapse()) {
            endCopyCollapseOperations(vm);
        } else {
            updateSnapshotOvf(vm);
        }
        setSucceeded(true);
    }

    private void endDiskRelatedActions(VM vm) {
        VmHandler.updateDisksFromDb(vm);
        VmDeviceUtils.setVmDevices(vm.getStaticData());
    }

    private void endCopyCollapseOperations(VM vm) {
        vm.setVmtGuid(VmTemplateHandler.BlankVmTemplateId);
        vm.setVmtName(null);
        Snapshot activeSnapshot = getDbFacade().getSnapshotDao().get(
                getDbFacade().getSnapshotDao().getId(vm.getId(), SnapshotType.ACTIVE));
        vm.setSnapshots(Arrays.asList(activeSnapshot));
        updateCopyVmInSpm(getVm().getStoragePoolId(),
                vm, getParameters()
                        .getStorageDomainId());
    }

    private void updateSnapshotOvf(VM vm) {
        vm.setSnapshots(getDbFacade().getSnapshotDao().getAllWithConfiguration(getVm().getId()));
        updateVmImSpm();
    }

    protected void endSuccessfullySynchronous() {
        VM vm = getVm();
        VmHandler.UnLockVm(vm);
        this.updateSnapshotOvf(vm);
        setSucceeded(true);
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        return Collections.singletonMap(getVmId().toString(),
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.VM, VdcBllMessages.ACTION_TYPE_FAILED_OBJECT_LOCKED));
    }

    @Override
    protected void endWithFailure() {
        endActionOnAllImageGroups();
        VM vm = getVm();
        VmHandler.UnLockVm(vm);
        VmHandler.updateDisksFromDb(vm);
        setSucceeded(true);
    }

    @Override
    public Map<String, String> getJobMessageProperties() {
        if (jobProperties == null) {
            jobProperties = super.getJobMessageProperties();
            jobProperties.put(VdcObjectType.VM.name().toLowerCase(),
                    (getVmName() == null) ? "" : getVmName());
        }
        return jobProperties;
    }
}
