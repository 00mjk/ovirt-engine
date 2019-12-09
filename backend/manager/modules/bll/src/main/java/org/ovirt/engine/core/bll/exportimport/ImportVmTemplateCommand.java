package org.ovirt.engine.core.bll.exportimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.ovirt.engine.core.bll.DisableInPrepareMode;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.network.vm.VnicProfileHelper;
import org.ovirt.engine.core.bll.profiles.CpuProfileHelper;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageDependent;
import org.ovirt.engine.core.bll.storage.disk.image.ImagesHandler;
import org.ovirt.engine.core.bll.storage.utils.BlockStorageDiscardFunctionalityHelper;
import org.ovirt.engine.core.bll.validator.VmNicMacsUtils;
import org.ovirt.engine.core.bll.validator.storage.DiskImagesValidator;
import org.ovirt.engine.core.bll.validator.storage.StorageDomainValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ActionParametersBase;
import org.ovirt.engine.core.common.action.ActionReturnValue;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.ImportVmTemplateParameters;
import org.ovirt.engine.core.common.action.MoveOrCopyImageGroupParameters;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmTemplateStatus;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkStatistics;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.businessentities.storage.CopyVolumeType;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskImageDynamic;
import org.ovirt.engine.core.common.businessentities.storage.DiskVmElement;
import org.ovirt.engine.core.common.businessentities.storage.ImageDbOperationScope;
import org.ovirt.engine.core.common.businessentities.storage.ImageOperation;
import org.ovirt.engine.core.common.businessentities.storage.ImageStorageDomainMap;
import org.ovirt.engine.core.common.businessentities.storage.QemuImageInfo;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.businessentities.storage.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.storage.VolumeType;
import org.ovirt.engine.core.common.errors.EngineError;
import org.ovirt.engine.core.common.errors.EngineException;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.queries.GetAllFromExportDomainQueryParameters;
import org.ovirt.engine.core.common.queries.QueryReturnValue;
import org.ovirt.engine.core.common.queries.QueryType;
import org.ovirt.engine.core.common.utils.CompatibilityVersionUtils;
import org.ovirt.engine.core.common.validation.group.ImportClonedEntity;
import org.ovirt.engine.core.common.validation.group.ImportEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dao.BaseDiskDao;
import org.ovirt.engine.core.dao.DiskImageDynamicDao;
import org.ovirt.engine.core.dao.DiskVmElementDao;
import org.ovirt.engine.core.dao.ImageDao;
import org.ovirt.engine.core.dao.network.VmNetworkStatisticsDao;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;
import org.ovirt.engine.core.vdsbroker.vdsbroker.CloudInitHandler;

