package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.action.AttachDettachVmDiskParameters;
import org.ovirt.engine.core.common.action.ChangeQuotaParameters;
import org.ovirt.engine.core.common.action.GetDiskAlignmentParameters;
import org.ovirt.engine.core.common.action.HotPlugDiskToVmParameters;
import org.ovirt.engine.core.common.action.RemoveDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.Linq.DiskByAliasComparer;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ISupportSystemTreeContext;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.ChangeQuotaItemModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.ChangeQuotaModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

@SuppressWarnings("unused")
public class VmDiskListModel extends VmDiskListModelBase
{

    private UICommand privateNewCommand;

    public UICommand getNewCommand()
    {
        return privateNewCommand;
    }

    private void setNewCommand(UICommand value)
    {
        privateNewCommand = value;
    }

    private UICommand privateEditCommand;

    @Override
    public UICommand getEditCommand()
    {
        return privateEditCommand;
    }

    private void setEditCommand(UICommand value)
    {
        privateEditCommand = value;
    }

    private UICommand privateRemoveCommand;

    public UICommand getRemoveCommand()
    {
        return privateRemoveCommand;
    }

    private void setRemoveCommand(UICommand value)
    {
        privateRemoveCommand = value;
    }

    private UICommand privatePlugCommand;

    public UICommand getPlugCommand()
    {
        return privatePlugCommand;
    }

    private void setPlugCommand(UICommand value)
    {
        privatePlugCommand = value;
    }

    private UICommand privateUnPlugCommand;

    public UICommand getUnPlugCommand()
    {
        return privateUnPlugCommand;
    }

    private void setUnPlugCommand(UICommand value)
    {
        privateUnPlugCommand = value;
    }

    ISupportSystemTreeContext systemTreeContext;

    public ISupportSystemTreeContext getSystemTreeContext() {
        return systemTreeContext;
    }

    public void setSystemTreeContext(ISupportSystemTreeContext systemTreeContext) {
        this.systemTreeContext = systemTreeContext;
    }

    private UICommand privateChangeQuotaCommand;

    public UICommand getChangeQuotaCommand()
    {
        return privateChangeQuotaCommand;
    }

    private void setChangeQuotaCommand(UICommand value)
    {
        privateChangeQuotaCommand = value;
    }

    private UICommand privateMoveCommand;

    public UICommand getMoveCommand()
    {
        return privateMoveCommand;
    }

    private void setMoveCommand(UICommand value)
    {
        privateMoveCommand = value;
    }

    private UICommand privateScanAlignmentCommand;

    public UICommand getScanAlignmentCommand()
    {
        return privateScanAlignmentCommand;
    }

    private void setScanAlignmentCommand(UICommand value)
    {
        privateScanAlignmentCommand = value;
    }

    private boolean privateIsDiskHotPlugSupported;

    public boolean getIsDiskHotPlugSupported()
    {
        VM vm = getEntity();
        boolean isVmStatusApplicableForHotPlug =
                vm != null && (vm.getStatus() == VMStatus.Up || vm.getStatus() == VMStatus.Down ||
                        vm.getStatus() == VMStatus.Paused || vm.getStatus() == VMStatus.Suspended);

        return privateIsDiskHotPlugSupported && isVmStatusApplicableForHotPlug;
    }

    private void setIsDiskHotPlugSupported(boolean value)
    {
        if (privateIsDiskHotPlugSupported != value)
        {
            privateIsDiskHotPlugSupported = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsDiskHotPlugSupported")); //$NON-NLS-1$
        }
    }

    private boolean isLiveStorageMigrationEnabled;

    public boolean getIsLiveStorageMigrationEnabled()
    {
        return isLiveStorageMigrationEnabled;
    }

    private void setIsLiveStorageMigrationEnabled(boolean value)
    {
        if (isLiveStorageMigrationEnabled != value)
        {
            isLiveStorageMigrationEnabled = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsLiveStorageMigrationEnabled")); //$NON-NLS-1$
        }
    }

    private boolean isScanAlignmentEnabled;

    public boolean getIsScanAlignmentEnabled()
    {
        return isScanAlignmentEnabled;
    }

