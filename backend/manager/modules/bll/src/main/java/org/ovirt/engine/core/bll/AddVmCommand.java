package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.network.MacPoolManager;
import org.ovirt.engine.core.bll.network.VmInterfaceManager;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaSanityParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageDependent;
import org.ovirt.engine.core.bll.quota.QuotaVdsDependent;
import org.ovirt.engine.core.bll.snapshots.SnapshotsManager;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.StorageDomainValidator;
import org.ovirt.engine.core.bll.validator.VmWatchdogValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.CreateSnapshotFromTemplateParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.action.WatchdogParameters;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.ImageType;
import org.ovirt.engine.core.common.businessentities.InstanceType;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.OriginType;
import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmPayload;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmStatistics;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.PermissionDAO;
import org.ovirt.engine.core.dao.VmDynamicDAO;
import org.ovirt.engine.core.dao.VmStaticDAO;
import org.ovirt.engine.core.utils.customprop.ValidationError;
import org.ovirt.engine.core.utils.customprop.VmPropertiesUtils;
import org.ovirt.engine.core.utils.customprop.VmPropertiesUtils.VMCustomProperties;
import org.ovirt.engine.core.utils.linq.All;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;


@DisableInPrepareMode
@LockIdNameAttribute(isReleaseAtEndOfExecute = false)
@NonTransactiveCommandAttribute
public class AddVmCommand<T extends VmManagementParametersBase> extends VmManagementCommandBase<T>
        implements QuotaStorageDependent, QuotaVdsDependent {

    protected HashMap<Guid, DiskImage> diskInfoDestinationMap;
    protected Map<Guid, StorageDomain> destStorages = new HashMap<Guid, StorageDomain>();
    protected Map<Guid, List<DiskImage>> storageToDisksMap;
    private String cachedDiskSharedLockMessage;
    protected Guid instanceTypeId;
    protected InstanceType instanceType;
    protected Guid imageTypeId;
    protected ImageType imageType;
    private Guid vmInterfacesSourceId;
    private VmTemplate vmDisksSource;
    private Guid vmDevicesSourceId;

    private Map<Guid, Guid> srcDiskIdToTargetDiskIdMapping = new HashMap<>();
    private Map<Guid, Guid> srcVmNicIdToTargetVmNicIdMapping = new HashMap<>();

    public AddVmCommand(T parameters) {
        super(parameters);
        // if we came from endAction the VmId is not null
        setVmId((parameters.getVmId().equals(Guid.Empty)) ? Guid.newGuid() : parameters.getVmId());
        setVmName(parameters.getVm().getName());
        parameters.setVmId(getVmId());
        setStorageDomainId(getParameters().getStorageDomainId());
        if (parameters.getVmStaticData() != null) {
            Guid templateIdToUse = getParameters().getVmStaticData().getVmtGuid();

            if (parameters.getVmStaticData().isUseLatestVersion()) {
                VmTemplate latest = getVmTemplateDAO().getTemplateWithLatestVersionInChain(templateIdToUse);

                if (latest != null) {
                    // if not using original template, need to override storage mappings
                    // as it may have different set of disks
                    if (!templateIdToUse.equals(latest.getId())) {
                        getParameters().setDiskInfoDestinationMap(new HashMap<Guid, DiskImage>());
                    }

                    setVmTemplate(latest);
                    templateIdToUse = latest.getId();
                    getParameters().getVmStaticData().setVmtGuid(templateIdToUse);
                }
            }

            setVmTemplateId(templateIdToUse);

            // API backward compatibility
            if (parameters.isSoundDeviceEnabled() == null) {
                parameters.setSoundDeviceEnabled(parameters.getVmStaticData().getVmType() == VmType.Desktop);
            }

            if (parameters.isConsoleEnabled() == null) {
                parameters.setConsoleEnabled(false);
            }

            instanceTypeId = parameters.getVmStaticData().getInstanceTypeId();
            vmDevicesSourceId = (instanceTypeId != null) ?
                    instanceTypeId : parameters.getVmStaticData().getVmtGuid();
            imageTypeId = parameters.getVmStaticData().getImageTypeId();
            vmInterfacesSourceId = parameters.getVmStaticData().getVmtGuid();
            vmDisksSource = getVmTemplate();
        }

        parameters.setEntityInfo(new EntityInfo(VdcObjectType.VM, getVmId()));

        // override values here for canDoACtion to run with correct values, has to come before init-disks
        updateVmObject();

        initTemplateDisks();
        initStoragePoolId();
        diskInfoDestinationMap = getParameters().getDiskInfoDestinationMap();
        if (diskInfoDestinationMap == null) {
            diskInfoDestinationMap = new HashMap<Guid, DiskImage>();
        }

    }

    protected AddVmCommand(Guid commandId) {
        super(commandId);
    }

    @Override
    protected Map<String, Pair<String, String>> getSharedLocks() {
        Map<String, Pair<String, String>> locks = new HashMap<String, Pair<String, String>>();
        locks.put(getVmTemplateId().toString(),
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.TEMPLATE, getTemplateSharedLockMessage()));
        for (DiskImage image: getImagesToCheckDestinationStorageDomains()) {
            locks.put(image.getId().toString(),
                    LockMessagesMatchUtil.makeLockingPair(LockingGroup.DISK, getDiskSharedLockMessage()));
        }
        return locks;
    }

    private String getTemplateSharedLockMessage() {
        return new StringBuilder(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_IS_USED_FOR_CREATE_VM.name())
                .append(String.format("$VmName %1$s", getVmName()))
                .toString();
    }

    private String getDiskSharedLockMessage() {
        if (cachedDiskSharedLockMessage == null) {
            cachedDiskSharedLockMessage = new StringBuilder(VdcBllMessages.ACTION_TYPE_FAILED_DISK_IS_USED_FOR_CREATE_VM.name())
            .append(String.format("$VmName %1$s", getVmName()))
            .toString();
        }
        return cachedDiskSharedLockMessage;
    }
    protected InstanceType getInstanceType() {
        if (instanceType == null && instanceTypeId != null) {
            instanceType = getVmTemplateDAO().getInstanceType(instanceTypeId);
        }
        return instanceType;
    }

    protected ImageType getImageType() {
        if (imageType == null && imageTypeId != null) {
            imageType = getVmTemplateDAO().getImageType(imageTypeId);
        }
        return imageType;
    }

    protected void initStoragePoolId() {
        if (getVdsGroup() != null) {
            setStoragePoolId(getVdsGroup().getStoragePoolId() != null ? getVdsGroup().getStoragePoolId()
                    : Guid.Empty);
        }
    }

    protected void initTemplateDisks() {
        if (vmDisksSource != null) {
            VmTemplateHandler.updateDisksFromDb(vmDisksSource);
        }
    }

    private Guid _vmSnapshotId = Guid.Empty;

    protected Guid getVmSnapshotId() {
        return _vmSnapshotId;
    }

    protected List<VmNic> _vmInterfaces;

    protected List<VmNic> getVmInterfaces() {
        if (_vmInterfaces == null) {
            List<VmNic> vmNetworkInterfaces = getVmNicDao().getAllForTemplate(vmInterfacesSourceId);
            _vmInterfaces = vmNetworkInterfaces == null ? new ArrayList<VmNic>() : vmNetworkInterfaces;
        }
        return _vmInterfaces;
    }

    protected Map<Guid, VmDevice> getVmInterfaceDevices() {
        List<VmDevice> vmInterfaceDevicesList = getVmDeviceDao().getVmDeviceByVmIdAndType(vmInterfacesSourceId, VmDeviceGeneralType.INTERFACE);
        Map<Guid, VmDevice> vmInterfaceDevices = new HashMap<>();
        for (VmDevice device : vmInterfaceDevicesList) {
            vmInterfaceDevices.put(device.getDeviceId(), device);
        }
        return vmInterfaceDevices;
    }

    protected List<? extends Disk> _vmDisks;

    protected List<? extends Disk> getVmDisks() {
        if (_vmDisks == null) {
            _vmDisks =
                    DbFacade.getInstance()
                            .getDiskDao()
                            .getAllForVm(vmDisksSource.getId());
        }

        return _vmDisks;
    }

    protected boolean canAddVm(ArrayList<String> reasons, Collection<StorageDomain> destStorages) {
        VmStatic vmStaticFromParams = getParameters().getVmStaticData();
        boolean returnValue =
                canAddVm(reasons, vmStaticFromParams.getName(), getStoragePoolId(), vmStaticFromParams.getPriority());

        if (returnValue) {
            List<ValidationError> validationErrors = validateCustomProperties(vmStaticFromParams);
            if (!validationErrors.isEmpty()) {
                VmPropertiesUtils.getInstance().handleCustomPropertiesError(validationErrors, reasons);
                returnValue = false;
            }
        }

        // check that template image and vm are on the same storage pool
        if (returnValue
                && shouldCheckSpaceInStorageDomains()) {
            if (!getStoragePoolId().equals(getStoragePoolIdFromSourceImageContainer())) {
                reasons.add(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_POOL_NOT_MATCH.toString());
                returnValue = false;
            } else {
                for (StorageDomain domain : destStorages) {
                    StorageDomainValidator storageDomainValidator = new StorageDomainValidator(domain);
                    if (!validate(storageDomainValidator.isDomainExistAndActive())) {
                        return false;
                    }
                    if (!validate(storageDomainValidator.isDomainWithinThresholds())
                            || !validate(storageDomainValidator.isDomainHasSpaceForRequest(getNeededDiskSize(domain.getId())))) {
                        return false;
                    }
                }
            }
        }
        if (returnValue) {
            returnValue = isDedicatedVdsOnSameCluster(vmStaticFromParams);
        }
        return returnValue;
    }

    protected boolean shouldCheckSpaceInStorageDomains() {
        return !getImagesToCheckDestinationStorageDomains().isEmpty()
                && !LinqUtils.firstOrNull(getImagesToCheckDestinationStorageDomains(), new All<DiskImage>())
                        .getImageId().equals(VmTemplateHandler.BLANK_VM_TEMPLATE_ID);
    }

    protected Guid getStoragePoolIdFromSourceImageContainer() {
        return vmDisksSource.getStoragePoolId();
    }

    protected int getNeededDiskSize(Guid domainId) {
        return getBlockSparseInitSizeInGb() * storageToDisksMap.get(domainId).size();
    }

    protected boolean canDoAddVmCommand() {
        boolean returnValue = false;
        returnValue = areParametersLegal(getReturnValue().getCanDoActionMessages());
        // Check if number of monitors passed is legal
        returnValue =
                returnValue && checkNumberOfMonitors() && checkSingleQxlDisplay();

        returnValue =
                returnValue
                        && checkPciAndIdeLimit(getParameters().getVm().getOs(),
                                getVdsGroup().getcompatibility_version(),
                                getParameters().getVmStaticData().getNumOfMonitors(),
                                getVmInterfaces(),
                                getVmDisks(),
                                isVirtioScsiEnabled(),
                                hasWatchdog(),
                                isBalloonEnabled(),
                                getParameters().isSoundDeviceEnabled(),
                                getReturnValue().getCanDoActionMessages())
                        && canAddVm(getReturnValue().getCanDoActionMessages(), destStorages.values())
                        && hostToRunExist();
        return returnValue;
    }

    protected boolean checkSingleQxlDisplay() {
        if (!getParameters().getVmStaticData().getSingleQxlPci()) {
            return true;
        }
        return (VmHandler.isSingleQxlDeviceLegal(getParameters().getVm().getDefaultDisplayType(),
                        getParameters().getVm().getOs(),
                        getReturnValue().getCanDoActionMessages(),
                        getVdsGroup().getcompatibility_version()));
    }

    protected boolean checkNumberOfMonitors() {
        return VmHandler.isNumOfMonitorsLegal(getParameters().getVmStaticData().getDefaultDisplayType(),
                getParameters().getVmStaticData().getNumOfMonitors(),
                getReturnValue().getCanDoActionMessages());
    }

    protected boolean hostToRunExist() {
        if (getParameters().getVmStaticData().getDedicatedVmForVds() != null) {
            if (DbFacade.getInstance().getVdsDao().get(getParameters().getVmStaticData().getDedicatedVmForVds()) == null) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_HOST_NOT_EXIST);
                return false;
            }
        }
        return true;
    }

    public static boolean checkCpuSockets(int num_of_sockets, int cpu_per_socket, String compatibility_version,
                                          List<String> CanDoActionMessages) {
        boolean retValue = true;
        if (retValue
                && (num_of_sockets * cpu_per_socket) > Config.<Integer> getValue(ConfigValues.MaxNumOfVmCpus,
                        compatibility_version)) {
            CanDoActionMessages.add(VdcBllMessages.ACTION_TYPE_FAILED_MAX_NUM_CPU.toString());
            retValue = false;
        }
        if (retValue
                && num_of_sockets > Config.<Integer> getValue(ConfigValues.MaxNumOfVmSockets, compatibility_version)) {
            CanDoActionMessages.add(VdcBllMessages.ACTION_TYPE_FAILED_MAX_NUM_SOCKETS.toString());
            retValue = false;
        }
        if (retValue
                && cpu_per_socket > Config.<Integer> getValue(ConfigValues.MaxNumOfCpuPerSocket, compatibility_version)) {
            CanDoActionMessages.add(VdcBllMessages.ACTION_TYPE_FAILED_MAX_CPU_PER_SOCKET.toString());
            retValue = false;
        }
        if (retValue && cpu_per_socket < 1) {
            CanDoActionMessages.add(VdcBllMessages.ACTION_TYPE_FAILED_MIN_CPU_PER_SOCKET.toString());
            retValue = false;
        }
        if (retValue && num_of_sockets < 1) {
            CanDoActionMessages.add(VdcBllMessages.ACTION_TYPE_FAILED_MIN_NUM_SOCKETS.toString());
            retValue = false;
        }
        return retValue;
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(CreateEntity.class);
        return super.getValidationGroups();
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ADD);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM);
    }

    @Override
    protected boolean canDoAction() {
        if (getVmTemplate() == null) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_DOES_NOT_EXIST);
        }

        if (getVmTemplate().isDisabled()) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_IS_DISABLED);
        }

        // A VM cannot be added in a cluster without a defined architecture
        if (getVdsGroup().getArchitecture() == ArchitectureType.undefined) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_CLUSTER_UNDEFINED_ARCHITECTURE);
        }

        if (!buildAndCheckDestStorageDomains()) {
            return false;
        }
        // otherwise..
        storageToDisksMap =
                ImagesHandler.buildStorageToDiskMap(getImagesToCheckDestinationStorageDomains(),
                        diskInfoDestinationMap);

        if (!canDoAddVmCommand()) {
            return false;
        }

        VM vmFromParams = getParameters().getVm();

        // check if the selected template is compatible with Cluster architecture.
        if (!getVmTemplate().getId().equals(VmTemplateHandler.BLANK_VM_TEMPLATE_ID)
                && getVdsGroup().getArchitecture() != getVmTemplate().getClusterArch()) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_IS_INCOMPATIBLE);
        }

        if (StringUtils.isEmpty(vmFromParams.getName())) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_NAME_MAY_NOT_BE_EMPTY);
        }

        // check that VM name is not too long
        if (!isVmNameValidLength(vmFromParams)) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_NAME_LENGTH_IS_TOO_LONG);
        }

        // check for Vm Payload
        if (getParameters().getVmPayload() != null) {
            if (!checkPayload(getParameters().getVmPayload(),
                    getParameters().getVmStaticData().getIsoPath())) {
                return false;
            }

            // otherwise, we save the content in base64 string
            for (Map.Entry<String, String> entry : getParameters().getVmPayload().getFiles().entrySet()) {
                entry.setValue(Base64.encodeBase64String(entry.getValue().getBytes()));
            }
        }

        // check for Vm Watchdog Model
        if (getParameters().getWatchdog() != null) {
            if (!validate((new VmWatchdogValidator(vmFromParams.getOs(),
                    getParameters().getWatchdog(),
                    getVdsGroup().getcompatibility_version())).isModelCompatibleWithOs())) {
                return false;
            }
        }

        // Check that the USB policy is legal
        if (!VmHandler.isUsbPolicyLegal(vmFromParams.getUsbPolicy(), vmFromParams.getOs(),
                getVdsGroup(), getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        // check if the OS type is supported
        if (!VmHandler.isOsTypeSupported(vmFromParams.getOs(), getVdsGroup().getArchitecture(),
                getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        // Check if the display type is supported
        if (!VmHandler.isDisplayTypeSupported(getParameters().getVmStaticData().getOsId(),
                vmFromParams.getDefaultDisplayType(),
                getReturnValue().getCanDoActionMessages(),
                getVdsGroup().getcompatibility_version())) {
            return false;
        }

        // check cpuPinning if the check haven't failed yet
        if (!isCpuPinningValid(vmFromParams.getCpuPinning(), vmFromParams.getStaticData())) {
            return false;
        }

        if (vmFromParams.isUseHostCpuFlags()
                && vmFromParams.getMigrationSupport() == MigrationSupport.MIGRATABLE) {
            return failCanDoAction(VdcBllMessages.VM_HOSTCPU_MUST_BE_PINNED_TO_HOST);
        }

        if (instanceTypeId != null && getInstanceType() == null) {
            // invalid instance type
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_INSTANCE_TYPE_DOES_NOT_EXIST);
        }

        if (imageTypeId != null && getImageType() == null) {
            // invalid image type
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_IMAGE_TYPE_DOES_NOT_EXIST);
        }

        if (!checkCpuSockets()){
            return false;
        }

        if (!isCpuSharesValid(vmFromParams)) {
            return failCanDoAction(VdcBllMessages.QOS_CPU_SHARES_OUT_OF_RANGE);
        }

        if (Boolean.TRUE.equals(getParameters().isVirtioScsiEnabled()) &&
                !FeatureSupported.virtIoScsi(getVdsGroup().getcompatibility_version())) {
            return failCanDoAction(VdcBllMessages.VIRTIO_SCSI_INTERFACE_IS_NOT_AVAILABLE_FOR_CLUSTER_LEVEL);
        }

        return true;
    }

    protected boolean checkTemplateImages(List<String> reasons) {
        if (getParameters().getParentCommand() == VdcActionType.AddVmPoolWithVms) {
            return true;
        }

        for (StorageDomain storage : destStorages.values()) {
            if (!VmTemplateCommand.isVmTemplateImagesReady(vmDisksSource, storage.getId(),
                    reasons, false, false, true, true,
                    storageToDisksMap.get(storage.getId()))) {
                return false;
            }
        }
        return true;
    }

    protected boolean checkCpuSockets() {
        return AddVmCommand.checkCpuSockets(getParameters().getVmStaticData().getNumOfSockets(),
                getParameters().getVmStaticData().getCpuPerSocket(), getVdsGroup().getcompatibility_version()
                .toString(), getReturnValue().getCanDoActionMessages());
    }

    protected boolean buildAndCheckDestStorageDomains() {
        boolean retValue = true;
        if (diskInfoDestinationMap.isEmpty()) {
            retValue = fillDestMap();
        } else {
            retValue = validateProvidedDestinations();
        }
        if (retValue && getImagesToCheckDestinationStorageDomains().size() != diskInfoDestinationMap.size()) {
            log.errorFormat("Can not find any default active domain for one of the disks of template with id : {0}",
                    vmDisksSource.getId());
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_MISSED_STORAGES_FOR_SOME_DISKS);
            retValue = false;
        }

        return retValue && validateIsImagesOnDomains();
    }

    protected Collection<DiskImage> getImagesToCheckDestinationStorageDomains() {
        return vmDisksSource.getDiskTemplateMap().values();
    }

    private boolean validateProvidedDestinations() {
        for (DiskImage diskImage : diskInfoDestinationMap.values()) {
            if (diskImage.getStorageIds() == null || diskImage.getStorageIds().isEmpty()) {
                diskImage.setStorageIds(new ArrayList<Guid>());
                diskImage.getStorageIds().add(getParameters().getStorageDomainId());
            }
            Guid storageDomainId = diskImage.getStorageIds().get(0);
            if (destStorages.get(storageDomainId) == null) {
                StorageDomain storage = DbFacade.getInstance().getStorageDomainDao().getForStoragePool(
                        storageDomainId, getStoragePoolId());
                StorageDomainValidator validator =
                        new StorageDomainValidator(storage);
                if (!validate(validator.isDomainExistAndActive()) || !validate(validator.domainIsValidDestination())) {
                    return false;
                }
                destStorages.put(storage.getId(), storage);
            }
        }
        return true;
    }

    private boolean fillDestMap() {
        if (getParameters().getStorageDomainId() != null
                && !Guid.Empty.equals(getParameters().getStorageDomainId())) {
            Guid storageId = getParameters().getStorageDomainId();
            for (DiskImage image : getImagesToCheckDestinationStorageDomains()) {
                diskInfoDestinationMap.put(image.getId(), makeNewImage(storageId, image));
            }
            return validateProvidedDestinations();
        }
        fillImagesMapBasedOnTemplate();
        return true;
    }

    protected void fillImagesMapBasedOnTemplate() {
        ImagesHandler.fillImagesMapBasedOnTemplate(vmDisksSource,
                getStorageDomainDAO().getAllForStoragePool(vmDisksSource.getStoragePoolId()),
                diskInfoDestinationMap,
                destStorages, false);
    }

    protected boolean validateIsImagesOnDomains() {
        for (DiskImage image : getImagesToCheckDestinationStorageDomains()) {
            if (!image.getStorageIds().containsAll(diskInfoDestinationMap.get(image.getId()).getStorageIds())) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_NOT_FOUND_ON_DESTINATION_DOMAIN);
                return false;
            }
        }
        return true;
    }

    private DiskImage makeNewImage(Guid storageId, DiskImage image) {
        DiskImage newImage = new DiskImage();
        newImage.setImageId(image.getImageId());
        newImage.setDiskAlias(image.getDiskAlias());
        newImage.setvolumeFormat(image.getVolumeFormat());
        newImage.setVolumeType(image.getVolumeType());
        ArrayList<Guid> storageIds = new ArrayList<Guid>();
        storageIds.add(storageId);
        newImage.setStorageIds(storageIds);
        newImage.setQuotaId(image.getQuotaId());
        return newImage;
    }

    protected boolean canAddVm(List<String> reasons, String name, Guid storagePoolId,
            int vmPriority) {
        // Checking if a desktop with same name already exists
        if (isVmWithSameNameExists(name)) {
            reasons.add(VdcBllMessages.ACTION_TYPE_FAILED_NAME_ALREADY_USED.name());
            return false;
        }

        if (!verifyAddVM(reasons, vmPriority)) {
            return false;
        }

        if (!checkTemplateImages(reasons)) {
            return false;
        }

        return true;
    }

    protected boolean verifyAddVM(List<String> reasons, int vmPriority) {
        return VmHandler.verifyAddVm(reasons,
                getVmInterfaces().size(),
                vmPriority);
    }

    @Override
    protected void executeVmCommand() {
        VmHandler.warnMemorySizeLegal(getParameters().getVm().getStaticData(), getVdsGroup().getcompatibility_version());

        ArrayList<String> errorMessages = new ArrayList<String>();
        if (canAddVm(errorMessages, destStorages.values())) {
            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

                @Override
                public Void runInTransaction() {
                    addVmStatic();
                    addVmDynamic();
                    addVmNetwork();
                    addVmStatistics();
                    addActiveSnapshot();
                    addVmPermission();
                    addVmInit();
                    getCompensationContext().stateChanged();
                    return null;
                }
            });

            if (addVmImages()) {
                TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

                    @Override
                    public Void runInTransaction() {
                        copyVmDevices();
                        addDiskPermissions();
                        if (getInstanceType() == null) {
                            addVmPayload();
                            updateSmartCardDevices();
                            addVmWatchdog();
                        }
                        setActionReturnValue(getVm().getId());
                        setSucceeded(true);
                        return null;
                    }
                });
            }
        } else {
            log.errorFormat("Failed to add vm . The reasons are: {0}", StringUtils.join(errorMessages, ','));
        }
    }

    private void updateSmartCardDevices() {
        // if vm smartcard settings is different from template's
        // add or remove the smartcard according to user request
        if (getVm().isSmartcardEnabled() != getVmTemplate().isSmartcardEnabled()) {
            VmDeviceUtils.updateSmartcardDevice(getVm().getId(), getVm().isSmartcardEnabled());
        }
    }

    protected void addVmWatchdog() {
        VmWatchdog vmWatchdog = getParameters().getWatchdog();
        if (vmWatchdog != null) {
            WatchdogParameters parameters = new WatchdogParameters();
            parameters.setId(getParameters().getVmId());
            parameters.setAction(vmWatchdog.getAction());
            parameters.setModel(vmWatchdog.getModel());
            getBackend().runInternalAction(VdcActionType.AddWatchdog, parameters);
        }
    }

    protected void addVmPayload() {
        VmPayload payload = getParameters().getVmPayload();

        if (payload != null) {
            VmDeviceUtils.addManagedDevice(new VmDeviceId(Guid.newGuid(), getParameters().getVmId()),
                    VmDeviceGeneralType.DISK,
                    payload.getType(),
                    payload.getSpecParams(),
                    true,
                    true,
                    null);
        }
    }

    protected void copyVmDevices() {
        VmDeviceUtils.copyVmDevices(vmDevicesSourceId,
                getVmId(),
                getSrcDeviceIdToTargetDeviceIdMapping(),
                getParameters().isSoundDeviceEnabled(),
                getParameters().isConsoleEnabled(),
                isVirtioScsiEnabled(),
                isBalloonEnabled(),
                false);
    }

    protected static boolean isLegalClusterId(Guid clusterId, List<String> reasons) {
        // check given cluster id
        VDSGroup vdsGroup = DbFacade.getInstance().getVdsGroupDao().get(clusterId);
        boolean legalClusterId = (vdsGroup != null);
        if (!legalClusterId) {
            reasons.add(VdcBllErrors.VM_INVALID_SERVER_CLUSTER_ID.toString());
        }
        return legalClusterId;
    }

    protected boolean areParametersLegal(List<String> reasons) {
        boolean returnValue = false;
        final VmStatic vmStaticData = getParameters().getVmStaticData();

        if (vmStaticData != null) {

            returnValue = isLegalClusterId(vmStaticData.getVdsGroupId(), reasons);

            if (!validatePinningAndMigration(reasons, vmStaticData, getParameters().getVm().getCpuPinning())) {
                returnValue = false;
            }

        }
        return returnValue;
    }

    protected void addVmNetwork() {
        List<VmNic> nics = getVmInterfaces();
        VmInterfaceManager vmInterfaceManager = new VmInterfaceManager();
        vmInterfaceManager.sortVmNics(nics, getVmInterfaceDevices());

        List<String> macAddresses = MacPoolManager.getInstance().allocateMacAddresses(nics.size());

        // Add interfaces from template
        for (int i = 0; i < nics.size(); ++i) {
            VmNic iface = nics.get(i);
            Guid id = Guid.newGuid();
            srcVmNicIdToTargetVmNicIdMapping.put(iface.getId(), id);
            iface.setId(id);
            iface.setMacAddress(macAddresses.get(i));
            iface.setSpeed(VmInterfaceType.forValue(iface.getType()).getSpeed());
            iface.setVmTemplateId(null);
            iface.setVmId(getParameters().getVmStaticData().getId());
            getVmNicDao().save(iface);
            getCompensationContext().snapshotNewEntity(iface);
            DbFacade.getInstance().getVmNetworkStatisticsDao().save(iface.getStatistics());
            getCompensationContext().snapshotNewEntity(iface.getStatistics());
        }
    }

    protected void addVmInit() {
        VmHandler.addVmInitToDB(getParameters().getVmStaticData());
    }

    protected void addVmStatic() {
        VmStatic vmStatic = getParameters().getVmStaticData();

        if (vmStatic.getOrigin() == null) {
            vmStatic.setOrigin(OriginType.valueOf(Config.<String> getValue(ConfigValues.OriginType)));
        }
        vmStatic.setId(getVmId());
        vmStatic.setQuotaId(getQuotaId());
        vmStatic.setCreationDate(new Date());
        vmStatic.setCreatedByUserId(getUserId());
        // Parses the custom properties field that was filled by frontend to
        // predefined and user defined fields
        if (vmStatic.getCustomProperties() != null) {
            VMCustomProperties properties =
                    VmPropertiesUtils.getInstance().parseProperties(getVdsGroupDAO()
                            .get(getParameters().getVm().getVdsGroupId())
                            .getcompatibility_version(),
                            vmStatic.getCustomProperties());
            String predefinedProperties = properties.getPredefinedProperties();
            String userDefinedProperties = properties.getUseDefinedProperties();
            vmStatic.setPredefinedProperties(predefinedProperties);
            vmStatic.setUserDefinedProperties(userDefinedProperties);
        }

        vmStatic.setOriginalTemplateGuid(vmStatic.getVmtGuid());
        vmStatic.setOriginalTemplateName(getVmTemplate().getName());
        getVmStaticDao().save(vmStatic);
        getCompensationContext().snapshotNewEntity(vmStatic);
    }

    void addVmDynamic() {
        VmDynamic tempVar = new VmDynamic();
        tempVar.setId(getVmId());
        tempVar.setStatus(VMStatus.Down);
        tempVar.setVmHost("");
        tempVar.setVmIp("");
        tempVar.setVmFQDN("");
        tempVar.setDisplayType(getParameters().getVmStaticData().getDefaultDisplayType());
        tempVar.setLastStopTime(new Date());
        VmDynamic vmDynamic = tempVar;
        DbFacade.getInstance().getVmDynamicDao().save(vmDynamic);
        getCompensationContext().snapshotNewEntity(vmDynamic);
    }

    void addVmStatistics() {
        VmStatistics stats = new VmStatistics();
        stats.setId(getVmId());
        DbFacade.getInstance().getVmStatisticsDao().save(stats);
        getCompensationContext().snapshotNewEntity(stats);
    }

    protected boolean addVmImages() {
        if (vmDisksSource.getDiskTemplateMap().size() > 0) {
            if (getVm().getStatus() != VMStatus.Down) {
                log.error("Cannot add images. VM is not Down");
                throw new VdcBLLException(VdcBllErrors.IRS_IMAGE_STATUS_ILLEGAL);
            }
            VmHandler.lockVm(getVmId());
            for (DiskImage dit : getImagesToCheckDestinationStorageDomains()) {
                CreateSnapshotFromTemplateParameters tempVar = new CreateSnapshotFromTemplateParameters(
                        dit.getImageId(), getParameters().getVmStaticData().getId());
                tempVar.setDestStorageDomainId(diskInfoDestinationMap.get(dit.getId()).getStorageIds().get(0));
                tempVar.setDiskAlias(diskInfoDestinationMap.get(dit.getId()).getDiskAlias());
                tempVar.setStorageDomainId(dit.getStorageIds().get(0));
                tempVar.setVmSnapshotId(getVmSnapshotId());
                tempVar.setParentCommand(VdcActionType.AddVm);
                tempVar.setEntityInfo(getParameters().getEntityInfo());
                tempVar.setParentParameters(getParameters());
                tempVar.setQuotaId(diskInfoDestinationMap.get(dit.getId()).getQuotaId());
                VdcReturnValueBase result =
                        getBackend().runInternalAction(VdcActionType.CreateSnapshotFromTemplate,
                                tempVar,
                                ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));

                /**
                 * if couldn't create snapshot then stop the transaction and the command
                 */
                if (!result.getSucceeded()) {
                    throw new VdcBLLException(result.getFault().getError());
                } else {
                    getTaskIdList().addAll(result.getInternalVdsmTaskIdList());
                    DiskImage newImage = (DiskImage) result.getActionReturnValue();
                    srcDiskIdToTargetDiskIdMapping.put(dit.getId(), newImage.getId());
                }
            }
        }
        return true;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            return getSucceeded() ? (getReturnValue().getVdsmTaskIdList().size() > 0 ? AuditLogType.USER_ADD_VM_STARTED
                    : AuditLogType.USER_ADD_VM) : AuditLogType.USER_FAILED_ADD_VM;

        case END_SUCCESS:
            return getSucceeded() ? AuditLogType.USER_ADD_VM_FINISHED_SUCCESS
                    : AuditLogType.USER_ADD_VM_FINISHED_FAILURE;

        default:
            return AuditLogType.USER_ADD_VM_FINISHED_FAILURE;
        }
    }

    @Override
    protected VdcActionType getChildActionType() {
        return VdcActionType.CreateSnapshotFromTemplate;
    }

    @Override
    protected void endWithFailure() {
        super.endActionOnDisks();
        removeVmRelatedEntitiesFromDb();
        setSucceeded(true);
    }

    protected void removeVmRelatedEntitiesFromDb() {
        removeVmUsers();
        removeVmNetwork();
        // Note that currently newly added vm never have memory state
        // In case it will be changed (clone vm from snapshot will clone the memory state),
        // we'll need to remove the memory state images here as well.
        removeVmSnapshots();
        removeVmStatic();
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = new ArrayList<PermissionSubject>();
        permissionList.add(new PermissionSubject(getVdsGroupId(),
                VdcObjectType.VdsGroups,
                getActionType().getActionGroup()));
        permissionList.add(new PermissionSubject(getVmTemplateId(),
                VdcObjectType.VmTemplate,
                getActionType().getActionGroup()));
        if (getVmTemplate() != null && !getVmTemplate().getDiskList().isEmpty()) {
            for (DiskImage disk : getParameters().getDiskInfoDestinationMap().values()) {
                if (disk.getStorageIds() != null && !disk.getStorageIds().isEmpty()) {
                    permissionList.add(new PermissionSubject(disk.getStorageIds().get(0),
                            VdcObjectType.Storage, ActionGroup.CREATE_DISK));
                }
            }
        }
        // if using instance type, need create instance
        if (getInstanceType() != null) {
            permissionList.add(new PermissionSubject(instanceTypeId, VdcObjectType.VmTemplate, ActionGroup.CREATE_INSTANCE));
        }
        // if using image type, need create instance
        if (getImageType() != null) {
            permissionList.add(new PermissionSubject(imageTypeId, VdcObjectType.VmTemplate, ActionGroup.CREATE_INSTANCE));
        }
        addPermissionSubjectForAdminLevelProperties(permissionList);
        return permissionList;
    }

    /**
     * user need permission on each object used: template, instance type, image type..
     * for cluster: if instance type is used, create_instance is enough
     * but if user has create_template he can choose to use instance type or template
     */
    @Override
    protected boolean checkPermissions(final List<PermissionSubject> permSubjects) {
        for (PermissionSubject permSubject : permSubjects) {
            // if user is using instance type, then create_instance on the cluster is enough
            if (permSubject.getObjectType() == VdcObjectType.VdsGroups && instanceTypeId != null) {
                permSubject.setActionGroup(ActionGroup.CREATE_INSTANCE);
                if (checkSinglePermission(permSubject, getReturnValue().getCanDoActionMessages())) {
                    continue;
                }

                // create_vm is overriding in case no create_instance, try again with it
                permSubject.setActionGroup(getActionType().getActionGroup());
            }

            if (!checkSinglePermission(permSubject, getReturnValue().getCanDoActionMessages())) {
                return false;
            }
        }
        return true;
    }

    protected void addPermissionSubjectForAdminLevelProperties(List<PermissionSubject> permissionList) {
        VmStatic vmFromParams = getParameters().getVmStaticData();

        if (vmFromParams != null) {
            // user needs specific permission to change custom properties
            if (!StringUtils.isEmpty(vmFromParams.getCustomProperties())) {
                permissionList.add(new PermissionSubject(getVdsGroupId(),
                        VdcObjectType.VdsGroups, ActionGroup.CHANGE_VM_CUSTOM_PROPERTIES));
            }

            // host-specific parameters can be changed by administration role only
            if (vmFromParams.getDedicatedVmForVds() != null || !StringUtils.isEmpty(vmFromParams.getCpuPinning())) {
                permissionList.add(new PermissionSubject(getVdsGroupId(),
                        VdcObjectType.VdsGroups, ActionGroup.EDIT_ADMIN_VM_PROPERTIES));
            }
        }
    }

    protected void addVmPermission() {
        UniquePermissionsSet permissionsToAdd = new UniquePermissionsSet();
        if ((getParameters()).isMakeCreatorExplicitOwner()) {
            permissionsToAdd.addPermission(getCurrentUser().getId(), PredefinedRoles.VM_OPERATOR.getId(),
                    getVmId(), VdcObjectType.VM);
        }

        if (getParameters().isCopyTemplatePermissions() && !getVmTemplateId().equals(VmTemplateHandler.BLANK_VM_TEMPLATE_ID)) {
            copyTemplatePermissions(permissionsToAdd);
        }

        if (!permissionsToAdd.isEmpty()) {
            List<Permissions> permissionsList = permissionsToAdd.asPermissionList();
            MultiLevelAdministrationHandler.addPermission(permissionsList.toArray(new Permissions[permissionsList.size()]));

            getCompensationContext().snapshotNewEntities(permissionsList);
        }
    }

    private void copyTemplatePermissions(UniquePermissionsSet permissionsToAdd) {
        PermissionDAO dao = getDbFacade().getPermissionDao();

        List<Permissions> templatePermissions = dao.getAllForEntity(getVmTemplateId(), getCurrentUser().getId(), false);

        for (Permissions templatePermission : templatePermissions) {
            boolean templateOwnerRole = templatePermission.getrole_id().equals(PredefinedRoles.TEMPLATE_OWNER.getId());
            boolean templateUserRole = templatePermission.getrole_id().equals(PredefinedRoles.TEMPLATE_USER.getId());

            if (templateOwnerRole || templateUserRole) {
                continue;
            }

            permissionsToAdd.addPermission(templatePermission.getad_element_id(), templatePermission.getrole_id(),
                    getVmId(), VdcObjectType.VM);
        }

    }

    protected void addDiskPermissions() {
        List<Guid> newDiskImageIds = new ArrayList<>(srcDiskIdToTargetDiskIdMapping.values());
        Permissions[] permsArray = new Permissions[newDiskImageIds.size()];
        for (int i = 0; i < newDiskImageIds.size(); i++) {
            permsArray[i] =
                    new Permissions(getCurrentUser().getId(),
                            PredefinedRoles.DISK_OPERATOR.getId(),
                            newDiskImageIds.get(i),
                            VdcObjectType.Disk);
        }
        MultiLevelAdministrationHandler.addPermission(permsArray);
    }

    protected void addActiveSnapshot() {
        _vmSnapshotId = Guid.newGuid();
        new SnapshotsManager().addActiveSnapshot(_vmSnapshotId, getVm(), getCompensationContext());
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        if (!StringUtils.isBlank(getParameters().getVm().getName())) {
            return Collections.singletonMap(getParameters().getVm().getName(),
                    LockMessagesMatchUtil.makeLockingPair(LockingGroup.VM_NAME, VdcBllMessages.ACTION_TYPE_FAILED_OBJECT_LOCKED));
        }
        return null;
    }

    @Override
    protected VmDynamicDAO getVmDynamicDao() {
        return DbFacade.getInstance().getVmDynamicDao();
    }

    protected VmStaticDAO getVmStaticDao() {
        return DbFacade.getInstance().getVmStaticDao();
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

    private Guid getQuotaId() {
        return getParameters().getVmStaticData().getQuotaId();
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaStorageConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<QuotaConsumptionParameter>();

        for (DiskImage disk : diskInfoDestinationMap.values()) {
            list.add(new QuotaStorageConsumptionParameter(
                    disk.getQuotaId(),
                    null,
                    QuotaStorageConsumptionParameter.QuotaAction.CONSUME,
                    disk.getStorageIds().get(0),
                    (double)disk.getSizeInGigabytes()));
        }
        return list;
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaVdsConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<QuotaConsumptionParameter>();
        list.add(new QuotaSanityParameter(getQuotaId(), null));
        return list;
    }

    public Map<Guid, Guid> getSrcDiskIdToTargetDiskIdMapping() {
        return srcDiskIdToTargetDiskIdMapping;
    }

    public Map<Guid, Guid> getSrcDeviceIdToTargetDeviceIdMapping() {
        Map<Guid, Guid> srcDeviceIdToTargetDeviceIdMapping = new HashMap<>();
        srcDeviceIdToTargetDeviceIdMapping.putAll(srcVmNicIdToTargetVmNicIdMapping);
        srcDeviceIdToTargetDeviceIdMapping.putAll(srcDiskIdToTargetDiskIdMapping);
        return srcDeviceIdToTargetDeviceIdMapping;
    }

    protected boolean isVirtioScsiEnabled() {
        Boolean virtioScsiEnabled = getParameters().isVirtioScsiEnabled();
        return virtioScsiEnabled != null ? virtioScsiEnabled :
                FeatureSupported.virtIoScsi(getVdsGroup().getcompatibility_version());
    }

    protected boolean isBalloonEnabled() {
        return getParameters().isBalloonEnabled();
    }

    protected boolean hasWatchdog() {
        return getParameters().getWatchdog() != null;
    }

    protected boolean isVirtioScsiControllerAttached(Guid vmId) {
        return VmDeviceUtils.isVirtioScsiControllerAttached(vmId);
    }

    /**
     * This method override vm values with the instance type values
     * in case instance type is selected for this vm
     */
    private void updateVmObject() {
        InstanceType instanceType = getInstanceType();
        VmStatic vmStatic = getParameters().getVmStaticData();
        if (instanceType != null) {
            vmStatic.setMemSizeMb(instanceType.getMemSizeMb());
            vmStatic.setNumOfSockets(instanceType.getNumOfSockets());
            vmStatic.setCpuPerSocket(instanceType.getCpuPerSocket());
            vmStatic.setDefaultBootSequence(instanceType.getDefaultBootSequence());
            vmStatic.setDefaultDisplayType(instanceType.getDefaultDisplayType());
            vmStatic.setPriority(instanceType.getPriority());
            vmStatic.setMinAllocatedMem(instanceType.getMinAllocatedMem());
            vmStatic.setTunnelMigration(instanceType.getTunnelMigration());

            // use sound card only if instance type has it
            getParameters().setSoundDeviceEnabled(!getVmDeviceDao()
                    .getVmDeviceByVmIdAndType(instanceType.getId(), VmDeviceGeneralType.SOUND).isEmpty());

            // set vm interface source id to be the instance type, vm interface are taken from it
            vmInterfacesSourceId = instanceType.getId();
        }

        ImageType imageType = getImageType();
        if (imageType != null) {
            vmStatic.setOsId(imageType.getOsId());
            vmStatic.setIsoPath(imageType.getIsoPath());
            vmStatic.setInitrdUrl(imageType.getInitrdUrl());
            vmStatic.setKernelUrl(imageType.getKernelUrl());
            vmStatic.setKernelParams(imageType.getKernelParams());
            // set vm disks source to be the image type, vm disks are taken from it
            vmDisksSource = (VmTemplate)imageType;
        }

        OsRepository osRepository = SimpleDependecyInjector.getInstance().get(OsRepository.class);

        // Choose a proper default OS according to the cluster architecture
        if (getParameters().getVmStaticData().getOsId() == OsRepository.AUTO_SELECT_OS) {
            if (getVdsGroup().getArchitecture() != ArchitectureType.undefined) {
                Integer defaultOs = osRepository.getDefaultOSes().get(getVdsGroup().getArchitecture());

                getParameters().getVmStaticData().setOsId(defaultOs);
            }
        }

        // Choose a proper default display type according to the cluster architecture
        if (getParameters().getVmStaticData().getOsId() != OsRepository.AUTO_SELECT_OS &&
                getParameters().getVmStaticData().getDefaultDisplayType() == null) {
            DisplayType defaultDisplayType =
                    osRepository.getDisplayTypes(getParameters().getVmStaticData().getOsId(),
                            getVdsGroup().getcompatibility_version()).get(0);

            getParameters().getVmStaticData().setDefaultDisplayType(defaultDisplayType);
        }
    }
}