@DisableInPrepareMode
@NonTransactiveCommandAttribute(forceCompensation = true)
public class ImportVmTemplateCommand<T extends ImportVmTemplateParameters> extends MoveOrCopyTemplateCommand<T>
        implements QuotaStorageDependent {

    @Inject
    private VmNicMacsUtils vmNicMacsUtils;
    @Inject
    private CpuProfileHelper cpuProfileHelper;
    @Inject
    private BlockStorageDiscardFunctionalityHelper discardHelper;
    @Inject
    private BaseDiskDao baseDiskDao;
    @Inject
    private DiskImageDynamicDao diskImageDynamicDao;
    @Inject
    private DiskVmElementDao diskVmElementDao;
    @Inject
    private VmNetworkStatisticsDao vmNetworkStatisticsDao;
    @Inject
    protected ImageDao imageDao;
    @Inject
    private ImportUtils importUtils;
    @Inject
    private CloudInitHandler cloudInitHandler;

    private final Map<Guid, Guid> originalDiskIdMap = new HashMap<>();
    private final Map<Guid, Guid> originalDiskImageIdMap = new HashMap<>();

    private Version effectiveCompatibilityVersion;
    private Guid sourceTemplateId;
    private Map<Guid, QemuImageInfo> diskImageInfoMap = new HashMap<>();

    public ImportVmTemplateCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
        setVmTemplate(parameters.getVmTemplate());
        parameters.setEntityInfo(new EntityInfo(VdcObjectType.VmTemplate, getVmTemplateId()));
        setStoragePoolId(parameters.getStoragePoolId());
        setClusterId(parameters.getClusterId());
        setStorageDomainId(parameters.getStorageDomainId());
    }

    @Override
    public void init() {
        super.init();
        setEffectiveCompatibilityVersion(CompatibilityVersionUtils.getEffective(getVmTemplate(), this::getCluster));
        importUtils.updateGraphicsDevices(getVmTemplate(), getEffectiveCompatibilityVersion());
        vmHandler.updateMaxMemorySize(getVmTemplate(), getEffectiveCompatibilityVersion());
    }

    public ImportVmTemplateCommand(Guid commandId) {
        super(commandId);
    }

    public Version getEffectiveCompatibilityVersion() {
        return effectiveCompatibilityVersion;
    }

    public void setEffectiveCompatibilityVersion(Version effectiveCompatibilityVersion) {
        this.effectiveCompatibilityVersion = effectiveCompatibilityVersion;
    }

    @Override
    protected boolean validate() {
        if (getVmTemplate() == null) {
            return failValidation(EngineMessage.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        }
        if (getCluster() == null) {
            return failValidation(EngineMessage.ACTION_TYPE_FAILED_CLUSTER_CAN_NOT_BE_EMPTY);
        }
        if (!getCluster().getStoragePoolId().equals(getStoragePoolId())) {
            return failValidation(EngineMessage.ACTION_TYPE_FAILED_CLUSTER_IS_NOT_VALID);
        }
        setDescription(getVmTemplateName());

        // check that the storage pool is valid
        if (!validate(createStoragePoolValidator().existsAndUp())
                || !validateTemplateArchitecture()
                || !isClusterCompatible()) {
            return false;
        }

        if (!validateSourceStorageDomain()) {
            return false;
        }

        sourceTemplateId = getVmTemplateId();
        if (getParameters().isImportAsNewEntity()) {
            initImportClonedTemplate();
        }

        VmTemplate duplicateTemplate = vmTemplateDao.get(getParameters().getVmTemplate().getId());
        // check that the template does not exists in the target domain
        if (duplicateTemplate != null) {
            return failValidation(EngineMessage.VMT_CANNOT_IMPORT_TEMPLATE_EXISTS,
                    String.format("$TemplateName %1$s", duplicateTemplate.getName()));
        }
        if (getVmTemplate().isBaseTemplate() && isVmTemplateWithSameNameExist()) {
            return failValidation(EngineMessage.VM_CANNOT_IMPORT_TEMPLATE_NAME_EXISTS);
        }

        if (!validateNoDuplicateDiskImages(getImages())) {
            return false;
        }

        if (getImages() != null && !getImages().isEmpty() && !getParameters().isImagesExistOnTargetStorageDomain()) {
            if (!validateSpaceRequirements(getImages())) {
                return false;
            }
        }

        List<VmNetworkInterface> vmNetworkInterfaces = getVmTemplate().getInterfaces();
        vmNicMacsUtils.replaceInvalidEmptyStringMacAddressesWithNull(vmNetworkInterfaces);
        if (!validate(vmNicMacsUtils.validateMacAddress(vmNetworkInterfaces))) {
            return false;
        }

        // if this is a template version, check base template exist
        if (!getVmTemplate().isBaseTemplate()) {
            VmTemplate baseTemplate = vmTemplateDao.get(getVmTemplate().getBaseTemplateId());
            if (baseTemplate == null) {
                return failValidation(EngineMessage.VMT_CANNOT_IMPORT_TEMPLATE_VERSION_MISSING_BASE);
            }
        }

        if (!setAndValidateDiskProfiles()) {
            return false;
        }

        if (!setAndValidateCpuProfile()) {
            return false;
        }

        if (!validate(vmHandler.validateMaxMemorySize(getVmTemplate(), getEffectiveCompatibilityVersion()))) {
            return false;
        }

        List<EngineMessage> msgs = cloudInitHandler.validate(getVmTemplate().getVmInit());
        if (!CollectionUtils.isEmpty(msgs)) {
            return failValidation(msgs);
        }

        return true;
    }

    protected boolean validateSourceStorageDomain() {
        Guid sourceDomainId = getParameters().getSourceDomainId();
        StorageDomain sourceDomain = !Guid.isNullOrEmpty(sourceDomainId) ?
                storageDomainDao.getForStoragePool(sourceDomainId, getStoragePool().getId()) :
                null;

        if (!validate(new StorageDomainValidator(sourceDomain).isDomainExistAndActive())) {
            return false;
        }

        if ((sourceDomain.getStorageDomainType() != StorageDomainType.ImportExport)
                && !getParameters().isImagesExistOnTargetStorageDomain()) {
            return failValidation(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_TYPE_ILLEGAL);
        }

        if (!getParameters().isImagesExistOnTargetStorageDomain()) {
            // Set the template images from the Export domain and change each image id storage is to the import domain
            GetAllFromExportDomainQueryParameters tempVar = new GetAllFromExportDomainQueryParameters(getParameters()
                    .getStoragePoolId(), getParameters().getSourceDomainId());
            QueryReturnValue qretVal = runInternalQuery(
                    QueryType.GetTemplatesFromExportDomain, tempVar);
            if (!qretVal.getSucceeded()) {
                return false;
            }

            Map<VmTemplate, List<DiskImage>> templates = qretVal.getReturnValue();
            ArrayList<DiskImage> images = new ArrayList<>();
            for (Map.Entry<VmTemplate, List<DiskImage>> entry : templates.entrySet()) {
                if (entry.getKey().getId().equals(getVmTemplate().getId())) {
                    images = new ArrayList<>(entry.getValue());
                    getVmTemplate().setInterfaces(entry.getKey().getInterfaces());
                    getVmTemplate().setOvfVersion(entry.getKey().getOvfVersion());
                    break;
                }
            }
            getParameters().setImages(images);
            getVmTemplate().setImages(images);
            ensureDomainMap(getImages(), getParameters().getDestDomainId());
            Map<Guid, DiskImage> imageMap = new HashMap<>();
            for (DiskImage image : images) {
                if (Guid.Empty.equals(image.getVmSnapshotId())) {
                    return failValidation(EngineMessage.ACTION_TYPE_FAILED_CORRUPTED_VM_SNAPSHOT_ID);
                }

                StorageDomain storageDomain = storageDomainDao.getForStoragePool(
                        imageToDestinationDomainMap.get(image.getId()),
                        getStoragePool().getId());

                StorageDomainValidator validator = new StorageDomainValidator(storageDomain);
                if (!validate(validator.isDomainExistAndActive()) ||
                        !validate(validator.domainIsValidDestination())) {
                    return false;
                }

                StorageDomainStatic targetDomain = storageDomain.getStorageStaticData();
                changeRawToCowIfSparseOnBlockDevice(targetDomain.getStorageType(), image);
                if (!ImagesHandler.checkImageConfiguration(targetDomain, image,
                        getReturnValue().getValidationMessages())) {
                    return false;
                }

                image.setStoragePoolId(getParameters().getStoragePoolId());
                image.setStorageIds(new ArrayList<>(Collections.singletonList(storageDomain.getId())));
                imageMap.put(image.getImageId(), image);
            }
            getVmTemplate().setDiskImageMap(imageMap);
        }

        return true;
    }

    @Override
    protected void setActionMessageParameters() {
        addValidationMessage(EngineMessage.VAR__ACTION__IMPORT);
        addValidationMessage(EngineMessage.VAR__TYPE__VM_TEMPLATE);
    }

    protected boolean isClusterCompatible () {
        if (getCluster().getArchitecture() != getVmTemplate().getClusterArch()) {
            addValidationMessage(EngineMessage.ACTION_TYPE_FAILED_VM_CANNOT_IMPORT_TEMPLATE_ARCHITECTURE_NOT_SUPPORTED_BY_CLUSTER);
            return false;
        }
        return true;
    }

    private boolean validateTemplateArchitecture() {
        if (getVmTemplate().getClusterArch() == ArchitectureType.undefined) {
            addValidationMessage(EngineMessage.ACTION_TYPE_FAILED_VM_CANNOT_IMPORT_TEMPLATE_WITH_NOT_SUPPORTED_ARCHITECTURE);
            return false;
        }
        return true;
    }

    protected boolean isVmTemplateWithSameNameExist() {
        return vmTemplateDao.getByName(getParameters().getVmTemplate().getName(),
                getParameters().getStoragePoolId(),
                null,
                false) != null;
    }

    private void initImportClonedTemplate() {
        Guid newTemplateId = Guid.newGuid();
        getParameters().getVmTemplate().setId(newTemplateId);
        for (VmNetworkInterface iface : getParameters().getVmTemplate().getInterfaces()) {
            iface.setId(Guid.newGuid());
        }
        // cloned template is always base template, as its a new entity
        getParameters().getVmTemplate().setBaseTemplateId(newTemplateId);
    }

    private void initImportClonedTemplateDisks() {
        for (DiskImage image : getImages()) {
            // Update the virtual size with value queried from 'qemu-img info'
            updateDiskSizeByQcowImageInfo(image);
            if (getParameters().isImportAsNewEntity()) {
                generateNewDiskId(image);
                updateManagedDeviceMap(image, getVmTemplate().getManagedDeviceMap());
            } else {
                originalDiskIdMap.put(image.getId(), image.getId());
                originalDiskImageIdMap.put(image.getId(), image.getImageId());
            }
        }
    }

    protected boolean validateNoDuplicateDiskImages(Collection<DiskImage> images) {
        if (!getParameters().isImportAsNewEntity() && !getParameters().isImagesExistOnTargetStorageDomain()) {
            DiskImagesValidator diskImagesValidator = new DiskImagesValidator(images);
            return validate(diskImagesValidator.diskImagesAlreadyExist());
        }

        return true;
    }

    protected List<DiskImage> getImages() {
        return getParameters().getImages();
    }

    /**
     * Change the image format to {@link VolumeFormat#COW} in case the SD is a block device and the image format is
     * {@link VolumeFormat#RAW} and the type is {@link VolumeType#Sparse}.
     *
     * @param storageType
     *            The domain type.
     * @param image
     *            The image to check and change if needed.
     */
    private void changeRawToCowIfSparseOnBlockDevice(StorageType storageType, DiskImage image) {
        if (storageType.isBlockDomain()
                && image.getVolumeFormat() == VolumeFormat.RAW
                && image.getVolumeType() == VolumeType.Sparse) {
            image.setVolumeFormat(VolumeFormat.COW);
        }
    }

    private boolean validateLeaseStorageDomain(Guid leaseStorageDomainId) {
        StorageDomain domain = storageDomainDao.getForStoragePool(leaseStorageDomainId, getStoragePoolId());
        StorageDomainValidator validator = new StorageDomainValidator(domain);
        return validate(validator.isDomainExistAndActive()) && validate(validator.isDataDomain());
    }

    private Guid getVmLeaseToDefaultStorageDomain() {
        return storageDomainStaticDao.getAllForStoragePool(getStoragePoolId()).stream()
                .map(StorageDomainStatic::getId)
                .filter(this::validateLeaseStorageDomain)
                .findFirst()
                .orElse(null);
    }

    private boolean shouldAddLease(VmTemplate vm) {
        return vm.getLeaseStorageDomainId() != null;
    }

    private void handleVmLease() {
        Guid importedLeaseStorageDomainId = getVmTemplate().getLeaseStorageDomainId();
        if (importedLeaseStorageDomainId == null) {
            return;
        }
        if (!getVmTemplate().isAutoStartup() || !shouldAddLease(getVmTemplate())) {
            getVmTemplate().setLeaseStorageDomainId(null);
            return;
        }
        if (validateLeaseStorageDomain(importedLeaseStorageDomainId)) {
            return;
        }
        getVmTemplate().setLeaseStorageDomainId(getVmLeaseToDefaultStorageDomain());
        if (getVmTemplate().getLeaseStorageDomainId() == null) {
            auditLog(this, AuditLogType.CANNOT_IMPORT_VM_TEMPLATE_WITH_LEASE_STORAGE_DOMAIN);
        } else {
            log.warn("Setting the lease for the VM Template '{}' to the storage domain '{}', because the storage domain '{}' is unavailable",
                    getVmTemplate().getId(), getVmTemplate().getLeaseStorageDomainId(), importedLeaseStorageDomainId);
        }
    }

    @Override
    protected void executeCommand() {
        TransactionSupport.executeInNewTransaction(() -> {
            handleVmLease();
            initImportClonedTemplateDisks();
            addVmTemplateToDb();
            addPermissionsToDB();
            updateOriginalTemplateNameOnDerivedVms();
            addVmInterfaces();
            getCompensationContext().stateChanged();
            vmHandler.addVmInitToDB(getVmTemplate().getVmInit());
            return null;
        });

        boolean doesVmTemplateContainImages = !getImages().isEmpty();
        if (doesVmTemplateContainImages && !getParameters().isImagesExistOnTargetStorageDomain()) {
            copyImagesToTargetDomain();
        }

        getVmDeviceUtils().addImportedDevices(getVmTemplate(), getParameters().isImportAsNewEntity(), false);

        if (!doesVmTemplateContainImages || getParameters().isImagesExistOnTargetStorageDomain()) {
            endMoveOrCopyCommand();
        }
        discardHelper.logIfDisksWithIllegalPassDiscardExist(getVmTemplateId());
        checkTrustedService();
        incrementDbGeneration();
        setSucceeded(true);
    }

    protected void addPermissionsToDB() {
        // Left empty to be overridden in ImportVmTemplateFromConfigurationCommand
    }

    private void updateOriginalTemplateNameOnDerivedVms() {
        if (!getParameters().isImportAsNewEntity()) {
            // in case it has been renamed
            vmDao.updateOriginalTemplateName(getVmTemplate().getId(), getVmTemplate().getName());
        }
    }

    private void checkTrustedService() {
        if (getVmTemplate().isTrustedService() && !getCluster().supportsTrustedService()) {
            auditLog(this, AuditLogType.IMPORTEXPORT_IMPORT_TEMPLATE_FROM_TRUSTED_TO_UNTRUSTED);
        } else if (!getVmTemplate().isTrustedService() && getCluster().supportsTrustedService()) {
            auditLog(this, AuditLogType.IMPORTEXPORT_IMPORT_TEMPLATE_FROM_UNTRUSTED_TO_TRUSTED);
        }
    }

    protected void copyImagesToTargetDomain() {
        TransactionSupport.executeInNewTransaction(() -> {
            for (DiskImage disk : getImages()) {
                Guid originalDiskId = originalDiskIdMap.get(disk.getId());
                Guid destinationDomain = imageToDestinationDomainMap.get(originalDiskId);

                ActionReturnValue vdcRetValue = runInternalActionWithTasksContext(
                        ActionType.CopyImageGroup,
                        buildMoveOrCopyImageGroupParameters(getVmTemplateId(), disk, originalDiskId, destinationDomain));

                if (!vdcRetValue.getSucceeded()) {
                    throw vdcRetValue.getFault() != null ? new EngineException(vdcRetValue.getFault().getError())
                            : new EngineException(EngineError.ENGINE);
                }

                getReturnValue().getVdsmTaskIdList().addAll(vdcRetValue.getInternalVdsmTaskIdList());
            }
            return null;
        });
    }

    private MoveOrCopyImageGroupParameters buildMoveOrCopyImageGroupParameters(final Guid templateId,
            DiskImage disk,
            Guid originalDiskId,
            Guid destinationDomain) {
        MoveOrCopyImageGroupParameters p =
                new MoveOrCopyImageGroupParameters(templateId,
                        originalDiskId,
                        originalDiskImageIdMap.get(disk.getId()),
                        disk.getId(),
                        disk.getImageId(),
                        destinationDomain,
                        ImageOperation.Copy);

        p.setParentCommand(getActionType());
        p.setUseCopyCollapse(true);
        p.setVolumeType(disk.getVolumeType());
        p.setVolumeFormat(disk.getVolumeFormat());
        p.setCopyVolumeType(CopyVolumeType.SharedVol);
        p.setSourceDomainId(getParameters().getSourceDomainId());
        p.setForceOverride(getParameters().getForceOverride());
        p.setImportEntity(true);
        p.setEntityInfo(new EntityInfo(VdcObjectType.VmTemplate, templateId));
        p.setRevertDbOperationScope(ImageDbOperationScope.IMAGE);
        for (DiskImage diskImage : getParameters().getVmTemplate().getDiskList()) {
            if (originalDiskId.equals(diskImage.getId())) {
                p.setQuotaId(diskImage.getQuotaId());
                p.setDiskProfileId(diskImage.getDiskProfileId());
                break;
            }
        }

        p.setParentParameters(getParameters());
        return p;
    }

    private void addVmTemplateToDb() {
        getVmTemplate().setClusterId(getParameters().getClusterId());

        // if "run on host" field points to a non existent vds (in the current cluster) -> remove field and continue
        if(!vmHandler.validateDedicatedVdsExistOnSameCluster(getVmTemplate()).isValid()) {
            getVmTemplate().setDedicatedVmForVdsList(Collections.emptyList());
        }

        getVmTemplate().setStatus(VmTemplateStatus.Locked);
        getVmTemplate().setQuotaId(getParameters().getQuotaId());
        vmHandler.autoSelectResumeBehavior(getVmTemplate(), getCluster());
        vmTemplateDao.save(getVmTemplate());
        getCompensationContext().snapshotNewEntity(getVmTemplate());
        addDisksToDb();
    }

    protected void addDisksToDb() {
        int count = 1;
        for (DiskImage image : getImages()) {
            image.setActive(true);
            ImageStorageDomainMap map = imagesHandler.saveImage(image);
            getCompensationContext().snapshotNewEntity(image.getImage());
            getCompensationContext().snapshotNewEntity(map);
            if (!baseDiskDao.exists(image.getId())) {
                image.setDiskAlias(ImagesHandler.getSuggestedDiskAlias(image, getVmTemplateName(), count));
                count++;
                baseDiskDao.save(image);
                getCompensationContext().snapshotNewEntity(image);
            }

            DiskImageDynamic diskDynamic = new DiskImageDynamic();
            diskDynamic.setId(image.getImageId());
            diskDynamic.setActualSize(image.getActualSizeInBytes());
            diskImageDynamicDao.save(diskDynamic);

            DiskVmElement dve = DiskVmElement.copyOf(image.getDiskVmElementForVm(sourceTemplateId),
                    image.getId(), getVmTemplateId());
            diskVmElementDao.save(dve);

            getCompensationContext().snapshotNewEntity(diskDynamic);
        }
    }

    private void addVmInterfaces() {
        VnicProfileHelper vnicProfileHelper =
                new VnicProfileHelper(getVmTemplate().getClusterId(),
                        getStoragePoolId(),
                        AuditLogType.IMPORTEXPORT_IMPORT_TEMPLATE_INVALID_INTERFACES);

        for (VmNetworkInterface iface : getVmTemplate().getInterfaces()) {
            if (iface.getId() == null) {
                iface.setId(Guid.newGuid());
            }

            iface.setVmId(getVmTemplateId());
            VmNic nic = new VmNic();
            nic.setId(iface.getId());
            nic.setVmId(getVmTemplateId());
            nic.setName(iface.getName());
            nic.setLinked(iface.isLinked());
            nic.setSpeed(iface.getSpeed());
            nic.setType(iface.getType());

            vnicProfileHelper.updateNicWithVnicProfileForUser(iface, getCurrentUser());
            nic.setVnicProfileId(iface.getVnicProfileId());
            vmNicDao.save(nic);
            getCompensationContext().snapshotNewEntity(nic);

            VmNetworkStatistics iStat = new VmNetworkStatistics();
            nic.setStatistics(iStat);
            iStat.setId(iface.getId());
            iStat.setVmId(getVmTemplateId());
            vmNetworkStatisticsDao.save(iStat);
            getCompensationContext().snapshotNewEntity(iStat);
        }

        vnicProfileHelper.auditInvalidInterfaces(getVmTemplateName());
    }

    @Override
    protected void endMoveOrCopyCommand() {
        vmTemplateHandler.unlockVmTemplate(getVmTemplateId());
        endActionForImageGroups();
        setSucceeded(true);
    }

    private void removeNetwork() {
        List<VmNic> nics = vmNicDao.getAllForTemplate(getVmTemplateId());
        nics.stream().map(VmNic::getId).forEach(vmNicDao::remove);
    }

    private void endActionForImageGroups() {
        for (ActionParametersBase p : getParameters().getImagesParameters()) {
            p.setTaskGroupSuccess(getParameters().getTaskGroupSuccess());
            backend.endAction(ActionType.CopyImageGroup,
                    p,
                    getContext().clone().withoutCompensationContext().withoutExecutionContext().withoutLock());
        }
    }

    @Override
    protected void endWithFailure() {
        removeNetwork();
        endActionForImageGroups();
        vmTemplateDao.remove(getVmTemplateId());
        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            return getSucceeded() ? AuditLogType.IMPORTEXPORT_STARTING_IMPORT_TEMPLATE
                    : AuditLogType.IMPORTEXPORT_IMPORT_TEMPLATE_FAILED;

        case END_SUCCESS:
            return getSucceeded() ? AuditLogType.IMPORTEXPORT_IMPORT_TEMPLATE
                    : AuditLogType.IMPORTEXPORT_IMPORT_TEMPLATE_FAILED;

        default:
            return AuditLogType.IMPORTEXPORT_IMPORT_TEMPLATE_FAILED;
        }
    }

    @Override
    public Guid getVmTemplateId() {
        if (getParameters().isImportAsNewEntity()) {
            return getParameters().getVmTemplate().getId();
        } else {
            return super.getVmTemplateId();
        }
    }

    @Override
    public VmTemplate getVmTemplate() {
        if (getParameters().isImportAsNewEntity()) {
            return getParameters().getVmTemplate();
        } else {
            return super.getVmTemplate();
        }
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        if(getParameters().isImportAsNewEntity()){
            return addValidationGroup(ImportClonedEntity.class);
        }
        return addValidationGroup(ImportEntity.class);
    }

    @Override
    public Map<String, String> getJobMessageProperties() {
        if (jobProperties == null) {
            jobProperties =  super.getJobMessageProperties();
            jobProperties.put(VdcObjectType.VmTemplate.name().toLowerCase(),
                    (getVmTemplateName() == null) ? "" : getVmTemplateName());
            jobProperties.put(VdcObjectType.Cluster.name().toLowerCase(), getClusterName());
        }
        return jobProperties;
    }

    protected boolean setAndValidateDiskProfiles() {
        if (getParameters().getVmTemplate().getDiskList() != null) {
            Map<DiskImage, Guid> map = new HashMap<>();
            for (DiskImage diskImage : getParameters().getVmTemplate().getDiskList()) {
                map.put(diskImage, imageToDestinationDomainMap.get(diskImage.getId()));
            }
            return validate(diskProfileHelper.setAndValidateDiskProfiles(map, getCurrentUser()));
        }
        return true;
    }

    protected boolean setAndValidateCpuProfile() {
        getVmTemplate().setClusterId(getClusterId());
        getVmTemplate().setCpuProfileId(getParameters().getCpuProfileId());
        return validate(cpuProfileHelper.setAndValidateCpuProfile(
                getVmTemplate(),
                getUserIdIfExternal().orElse(null)));
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaStorageConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<>();

        for (DiskImage disk : getParameters().getVmTemplate().getDiskList()) {
            //TODO: handle import more than once;
            list.add(new QuotaStorageConsumptionParameter(
                    disk.getQuotaId(),
                    QuotaConsumptionParameter.QuotaAction.CONSUME,
                    imageToDestinationDomainMap.get(disk.getId()),
                    (double)disk.getSizeInGigabytes()));
        }
        return list;
    }

    private void updateDiskSizeByQcowImageInfo(DiskImage diskImage) {
        QemuImageInfo qemuImageInfo = getQemuImageInfo(diskImage, getParameters().getSourceDomainId());
        if (qemuImageInfo != null) {
            diskImage.setSize(qemuImageInfo.getSize());
        }
        imageDao.update(diskImage.getImage());
    }

    protected QemuImageInfo getQemuImageInfo(DiskImage diskImage, Guid storageId) {
        if (!diskImageInfoMap.containsKey(diskImage.getId())) {
            diskImageInfoMap.put(diskImage.getId(),
                    imagesHandler.getQemuImageInfoFromVdsm(diskImage.getStoragePoolId(),
                            storageId,
                            diskImage.getId(),
                            diskImage.getImageId(),
                            null,
                            true));
        }
        return diskImageInfoMap.get(diskImage.getId());
    }

    /**
     * Updating managed device map of VM, with the new disk {@link Guid}s.<br/>
     * The update of managedDeviceMap is based on the newDiskIdForDisk map,
     * so this method should be called only after newDiskIdForDisk is initialized.
     *
     * @param disk
     *            - The disk which is about to be cloned
     * @param managedDeviceMap
     *            - The managed device map contained in the VM.
     */
    protected void updateManagedDeviceMap(DiskImage disk, Map<Guid, VmDevice> managedDeviceMap) {
        Guid oldDiskId = originalDiskIdMap.get(disk.getId());
        managedDeviceMap.put(disk.getId(), managedDeviceMap.get(oldDiskId));
        managedDeviceMap.remove(oldDiskId);
    }

    /**
     * Cloning a new disk with a new generated id, with the same parameters as <code>disk</code>. Also
     * adding the disk to <code>newDiskGuidForDisk</code> map, so we will be able to link between the new cloned disk
     * and the old disk id.
     *
     * @param disk
     *            - The disk which is about to be cloned
     */
    protected void generateNewDiskId(DiskImage disk) {
        Guid newGuidForDisk = Guid.newGuid();

        originalDiskIdMap.put(newGuidForDisk, disk.getId());
        originalDiskImageIdMap.put(newGuidForDisk, disk.getImageId());
        disk.setId(newGuidForDisk);
        disk.setImageId(Guid.newGuid());
    }

    protected Guid getOriginalDiskIdMap(Guid diskId) {
        return originalDiskIdMap.get(diskId);
    }

    protected Guid getOriginalDiskImageIdMap(Guid diskId) {
        return originalDiskImageIdMap.get(diskId);
    }
}
