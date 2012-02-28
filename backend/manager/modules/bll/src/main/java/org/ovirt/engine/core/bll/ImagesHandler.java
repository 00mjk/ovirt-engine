package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ovirt.engine.core.bll.command.utils.StorageDomainSpaceChecker;
import org.ovirt.engine.core.bll.validator.StorageDomainValidator;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskImageBase;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.common.businessentities.storage_domain_static;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.vdscommands.GetImageInfoVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.IrsBaseVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.RefObject;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.backendcompat.Path;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public final class ImagesHandler {
    public static final Guid BlankImageTemplateId = new Guid("00000000-0000-0000-0000-000000000000");
    public static final String DefaultDriveName = "1";

    public static List<DiskImage> retrieveImagesByVm(Guid vmId) {
        List<DiskImage> disks = DbFacade.getInstance().getDiskImageDAO().getAllForVm(vmId);
        for (DiskImage disk : disks) {
            List<Guid> domainsIds = DbFacade.getInstance()
                    .getStorageDomainDAO()
                    .getAllImageStorageDomainIdsForImage(disk.getId());
            disk.setstorage_ids(new ArrayList<Guid>(domainsIds));
        }
        return disks;
    }

    /**
     * This function was developed especially for GUI needs.
     * It returns a list of all the snapshots of current image of a specific VM.
     * If there are two images mapped to same VM, it's assumed that this is a TryBackToImage case and
     * the function returns a list of snapshots of inactive images.
     * In this case the parent of the active image appears to be trybackfrom image
     *
     * @param imageId
     * @param imageTemplateId
     * @return
     */
    public static ArrayList<DiskImage> getAllImageSnapshots(Guid imageId, Guid imageTemplateId) {
        ArrayList<DiskImage> snapshots = new ArrayList<DiskImage>();
        Guid curImage = imageId;
        while (!imageTemplateId.equals(curImage) && !curImage.equals(Guid.Empty)) {
            DiskImage curDiskImage = DbFacade.getInstance().getDiskImageDAO().getSnapshotById(curImage);
            snapshots.add(curDiskImage);
            curImage = curDiskImage.getParentId();
        }
        return snapshots;
    }

    public static int getImagesMappedToDrive(Guid vmId, String drive, RefObject<DiskImage> activeImage,
            RefObject<DiskImage> inactiveImage) {
        return getImagesMappedToDrive(DbFacade.getInstance().getDiskImageDAO().getAllForVm(vmId),
                drive,
                activeImage,
                inactiveImage);
    }

    public static int getImagesMappedToDrive(
            List<DiskImage> disks,
            String drive,
            RefObject<DiskImage> activeImage,
            RefObject<DiskImage> inactiveImage) {
        String currentDrive = StringHelper.isNullOrEmpty(drive) ? DefaultDriveName : drive;
        activeImage.argvalue = null;
        inactiveImage.argvalue = null;
        int count = 0;
        for (DiskImage disk : disks) {
            if (StringHelper.EqOp(disk.getinternal_drive_mapping(), currentDrive)) {
                if (disk.getactive() != null && disk.getactive().equals(true)) {
                    activeImage.argvalue = disk;
                } else {
                    inactiveImage.argvalue = disk;
                }
                count++;
            }
        }
        return count;
    }

    public static String cdPathWindowsToLinux(String windowsPath, Guid storagePoolId) {
        if (StringHelper.isNullOrEmpty(windowsPath)) {
            return windowsPath; // empty string is used for 'eject'.
        }
        String fileName = Path.GetFileName(windowsPath);
        String isoPrefix = (String) Backend.getInstance().getResourceManager()
                .RunVdsCommand(VDSCommandType.IsoPrefix, new IrsBaseVDSCommandParameters(storagePoolId))
                .getReturnValue();
        return String.format("%1$s/%2$s", isoPrefix, fileName);
    }

    public static boolean isImagesExists(Iterable<DiskImage> images, Guid storagePoolId, Guid storageDomainId) {
        return isImagesExists(images, storagePoolId, storageDomainId, new RefObject<ArrayList<DiskImage>>());
    }

    private static boolean isImagesExists(Iterable<DiskImage> images, Guid storagePoolId, Guid domainId,
            RefObject<java.util.ArrayList<DiskImage>> irsImages) {
        irsImages.argvalue = new java.util.ArrayList<DiskImage>();

        for (DiskImage image : images) {
            DiskImage fromIrs;
            try {
                Guid storageDomainId =
                        image.getstorage_ids() != null && !image.getstorage_ids().isEmpty() ? image.getstorage_ids()
                                .get(0) : domainId;
                Guid imageGroupId = image.getimage_group_id() != null ? image.getimage_group_id().getValue()
                        : Guid.Empty;
                fromIrs = (DiskImage) Backend
                        .getInstance()
                        .getResourceManager()
                        .RunVdsCommand(
                                VDSCommandType.GetImageInfo,
                                new GetImageInfoVDSCommandParameters(storagePoolId, storageDomainId, imageGroupId,
                                        image.getId())).getReturnValue();
            } catch (java.lang.Exception e) {
                return false;
            }
            if (fromIrs == null) {
                return false;
            }

            irsImages.argvalue.add(fromIrs);
        }
        return true;
    }

    public static boolean isVmInPreview(List<DiskImage> images) {
        java.util.ArrayList<String> drives = new java.util.ArrayList<String>();
        for (DiskImage image : images) {
            if (drives.contains(image.getinternal_drive_mapping())) {
                return true;
            }
            drives.add(image.getinternal_drive_mapping());
        }

        return false;
    }

    public static boolean CheckImageConfiguration(storage_domain_static storageDomain,
            DiskImageBase diskInfo, java.util.ArrayList<String> messages) {
        boolean result = true;
        if ((diskInfo.getvolume_type() == VolumeType.Preallocated && diskInfo.getvolume_format() == VolumeFormat.COW)
                || ((storageDomain.getstorage_type() == StorageType.FCP || storageDomain.getstorage_type() == StorageType.ISCSI) && (diskInfo
                        .getvolume_type() == VolumeType.Sparse && diskInfo.getvolume_format() == VolumeFormat.RAW))
                || (diskInfo.getvolume_format() == VolumeFormat.Unassigned || diskInfo.getvolume_type() == VolumeType.Unassigned)) {
            // not supported
            result = false;
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_DISK_CONFIGURATION_NOT_SUPPORTED.toString());
        }
        return result;
    }

    public static boolean CheckImagesConfiguration(Guid storageDomainId,
            Collection<? extends DiskImageBase> disksConfigList,
            java.util.ArrayList<String> messages) {
        boolean result = true;
        storage_domain_static storageDomain = DbFacade.getInstance().getStorageDomainStaticDAO().get(storageDomainId);
        for (DiskImageBase diskInfo : disksConfigList) {
            result = CheckImageConfiguration(storageDomain, diskInfo, messages);
            if (!result)
                break;
        }
        return result;
    }

    public static boolean PerformImagesChecks(Guid vmGuid,
            java.util.ArrayList<String> messages,
            Guid storagePoolId,
            Guid storageDomainId,
            boolean diskSpaceCheck,
            boolean checkImagesLocked,
            boolean checkImagesIllegal,
            boolean checkImagesExist,
            boolean checkVmInPreview,
            boolean checkVmIsDown,
            boolean checkStorageDomain) {
        return PerformImagesChecks(vmGuid, messages, storagePoolId, storageDomainId, diskSpaceCheck,
                checkImagesLocked, checkImagesIllegal, checkImagesExist, checkVmInPreview,
                checkVmIsDown, checkStorageDomain, true);
    }

    public static boolean PerformImagesChecks(Guid vmGuid,
            ArrayList<String> messages,
            Guid storagePoolId,
            Guid storageDomainId,
            boolean diskSpaceCheck,
            boolean checkImagesLocked,
            boolean checkImagesIllegal,
            boolean checkImagesExist,
            boolean checkVmInPreview,
            boolean checkVmIsDown,
            boolean checkStorageDomain,
            boolean checkIsValid) {
        return PerformImagesChecks(vmGuid, messages, storagePoolId, storageDomainId, diskSpaceCheck,
                checkImagesLocked, checkImagesIllegal, checkImagesExist, checkVmInPreview,
                checkVmIsDown, checkStorageDomain, checkIsValid, null);
    }

    public static boolean PerformImagesChecks(Guid vmGuid,
            java.util.ArrayList<String> messages,
            Guid storagePoolId,
            Guid storageDomainId,
            boolean diskSpaceCheck,
            boolean checkImagesLocked,
            boolean checkImagesIllegal,
            boolean checkImagesExist,
            boolean checkVmInPreview,
            boolean checkVmIsDown,
            boolean checkStorageDomain,
            boolean checkIsValid, List<DiskImage> diskImageList) {

        boolean returnValue = true;
        boolean isValid = checkIsValid
                && ((Boolean) Backend.getInstance().getResourceManager()
                        .RunVdsCommand(VDSCommandType.IsValid, new IrsBaseVDSCommandParameters(storagePoolId))
                        .getReturnValue()).booleanValue();
        if (checkIsValid && !isValid) {
            returnValue = false;
            if (messages != null) {
                messages.add(VdcBllMessages.ACTION_TYPE_FAILED_IMAGE_REPOSITORY_NOT_FOUND.toString());
            }
        } else if (checkStorageDomain) {
            StorageDomainValidator storageDomainValidator =
                    new StorageDomainValidator(DbFacade.getInstance().getStorageDomainDAO().getForStoragePool(
                            storageDomainId, storagePoolId));
            returnValue = storageDomainValidator.isDomainExistAndActive(messages);
        }

        VmDynamic vm = DbFacade.getInstance().getVmDynamicDAO().get(vmGuid);
        if (returnValue && checkImagesLocked && vm.getstatus() == VMStatus.ImageLocked) {
            returnValue = false;
            if (messages != null) {
                messages.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_IMAGE_IS_LOCKED.toString());
            }
        } else if (returnValue && checkVmIsDown && vm.getstatus() != VMStatus.Down) {
            returnValue = false;
            if (messages != null) {
                messages.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_IS_NOT_DOWN.toString());
            }
        } else if (returnValue && isValid) {
            List<DiskImage> images;
            if (diskImageList == null) {
                images = DbFacade.getInstance().getDiskImageDAO().getAllForVm(vmGuid);
            } else {
                images = diskImageList;
            }
            if (images.size() > 0) {
                ArrayList<DiskImage> irsImages = null;
                Guid domainId = !Guid.Empty.equals(storageDomainId) ? storageDomainId : images.get(0)
                        .getstorage_ids().get(0);

                if (checkImagesExist) {
                    RefObject<ArrayList<DiskImage>> tempRefObject =
                            new RefObject<ArrayList<DiskImage>>();
                    boolean isImagesExist = isImagesExists(images, storagePoolId, domainId, tempRefObject);
                    irsImages = tempRefObject.argvalue;
                    if (!isImagesExist) {
                        returnValue = false;
                        if (messages != null) {
                            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_IMAGE_DOES_NOT_EXIST.toString());
                        }
                    }
                }
                if (returnValue && checkImagesIllegal) {
                    returnValue = CheckImagesLegality(messages, images, vm, irsImages);
                }
                if (returnValue && checkVmInPreview && isVmInPreview(images)) {
                    returnValue = false;
                    if (messages != null) {
                        messages.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_IN_PREVIEW.toString());
                    }
                }
                if (returnValue && diskSpaceCheck) {
                    storage_domains domain = DbFacade.getInstance().getStorageDomainDAO().get(domainId);
                    if (!StorageDomainSpaceChecker.isBelowThresholds(domain)) {
                        returnValue = false;
                        if (messages != null) {
                            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW.toString());
                        }
                    }
                }
            } else if (checkImagesExist) {
                returnValue = false;
                if (messages != null) {
                    messages.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_HAS_NO_DISKS.toString());
                }
            }
        }
        return returnValue;
    }

    private static boolean CheckImagesLegality(List<String> messages,
            List<DiskImage> images, VmDynamic vm, List<DiskImage> irsImages) {
        boolean returnValue = true;
        if (vm.getstatus() == VMStatus.ImageIllegal) {
            returnValue = false;
            if (messages != null) {
                messages.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_IMAGE_IS_ILLEGAL.toString());
            }
        } else {
            int i = 0;
            for (DiskImage diskImage : images) {
                if (diskImage != null) {
                    DiskImage image = irsImages.get(i++);

                    if (image.getimageStatus() != ImageStatus.OK) {
                        diskImage.setimageStatus(image.getimageStatus());
                        DbFacade.getInstance().getDiskImageDAO().update(diskImage);

                        returnValue = false;
                        if (messages != null) {
                            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_IMAGE_IS_ILLEGAL.toString());
                        }

                        break;
                    }
                }
            }
        }
        return returnValue;
    }
}
