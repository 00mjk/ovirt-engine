package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;
import java.util.Collections;

import org.ovirt.engine.core.common.action.MoveOrCopyImageGroupParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.comparators.NameableComparator;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.ICommandTarget;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.SelectedQuotaValidation;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public abstract class MoveOrCopyDiskModel extends DisksAllocationModel implements ICommandTarget
{
    private ArrayList<DiskModel> allDisks;

    public ArrayList<DiskModel> getAllDisks()
    {
        return allDisks;
    }

    public void setAllDisks(ArrayList<DiskModel> value)
    {
        if (allDisks != value)
        {
            allDisks = value;
            onPropertyChanged(new PropertyChangedEventArgs("All Disks")); //$NON-NLS-1$
        }
    }

    private ArrayList<DiskImage> diskImages;

    public ArrayList<DiskImage> getDiskImages()
    {
        return diskImages;
    }

    public void setDiskImages(ArrayList<DiskImage> value)
    {
        if (diskImages != value)
        {
            diskImages = value;
            onPropertyChanged(new PropertyChangedEventArgs("Disk Images")); //$NON-NLS-1$
        }
    }

    private Guid vmId;

    public Guid getVmId() {
        return vmId;
    }

    public void setVmId(Guid vmId) {
        this.vmId = vmId;
    }

    public abstract void init(ArrayList<DiskImage> diskImages);

    protected abstract void initStorageDomains();

    protected abstract VdcActionType getActionType();

    protected abstract String getWarning();

    protected abstract String getNoActiveSourceDomainMessage();

    protected abstract String getNoActiveTargetDomainMessage();

    protected abstract MoveOrCopyImageGroupParameters createParameters(
            Guid sourceStorageDomainGuid,
            Guid destStorageDomainGuid,
            DiskImage disk);

    public MoveOrCopyDiskModel() {
        setAllDisks(new ArrayList<DiskModel>());
        setActiveStorageDomains(new ArrayList<StorageDomain>());
    }

    protected void onInitDisks() {
        ArrayList<DiskModel> disks = new ArrayList<DiskModel>();
        for (DiskImage disk : getDiskImages())
        {
            disks.add(Linq.diskToModel(disk));
        }
        setDisks(disks);

        initStorageDomains();
    }

    protected void onInitAllDisks(ArrayList<Disk> disks) {
        for (Disk disk : disks)
        {
            if (disk.getDiskStorageType() == DiskStorageType.IMAGE) {
                allDisks.add(Linq.diskToModel(disk));
            }
        }
    }

    protected void onInitStorageDomains(ArrayList<StorageDomain> storages) {
        for (StorageDomain storage : storages) {
            if (Linq.isDataActiveStorageDomain(storage)) {
                getActiveStorageDomains().add(storage);
            }
        }
        Collections.sort(getActiveStorageDomains(), new NameableComparator());

        if (!storages.isEmpty()) {
            AsyncDataProvider.getDataCenterById(new AsyncQuery(this, new INewAsyncCallback() {
                @Override
                public void onSuccess(Object target, Object returnValue) {
                    MoveOrCopyDiskModel model = (MoveOrCopyDiskModel) target;
                    StoragePool dataCenter = (StoragePool) returnValue;

                    model.setQuotaEnforcementType(dataCenter.getQuotaEnforcementType());
                    model.postInitStorageDomains();
                }
            }), storages.get(0).getStoragePoolId());
        }
        else {
            postInitStorageDomains();
        }
    }

    protected void postInitStorageDomains() {
        boolean showWarning = false;

        for (DiskModel disk : getDisks()) {
            DiskImage diskImage = ((DiskImage) disk.getDisk());

            // Source storage domains
            ArrayList<Guid> diskStorageIds = diskImage.getStorageIds();
            ArrayList<StorageDomain> sourceStorageDomains =
                    Linq.getStorageDomainsByIds(diskStorageIds, getActiveStorageDomains());

            // Destination storage domains
            ArrayList<StorageDomain> destStorageDomains =
                    Linq.except(getActiveStorageDomains(), sourceStorageDomains);
            destStorageDomains = filterStoragesByDatacenterId(destStorageDomains, diskImage.getStoragePoolId());

            // Filter storage domains with missing template disk
            boolean isDiskBasedOnTemplate = !diskImage.getParentId().equals(Guid.Empty);
            if (isDiskBasedOnTemplate) {
                destStorageDomains = Linq.except(destStorageDomains, getMissingStorages(destStorageDomains, disk));
            }

            // Add prohibition reasons
            if (sourceStorageDomains.isEmpty() || destStorageDomains.isEmpty()) {
                showWarning = true;
                updateChangeability(disk, isDiskBasedOnTemplate,
                        sourceStorageDomains.isEmpty(), destStorageDomains.isEmpty());
            }

            // Sort and add storage domains
            Collections.sort(destStorageDomains, new NameableComparator());
            Collections.sort(sourceStorageDomains, new NameableComparator());
            disk.getStorageDomain().setItems(destStorageDomains);
            disk.getSourceStorageDomain().setItems(sourceStorageDomains);
            addSourceStorageDomainName(disk, sourceStorageDomains);
        }

        sortDisks();
        postCopyOrMoveInit(showWarning);
    }

    private void updateChangeability(DiskModel disk, boolean isDiskBasedOnTemplate, boolean noSources, boolean noTargets) {
        disk.getStorageDomain().setIsChangable(!noTargets);
        disk.getSourceStorageDomain().setIsChangable(!noSources);
        disk.getSourceStorageDomainName().setIsChangable(!noSources);
        disk.getStorageDomain().setChangeProhibitionReason(isDiskBasedOnTemplate ?
                constants.noActiveStorageDomainWithTemplateMsg() : getNoActiveTargetDomainMessage());
        disk.getSourceStorageDomain().setChangeProhibitionReason(getNoActiveSourceDomainMessage());
        disk.getSourceStorageDomainName().setChangeProhibitionReason(getNoActiveSourceDomainMessage());
    }

    private void addSourceStorageDomainName(DiskModel disk, ArrayList<StorageDomain> sourceStorageDomains) {
        String sourceStorageName = sourceStorageDomains.isEmpty() ?
                constants.notAvailableLabel() : sourceStorageDomains.get(0).getStorageName();
        disk.getSourceStorageDomainName().setEntity(sourceStorageName);
    }

    protected void postCopyOrMoveInit(boolean showWarning) {
        ICommandTarget target = (ICommandTarget) getEntity();

        if (getActiveStorageDomains().isEmpty()) {
            setMessage(constants.noStorageDomainAvailableMsg());

            UICommand closeCommand = new UICommand("Cancel", target); //$NON-NLS-1$
            closeCommand.setTitle(constants.close());
            closeCommand.setIsDefault(true);
            closeCommand.setIsCancel(true);
            getCommands().add(closeCommand);
        }
        else
        {
            if (showWarning) {
                setMessage(getWarning());
            }

            UICommand actionCommand = new UICommand("OnExecute", this); //$NON-NLS-1$
            actionCommand.setTitle(constants.ok());
            actionCommand.setIsDefault(true);
            getCommands().add(actionCommand);
            UICommand cancelCommand = new UICommand("Cancel", target); //$NON-NLS-1$
            cancelCommand.setTitle(constants.cancel());
            cancelCommand.setIsCancel(true);
            getCommands().add(cancelCommand);
        }

        stopProgress();
    }

    protected ArrayList<StorageDomain> filterStoragesByDatacenterId(ArrayList<StorageDomain> storageDomains,
            Guid diskDatacenterId) {

        ArrayList<StorageDomain> storages = new ArrayList<StorageDomain>();
        for (StorageDomain storage : storageDomains) {
            if (storage.getStoragePoolId().equals(diskDatacenterId)) {
                storages.add(storage);
            }
        }

        return storages;
    }

    protected ArrayList<StorageDomain> getMissingStorages(ArrayList<StorageDomain> storageDomains, DiskModel vmdisk) {
        ArrayList<StorageDomain> missingStorageDomains = new ArrayList<StorageDomain>();
        DiskModel templateDisk = getTemplateDiskByVmDisk(vmdisk);

        if (templateDisk != null) {
            for (StorageDomain storageDomain : storageDomains) {
                if (!((DiskImage) templateDisk.getDisk()).getStorageIds().contains(storageDomain.getId())) {
                    missingStorageDomains.add(storageDomain);
                }
            }
        }

        return missingStorageDomains;
    }

    protected DiskModel getTemplateDiskByVmDisk(DiskModel vmdisk) {
        for (DiskModel disk : getAllDisks()) {
            if (((DiskImage) disk.getDisk()).getImageId().equals(((DiskImage) vmdisk.getDisk()).getParentId())) {
                return disk;
            }
        }

        return null;
    }

    protected void onExecute() {
        if (this.getProgress() != null)
        {
            return;
        }

        if (!this.validate()) {
            return;
        }

        startProgress(null);
    }

    protected ArrayList<VdcActionParametersBase> getParameters() {
        ArrayList<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();
        for (DiskModel diskModel : getDisks())
        {
            StorageDomain destStorageDomain = (StorageDomain) diskModel.getStorageDomain().getSelectedItem();
            StorageDomain sourceStorageDomain =
                    (StorageDomain) diskModel.getSourceStorageDomain().getSelectedItem();

            Guid sourceStorageDomainGuid = sourceStorageDomain != null ? sourceStorageDomain.getId() : Guid.Empty;
            DiskImage disk = (DiskImage) diskModel.getDisk();
            if (diskModel.getQuota().getSelectedItem() != null) {
                disk.setQuotaId(((Quota) diskModel.getQuota().getSelectedItem()).getId());
            }

            if (destStorageDomain == null || sourceStorageDomain == null) {
                continue;
            }

            Guid destStorageDomainGuid = destStorageDomain.getId();
            addMoveOrCopyParameters(parameters,
                    sourceStorageDomainGuid,
                    destStorageDomainGuid,
                    disk);
        }

        return parameters;
    }

    protected void addMoveOrCopyParameters(ArrayList<VdcActionParametersBase> parameters,
            Guid sourceStorageDomainGuid,
            Guid destStorageDomainGuid,
            DiskImage disk) {

        MoveOrCopyImageGroupParameters params = createParameters(sourceStorageDomainGuid, destStorageDomainGuid, disk);
        params.setQuotaId(disk.getQuotaId());

        parameters.add(params);
    }

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);

        onExecute();
    }

    public boolean validate() {
        if (getQuotaEnforcementType() == QuotaEnforcementTypeEnum.DISABLED
                || getQuotaEnforcementType() == QuotaEnforcementTypeEnum.SOFT_ENFORCEMENT) {
            return true;
        }

        boolean isValid = true;
        for (DiskModel diskModel : getDisks()) {
            diskModel.getQuota().validateSelectedItem(new IValidation[] { new SelectedQuotaValidation() });
            isValid &= diskModel.getQuota().getIsValid();
        }

        return isValid;
    }

    protected void cancel() {
        stopProgress();
        ((ListModel) getEntity()).setWindow(null);
    }
}
