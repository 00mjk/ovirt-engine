package org.ovirt.engine.core.bll.validator;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.VolumeType;

@RunWith(Parameterized.class)
public class StorageDomainValidatorFreeSpaceTest {
    private DiskImage disk;
    private StorageDomain sd;
    private boolean isValidForNew;

    public StorageDomainValidatorFreeSpaceTest(DiskImage disk,
                                               StorageDomain sd,
                                               boolean isValidForNew) {
        this.disk = disk;
        this.sd = sd;
        this.isValidForNew = isValidForNew;
    }

    @Parameters
    public static Collection<Object[]> createParams() {
        List<Object[]> params = new ArrayList<>();

        for (StorageType storageType : StorageType.values()) {
            if (storageType.isConcreteStorageType()) {
                List<VolumeType> volumeTypes =
                        storageType.isFileDomain() ? Arrays.asList(VolumeType.Preallocated, VolumeType.Sparse)
                                : Collections.singletonList(VolumeType.Preallocated);
                for (VolumeType volumeType : volumeTypes) {
                    for (VolumeFormat volumeFormat : new VolumeFormat[] { VolumeFormat.COW, VolumeFormat.RAW }) {
                        DiskImage disk = new DiskImage();
                        disk.setvolumeFormat(volumeFormat);
                        disk.setVolumeType(volumeType);
                        disk.getSnapshots().add(disk);
                        disk.setSizeInGigabytes(200);
                        disk.setActualSize(100); // GB

                        StorageDomain sd = new StorageDomain();
                        sd.setStorageType(storageType);
                        sd.setAvailableDiskSize(107); // GB

                        params.add(new Object[] { disk, sd,
                                volumeFormat == VolumeFormat.COW || volumeType == VolumeType.Sparse });
                    }
                }
            }
        }

        return params;
    }

    @Test
    public void testValidateNewDisk() {
        StorageDomainValidator sdValidator = new StorageDomainValidator(sd);
        assertEquals(disk.getVolumeFormat() + ", " + disk.getVolumeType() + ", " + sd.getStorageType(),
                isValidForNew,
                sdValidator.hasSpaceForNewDisk(disk).isValid());
    }

}
