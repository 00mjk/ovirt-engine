package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ovirt.engine.core.common.action.ImportVmParameters;
import org.ovirt.engine.core.common.action.RemoveVmFromImportExportParamenters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.StorageDomainSharedStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.queries.GetAllFromExportDomainQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportEntityData;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmData;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmAppListModel;
import org.ovirt.engine.ui.uicommonweb.validation.I18NNameValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.LengthValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotInCollectionValidation;
import org.ovirt.engine.ui.uicommonweb.validation.ValidationResult;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class VmBackupModel extends ManageBackupModel {

    private VmAppListModel privateAppListModel;
    protected List<Object> objectsToClone;
    /** used to save the names that were assigned for VMs which are going
     *  to be created using import in case of choosing multiple VM imports */
    protected Set<String> assignedVmNames = new HashSet<String>();
    protected Map<Guid, Object> cloneObjectMap;
    protected ImportVmModel importModel;

    public VmAppListModel getAppListModel() {
        return privateAppListModel;
    }

    protected void setAppListModel(VmAppListModel value) {
        privateAppListModel = value;
    }

    public VmBackupModel() {
        setTitle(ConstantsManager.getInstance().getConstants().vmImportTitle());
        setHashName("vm_import"); // //$NON-NLS-1$

        setAppListModel(new VmAppListModel());
    }

    @Override
    protected void OnSelectedItemChanged() {
        super.OnSelectedItemChanged();
        if (getAppListModel() != null) {
            getAppListModel().setEntity(getSelectedItem());
        }
    }

    @Override
    protected void remove() {
        super.remove();

        if (getWindow() != null) {
            return;
        }

        ConfirmationModel model = new ConfirmationModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().removeBackedUpVMsTitle());
        model.setHashName("remove_backed_up_vm"); //$NON-NLS-1$
        model.setMessage(ConstantsManager.getInstance().getConstants().vmsMsg());

        ArrayList<String> items = new ArrayList<String>();
        for (Object item : getSelectedItems()) {
            VM vm = (VM) item;
            items.add(vm.getName());
        }
        model.setItems(items);

        model.setNote(ConstantsManager.getInstance().getConstants().noteTheDeletedItemsMightStillAppearOntheSubTab());

        UICommand tempVar = new UICommand("OnRemove", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    private void OnRemove() {
        ConfirmationModel model = (ConfirmationModel) getWindow();

        if (model.getProgress() != null) {
            return;
        }

        model.StartProgress(null);

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.Model = this;
        _asyncQuery.asyncCallback = new INewAsyncCallback() {

            @Override
            public void OnSuccess(Object model, Object returnValue) {
                ArrayList<storage_pool> pools = (ArrayList<storage_pool>) returnValue;
                if (pools != null && pools.size() > 0) {
                    storage_pool pool = pools.get(0);
                    VmBackupModel backupModel = (VmBackupModel) model;
                    ArrayList<VdcActionParametersBase> list =
                            new ArrayList<VdcActionParametersBase>();
                    for (Object item : backupModel.getSelectedItems()) {
                        VM vm = (VM) item;
                        list.add(new RemoveVmFromImportExportParamenters(vm,
                                backupModel.getEntity().getId(), pool.getId()));
                    }

                    Frontend.RunMultipleAction(
                            VdcActionType.RemoveVmFromImportExport, list,
                            new IFrontendMultipleActionAsyncCallback() {
                                @Override
                                public void Executed(
                                        FrontendMultipleActionAsyncResult result) {

                                    ConfirmationModel localModel = (ConfirmationModel) result
                                            .getState();
                                    localModel.StopProgress();
                                    Cancel();
                                    OnEntityChanged();

                                }
                            }, backupModel.getWindow());
                }
            }
        };
        AsyncDataProvider.GetDataCentersByStorageDomain(_asyncQuery,
                getEntity().getId());
    }

    @Override
    protected void Restore() {
        super.Restore();

        if (getWindow() != null) {
            return;
        }

        ImportVmModel model = getImportModel();
        setWindow(model);
        model.StartProgress(null);
        UICommand restoreCommand;
        restoreCommand = new UICommand("OnRestore", this); //$NON-NLS-1$
        restoreCommand.setTitle(ConstantsManager.getInstance().getConstants().ok());
        restoreCommand.setIsDefault(true);
        model.getCommands().add(restoreCommand);
        UICommand tempVar3 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar3.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar3.setIsCancel(true);
        model.getCommands().add(tempVar3);
        model.setItems(getSelectedItems());
        model.init(getEntity().getId());

        // Add 'Close' command
        UICommand closeCommand = new UICommand("Cancel", this); //$NON-NLS-1$
        closeCommand.setTitle(ConstantsManager.getInstance().getConstants().close());
        closeCommand.setIsDefault(true);
        closeCommand.setIsCancel(true);
        model.setCloseCommand(closeCommand);
    }

    protected ImportVmModel getImportModel() {
        ImportVmModel model = new ImportVmModel();
        model.setTitle(ConstantsManager.getInstance().getConstants().importVirtualMachinesTitle());
        model.setHashName("import_virtual_machine"); //$NON-NLS-1$
        model.setEntity(getEntity());
        return model;
    }

    public void OnRestore() {
        importModel = (ImportVmModel) getWindow();

        if (importModel.getProgress() != null) {
            return;
        }

        if (!importModel.Validate()) {
            return;
        }
        cloneObjectMap = new HashMap<Guid, Object>();

        objectsToClone = new ArrayList<Object>();
        for (Object object : (ArrayList<Object>) importModel.getItems()) {
            ImportEntityData item = (ImportEntityData) object;
            if ((Boolean) item.getClone().getEntity()) {
                objectsToClone.add(object);
            }
        }
        executeImportClone();
    }

    private void executeImportClone() {
        // TODO: support running numbers (for suffix)
        if (objectsToClone.size() == 0) {
            clearCachedAssignedVmNames();
            executeImport();
            return;
        }
        ImportCloneModel entity = new ImportCloneModel();
        Object object = objectsToClone.iterator().next();
        entity.setEntity(object);
        entity.setTitle(ConstantsManager.getInstance().getConstants().importConflictTitle());
        entity.setHashName("import_conflict"); //$NON-NLS-1$
        UICommand command = new UICommand("onClone", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().ok());
        command.setIsDefault(true);
        entity.getCommands().add(command);
        command = new UICommand("closeClone", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        command.setIsCancel(true);
        entity.getCommands().add(command);

        setConfirmWindow(entity);
    }

    private void onClone() {
        ImportCloneModel cloneModel = (ImportCloneModel) getConfirmWindow();
        if ((Boolean) cloneModel.getApplyToAll().getEntity()) {
            if (!(Boolean) cloneModel.getNoClone().getEntity()) {
                String suffix = (String) cloneModel.getSuffix().getEntity();
                if (!validateSuffix(suffix, cloneModel.getSuffix())) {
                    return;
                }
                for (Object object : objectsToClone) {
                    setObjectName(object, suffix, true);
                    cloneObjectMap.put((Guid) ((IVdcQueryable) (((ImportEntityData) object).getEntity())).getQueryableId(),
                            object);
                }
            }
            objectsToClone.clear();
        } else {
            Object object = cloneModel.getEntity();
            if (!(Boolean) cloneModel.getNoClone().getEntity()) {
                String vmName = (String) cloneModel.getName().getEntity();
                if (!validateName(vmName, cloneModel.getName(), getClonedNameValidators(object))) {
                    return;
                }
                setObjectName(object, vmName, false);
                cloneObjectMap.put((Guid) ((IVdcQueryable) ((ImportEntityData) object).getEntity()).getQueryableId(),
                        object);
            }
            objectsToClone.remove(object);
        }

        setConfirmWindow(null);
        executeImportClone();
    }

    private void setObjectName(Object object, String input, boolean isSuffix) {
        String nameForTheClonedVm = isSuffix ? getObjectName(object) + input : input;
        setObjectName(object, nameForTheClonedVm);
        assignedVmNames.add(nameForTheClonedVm);
    }

    protected String getObjectName(Object object) {
        return ((ImportVmData) object).getVm().getName();
    }

    protected void setObjectName(Object object, String name) {
        ((ImportVmData) object).getVm().setName(name);
    }

    protected boolean validateSuffix(String suffix, EntityModel entityModel) {
        for (Object object : objectsToClone) {
            VM vm = ((ImportVmData) object).getVm();
            if (!validateName(vm.getName() + suffix, entityModel, getClonedAppendedNameValidators(object))) {
                return false;
            }
        }
        return true;
    }

    protected IValidation[] getClonedNameValidators(Object object) {
        final int maxClonedNameLength = getMaxClonedNameLength(object);
        return new IValidation[] {
                new NotEmptyValidation(),
                new LengthValidation(maxClonedNameLength),
                new I18NNameValidation() {
                    @Override
                    protected String composeMessage() {
                        return ConstantsManager.getInstance()
                                .getMessages()
                                .nameMustConataionOnlyAlphanumericChars(maxClonedNameLength);
                    };
                },
                new UniqueClonedNameValidator(assignedVmNames)
        };
    }

    protected IValidation[] getClonedAppendedNameValidators(Object object) {
        final int maxClonedNameLength = getMaxClonedNameLength(object);
        return new IValidation[] {
                new NotEmptyValidation(),
                new LengthValidation(maxClonedNameLength),
                new I18NNameValidation() {
                    @Override
                    protected String composeMessage() {
                        return ConstantsManager.getInstance()
                                .getMessages()
                                .newNameWithSuffixCannotContainBlankOrSpecialChars(maxClonedNameLength);
                    };
                },
                new UniqueClonedAppendedNameValidator(assignedVmNames)
        };
    }

    protected String getAlreadyAssignedClonedNameMessage() {
        return ConstantsManager.getInstance()
                .getMessages()
                .alreadyAssignedClonedVmName();
    }

    protected String getSuffixCauseToClonedNameCollisionMessage(String existingName) {
        return ConstantsManager.getInstance()
                .getMessages()
                .suffixCauseToClonedVmNameCollision(existingName);
    }

    protected int getMaxClonedNameLength(Object object) {
        VM vm = ((ImportVmData) object).getVm();
        return AsyncDataProvider.IsWindowsOsType(vm.getOs()) ? UnitVmModel.WINDOWS_VM_NAME_MAX_LIMIT
                : UnitVmModel.NON_WINDOWS_VM_NAME_MAX_LIMIT;
    }

    protected boolean validateName(String newVmName, EntityModel entity, IValidation[] validators) {
        EntityModel temp = new EntityModel();
        temp.setIsValid(true);
        temp.setEntity(newVmName);
        temp.ValidateEntity(validators);
        if (!temp.getIsValid()) {
            entity.setInvalidityReasons(temp.getInvalidityReasons());
            entity.setIsValid(false);
        }

        return temp.getIsValid();
    }

    private void closeClone() {
        setConfirmWindow(null);
        clearCachedAssignedVmNames();
    }

    protected void executeImport() {
        ArrayList<VdcActionParametersBase> prms = new ArrayList<VdcActionParametersBase>();

        for (Object item : importModel.getItems()) {
            VM vm = ((ImportVmData) item).getVm();

            ImportVmParameters prm = new ImportVmParameters(vm, getEntity().getId(),
                    Guid.Empty, importModel.getStoragePool().getId(),
                    ((VDSGroup) importModel.getCluster().getSelectedItem()).getId());

            if (importModel.getClusterQuota().getSelectedItem() != null &&
                    importModel.getClusterQuota().getIsAvailable()) {
                prm.setQuotaId(((Quota) importModel.getClusterQuota().getSelectedItem()).getId());
            }

            prm.setForceOverride(true);
            prm.setCopyCollapse((Boolean) ((ImportVmData) item).getCollapseSnapshots().getEntity());

            Map<Guid, Guid> map = new HashMap<Guid, Guid>();
            for (Map.Entry<Guid, Disk> entry : vm.getDiskMap().entrySet()) {
                DiskImage disk = (DiskImage) entry.getValue();
                map.put(disk.getId(), importModel.getDiskImportData(disk.getId()).getSelectedStorageDomain().getId());
                disk.setvolumeFormat(
                        AsyncDataProvider.GetDiskVolumeFormat(
                                importModel.getDiskImportData(disk.getId()).getSelectedVolumeType(),
                                importModel.getDiskImportData(
                                        disk.getId()).getSelectedStorageDomain().getStorageType()));
                disk.setVolumeType(importModel.getDiskImportData(disk.getId()).getSelectedVolumeType());

                if (importModel.getDiskImportData(disk.getId()).getSelectedQuota() != null) {
                    disk.setQuotaId(
                            importModel.getDiskImportData(disk.getId()).getSelectedQuota().getId());
                }
            }

            prm.setImageToDestinationDomainMap(map);

            if (((ImportVmData) item).isExistsInSystem() ||
                    (Boolean) ((ImportVmData) item).getClone().getEntity()) {
                if (!cloneObjectMap.containsKey(vm.getId())) {
                    continue;
                }
                prm.setImportAsNewEntity(true);
                prm.setCopyCollapse(true);
                prm.getVm().setName(((ImportVmData) cloneObjectMap.get(vm.getId())).getVm().getName());
            }

            prms.add(prm);

        }

        importModel.StartProgress(null);

        Frontend.RunMultipleAction(VdcActionType.ImportVm, prms,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void Executed(
                            FrontendMultipleActionAsyncResult result) {

                        VmBackupModel vmBackupModel = (VmBackupModel) result
                                .getState();
                        vmBackupModel.getWindow().StopProgress();
                        vmBackupModel.Cancel();
                        ArrayList<VdcReturnValueBase> retVals =
                                (ArrayList<VdcReturnValueBase>) result
                                        .getReturnValue();
                        if (retVals != null
                                && vmBackupModel.getSelectedItems().size() == retVals
                                        .size()) {
                            String importedVms = ""; //$NON-NLS-1$
                            int counter = 0;
                            boolean toShowConfirmWindow = false;
                            for (Object item : vmBackupModel.getSelectedItems()) {
                                VM vm = (VM) item;
                                if (retVals.get(counter) != null
                                        && retVals.get(counter).getCanDoAction()) {
                                    importedVms += vm.getName() + ", "; //$NON-NLS-1$
                                    toShowConfirmWindow = true;
                                }
                                counter++;
                            }
                            // show the confirm window only if the import has been successfully started for at least one
                            // VM
                            if (toShowConfirmWindow) {
                                ConfirmationModel confirmModel = new ConfirmationModel();
                                vmBackupModel.setConfirmWindow(confirmModel);
                                confirmModel.setTitle(ConstantsManager.getInstance()
                                        .getConstants()
                                        .importVirtualMachinesTitle());
                                confirmModel.setHashName("import_virtual_machine"); //$NON-NLS-1$
                                importedVms = StringHelper.trimEnd(importedVms.trim(), ',');
                                confirmModel.setMessage(ConstantsManager.getInstance()
                                        .getMessages()
                                        .importProcessHasBegunForVms(importedVms));
                                UICommand tempVar2 = new UICommand("CancelConfirm", //$NON-NLS-1$
                                        vmBackupModel);
                                tempVar2.setTitle(ConstantsManager.getInstance().getConstants().close());
                                tempVar2.setIsDefault(true);
                                tempVar2.setIsCancel(true);
                                confirmModel.getCommands().add(tempVar2);
                            }
                        }

                    }
                },
                this);
    }

    @Override
    protected void EntityPropertyChanged(Object sender,
            PropertyChangedEventArgs e) {
        super.EntityPropertyChanged(sender, e);

        if (e.PropertyName.equals("storage_domain_shared_status")) { //$NON-NLS-1$
            getSearchCommand().Execute();
        }
    }

    @Override
    protected void SyncSearch() {
        super.SyncSearch();

        if (getEntity() == null
                || getEntity().getStorageDomainType() != StorageDomainType.ImportExport
                || getEntity().getStorageDomainSharedStatus() != StorageDomainSharedStatus.Active) {
            setItems(Collections.emptyList());
        } else {
            AsyncQuery _asyncQuery = new AsyncQuery();
            _asyncQuery.setModel(this);
            _asyncQuery.asyncCallback = new INewAsyncCallback() {
                @Override
                public void OnSuccess(Object model, Object ReturnValue) {
                    VmBackupModel backupModel = (VmBackupModel) model;
                    ArrayList<storage_pool> list = (ArrayList<storage_pool>) ReturnValue;
                    if (list != null && list.size() > 0) {
                        storage_pool dataCenter = list.get(0);
                        AsyncQuery _asyncQuery1 = new AsyncQuery();
                        _asyncQuery1.setModel(backupModel);
                        _asyncQuery1.asyncCallback = new INewAsyncCallback() {
                            @Override
                            public void OnSuccess(Object model1,
                                    Object ReturnValue1) {
                                VmBackupModel backupModel1 = (VmBackupModel) model1;

                                backupModel1.setItems((ArrayList<VM>) ((VdcQueryReturnValue) ReturnValue1).getReturnValue());
                            }
                        };
                        GetAllFromExportDomainQueryParameters tempVar = new GetAllFromExportDomainQueryParameters(
                                dataCenter.getId(), backupModel.getEntity()
                                        .getId());
                        Frontend.RunQuery(VdcQueryType.GetVmsFromExportDomain,
                                tempVar, _asyncQuery1);
                    }
                }
            };
            AsyncDataProvider.GetDataCentersByStorageDomain(_asyncQuery,
                    getEntity().getId());
        }
    }

    @Override
    protected void AsyncSearch() {
        super.AsyncSearch();
        SyncSearch();
    }

    @Override
    public void ExecuteCommand(UICommand command) {
        super.ExecuteCommand(command);

        if (command.getName().equals("OnRemove")) { //$NON-NLS-1$
            OnRemove();
        } else if (command.getName().equals("OnRestore")) { //$NON-NLS-1$
            OnRestore();
        } else if (command.getName().equals("onClone")) { //$NON-NLS-1$
            onClone();
        } else if (command.getName().equals("closeClone")) { //$NON-NLS-1$
            closeClone();
        }
    }

    @Override
    protected final void Cancel() {
        super.Cancel();
        clearCachedAssignedVmNames();
    }

    @Override
    protected String getListName() {
        return "VmBackupModel"; //$NON-NLS-1$
    }

    private void clearCachedAssignedVmNames() {
        assignedVmNames.clear();
    }

    private class UniqueClonedNameValidator extends NotInCollectionValidation {

        public UniqueClonedNameValidator(Collection<?> collection) {
            super(collection);
        }

        @Override
        public ValidationResult validate(Object value) {
            ValidationResult result = super.validate(value);
            if (!result.getSuccess()) {
                result.getReasons().add(getAlreadyAssignedClonedNameMessage());
            }
            return result;
        }
    }

    private class UniqueClonedAppendedNameValidator extends NotInCollectionValidation {

        public UniqueClonedAppendedNameValidator(Collection<?> collection) {
            super(collection);
        }

        @Override
        public ValidationResult validate(Object value) {
            ValidationResult result = super.validate(value);
            if (!result.getSuccess()) {
                result.getReasons().add(getSuffixCauseToClonedNameCollisionMessage((String) value));
            }
            return result;
        }
    }
}