    private void setIsScanAlignmentEnabled(boolean value)
    {
        if (isScanAlignmentEnabled != value)
        {
            isScanAlignmentEnabled = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsScanAlignmentEnabled")); //$NON-NLS-1$
        }
    }

    public boolean isExtendImageSizeEnabled() {
        return (getEntity() != null) ?
                VdcActionUtils.canExecute(Arrays.asList(getEntity()), VM.class, VdcActionType.ExtendImageSize) : false;
    }

    public VmDiskListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().disksTitle());
        setHashName("disks"); //$NON-NLS-1$

        setNewCommand(new UICommand("New", this)); //$NON-NLS-1$
        setEditCommand(new UICommand("Edit", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$
        setPlugCommand(new UICommand("Plug", this)); //$NON-NLS-1$
        setUnPlugCommand(new UICommand("Unplug", this)); //$NON-NLS-1$
        setMoveCommand(new UICommand("Move", this)); //$NON-NLS-1$
        setScanAlignmentCommand(new UICommand("Scan Alignment", this)); //$NON-NLS-1$
        setChangeQuotaCommand(new UICommand("changeQuota", this)); //$NON-NLS-1$
        getChangeQuotaCommand().setIsAvailable(false);

        updateActionAvailability();
    }

    @Override
    public VM getEntity()
    {
        return (VM) super.getEntity();
    }

    public void setEntity(VM value)
    {
        super.setEntity(value);
    }

    @Override
    protected void onEntityChanged()
    {
        super.onEntityChanged();

        if (getEntity() != null)
        {
            updateDataCenterVersion();
            getSearchCommand().execute();
            updateIsDiskHotPlugAvailable();
            updateLiveStorageMigrationEnabled();
            updateScanAlignmentEnabled();
        }

        updateActionAvailability();
    }

    @Override
    protected void syncSearch()
    {
        if (getEntity() == null)
        {
            return;
        }
        VM vm = getEntity();

        super.syncSearch(VdcQueryType.GetAllDisksByVmId, new IdQueryParameters(vm.getId()));
    }

    @Override
    public void setItems(Iterable value)
    {
        ArrayList<Disk> disks =
                value != null ? Linq.<Disk> cast(value) : new ArrayList<Disk>();

        Collections.sort(disks, new DiskByAliasComparer());
        super.setItems(disks);

        updateActionAvailability();
    }

    private void newEntity()
    {
        final VM vm = getEntity();

        if (getWindow() != null)
        {
            return;
        }

        NewDiskModel model = new NewDiskModel();
        model.setTitle(ConstantsManager.getInstance().getConstants().addVirtualDiskTitle());
        model.setHashName("new_virtual_disk"); //$NON-NLS-1$
        model.setVm(vm);
        setWindow(model);

        UICommand cancelCommand = new UICommand("Cancel", this); //$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);
        model.setCancelCommand(cancelCommand);

        model.initialize();
    }

    private void changeQuota() {
        ArrayList<DiskImage> disks = (ArrayList<DiskImage>) getSelectedItems();

        if (disks == null || getWindow() != null)
        {
            return;
        }

        ChangeQuotaModel model = new ChangeQuotaModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().assignQuotaForDisk());
        model.setHashName("change_quota_disks"); //$NON-NLS-1$
        model.startProgress(null);
        model.init(disks);

        UICommand command = new UICommand("onChangeQuota", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().ok());
        command.setIsDefault(true);
        model.getCommands().add(command);
        command = new UICommand("Cancel", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        command.setIsCancel(true);
        model.getCommands().add(command);
    }

    private void onChangeQuota() {
        ChangeQuotaModel model = (ChangeQuotaModel) getWindow();
        ArrayList<VdcActionParametersBase> paramerterList = new ArrayList<VdcActionParametersBase>();

        for (Object item : model.getItems())
        {
            ChangeQuotaItemModel itemModel = (ChangeQuotaItemModel) item;
            DiskImage disk = (DiskImage) itemModel.getEntity();
            VdcActionParametersBase parameters =
                    new ChangeQuotaParameters(((Quota) itemModel.getQuota().getSelectedItem()).getId(),
                            disk.getId(),
                            disk.getStorageIds().get(0),
                            disk.getStoragePoolId());
            paramerterList.add(parameters);
        }

        model.startProgress(null);

        Frontend.RunMultipleAction(VdcActionType.ChangeQuotaForDisk, paramerterList,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {
                        cancel();
                    }
                },
                this);
    }

    private void edit()
    {
        final Disk disk = (Disk) getSelectedItem();

        if (getWindow() != null)
        {
            return;
        }

        EditDiskModel model = new EditDiskModel();
        model.setTitle(ConstantsManager.getInstance().getConstants().editVirtualDiskTitle());
        model.setHashName("edit_virtual_disk"); //$NON-NLS-1$
        model.setVm(getEntity());
        model.setDisk(disk);
        setWindow(model);

        UICommand cancelCommand = new UICommand("Cancel", this); //$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);
        model.setCancelCommand(cancelCommand);

        model.initialize();
    }

    private void remove()
    {
        if (getWindow() != null)
        {
            return;
        }

        boolean hasSystemDiskWarning = false;
        RemoveDiskModel model = new RemoveDiskModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().removeDisksTitle());
        model.setHashName("remove_disk"); //$NON-NLS-1$

        model.getLatch().setEntity(false);

        ArrayList<DiskModel> items = new ArrayList<DiskModel>();
        for (Object item : getSelectedItems())
        {
            Disk disk = (Disk) item;

            DiskModel diskModel = new DiskModel();
            diskModel.setDisk(disk);
            diskModel.setVm(getEntity());

            items.add(diskModel);

            // A shared disk or a disk snapshot can only be detached
            if (disk.getNumberOfVms() > 1) {
                model.getLatch().setIsChangable(false);
            }
        }
        model.setItems(items);

        UICommand tempVar = new UICommand("OnRemove", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    private void onRemove() {
        VM vm = getEntity();
        RemoveDiskModel model = (RemoveDiskModel) getWindow();
        boolean removeDisk = (Boolean) model.getLatch().getEntity();
        VdcActionType actionType = removeDisk ? VdcActionType.RemoveDisk : VdcActionType.DetachDiskFromVm;
        ArrayList<VdcActionParametersBase> paramerterList = new ArrayList<VdcActionParametersBase>();

        for (Object item : getSelectedItems()) {
            Disk disk = (Disk) item;
            VdcActionParametersBase parameters = removeDisk ?
                    new RemoveDiskParameters(disk.getId()) :
                    new AttachDettachVmDiskParameters(vm.getId(), disk.getId(), true);

            paramerterList.add(parameters);
        }

        model.startProgress(null);

        Frontend.RunMultipleAction(actionType, paramerterList,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {
                        VmDiskListModel localModel = (VmDiskListModel) result.getState();
                        localModel.stopProgress();
                        cancel();
                    }
                },
                this);
    }

    private void plug(boolean plug) {
        VM vm = getEntity();

        ArrayList<VdcActionParametersBase> paramerterList = new ArrayList<VdcActionParametersBase>();
        for (Object item : getSelectedItems()) {
            Disk disk = (Disk) item;
            disk.setPlugged(plug);

            paramerterList.add(new HotPlugDiskToVmParameters(vm.getId(), disk.getId()));
        }

        VdcActionType plugAction = VdcActionType.HotPlugDiskToVm;
        if (!plug) {
            plugAction = VdcActionType.HotUnPlugDiskFromVm;
        }

        Frontend.RunMultipleAction(plugAction, paramerterList,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {
                    }
                },
                this);
    }

    private void move()
    {
        ArrayList<DiskImage> disks = (ArrayList<DiskImage>) getSelectedItems();

        if (disks == null)
        {
            return;
        }

        if (getWindow() != null)
        {
            return;
        }

        VM vm = getEntity();

        MoveDiskModel model = new MoveDiskModel();
        setWindow(model);
        model.setVmId(vm.getStatus() == VMStatus.Up ? vm.getId() : null);
        model.setWarningAvailable(vm.getStatus() == VMStatus.Up);
        model.setMessage(vm.getStatus() == VMStatus.Up ?
                ConstantsManager.getInstance().getConstants().liveStorageMigrationWarning() :
                null);
        model.setTitle(ConstantsManager.getInstance().getConstants().moveDisksTitle());
        model.setHashName("move_disk"); //$NON-NLS-1$
        model.setIsSourceStorageDomainNameAvailable(true);
        model.setEntity(this);
        model.init(disks);
        model.startProgress(null);
    }

    private void scanAlignment()
    {
        ArrayList<VdcActionParametersBase> parameterList = new ArrayList<VdcActionParametersBase>();

        for (Disk disk : (ArrayList<Disk>) getSelectedItems())
        {
            parameterList.add(new GetDiskAlignmentParameters(disk.getId()));
        }

        Frontend.RunMultipleAction(VdcActionType.GetDiskAlignment, parameterList,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {
                    }
                },
                this);
    }

    private void cancel()
    {
        setWindow(null);
        Frontend.getInstance().unsubscribe();
    }

    @Override
    protected void onSelectedItemChanged()
    {
        super.onSelectedItemChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemsChanged()
    {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    @Override
    protected void entityPropertyChanged(Object sender, PropertyChangedEventArgs e)
    {
        super.entityPropertyChanged(sender, e);

        if (e.PropertyName.equals("status")) //$NON-NLS-1$
        {
            updateActionAvailability();
        }
    }

    private void updateActionAvailability()
    {
        Disk disk = (Disk) getSelectedItem();

        getNewCommand().setIsExecutionAllowed(true);

        getEditCommand().setIsExecutionAllowed(disk != null && isSingleDiskSelected() && !isDiskLocked(disk) &&
                (isVmDown() || !disk.getPlugged() || (isExtendImageSizeSupported() && isExtendImageSizeEnabled())));

        getRemoveCommand().setIsExecutionAllowed(atLeastOneDiskSelected() && isRemoveCommandAvailable());

        getMoveCommand().setIsExecutionAllowed(atLeastOneDiskSelected()
                && (isMoveCommandAvailable() || isLiveMoveCommandAvailable()));

        updateGetAlignmentCommandAvailability();

        getPlugCommand().setIsExecutionAllowed(isPlugCommandAvailable(true));

        getUnPlugCommand().setIsExecutionAllowed(isPlugCommandAvailable(false));

        ChangeQuotaModel.updateChangeQuotaActionAvailability(getItems() != null ? (List<Disk>) getItems() : null,
                getSelectedItems() != null ? (List<Disk>) getSelectedItems() : null,
                getSystemTreeSelectedItem(),
                getChangeQuotaCommand());
    }

    public boolean isVmDown() {
        VM vm = getEntity();
        return vm != null && vm.getStatus() == VMStatus.Down;
    }

    private boolean isDiskLocked(Disk disk) {
        return disk != null && disk.getDiskStorageType() == DiskStorageType.IMAGE &&
                ((DiskImage) disk).getImageStatus() == ImageStatus.LOCKED;
    }

    private boolean isSingleDiskSelected() {
        return getSelectedItems() != null && getSelectedItems().size() == 1;
    }

    private boolean atLeastOneDiskSelected() {
        return getSelectedItems() != null && getSelectedItems().size() > 0;
    }

    public boolean isHotPlugAvailable() {
        VM vm = getEntity();
        return vm != null && (vm.getStatus() == VMStatus.Up ||
                vm.getStatus() == VMStatus.Paused || vm.getStatus() == VMStatus.Suspended);
    }

    private boolean isPlugCommandAvailable(boolean plug) {
        return getSelectedItems() != null && getSelectedItems().size() > 0
                && isPlugAvailableByDisks(plug) &&
                (isVmDown() || (isHotPlugAvailable() && getIsDiskHotPlugSupported()));
    }

    private boolean isPlugAvailableByDisks(boolean plug) {
        ArrayList<Disk> disks =
                getSelectedItems() != null ? Linq.<Disk> cast(getSelectedItems()) : new ArrayList<Disk>();

        for (Disk disk : disks)
        {
            boolean isLocked =
                    disk.getDiskStorageType() == DiskStorageType.IMAGE
                            && ((DiskImage) disk).getImageStatus() == ImageStatus.LOCKED;

            boolean isDiskHotpluggableInterface = false;
            if (getEntity() != null) {
                isDiskHotpluggableInterface = AsyncDataProvider.getDiskHotpluggableInterfaces(getEntity().getOs(),
                        getEntity().getVdsGroupCompatibilityVersion()).contains(disk.getDiskInterface());
            }

            if (disk.getPlugged() == plug
                    || isLocked
                    || (!isDiskHotpluggableInterface && !isVmDown())) {
                return false;
            }
        }

        return true;
    }

    private boolean isImageDiskOK(Disk disk) {
        return disk.getDiskStorageType() == DiskStorageType.IMAGE &&
                ((DiskImage) disk).getImageStatus() == ImageStatus.OK;
    }

    private boolean isMoveCommandAvailable() {
        ArrayList<Disk> disks =
                getSelectedItems() != null ? Linq.<Disk> cast(getSelectedItems()) : new ArrayList<Disk>();

        for (Disk disk : disks) {
            if (!isImageDiskOK(disk) || (!isVmDown() && disk.getPlugged()) || disk.isDiskSnapshot()) {
                return false;
            }
        }

        return true;
    }

    private boolean isLiveMoveCommandAvailable() {
        if (!getIsLiveStorageMigrationEnabled()) {
            return false;
        }

        VM vm = getEntity();
        if (vm == null || !vm.getStatus().isUpOrPaused() || vm.isStateless()) {
            return false;
        }

        ArrayList<Disk> disks = getSelectedItems() != null ?
                Linq.<Disk> cast(getSelectedItems()) : new ArrayList<Disk>();

        for (Disk disk : disks) {
            if (!isImageDiskOK(disk) || disk.isDiskSnapshot()) {
                return false;
            }
        }

        return true;
    }

    private boolean isRemoveCommandAvailable() {
        ArrayList<Disk> disks =
                getSelectedItems() != null ? Linq.<Disk> cast(getSelectedItems()) : new ArrayList<Disk>();

        for (Disk disk : disks)
        {
            if (disk.getDiskStorageType() == DiskStorageType.IMAGE &&
                    ((DiskImage) disk).getImageStatus() == ImageStatus.LOCKED || (!isVmDown() && disk.getPlugged()))
            {
                return false;
            }
        }

        return true;
    }

    private void updateGetAlignmentCommandAvailability() {
        getScanAlignmentCommand().setIsExecutionAllowed(false);

        if (!getIsScanAlignmentEnabled()) {
            return;
        }

        if (getSelectedItems() == null || getSelectedItems().size() != 1) {
            return; // leave the command disabled
        }

        ArrayList<Disk> disks =
                getSelectedItems() != null ? Linq.<Disk> cast(getSelectedItems()) : new ArrayList<Disk>();

        for (Disk disk : disks)
        {
            if (!isImageDiskOK(disk)) {
                return; // leave the command disabled
            }
        }

        final VM vm = getEntity();
        if (vm == null) {
            return; // leave the command disabled
        }

        getScanAlignmentCommand().setIsExecutionAllowed(true);
    }

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);

        if (command == getNewCommand())
        {
            newEntity();
        }
        else if (command == getEditCommand())
        {
            edit();
        }
        else if (command == getRemoveCommand())
        {
            remove();
        }
        else if (command == getMoveCommand())
        {
            move();
        }
        else if (command == getScanAlignmentCommand())
        {
            scanAlignment();
        }
        else if (StringHelper.stringsEqual(command.getName(), "Cancel")) //$NON-NLS-1$
        {
            cancel();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnRemove")) //$NON-NLS-1$
        {
            onRemove();
        }
        else if (command == getPlugCommand())
        {
            plug(true);
        }
        else if (command == getUnPlugCommand())
        {
            plug(false);
        } else if (command == getChangeQuotaCommand()) {
            changeQuota();
        } else if (command.getName().equals("onChangeQuota")) { //$NON-NLS-1$
            onChangeQuota();
        }
    }

    protected void updateIsDiskHotPlugAvailable()
    {
        VM vm = getEntity();
        Version clusterCompatibilityVersion = vm.getVdsGroupCompatibilityVersion();
        if (clusterCompatibilityVersion == null) {
            setIsDiskHotPlugSupported(false);
        } else {
            setIsDiskHotPlugSupported((Boolean) !AsyncDataProvider.getDiskHotpluggableInterfaces(
                    getEntity().getOs(), clusterCompatibilityVersion).isEmpty());
        }
    }

    protected void updateLiveStorageMigrationEnabled()
    {
        final VM vm = getEntity();

        AsyncDataProvider.getDataCenterById(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                VmDiskListModel model = (VmDiskListModel) target;

                StoragePool dataCenter = (StoragePool) returnValue;
                Version dcCompatibilityVersion = dataCenter.getcompatibility_version() != null
                        ? dataCenter.getcompatibility_version() : new Version();

                AsyncDataProvider.isCommandCompatible(new AsyncQuery(model,
                        new INewAsyncCallback() {
                            @Override
                            public void onSuccess(Object target, Object returnValue) {
                                VmDiskListModel model = (VmDiskListModel) target;
                                model.setIsLiveStorageMigrationEnabled((Boolean) returnValue);
                            }
                        }),
                        VdcActionType.LiveMigrateVmDisks,
                        vm.getVdsGroupCompatibilityVersion(),
                        dcCompatibilityVersion);
            }
        }), vm.getStoragePoolId());
    }

    private void updateScanAlignmentEnabled() {
        final VM vm = getEntity();

        AsyncDataProvider.getDataCenterById(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                VmDiskListModel model = (VmDiskListModel) target;
                StoragePool dataCenter = (StoragePool) returnValue;

                Version minClusterVersion = vm.getVdsGroupCompatibilityVersion();
                Version minDcVersion = dataCenter.getcompatibility_version();

                AsyncDataProvider.isCommandCompatible(new AsyncQuery(model, new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {
                        VmDiskListModel model = (VmDiskListModel) target;
                        model.setIsScanAlignmentEnabled((Boolean) returnValue);
                    }
                }), VdcActionType.GetDiskAlignment, minClusterVersion, minDcVersion);
            }
        }), vm.getStoragePoolId());
    }

    protected void updateDataCenterVersion()
    {
        AsyncQuery query = new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                VmDiskListModel model = (VmDiskListModel) target;
                StoragePool storagePool = (StoragePool) returnValue;
                model.setDataCenterVersion(storagePool.getcompatibility_version());
            }
        });
        AsyncDataProvider.getDataCenterById(query, getEntity().getStoragePoolId());
    }

    protected void updateExtendImageSizeSupported()
    {
        VM vm = getEntity();
        AsyncQuery query = new AsyncQuery(this, new INewAsyncCallback()
        {
            @Override
            public void onSuccess(Object target, Object returnValue)
            {
                VmDiskListModel model = (VmDiskListModel) target;
                model.setExtendImageSizeSupported((Boolean) returnValue);
            }
        });
        AsyncDataProvider.isCommandCompatible(query, VdcActionType.ExtendImageSize,
                vm.getVdsGroupCompatibilityVersion(), dataCenterVersion);
    }


    private Version dataCenterVersion;

    public Version getDataCenterVersion()
    {
        return dataCenterVersion;
    }

    public void setDataCenterVersion(Version dataCenterVersion)
    {
        if (dataCenterVersion != null && !dataCenterVersion.equals(this.dataCenterVersion))
        {
            this.dataCenterVersion = dataCenterVersion;
            updateExtendImageSizeSupported();
        }
    }

    private boolean extendImageSizeSupported = true;

    public boolean isExtendImageSizeSupported()
    {
        return extendImageSizeSupported;
    }

    public void setExtendImageSizeSupported(boolean extendImageSizeSupported)
    {
        if (this.extendImageSizeSupported != extendImageSizeSupported) {
            this.extendImageSizeSupported = extendImageSizeSupported;
            updateActionAvailability();
        }
    }

    @Override
    protected String getListName() {
        return "VmDiskListModel"; //$NON-NLS-1$
    }

    public SystemTreeItemModel getSystemTreeSelectedItem() {
        if (getSystemTreeContext() == null) {
            return null;
        }
        return getSystemTreeContext().getSystemTreeSelectedItem();
    }
}
