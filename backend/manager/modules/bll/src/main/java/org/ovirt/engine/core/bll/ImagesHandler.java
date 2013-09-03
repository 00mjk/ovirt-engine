package org.ovirt.engine.core.bll;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.storage.StorageHelperDirector;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.StorageDomainValidator;
import org.ovirt.engine.core.common.businessentities.BaseDisk;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskImageBase;
import org.ovirt.engine.core.common.businessentities.DiskImageDynamic;
import org.ovirt.engine.core.common.businessentities.DiskLunMapId;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.LUNs;
import org.ovirt.engine.core.common.businessentities.LunDisk;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.common.businessentities.image_storage_domain_map;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.common.vdscommands.GetImageInfoVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VdsAndPoolIDVDSParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.collections.MultiValueMapUtils;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public final class ImagesHandler {
    public static final String DISK = "_Disk";
    public static final String DefaultDriveName = "1";
    private static final Log log = LogFactory.getLog(ImagesHandler.class);

    /**
     * The following method will find all images and storages where they located for provide template and will fill an
     * diskInfoDestinationMap by imageId mapping on active storage id where image is located. The second map is
     * mapping of founded storage ids to storage object
     * @param template
     * @param diskInfoDestinationMap
     * @param destStorages
     * @param notCheckSize - if we need to perform a size check for storage or not
     */
    public static void fillImagesMapBasedOnTemplate(VmTemplate template,
            Map<Guid, DiskImage> diskInfoDestinationMap,
            Map<Guid, StorageDomain> destStorages, boolean notCheckSize) {
        List<StorageDomain> domains =
                DbFacade.getInstance()
                        .getStorageDomainDao()
                        .getAllForStoragePool(template.getStoragePoolId());
        fillImagesMapBasedOnTemplate(template, domains, diskInfoDestinationMap, destStorages, notCheckSize);
    }

    public static void fillImagesMapBasedOnTemplate(VmTemplate template,
            List<StorageDomain> domains,
            Map<Guid, DiskImage> diskInfoDestinationMap,
            Map<Guid, StorageDomain> destStorages, boolean notCheckSize) {
        Map<Guid, StorageDomain> storageDomainsMap = new HashMap<Guid, StorageDomain>();
        for (StorageDomain storageDomain : domains) {
            StorageDomainValidator validator = new StorageDomainValidator(storageDomain);
            if (validator.isDomainExistAndActive().isValid() && validator.domainIsValidDestination().isValid()
                    && (notCheckSize || validator.isDomainWithinThresholds().isValid())) {
                storageDomainsMap.put(storageDomain.getId(), storageDomain);
            }
        }
        for (DiskImage image : template.getDiskTemplateMap().values()) {
            for (Guid storageId : image.getStorageIds()) {
                if (storageDomainsMap.containsKey(storageId)) {
                    ArrayList<Guid> storageIds = new ArrayList<Guid>();
                    storageIds.add(storageId);
                    image.setStorageIds(storageIds);
                    diskInfoDestinationMap.put(image.getId(), image);
                    break;
                }
            }
        }

        if (destStorages != null) {
            for (DiskImage diskImage : diskInfoDestinationMap.values()) {
                Guid storageDomainId = diskImage.getStorageIds().get(0);
                destStorages.put(storageDomainId, storageDomainsMap.get(storageDomainId));
            }
        }
    }

    public static boolean setDiskAlias(BaseDisk disk, VM vm) {
        return setDiskAlias(disk, vm, nullSafeGetCount(vm));
    }

    public static boolean setDiskAlias(BaseDisk disk, VM vm, int count) {
        if (disk == null) {
            log.error("Disk object is null");
            return false;
        }

        String vmName =  nullSafeGetVmName(vm);
        disk.setDiskAlias(getSuggestedDiskAlias(disk, vmName, count));
        return true;
    }

    private static String nullSafeGetVmName(VM vm) {
        return vm == null ? "" : vm.getName();
    }

    private static int nullSafeGetCount(VM vm) {
        return vm == null ? 1 : vm.getDiskMapCount() + 1;
    }

    /**
     * Suggests an alias for a disk.
     * If the disk does not already have an alias, one will be generated for it.
     * The generated alias will be formed as prefix_DiskXXX, where XXX is an ordinal.
     *
     * @param disk
     *            - The disk that (possibly) requires a new alias
     * @param diskPrefix
     *            - The prefix for the newly generated alias
     * @param count
     *            - The ordinal of disk to create an alias for (first, second, etc.).
     * @return The suggested alias
     */
    public static String getSuggestedDiskAlias(BaseDisk disk, String diskPrefix, int count) {
        String diskAlias;
        if (disk == null) {
            diskAlias = getDefaultDiskAlias(diskPrefix, DefaultDriveName);
            log.warnFormat("Disk object is null, the suggested default disk alias to be used is {0}",
                    diskAlias);
        } else {
            String defaultAlias = getDefaultDiskAlias(diskPrefix, String.valueOf(count));
            diskAlias = getDiskAliasWithDefault(disk, defaultAlias);
        }
        return diskAlias;
    }

    /**
     * Returns an alias for the given disk. If the disk already has an alias, it is returned. If not,
     * {@link #aliasIfNull} is returned.
     *
     * @param disk
     *            The disk
     * @param aliasIfNull
     *            The alias to return if the disk does not have an alias
     * @return The alias in question
     */
    public static String getDiskAliasWithDefault(BaseDisk disk, String aliasIfNull) {
        String diskAlias = disk.getDiskAlias();
        if (StringUtils.isEmpty(diskAlias)) {
            log.infoFormat("Disk alias retrieved from the client is null or empty, the suggested default disk alias to be used is {0}",
                    aliasIfNull);
            return aliasIfNull;
        }
        return diskAlias;
    }

    public static String getDefaultDiskAlias(String prefix, String suffix) {
        return prefix + DISK + suffix;
    }

    public static Map<Guid, List<DiskImage>> buildStorageToDiskMap(Collection<DiskImage> images,
            Map<Guid, DiskImage> diskInfoDestinationMap) {
        Map<Guid, List<DiskImage>> storageToDisksMap = new HashMap<Guid, List<DiskImage>>();
        for (DiskImage disk : images) {
            DiskImage diskImage = diskInfoDestinationMap.get(disk.getId());
            Guid storageDomainId = diskImage.getStorageIds().get(0);
            List<DiskImage> diskList = storageToDisksMap.get(storageDomainId);
            if (diskList == null) {
                diskList = new ArrayList<DiskImage>();
                storageToDisksMap.put(storageDomainId, diskList);
            }
            diskList.add(disk);
        }
        return storageToDisksMap;
    }

    /**
     * Adds a disk image (Adds image, disk and relevant entities)
     *
     * @param image
     *            DiskImage to add
     * @param active
     *            true if the image should be added as active
     * @param imageStorageDomainMap
     *            storage domain map entry to map between the image and its storage domain
     */
    public static void addDiskImage(DiskImage image, boolean active, image_storage_domain_map imageStorageDomainMap, Guid vmId) {
        try {
            addImage(image, active, imageStorageDomainMap);
            addDiskToVmIfNotExists(image, vmId);
        } catch (RuntimeException ex) {
            log.error("Failed adding new disk image and related entities to db", ex);
            throw new VdcBLLException(VdcBllErrors.DB, ex);
        }
    }

    /**
     * Gets a map of DiskImage IDs to DiskImage objects
     *
     * @param diskImages
     *            collection of DiskImage objects to create the map for
     * @return map object is the collection is not null
     */
    public static Map<Guid, DiskImage> getDiskImagesByIdMap(Collection<DiskImage> diskImages) {
        Map<Guid, DiskImage> result = null;
        if (diskImages != null) {
            result = new HashMap<Guid, DiskImage>();
            for (DiskImage diskImage : diskImages) {
                result.put(diskImage.getImageId(), diskImage);
            }
        }
        return result;
    }

    /**
     * Adds a disk image (Adds image, disk, and relevant entities , but not VmDevice) This may be useful for Clone VMs,
     * where besides adding images it is required to copy all vm devices (VmDeviceUtils.copyVmDevices) from the source
     * VM
     *
     * @param image
     *            image to add
     * @param active
     *            true if to add as active image
     * @param imageStorageDomainMap
     *            entry of image storagte domain map
     */
    public static void addDiskImageWithNoVmDevice(DiskImage image,
            boolean active,
            image_storage_domain_map imageStorageDomainMap) {
        try {
            addImage(image, active, imageStorageDomainMap);
            addDisk(image);
        } catch (RuntimeException ex) {
            log.error("Failed adding new disk image and related entities to db", ex);
            throw new VdcBLLException(VdcBllErrors.DB, ex);
        }
    }

    /**
     * Adds a disk image (Adds image, disk, and relevant entities , but not VmDevice) This may be useful for Clone VMs,
     * where besides adding images it is required to copy all vm devices (VmDeviceUtils.copyVmDevices) from the source
     * VM
     *
     * @param image
     */
    public static void addDiskImageWithNoVmDevice(DiskImage image) {
        addDiskImageWithNoVmDevice(image,
                image.getActive(),
                new image_storage_domain_map(image.getImageId(), image.getStorageIds().get(0)));
    }

    /**
     * Adds disk to a VM without creating a VmDevice entry
     *
     * @param disk
     *            disk to add
     */
    public static void addDisk(BaseDisk disk) {
        if (!DbFacade.getInstance().getBaseDiskDao().exists(disk.getId())) {
            DbFacade.getInstance().getBaseDiskDao().save(disk);
        }
    }

    /**
     * Adds a disk image (Adds image with active flag according to the value in image, using the first storage domain in
     * the storage id as entry to the storage domain map)
     *
     * @param image
     *            DiskImage to add
     */
    public static void addDiskImage(DiskImage image, Guid vmId) {
        addDiskImage(image, image.getActive(), new image_storage_domain_map(image.getImageId(), image.getStorageIds()
                .get(0)), vmId);
    }

    /**
     * Add image and related entities to DB (Adds image, disk image dynamic and image storage domain map)
     *
     * @param image
     *            the image to add
     * @param active
     *            if true the image will be active
     * @param imageStorageDomainMap
     *            entry of mapping between the storage domain and the image
     */
    public static void addImage(DiskImage image, boolean active, image_storage_domain_map imageStorageDomainMap) {
        image.setActive(active);
        DbFacade.getInstance().getImageDao().save(image.getImage());
        DiskImageDynamic diskDynamic = new DiskImageDynamic();
        diskDynamic.setId(image.getImageId());
        diskDynamic.setactual_size(image.getActualSizeInBytes());
        DbFacade.getInstance().getDiskImageDynamicDao().save(diskDynamic);
        if (imageStorageDomainMap != null) {
            DbFacade.getInstance().getImageStorageDomainMapDao().save(imageStorageDomainMap);
        }
    }

    /**
     * Add disk if it does not exist to a given vm
     *
     * @param disk
     *            the disk to add
     * @param vmId
     *            the ID of the vm to add to if the disk does not exist for this VM
     */
    public static void addDiskToVmIfNotExists(BaseDisk disk, Guid vmId) {
        if (!DbFacade.getInstance().getBaseDiskDao().exists(disk.getId())) {
            addDiskToVm(disk, vmId);
        }
    }

    /**
     * Adds disk to vm
     *
     * @param disk
     *            the disk to add
     * @param vmId
     *            the ID of the VM to add to
     */
    public static void addDiskToVm(BaseDisk disk, Guid vmId) {
        DbFacade.getInstance().getBaseDiskDao().save(disk);
        VmDeviceUtils.addManagedDevice(new VmDeviceId(disk.getId(), vmId),
                VmDeviceGeneralType.DISK,
                VmDeviceType.DISK,
                null,
                true,
                false,
                null);
    }

    /**
     * This function was developed especially for GUI needs. It returns a list of all the snapshots of current image of
     * a specific VM. If there are two images mapped to same VM, it's assumed that this is a TryBackToImage case and the
     * function returns a list of snapshots of inactive images. In this case the parent of the active image appears to
     * be trybackfrom image
     *
     * @param imageId
     * @param imageTemplateId
     * @return
     */
    public static ArrayList<DiskImage> getAllImageSnapshots(Guid imageId, Guid imageTemplateId) {
        ArrayList<DiskImage> snapshots = new ArrayList<DiskImage>();
        Guid curImage = imageId;
        while (!imageTemplateId.equals(curImage) && !curImage.equals(Guid.Empty)) {
            DiskImage curDiskImage = DbFacade.getInstance().getDiskImageDao().getSnapshotById(curImage);
            snapshots.add(curDiskImage);
            curImage = curDiskImage.getParentId();
        }
        return snapshots;
    }

    public static String cdPathWindowsToLinux(String windowsPath, Guid storagePoolId, Guid vdsId) {
        return cdPathWindowsToLinux(windowsPath, (String) Backend.getInstance()
                .getResourceManager()
                .RunVdsCommand(VDSCommandType.IsoPrefix, new VdsAndPoolIDVDSParametersBase(vdsId, storagePoolId))
                .getReturnValue());
    }

    public static String cdPathWindowsToLinux(String windowsPath, String isoPrefix) {
        if (StringUtils.isEmpty(windowsPath)) {
            return windowsPath; // empty string is used for 'eject'.
        }
        String fileName = new File(windowsPath).getName();
        return String.format("%1$s/%2$s", isoPrefix, fileName);
    }

    public static boolean isImagesExists(Iterable<DiskImage> images, Guid storagePoolId) {
        return isImagesExists(images, storagePoolId, new ArrayList<DiskImage>());
    }

    private static boolean isImagesExists(Iterable<DiskImage> images, Guid storagePoolId, ArrayList<DiskImage> irsImages) {
        boolean returnValue = true;

        for (DiskImage image : images) {
            DiskImage fromIrs = isImageExist(storagePoolId, image);
            if (fromIrs == null) {
                returnValue = false;
                break;
            }

            irsImages.add(fromIrs);
        }
        return returnValue;
    }

    private static DiskImage isImageExist(Guid storagePoolId, DiskImage image) {
        DiskImage fromIrs = null;
        try {
            Guid storageDomainId = image.getStorageIds().get(0);
            Guid imageGroupId = image.getId() != null ? image.getId() : Guid.Empty;
            fromIrs = (DiskImage) Backend
                    .getInstance()
                    .getResourceManager()
                    .RunVdsCommand(
                            VDSCommandType.GetImageInfo,
                            new GetImageInfoVDSCommandParameters(storagePoolId, storageDomainId, imageGroupId,
                                    image.getImageId())).getReturnValue();
        } catch (Exception e) {
            log.debug("Unable to get image info from from storage.", e);
        }
        return fromIrs;
    }

    public static boolean checkImageConfiguration(StorageDomainStatic storageDomain,
            DiskImageBase diskInfo, List<String> messages) {
        if (!checkImageConfiguration(storageDomain, diskInfo.getVolumeType(), diskInfo.getVolumeFormat())) {
            // not supported
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_DISK_CONFIGURATION_NOT_SUPPORTED.toString());
            messages.add(String.format("$%1$s %2$s", "volumeFormat", diskInfo.getVolumeFormat()));
            messages.add(String.format("$%1$s %2$s", "volumeType", diskInfo.getVolumeType()));
            return false;
        }
        return true;
    }

    public static boolean checkImageConfiguration(StorageDomainStatic storageDomain, VolumeType volumeType, VolumeFormat volumeFormat) {
        return !((volumeType == VolumeType.Preallocated && volumeFormat == VolumeFormat.COW)
                || (storageDomain.getStorageType().isBlockDomain() && volumeType == VolumeType.Sparse && volumeFormat == VolumeFormat.RAW)
                || volumeFormat == VolumeFormat.Unassigned
                || volumeType == VolumeType.Unassigned);
    }

    public static boolean checkImagesConfiguration(Guid storageDomainId,
            Collection<? extends Disk> disksConfigList,
            List<String> messages) {
        boolean result = true;
        StorageDomainStatic storageDomain = DbFacade.getInstance().getStorageDomainStaticDao().get(storageDomainId);
        for (Disk diskInfo : disksConfigList) {
            if (DiskStorageType.IMAGE == diskInfo.getDiskStorageType()) {
                result = checkImageConfiguration(storageDomain, (DiskImage) diskInfo, messages);
            }
            if (!result)
                break;
        }
        return result;
    }

    public static List<DiskImage> getPluggedImagesForVm(Guid vmId) {
        return filterImageDisks(DbFacade.getInstance().getDiskDao().getAllForVm(vmId, true), true, false);
    }

    /**
     * @return A unique {@link Set} of all the storage domain IDs relevant to all the given images
     * @param images The images to get the storage domain IDs for
     */
    public static Set<Guid> getAllStorageIdsForImageIds(Collection<DiskImage> images) {
        Set<Guid> domainsIds = new HashSet<Guid>();
        for (DiskImage image : images) {
            domainsIds.addAll(image.getStorageIds());
        }
        return domainsIds;
    }

    public static void fillImagesBySnapshots(VM vm) {
        for (Disk disk : vm.getDiskMap().values()) {
            if (disk.getDiskStorageType() == DiskStorageType.IMAGE) {
                DiskImage diskImage = (DiskImage) disk;
                diskImage.getSnapshots().addAll(
                        ImagesHandler.getAllImageSnapshots(diskImage.getImageId(),
                                diskImage.getImageTemplateId()));
            }
        }
    }

    /**
     * Filter image disks by attributes.
     *
     * @param listOfDisks
     *            - The list of disks to be filtered.
     * @param allowOnlyNotShareableDisks
     *            - Indication whether to allow only disks that are not shareable
     * @param allowOnlySnapableDisks
     *            - Indication whether to allow only disks which are allowed to be snapshoted.
     * @return - List filtered of disk images.
     */
    public static List<DiskImage> filterImageDisks(Collection<? extends Disk> listOfDisks,
            boolean allowOnlyNotShareableDisks,
            boolean allowOnlySnapableDisks) {
        List<DiskImage> diskImages = new ArrayList<DiskImage>();
        for (Disk disk : listOfDisks) {
            if (disk.getDiskStorageType() == DiskStorageType.IMAGE &&
                    (!allowOnlyNotShareableDisks || !disk.isShareable()) &&
                    (!allowOnlySnapableDisks || disk.isAllowSnapshot())) {
                diskImages.add((DiskImage) disk);
            }
        }
        return diskImages;
    }

    public static List<LunDisk> filterDiskBasedOnLuns(Collection<Disk> listOfDisks) {
        List<LunDisk> lunDisks = new ArrayList<LunDisk>();
        for (Disk disk : listOfDisks) {
            if (disk.getDiskStorageType() == DiskStorageType.LUN) {
                lunDisks.add((LunDisk) disk);
            }
        }
        return lunDisks;
    }

    public static void removeDiskImage(DiskImage diskImage, Guid vmId) {
        try {
            removeDiskFromVm(vmId, diskImage.getId());
            removeImage(diskImage);
        } catch (RuntimeException ex) {
            log.error("Failed adding new disk image and related entities to db", ex);
            throw new VdcBLLException(VdcBllErrors.DB, ex);
        }
    }

    public static void removeLunDisk(LunDisk lunDisk) {
        DbFacade.getInstance()
                .getVmDeviceDao()
                .remove(new VmDeviceId(lunDisk.getId(),
                        null));
        LUNs lun = lunDisk.getLun();
        DbFacade.getInstance()
                .getDiskLunMapDao()
                .remove(new DiskLunMapId(lunDisk.getId(), lun.getLUN_id()));
        DbFacade.getInstance().getBaseDiskDao().remove(lunDisk.getId());

        lun.setLunConnections(new ArrayList<StorageServerConnections>(DbFacade.getInstance()
                .getStorageServerConnectionDao()
                .getAllForLun(lun.getLUN_id())));

        if (!lun.getLunConnections().isEmpty()) {
            StorageHelperDirector.getInstance().getItem(
                    lun.getLunConnections().get(0).getstorage_type()).removeLun(lun);
        } else {
            // if there are no connections then the lun is fcp.
            StorageHelperDirector.getInstance().getItem(StorageType.FCP).removeLun(lun);
        }

    }

    // the last image in each list is the leaf
    public static Map<Guid, List<DiskImage>> getImagesLeaf(List<DiskImage> images) {
        Map<Guid, List<DiskImage>> retVal = new HashMap<Guid, List<DiskImage>>();
        for (DiskImage image : images) {
            MultiValueMapUtils.addToMap(image.getId(), image, retVal);
        }

        for (List<DiskImage> list : retVal.values()) {
            sortImageList(list);
        }
        return retVal;
    }

    private static void sortImageList(List<DiskImage> images) {
        List<DiskImage> hold = new ArrayList<DiskImage>();
        DiskImage curr = null;

        // find the first image
        for (int i = 0; i < images.size(); i++) {
            int pos = getFirstImage(images, images.get(i));
            if (pos == -1) {
                curr = images.get(i);
                hold.add(images.get(i));
                images.remove(images.get(i));
                break;
            }
        }

        while (images.size() > 0) {
            int pos = getNextImage(images, curr);
            if (pos == -1) {
                log.error("Image list error in SortImageList");
                break;
            }
            curr = images.get(pos);
            hold.add(images.get(pos));
            images.remove(images.get(pos));
        }

        for (DiskImage image : hold) {
            images.add(image);
        }
    }

    // function return the index of image that is its child
    private static int getNextImage(List<DiskImage> images, DiskImage curr) {
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getParentId().equals(curr.getImageId())) {
                return i;
            }
        }
        return -1;
    }

    // function return the index of the image that has no parent
    private static int getFirstImage(List<DiskImage> images, DiskImage curr) {
        for (int i = 0; i < images.size(); i++) {
            if (curr.getParentId().equals(images.get(i).getImageId())) {
                return i;
            }
        }
        return -1;
    }

    public static DiskImage getActiveVolumeDisk(List<DiskImage> diskList) {
        return diskList.get(diskList.size() - 1);
    }

    public static void removeImage(DiskImage diskImage) {
        DbFacade.getInstance()
                .getImageStorageDomainMapDao()
                .remove(diskImage.getImageId());
        DbFacade.getInstance().getDiskImageDynamicDao().remove(diskImage.getImageId());
        DbFacade.getInstance().getImageDao().remove(diskImage.getImageId());
    }

    public static void removeDiskFromVm(Guid vmGuid, Guid diskId) {
        DbFacade.getInstance().getVmDeviceDao().remove(new VmDeviceId(diskId, vmGuid));
        DbFacade.getInstance().getBaseDiskDao().remove(diskId);
    }

    public static void updateImageStatus(Guid imageId, ImageStatus imageStatus) {
        DbFacade.getInstance().getImageDao().updateStatus(imageId, imageStatus);
    }

}
