package org.ovirt.engine.core.bll.storage.disk.managedblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.CommandBase;
import org.ovirt.engine.core.bll.InternalCommandAttribute;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.storage.disk.managedblock.util.ManagedBlockStorageDiskUtil;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddManagedBlockStorageDiskParameters;
import org.ovirt.engine.core.common.businessentities.SubjectEntity;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.storage.DiskVmElement;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.businessentities.storage.ManagedBlockStorage;
import org.ovirt.engine.core.common.businessentities.storage.ManagedBlockStorageDisk;
import org.ovirt.engine.core.common.businessentities.storage.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.storage.VolumeType;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.SizeConverter;
import org.ovirt.engine.core.common.utils.cinderlib.CinderlibCommandParameters;
import org.ovirt.engine.core.common.utils.cinderlib.CinderlibExecutor;
import org.ovirt.engine.core.common.utils.cinderlib.CinderlibExecutor.CinderlibCommand;
import org.ovirt.engine.core.common.utils.cinderlib.CinderlibReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.BaseDiskDao;
import org.ovirt.engine.core.dao.CinderStorageDao;
import org.ovirt.engine.core.dao.DiskVmElementDao;
import org.ovirt.engine.core.dao.ImageDao;
import org.ovirt.engine.core.utils.JsonHelper;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@NonTransactiveCommandAttribute
@InternalCommandAttribute
public class AddManagedBlockStorageDiskCommand<T extends AddManagedBlockStorageDiskParameters> extends CommandBase<T> {

    @Inject
    private CinderStorageDao cinderStorageDao;

    @Inject
    private CinderlibExecutor cinderlibExecutor;

    @Inject
    private BaseDiskDao baseDiskDao;

    @Inject
    private ImageDao imageDao;

    @Inject
    private ManagedBlockStorageDiskUtil managedBlockStorageDiskUtil;

    @Inject
    private DiskVmElementDao diskVmElementDao;

    @Inject
    private VmDeviceUtils vmDeviceUtils;

    public AddManagedBlockStorageDiskCommand(Guid commandId) {
        super(commandId);
    }

    public AddManagedBlockStorageDiskCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    @Override
    protected void executeCommand() {
        ManagedBlockStorage managedBlockStorage = cinderStorageDao.get(getParameters().getStorageDomainId());
        Guid volumeId = getParameters().getDiskInfo().getId();
        List<String> extraParams = new ArrayList<>();
        extraParams.add(volumeId.toString());
        Number sizeInGiB = SizeConverter.convert(getParameters().getDiskInfo().getSize(),
                SizeConverter.SizeUnit.BYTES,
                SizeConverter.SizeUnit.GiB);
        extraParams.add(Long.toString(sizeInGiB.longValue()));
        CinderlibReturnValue returnValue;

        try {
            CinderlibCommandParameters params =
                    new CinderlibCommandParameters(JsonHelper.mapToJson(
                                managedBlockStorage.getAllDriverOptions(),
                            false),
                                extraParams,
                                getCorrelationId());
            returnValue = cinderlibExecutor.runCommand(CinderlibCommand.CREATE_VOLUME, params);
        } catch (Exception e) {
            log.error("Failed executing volume creation", e);
            return;
        }

        if (!returnValue.getSucceed()) {
            return;
        }

        saveDisk(volumeId);
        getReturnValue().setActionReturnValue(volumeId);
        setSucceeded(true);
        persistCommandIfNeeded();
    }

    private void saveDisk(Guid volumeId) {
        ManagedBlockStorageDisk disk = createDisk();
        disk.setImageId(volumeId);
        disk.setId(volumeId);
        TransactionSupport.executeInNewTransaction(() -> {
            baseDiskDao.save(disk);
            imageDao.save(disk.getImage());
            managedBlockStorageDiskUtil.saveDisk(disk);

            if (getParameters().getVmId() != null) {
                // Set correct device id
                DiskVmElement diskVmElement = getParameters().getDiskVmElement();
                diskVmElement.getId().setDeviceId(volumeId);
                addDiskVmElementForDisk(diskVmElement);
                addManagedDeviceForDisk(volumeId);
            }

            managedBlockStorageDiskUtil.lockImage(disk.getImageId());

            return null;
        });
    }

    private DiskVmElement addDiskVmElementForDisk(DiskVmElement diskVmElement) {
        diskVmElementDao.save(diskVmElement);
        return diskVmElement;
    }

    protected VmDevice addManagedDeviceForDisk(Guid diskId) {
        return vmDeviceUtils.addDiskDevice(
                getParameters().getVmId(),
                diskId,
                getParameters().isShouldPlugDiskToVm(),
                Boolean.TRUE.equals(getParameters().getDiskVmElement().isReadOnly()));
    }

    private ManagedBlockStorageDisk createDisk() {
        ManagedBlockStorageDisk disk = new ManagedBlockStorageDisk();
        disk.setDiskAlias(getParameters().getDiskInfo().getDiskAlias());
        disk.setSize(getParameters().getDiskInfo().getSize());
        disk.setDiskDescription(getParameters().getDiskInfo().getDiskDescription());
        disk.setShareable(getParameters().getDiskInfo().isShareable());
        disk.setStorageIds(new ArrayList<>(Arrays.asList(getParameters().getStorageDomainId())));
        disk.setVolumeType(VolumeType.Unassigned);
        disk.setVolumeFormat(VolumeFormat.RAW);
        disk.setImageStatus(ImageStatus.OK);
        disk.setCreationDate(new Date());
        disk.setLastModified(new Date());
        disk.setActive(true);
        disk.setVmSnapshotId(getParameters().getVmSnapshotId());
        disk.setDiskVmElements(Collections.singletonList(getParameters().getDiskVmElement()));

        return disk;
    }

    @Override
    protected boolean validate() {
        return true;
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        return Collections.emptyMap();
    }

    @Override
    protected Map<String, Pair<String, String>> getSharedLocks() {
        return Collections.emptyMap();
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return null;
    }

    @Override
    protected Collection<SubjectEntity> getSubjectEntities() {
        return Collections.singleton(new SubjectEntity(VdcObjectType.Storage, getParameters().getStorageDomainId()));
    }

    @Override
    protected void endSuccessfully() {
        managedBlockStorageDiskUtil.unlockImage(getParameters().getDiskInfo().getId());
        super.endSuccessfully();
    }
}
