package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.action.AddVmFromScratchParameters;
import org.ovirt.engine.core.common.action.AddVmFromTemplateParameters;
import org.ovirt.engine.core.common.action.AddVmTemplateParameters;
import org.ovirt.engine.core.common.action.AttachEntityToTagParameters;
import org.ovirt.engine.core.common.action.ChangeDiskCommandParameters;
import org.ovirt.engine.core.common.action.ChangeVMClusterParameters;
import org.ovirt.engine.core.common.action.MigrateVmParameters;
import org.ovirt.engine.core.common.action.MigrateVmToServerParameters;
import org.ovirt.engine.core.common.action.MoveVmParameters;
import org.ovirt.engine.core.common.action.RemoveVmParameters;
import org.ovirt.engine.core.common.action.RunVmParams;
import org.ovirt.engine.core.common.action.ShutdownVmParameters;
import org.ovirt.engine.core.common.action.StopVmParameters;
import org.ovirt.engine.core.common.action.StopVmTypeEnum;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.action.VmOperationParameterBase;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.Tags;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.common.businessentities.VmWatchdogAction;
import org.ovirt.engine.core.common.businessentities.VmWatchdogType;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.searchbackend.SearchObjects;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Cloner;
import org.ovirt.engine.ui.uicommonweb.ConsoleOptionsFrontendPersister.ConsoleContext;
import org.ovirt.engine.ui.uicommonweb.ErrorPopupManager;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.TagsEqualityComparer;
import org.ovirt.engine.ui.uicommonweb.TypeResolver;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.builders.BuilderExecutor;
import org.ovirt.engine.ui.uicommonweb.builders.vm.CommonUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.FullUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.VmSpecificUnitToVmBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.DedicatedVmForVdsVmBaseToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.KernelParamsVmBaseToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.MigrationOptionsVmBaseToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.UsbPolicyVmBaseToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.ConsoleModelsCache;
import org.ovirt.engine.ui.uicommonweb.models.ConsolePopupModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ISupportSystemTreeContext;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicommonweb.models.VmConsoles;
import org.ovirt.engine.ui.uicommonweb.models.configure.ChangeCDModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.scheduling.affinity_groups.list.VmAffinityGroupListModel;
import org.ovirt.engine.ui.uicommonweb.models.tags.TagListModel;
import org.ovirt.engine.ui.uicommonweb.models.tags.TagModel;
import org.ovirt.engine.ui.uicommonweb.models.templates.VmBaseListModel;
import org.ovirt.engine.ui.uicommonweb.models.userportal.AttachCdModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.FrontendMultipleQueryAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleQueryAsyncCallback;
import org.ovirt.engine.ui.uicompat.ObservableCollection;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.UIConstants;

public class VmListModel extends VmBaseListModel<VM> implements ISupportSystemTreeContext {

    private final UIConstants constants = ConstantsManager.getInstance().getConstants();
    public static final Version BALLOON_DEVICE_MIN_VERSION = Version.v3_2;
    private UICommand newVMCommand;

    private final static String SHUTDOWN = "Shutdown"; //$NON-NLS-1$
    private final static String STOP     = "Stop"; //$NON-NLS-1$
    private final static String REBOOT   = "Reboot"; //$NON-NLS-1$

    public UICommand getNewVmCommand() {
        return newVMCommand;
    }

    private void setNewVmCommand(UICommand newVMCommand) {
        this.newVMCommand = newVMCommand;
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

    private UICommand privateRunCommand;

    public UICommand getRunCommand()
    {
        return privateRunCommand;
    }

    private void setRunCommand(UICommand value)
    {
        privateRunCommand = value;
    }

    private UICommand privatePauseCommand;

    public UICommand getPauseCommand()
    {
        return privatePauseCommand;
    }

    private void setPauseCommand(UICommand value)
    {
        privatePauseCommand = value;
    }

    private UICommand privateStopCommand;

    public UICommand getStopCommand()
    {
        return privateStopCommand;
    }

    private void setStopCommand(UICommand value)
    {
        privateStopCommand = value;
    }

    private UICommand privateShutdownCommand;

    public UICommand getShutdownCommand()
    {
        return privateShutdownCommand;
    }

    private void setShutdownCommand(UICommand value)
    {
        privateShutdownCommand = value;
    }

    private UICommand privateRebootCommand;

    public UICommand getRebootCommand() {
        return privateRebootCommand;
    }

    public void setRebootCommand(UICommand value) {
        privateRebootCommand = value;
    }

    private UICommand privateCancelMigrateCommand;

    public UICommand getCancelMigrateCommand() {
        return privateCancelMigrateCommand;
    }

    private void setCancelMigrateCommand(UICommand value) {
        privateCancelMigrateCommand = value;
    }

    private UICommand privateMigrateCommand;

    public UICommand getMigrateCommand()
    {
        return privateMigrateCommand;
    }

    private void setMigrateCommand(UICommand value)
    {
        privateMigrateCommand = value;
    }

    private UICommand privateNewTemplateCommand;

    public UICommand getNewTemplateCommand()
    {
        return privateNewTemplateCommand;
    }

    private void setNewTemplateCommand(UICommand value)
    {
        privateNewTemplateCommand = value;
    }

    private UICommand privateRunOnceCommand;

    public UICommand getRunOnceCommand()
    {
        return privateRunOnceCommand;
    }

    private void setRunOnceCommand(UICommand value)
    {
        privateRunOnceCommand = value;
    }

    private UICommand privateExportCommand;

    public UICommand getExportCommand()
    {
        return privateExportCommand;
    }

    private void setExportCommand(UICommand value)
    {
        privateExportCommand = value;
    }

    private UICommand privateCreateSnapshotCommand;

    public UICommand getCreateSnapshotCommand()
    {
        return privateCreateSnapshotCommand;
    }

    private void setCreateSnapshotCommand(UICommand value)
    {
        privateCreateSnapshotCommand = value;
    }

    private UICommand privateRetrieveIsoImagesCommand;

    public UICommand getRetrieveIsoImagesCommand()
    {
        return privateRetrieveIsoImagesCommand;
    }

    private void setRetrieveIsoImagesCommand(UICommand value)
    {
        privateRetrieveIsoImagesCommand = value;
    }

    private UICommand privateGuideCommand;

    public UICommand getGuideCommand()
    {
        return privateGuideCommand;
    }

    private void setGuideCommand(UICommand value)
    {
        privateGuideCommand = value;
    }

    private UICommand privateChangeCdCommand;

    public UICommand getChangeCdCommand()
    {
        return privateChangeCdCommand;
    }

    private void setChangeCdCommand(UICommand value)
    {
        privateChangeCdCommand = value;
    }

    private UICommand privateAssignTagsCommand;

    public UICommand getAssignTagsCommand()
    {
        return privateAssignTagsCommand;
    }

    private void setAssignTagsCommand(UICommand value)
    {
        privateAssignTagsCommand = value;
    }

    UICommand editConsoleCommand;

    public void setEditConsoleCommand(UICommand editConsoleCommand) {
        this.editConsoleCommand = editConsoleCommand;
    }

    public UICommand getEditConsoleCommand() {
        return editConsoleCommand;
    }

    public UICommand consoleConnectCommand;

    public UICommand getConsoleConnectCommand() {
        return consoleConnectCommand;
    }

    public void setConsoleConnectCommand(UICommand consoleConnectCommand) {
        this.consoleConnectCommand = consoleConnectCommand;
    }

    public ObservableCollection<ChangeCDModel> isoImages;

    public ObservableCollection<ChangeCDModel> getIsoImages()
    {
        return isoImages;
    }

    private void setIsoImages(ObservableCollection<ChangeCDModel> value)
    {
        if ((isoImages == null && value != null) || (isoImages != null && !isoImages.equals(value)))
        {
            isoImages = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsoImages")); //$NON-NLS-1$
        }
    }

    private Object privateGuideContext;

    public Object getGuideContext()
    {
        return privateGuideContext;
    }

    public void setGuideContext(Object value)
    {
        privateGuideContext = value;
    }

    private VM privatecurrentVm;

    public VM getcurrentVm()

    {
        return privatecurrentVm;
    }

    public void setcurrentVm(VM value)
    {
        privatecurrentVm = value;
    }

    private final ConsoleModelsCache consoleModelsCache;

    private HashMap<Version, ArrayList<String>> privateCustomPropertiesKeysList;

    private HashMap<Version, ArrayList<String>> getCustomPropertiesKeysList() {
        return privateCustomPropertiesKeysList;
    }

    private void setCustomPropertiesKeysList(HashMap<Version, ArrayList<String>> value) {
        privateCustomPropertiesKeysList = value;
    }

    private ErrorPopupManager errorPopupManager;

    public VmListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().virtualMachinesTitle());
        setHelpTag(HelpTag.virtual_machines);
        setHashName("virtual_machines"); //$NON-NLS-1$

        setDefaultSearchString("Vms:"); //$NON-NLS-1$
        setSearchString(getDefaultSearchString());
        setSearchObjects(new String[] { SearchObjects.VM_OBJ_NAME, SearchObjects.VM_PLU_OBJ_NAME });
        setAvailableInModes(ApplicationMode.VirtOnly);

        consoleModelsCache = new ConsoleModelsCache(ConsoleContext.WA, this);
        setConsoleHelpers();

        setNewVmCommand(new UICommand("NewVm", this)); //$NON-NLS-1$
        setEditCommand(new UICommand("Edit", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$
        setRunCommand(new UICommand("Run", this, true)); //$NON-NLS-1$
        setPauseCommand(new UICommand("Pause", this)); //$NON-NLS-1$
        setStopCommand(new UICommand("Stop", this)); //$NON-NLS-1$
        setShutdownCommand(new UICommand("Shutdown", this)); //$NON-NLS-1$
        setRebootCommand(new UICommand("Reboot", this)); //$NON-NLS-1$
        setEditConsoleCommand(new UICommand("EditConsoleCommand", this)); //$NON-NLS-1$
        setConsoleConnectCommand(new UICommand("ConsoleConnectCommand", this)); //$NON-NLS-1$
        setMigrateCommand(new UICommand("Migrate", this)); //$NON-NLS-1$
        setCancelMigrateCommand(new UICommand("CancelMigration", this)); //$NON-NLS-1$
        setNewTemplateCommand(new UICommand("NewTemplate", this)); //$NON-NLS-1$
        setRunOnceCommand(new UICommand("RunOnce", this)); //$NON-NLS-1$
        setExportCommand(new UICommand("Export", this)); //$NON-NLS-1$
        setCreateSnapshotCommand(new UICommand("CreateSnapshot", this)); //$NON-NLS-1$
        setGuideCommand(new UICommand("Guide", this)); //$NON-NLS-1$
        setRetrieveIsoImagesCommand(new UICommand("RetrieveIsoImages", this)); //$NON-NLS-1$
        setChangeCdCommand(new UICommand("ChangeCD", this)); //$NON-NLS-1$
        setAssignTagsCommand(new UICommand("AssignTags", this)); //$NON-NLS-1$

        setIsoImages(new ObservableCollection<ChangeCDModel>());
        ChangeCDModel tempVar = new ChangeCDModel();
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().retrievingCDsTitle());
        getIsoImages().add(tempVar);

        updateActionAvailability();

        getSearchNextPageCommand().setIsAvailable(true);
        getSearchPreviousPageCommand().setIsAvailable(true);
        if (getCustomPropertiesKeysList() == null) {
            AsyncDataProvider.getCustomPropertiesList(new AsyncQuery(this,
                    new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object target, Object returnValue) {
                            VmListModel model = (VmListModel) target;
                            if (returnValue != null) {
                                model.setCustomPropertiesKeysList((HashMap<Version, ArrayList<String>>) returnValue);
                            }
                        }
                    }));
        }

        // Call 'IsCommandCompatible' for precaching
        AsyncDataProvider.isCommandCompatible(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {
                    }
                }), null, null, null);
    }

    private void setConsoleHelpers() {
        this.errorPopupManager = (ErrorPopupManager) TypeResolver.getInstance().resolve(ErrorPopupManager.class);
    }

    private void assignTags()
    {
        if (getWindow() != null)
        {
            return;
        }

        TagListModel model = new TagListModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().assignTagsTitle());
        model.setHelpTag(HelpTag.assign_tags_vms);
        model.setHashName("assign_tags_vms"); //$NON-NLS-1$

        getAttachedTagsToSelectedVMs(model);

        UICommand tempVar = new UICommand("OnAssignTags", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    public Map<Guid, Boolean> attachedTagsToEntities;
    public ArrayList<Tags> allAttachedTags;
    public int selectedItemsCounter;

    private void getAttachedTagsToSelectedVMs(TagListModel model)
    {
        ArrayList<Guid> vmIds = new ArrayList<Guid>();
        for (Object item : getSelectedItems())
        {
            VM vm = (VM) item;
            vmIds.add(vm.getId());
        }

        attachedTagsToEntities = new HashMap<Guid, Boolean>();
        allAttachedTags = new ArrayList<Tags>();
        selectedItemsCounter = 0;

        for (Guid id : vmIds)
        {
            AsyncDataProvider.getAttachedTagsToVm(new AsyncQuery(new Object[] { this, model },
                    new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object target, Object returnValue) {

                            Object[] array = (Object[]) target;
                            VmListModel vmListModel = (VmListModel) array[0];
                            TagListModel tagListModel = (TagListModel) array[1];
                            vmListModel.allAttachedTags.addAll((ArrayList<Tags>) returnValue);
                            vmListModel.selectedItemsCounter++;
                            if (vmListModel.selectedItemsCounter == vmListModel.getSelectedItems().size())
                            {
                                postGetAttachedTags(vmListModel, tagListModel);
                            }

                        }
                    }),
                    id);
        }
    }

    private void postGetAttachedTags(VmListModel vmListModel, TagListModel tagListModel)
    {
        if (vmListModel.getLastExecutedCommand() == getAssignTagsCommand())
        {
            ArrayList<Tags> attachedTags =
                    Linq.distinct(vmListModel.allAttachedTags, new TagsEqualityComparer());
            for (Tags tag : attachedTags)
            {
                int count = 0;
                for (Tags tag2 : vmListModel.allAttachedTags)
                {
                    if (tag2.gettag_id().equals(tag.gettag_id()))
                    {
                        count++;
                    }
                }
                vmListModel.attachedTagsToEntities.put(tag.gettag_id(), count == vmListModel.getSelectedItems().size());
            }
            tagListModel.setAttachedTagsToEntities(vmListModel.attachedTagsToEntities);
        }
        else if (StringHelper.stringsEqual(vmListModel.getLastExecutedCommand().getName(), "OnAssignTags")) //$NON-NLS-1$
        {
            vmListModel.postOnAssignTags(tagListModel.getAttachedTagsToEntities());
        }
    }

    private void onAssignTags()
    {
        TagListModel model = (TagListModel) getWindow();

        getAttachedTagsToSelectedVMs(model);
    }

    public void postOnAssignTags(Map<Guid, Boolean> attachedTags)
    {
        TagListModel model = (TagListModel) getWindow();
        ArrayList<Guid> vmIds = new ArrayList<Guid>();

        for (Object item : getSelectedItems())
        {
            VM vm = (VM) item;
            vmIds.add(vm.getId());
        }

        // prepare attach/detach lists
        ArrayList<Guid> tagsToAttach = new ArrayList<Guid>();
        ArrayList<Guid> tagsToDetach = new ArrayList<Guid>();

        if (model.getItems() != null && ((ArrayList<TagModel>) model.getItems()).size() > 0)
        {
            ArrayList<TagModel> tags = (ArrayList<TagModel>) model.getItems();
            TagModel rootTag = tags.get(0);
            TagModel.recursiveEditAttachDetachLists(rootTag, attachedTags, tagsToAttach, tagsToDetach);
        }

        ArrayList<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();
        for (Guid a : tagsToAttach)
        {
            parameters.add(new AttachEntityToTagParameters(a, vmIds));
        }
        Frontend.getInstance().runMultipleAction(VdcActionType.AttachVmsToTag, parameters);

        parameters = new ArrayList<VdcActionParametersBase>();
        for (Guid a : tagsToDetach)
        {
            parameters.add(new AttachEntityToTagParameters(a, vmIds));
        }
        Frontend.getInstance().runMultipleAction(VdcActionType.DetachVmFromTag, parameters);

        cancel();
    }

    private void guide()
    {
        VmGuideModel model = new VmGuideModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().newVirtualMachineGuideMeTitle());
        model.setHelpTag(HelpTag.new_virtual_machine___guide_me);
        model.setHashName("new_virtual_machine_-_guide_me"); //$NON-NLS-1$

        if (getGuideContext() == null) {
            VM vm = (VM) getSelectedItem();
            setGuideContext(vm.getId());
        }

        AsyncDataProvider.getVmById(new AsyncQuery(this,
                                                   new INewAsyncCallback() {
                                                       @Override
                                                       public void onSuccess(Object target, Object returnValue) {
                                                           VmListModel vmListModel = (VmListModel) target;
                                                           VmGuideModel model = (VmGuideModel) vmListModel.getWindow();
                                                           model.setEntity(returnValue);

                                                           UICommand tempVar = new UICommand("Cancel", vmListModel); //$NON-NLS-1$
                                                           tempVar.setTitle(ConstantsManager.getInstance().getConstants().configureLaterTitle());
                                                           tempVar.setIsDefault(true);
                                                           tempVar.setIsCancel(true);
                                                           model.getCommands().add(tempVar);
                                                       }
                                                   }), (Guid) getGuideContext());
    }

    @Override
    protected void initDetailModels()
    {
        super.initDetailModels();

        ObservableCollection<EntityModel> list = new ObservableCollection<EntityModel>();
        list.add(new VmGeneralModel());
        list.add(new VmInterfaceListModel());
        VmDiskListModel diskListModel = new VmDiskListModel();
        diskListModel.setSystemTreeContext(this);
        list.add(diskListModel);
        list.add(new VmSnapshotListModel());
        list.add(new VmEventListModel());
        list.add(new VmAppListModel());
        list.add(new PermissionListModel());
        list.add(new VmAffinityGroupListModel());
        list.add(new VmSessionsModel());
        setDetailModels(list);
    }

    @Override
    public boolean isSearchStringMatch(String searchString)
    {
        return searchString.trim().toLowerCase().startsWith("vm"); //$NON-NLS-1$
    }

    @Override
    protected void syncSearch() {
        SearchParameters tempVar = new SearchParameters(getSearchString(), SearchType.VM, isCaseSensitiveSearch());
        tempVar.setMaxCount(getSearchPageSize());
        super.syncSearch(VdcQueryType.Search, tempVar);
    }

    @Override
    public void setItems(Iterable value) {
        consoleModelsCache.updateVmCache(value);

        super.setItems(value);
    }

    private void newVm() {
        if (getWindow() != null) {
            return;
        }

        UnitVmModel model = new UnitVmModel(new NewVmModelBehavior());
        model.setTitle(ConstantsManager.getInstance().getConstants().newVmTitle());
        model.setHelpTag(HelpTag.new_vm);
        model.setHashName("new_vm"); //$NON-NLS-1$
        model.setIsNew(true);
        model.getVmType().setSelectedItem(VmType.Server);
        model.setCustomPropertiesKeysList(getCustomPropertiesKeysList());

        setWindow(model);

        model.initialize(getSystemTreeSelectedItem());

        VmBasedWidgetSwitchModeCommand switchModeCommand = new VmBasedWidgetSwitchModeCommand();
        switchModeCommand.init(model);
        model.getCommands().add(switchModeCommand);

        model.getProvisioning().setEntity(true);

        UICommand tempVar = new UICommand("OnSave", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    private void editConsole() {
        if (getWindow() != null || getSelectedItem() == null) {
            return;
        }

        final VmConsoles activeVmConsoles = consoleModelsCache.getVmConsolesForEntity(getSelectedItem());

        final ConsolePopupModel model = new ConsolePopupModel();
        model.setVmConsoles(activeVmConsoles);
        model.setHelpTag(HelpTag.editConsole);
        model.setHashName("editConsole"); //$NON-NLS-1$
        setWindow(model);

        final UICommand saveCommand = new UICommand("OnEditConsoleSave", this); //$NON-NLS-1$
        saveCommand.setTitle(ConstantsManager.getInstance().getConstants().ok());
        saveCommand.setIsDefault(true);
        model.getCommands().add(saveCommand);
        final UICommand cancelCommand = new UICommand("Cancel", this); //$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);
        model.getCommands().add(cancelCommand);
    }

    private void edit() {
        VM vm = (VM) getSelectedItem();
        if (vm == null)
        {
            return;
        }

        if (getWindow() != null)
        {
            return;
        }

        // populating VMInit
        AsyncQuery getVmInitQuery = new AsyncQuery();
        getVmInitQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result) {
                vmInitLoaded((VM) result);
            }
        };
        AsyncDataProvider.getVmById(getVmInitQuery, vm.getId());

    }

    private void vmInitLoaded(VM vm) {
        UnitVmModel model = new UnitVmModel(new ExistingVmModelBehavior(vm));
        model.getVmType().setSelectedItem(vm.getVmType());
        model.setVmAttachedToPool(vm.getVmPoolId() != null);
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance()
                .getConstants().editVmTitle());
        model.setHelpTag(HelpTag.edit_vm);
        model.setHashName("edit_vm"); //$NON-NLS-1$
        model.setCustomPropertiesKeysList(getCustomPropertiesKeysList());

        model.initialize(this.getSystemTreeSelectedItem());

        VmBasedWidgetSwitchModeCommand switchModeCommand = new VmBasedWidgetSwitchModeCommand();
        switchModeCommand.init(model);
        model.getCommands().add(switchModeCommand);

        UICommand tempVar = new UICommand("OnSave", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    private Map<Guid, EntityModel> vmsRemoveMap;

    private void remove()
    {
        if (getWindow() != null)
        {
            return;
        }

        ConfirmationModel window = new ConfirmationModel();
        setWindow(window);
        window.setTitle(ConstantsManager.getInstance().getConstants().removeVirtualMachinesTitle());
        window.setHelpTag(HelpTag.remove_virtual_machine);
        window.setHashName("remove_virtual_machine"); //$NON-NLS-1$

        vmsRemoveMap = new HashMap<Guid, EntityModel>();

        for (Object selectedItem : getSelectedItems())
        {
            VM vm = (VM) selectedItem;
            EntityModel removeDisksCheckbox = new EntityModel(true);
            removeDisksCheckbox.setTitle(ConstantsManager.getInstance().getConstants().removeDisksTitle());
            removeDisksCheckbox.setMessage(vm.getName());
            if (!Guid.Empty.equals(vm.getVmtGuid())) {
                updateRemoveDisksCheckBox(removeDisksCheckbox, true, false, ConstantsManager.getInstance()
                        .getConstants()
                        .removeVmDisksTemplateMsg());
            }
            vmsRemoveMap.put(vm.getId(), removeDisksCheckbox);
        }
        window.setItems(vmsRemoveMap.entrySet());
        initRemoveDisksCheckboxes(vmsRemoveMap);

        UICommand tempVar = new UICommand("OnRemove", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        window.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        window.getCommands().add(tempVar2);
    }

    private void updateRemoveDisksCheckBox(EntityModel model,
            boolean deleteDisks,
            boolean isChangable,
            String changeProhibitionReason) {

        model.setEntity(deleteDisks);
        if (!isChangable && changeProhibitionReason != null) {
            model.setChangeProhibitionReason(changeProhibitionReason);
        }
        model.setIsChangable(isChangable);
    }

    private void initRemoveDisksCheckboxes(final Map<Guid, EntityModel> vmsMap) {
        ArrayList<VdcQueryParametersBase> params = new ArrayList<VdcQueryParametersBase>();
        ArrayList<VdcQueryType> queries = new ArrayList<VdcQueryType>();

        for (Entry<Guid, EntityModel> entry : vmsMap.entrySet()) {
            if (entry.getValue().getIsChangable()) { // No point in fetching VM disks from ones that already determined
                                                     // is unchangeable since they are already initialized
                params.add(new IdQueryParameters(entry.getKey()));
                queries.add(VdcQueryType.GetAllDisksByVmId);
            }
        }

        // TODO: There's no point in creating a VdcQueryType list when you wanna run the same query for all parameters,
        // revise when refactoring org.ovirt.engine.ui.Frontend to support runMultipleQuery with a single query
        if (!params.isEmpty()) {
            Frontend.getInstance().runMultipleQueries(queries, params, new IFrontendMultipleQueryAsyncCallback() {
                @Override
                public void executed(FrontendMultipleQueryAsyncResult result) {
                    for (int i = 0; i < result.getReturnValues().size(); i++) {
                        if (result.getReturnValues().get(i).getSucceeded()) {
                            Guid vmId = ((IdQueryParameters) result.getParameters().get(i)).getId();
                            initRemoveDisksChecboxesPost(vmId, (List<Disk>) result.getReturnValues()
                                    .get(i)
                                    .getReturnValue());
                        }
                    }
                }
            });
        }
    }

    private void initRemoveDisksChecboxesPost(Guid vmId, List<Disk> disks) {
        EntityModel model = vmsRemoveMap.get(vmId);
        if (disks.isEmpty()) {
            updateRemoveDisksCheckBox(model, false, false, ConstantsManager.getInstance()
                    .getConstants()
                    .removeVmDisksNoDisksMsg());
            return;
        }

        boolean isOnlySharedDisks = true;
        boolean isSnapshotExists = false;
        for (Disk disk : disks) {
            if (!disk.isShareable()) {
                isOnlySharedDisks = false;
                if (disk.getDiskStorageType() == DiskStorageType.IMAGE) {
                    if (((DiskImage) disk).getSnapshots().size() > 1) {
                        isSnapshotExists = true;
                        break;
                    }
                }
            }
        }

        if (isSnapshotExists) {
            updateRemoveDisksCheckBox(model, true, false, ConstantsManager.getInstance()
                    .getConstants()
                    .removeVmDisksSnapshotsMsg());
            return;
        }

        if (isOnlySharedDisks) {
            updateRemoveDisksCheckBox(model, false, false, ConstantsManager.getInstance()
                    .getConstants()
                    .removeVmDisksAllSharedMsg());
            return;
        }
    }

    private void createSnapshot() {
        VM vm = (VM) getSelectedItem();
        if (vm == null || getWindow() != null) {
            return;
        }

        SnapshotModel model = SnapshotModel.createNewSnapshotModel(this);
        model.setValidateByVmSnapshots(true);
        setWindow(model);
        model.setVm(vm);
        model.initialize();
    }

    @Override
    protected String thereIsNoExportDomainBackupEntityAttachExportDomainToVmsDcMsg() {
        return ConstantsManager.getInstance()
                .getConstants()
                .thereIsNoExportDomainBackupVmAttachExportDomainToVmsDcMsg();
    }

    @Override
    protected VdcQueryType getEntityExportDomain() {
        return VdcQueryType.GetVmsFromExportDomain;
    }

    @Override
    protected String entityResideOnSeveralDCsMakeSureTheExportedVMResideOnSameDcMsg() {
        return ConstantsManager.getInstance()
                .getConstants()
                .vmsResideOnSeveralDCsMakeSureTheExportedVMResideOnSameDcMsg();
    }

    @Override
    protected boolean entitiesSelectedOnDifferentDataCenters()
    {
        ArrayList<VM> vms = new ArrayList<VM>();
        for (Object selectedItem : getSelectedItems())
        {
            VM a = (VM) selectedItem;
            vms.add(a);
        }

        Map<Guid, ArrayList<VM>> t = new HashMap<Guid, ArrayList<VM>>();
        for (VM a : vms)
        {
            if (!t.containsKey(a.getStoragePoolId()))
            {
                t.put(a.getStoragePoolId(), new ArrayList<VM>());
            }

            ArrayList<VM> list = t.get(a.getStoragePoolId());
            list.add(a);
        }

        return t.size() > 1;
    }

    @Override
    protected String extractNameFromEntity(VM entity) {
        return entity.getName();
    }

    @Override
    protected boolean entititesEqualsNullSafe(VM e1, VM e2) {
        return e1.getId().equals(e2.getId());
    }

    @Override
    protected String composeEntityOnStorage(String entities) {
        return ConstantsManager.getInstance()
                .getMessages()
                .vmsAlreadyExistOnTargetExportDomain(entities);
    }

    @Override
    protected Iterable<VM> asIterableReturnValue(Object returnValue) {
        return (List<VM>) returnValue;
    }

    private void getTemplatesNotPresentOnExportDomain()
    {
        ExportVmModel model = (ExportVmModel) getWindow();
        Guid storageDomainId = ((StorageDomain) model.getStorage().getSelectedItem()).getId();

        AsyncDataProvider.getDataCentersByStorageDomain(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {
                        VmListModel vmListModel = (VmListModel) target;
                        ArrayList<StoragePool> storagePools =
                                (ArrayList<StoragePool>) returnValue;
                        StoragePool storagePool = storagePools.size() > 0 ? storagePools.get(0) : null;

                        vmListModel.postGetTemplatesNotPresentOnExportDomain(storagePool);
                    }
                }), storageDomainId);
    }

    private void postGetTemplatesNotPresentOnExportDomain(StoragePool storagePool)
    {
        ExportVmModel model = (ExportVmModel) getWindow();
        Guid storageDomainId = ((StorageDomain) model.getStorage().getSelectedItem()).getId();

        if (storagePool != null)
        {
            AsyncDataProvider.getAllTemplatesFromExportDomain(new AsyncQuery(this,
                    new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object target, Object returnValue) {
                            VmListModel vmListModel = (VmListModel) target;
                            HashMap<VmTemplate, ArrayList<DiskImage>> templatesDiskSet =
                                    (HashMap<VmTemplate, ArrayList<DiskImage>>) returnValue;
                            HashMap<String, ArrayList<String>> templateDic =
                                    new HashMap<String, ArrayList<String>>();

                            // check if relevant templates are already there
                            for (Object selectedItem : vmListModel.getSelectedItems())
                            {
                                VM vm = (VM) selectedItem;
                                boolean hasMatch = false;
                                for (VmTemplate a : templatesDiskSet.keySet())
                                {
                                    if (vm.getVmtGuid().equals(a.getId()))
                                    {
                                        hasMatch = true;
                                        break;
                                    }
                                }

                                if (!vm.getVmtGuid().equals(Guid.Empty) && !hasMatch)
                                {
                                    if (!templateDic.containsKey(vm.getVmtName()))
                                    {
                                        templateDic.put(vm.getVmtName(), new ArrayList<String>());
                                    }
                                    templateDic.get(vm.getVmtName()).add(vm.getName());
                                }
                            }

                            String tempStr;
                            ArrayList<String> tempList;
                            ArrayList<String> missingTemplates = new ArrayList<String>();
                            for (Map.Entry<String, ArrayList<String>> keyValuePair : templateDic.entrySet())
                            {
                                tempList = keyValuePair.getValue();
                                StringBuilder sb = new StringBuilder("Template " + keyValuePair.getKey() + " (for "); //$NON-NLS-1$ //$NON-NLS-2$
                                int i;
                                for (i = 0; i < tempList.size() - 1; i++)
                                {
                                    sb.append(tempList.get(i));
                                    sb.append(", "); //$NON-NLS-1$
                                }
                                sb.append(tempList.get(i));
                                sb.append(")"); //$NON-NLS-1$
                                missingTemplates.add(sb.toString());
                            }

                            vmListModel.postExportGetMissingTemplates(missingTemplates);
                        }
                    }),
                    storagePool.getId(),
                    storageDomainId);
        }
    }

    private void postExportGetMissingTemplates(ArrayList<String> missingTemplatesFromVms)
    {
        ExportVmModel model = (ExportVmModel) getWindow();
        Guid storageDomainId = ((StorageDomain) model.getStorage().getSelectedItem()).getId();
        ArrayList<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();

        model.stopProgress();

        for (Object a : getSelectedItems())
        {
            VM vm = (VM) a;
            MoveVmParameters parameter = new MoveVmParameters(vm.getId(), storageDomainId);
            parameter.setForceOverride((Boolean) model.getForceOverride().getEntity());
            parameter.setCopyCollapse((Boolean) model.getCollapseSnapshots().getEntity());
            parameter.setTemplateMustExists(true);

            parameters.add(parameter);
        }

        if (!(Boolean) model.getCollapseSnapshots().getEntity())
        {
            if ((missingTemplatesFromVms == null || missingTemplatesFromVms.size() > 0))
            {
                ConfirmationModel confirmModel = new ConfirmationModel();
                setConfirmWindow(confirmModel);
                confirmModel.setTitle(ConstantsManager.getInstance()
                        .getConstants()
                        .templatesNotFoundOnExportDomainTitle());
                confirmModel.setHelpTag(HelpTag.template_not_found_on_export_domain);
                confirmModel.setHashName("template_not_found_on_export_domain"); //$NON-NLS-1$

                confirmModel.setMessage(missingTemplatesFromVms == null ? ConstantsManager.getInstance()
                        .getConstants()
                        .couldNotReadTemplatesFromExportDomainMsg()
                        : ConstantsManager.getInstance()
                                .getConstants()
                                .theFollowingTemplatesAreMissingOnTargetExportDomainMsg());
                confirmModel.setItems(missingTemplatesFromVms);

                UICommand tempVar = new UICommand("OnExportNoTemplates", this); //$NON-NLS-1$
                tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
                tempVar.setIsDefault(true);
                confirmModel.getCommands().add(tempVar);
                UICommand tempVar2 = new UICommand("CancelConfirmation", this); //$NON-NLS-1$
                tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
                tempVar2.setIsCancel(true);
                confirmModel.getCommands().add(tempVar2);
            }
            else
            {
                if (model.getProgress() != null)
                {
                    return;
                }

                model.startProgress(null);

                Frontend.getInstance().runMultipleAction(VdcActionType.ExportVm, parameters,
                        new IFrontendMultipleActionAsyncCallback() {
                            @Override
                            public void executed(FrontendMultipleActionAsyncResult result) {
                                ExportVmModel localModel = (ExportVmModel) result.getState();
                                localModel.stopProgress();
                                cancel();
                            }
                        }, model);
            }
        }
        else
        {
            if (model.getProgress() != null)
            {
                return;
            }

            for (VdcActionParametersBase item : parameters)
            {
                MoveVmParameters parameter = (MoveVmParameters) item;
                parameter.setTemplateMustExists(false);
            }

            model.startProgress(null);

            Frontend.getInstance().runMultipleAction(VdcActionType.ExportVm, parameters,
                                                     new IFrontendMultipleActionAsyncCallback() {
                                                         @Override
                                                         public void executed(FrontendMultipleActionAsyncResult result) {
                                                             ExportVmModel localModel = (ExportVmModel) result.getState();
                                                             localModel.stopProgress();
                                                             cancel();
                                                         }
                                                     }, model);
        }
    }

    public void onExport()
    {
        ExportVmModel model = (ExportVmModel) getWindow();
        if (!model.validate())
        {
            return;
        }

        model.startProgress(null);

        getTemplatesNotPresentOnExportDomain();
    }

    private void onExportNoTemplates()
    {
        ExportVmModel model = (ExportVmModel) getWindow();
        Guid storageDomainId = ((StorageDomain) model.getStorage().getSelectedItem()).getId();

        if (model.getProgress() != null)
        {
            return;
        }

        ArrayList<VdcActionParametersBase> list = new ArrayList<VdcActionParametersBase>();
        for (Object item : getSelectedItems())
        {
            VM a = (VM) item;
            MoveVmParameters parameters = new MoveVmParameters(a.getId(), storageDomainId);
            parameters.setForceOverride((Boolean) model.getForceOverride().getEntity());
            parameters.setCopyCollapse((Boolean) model.getCollapseSnapshots().getEntity());
            parameters.setTemplateMustExists(false);

            list.add(parameters);
        }

        model.startProgress(null);

        Frontend.getInstance().runMultipleAction(VdcActionType.ExportVm, list,
                                                 new IFrontendMultipleActionAsyncCallback() {
                                                     @Override
                                                     public void executed(FrontendMultipleActionAsyncResult result) {

                                                         ExportVmModel localModel = (ExportVmModel) result.getState();
                                                         localModel.stopProgress();
                                                         cancel();

                                                     }
                                                 }, model);
    }

    @Override
    protected void sendWarningForNonExportableDisks(VM entity) {
        // load VM disks and check if there is one which doesn't allow snapshot
        AsyncDataProvider.getVmDiskList(new AsyncQuery(getWindow(),
                                                       new INewAsyncCallback() {
                                                           @Override
                                                           public void onSuccess(Object target, Object returnValue) {
                                                               final ExportVmModel model = (ExportVmModel) target;
                                                               @SuppressWarnings("unchecked")
                                                               final ArrayList<Disk> vmDisks = (ArrayList<Disk>) returnValue;
                                                               VmModelHelper.sendWarningForNonExportableDisks(model,
                                                                                                              vmDisks,
                                                                                                              VmModelHelper.WarningType.VM_EXPORT);
                                                           }
                                                       }),
                                        entity.getId());
    }

    private void runOnce()
    {
        VM vm = (VM) getSelectedItem();
        // populating VMInit
        AsyncQuery getVmInitQuery = new AsyncQuery();
        getVmInitQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result) {
                RunOnceModel runOnceModel = new WebadminRunOnceModel((VM) result,
                        getCustomPropertiesKeysList().get(((VM) result).getVdsGroupCompatibilityVersion()),
                        VmListModel.this);
                setWindow(runOnceModel);
                runOnceModel.init();
            }
        };
        AsyncDataProvider.getVmById(getVmInitQuery, vm.getId());


    }

    private void newTemplate()
    {
        VM vm = (VM) getSelectedItem();
        if (vm == null || getWindow() != null)
        {
            return;
        }

        UnitVmModel model = new UnitVmModel(new NewTemplateVmModelBehavior(vm));
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().newTemplateTitle());
        model.setHelpTag(HelpTag.new_template);
        model.setHashName("new_template"); //$NON-NLS-1$
        model.setIsNew(true);
        model.getVmType().setSelectedItem(vm.getVmType());

        model.initialize(getSystemTreeSelectedItem());

        model.getCommands().add(
                new UICommand("OnNewTemplate", this) //$NON-NLS-1$
                        .setTitle(ConstantsManager.getInstance().getConstants().ok())
                        .setIsDefault(true));

        model.getCommands().add(
                new UICommand("Cancel", this) //$NON-NLS-1$
                .setTitle(ConstantsManager.getInstance().getConstants().cancel())
                .setIsCancel(true));

        model.getIsHighlyAvailable().setEntity(vm.getStaticData().isAutoStartup());
    }

    private void onNewTemplate()
    {
        UnitVmModel model = (UnitVmModel) getWindow();
        VM vm = (VM) getSelectedItem();
        if (vm == null)
        {
            cancel();
            return;
        }

        if (model.getProgress() != null)
        {
            return;
        }

        if (!model.validate())
        {
            model.setIsValid(false);
        }
        else
        {
            String name = model.getName().getEntity();

            // Check name unicitate.
            AsyncDataProvider.isTemplateNameUnique(new AsyncQuery(this,
                    new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object target, Object returnValue) {

                            VmListModel vmListModel = (VmListModel) target;
                            boolean isNameUnique = (Boolean) returnValue;
                            if (!isNameUnique)
                            {
                                UnitVmModel VmModel = (UnitVmModel) vmListModel.getWindow();
                                VmModel.getInvalidityReasons().clear();
                                VmModel.getName()
                                        .getInvalidityReasons()
                                        .add(ConstantsManager.getInstance()
                                                .getConstants()
                                                .nameMustBeUniqueInvalidReason());
                                VmModel.getName().setIsValid(false);
                                VmModel.setIsValid(false);
                            }
                            else
                            {
                                vmListModel.postNameUniqueCheck();
                            }

                        }
                    }),
                    name);
        }
    }

    private void postNameUniqueCheck()
    {
        UnitVmModel model = (UnitVmModel) getWindow();
        VM vm = (VM) getSelectedItem();

        VM newVm = buildVmOnNewTemplate(model, vm);

        AddVmTemplateParameters addVmTemplateParameters =
                new AddVmTemplateParameters(newVm,
                        model.getName().getEntity(),
                        model.getDescription().getEntity());
        addVmTemplateParameters.setPublicUse(model.getIsTemplatePublic().getEntity());

        addVmTemplateParameters.setDiskInfoDestinationMap(
                model.getDisksAllocationModel().getImageToDestinationDomainMap());
        addVmTemplateParameters.setSoundDeviceEnabled(model.getIsSoundcardEnabled().getEntity());
        addVmTemplateParameters.setCopyVmPermissions(model.getCopyPermissions().getEntity());
        model.startProgress(null);
        addVmTemplateParameters.setConsoleEnabled(model.getIsConsoleDeviceEnabled().getEntity());
        if (model.getIsSubTemplate().getEntity()) {
            addVmTemplateParameters.setBaseTemplateId(model.getBaseTemplate().getSelectedItem().getId());
            addVmTemplateParameters.setTemplateVersionName(model.getTemplateVersionName().getEntity());
        }

        Frontend.getInstance().runAction(VdcActionType.AddVmTemplate, addVmTemplateParameters,
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {

                        VmListModel vmListModel = (VmListModel) result.getState();
                        vmListModel.getWindow().stopProgress();
                        VdcReturnValueBase returnValueBase = result.getReturnValue();
                        if (returnValueBase != null && returnValueBase.getSucceeded())
                        {
                            vmListModel.cancel();
                        }

                    }
                }, this);
    }

    protected static VM buildVmOnNewTemplate(UnitVmModel model, VM vm) {
        VM tempVar = new VM();
        tempVar.setId(vm.getId());
        BuilderExecutor.build(model, tempVar.getStaticData(), new CommonUnitToVmBaseBuilder());
        BuilderExecutor.build(vm.getStaticData(), tempVar.getStaticData(),
                              new KernelParamsVmBaseToVmBaseBuilder(),
                              new DedicatedVmForVdsVmBaseToVmBaseBuilder(),
                              new MigrationOptionsVmBaseToVmBaseBuilder(),
                              new UsbPolicyVmBaseToVmBaseBuilder());
        return tempVar;
    }

    private void migrate()
    {
        VM vm = (VM) getSelectedItem();
        if (vm == null)
        {
            return;
        }

        if (getWindow() != null)
        {
            return;
        }

        MigrateModel model = new MigrateModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().migrateVirtualMachinesTitle());
        model.setHelpTag(HelpTag.migrate_virtual_machine);
        model.setHashName("migrate_virtual_machine"); //$NON-NLS-1$
        model.setVmsOnSameCluster(true);
        model.setIsAutoSelect(true);
        model.setVmList(Linq.<VM> cast(getSelectedItems()));

        AsyncDataProvider.getUpHostListByCluster(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {
                        VmListModel vmListModel = (VmListModel) target;
                        vmListModel.postMigrateGetUpHosts((ArrayList<VDS>) returnValue);
                    }
                }), vm.getVdsGroupName());
    }

    private void cancelMigration()
    {
        ArrayList<VdcActionParametersBase> list = new ArrayList<VdcActionParametersBase>();
        for (Object item : getSelectedItems()) {
            VM a = (VM) item;
            list.add(new VmOperationParameterBase(a.getId()));
        }

        Frontend.getInstance().runMultipleAction(VdcActionType.CancelMigrateVm, list,
                                                 new IFrontendMultipleActionAsyncCallback() {
                                                     @Override
                                                     public void executed(
                                                             FrontendMultipleActionAsyncResult result) {
                                                     }
                                                 }, null);
    }

    private void postMigrateGetUpHosts(ArrayList<VDS> hosts)
    {
        MigrateModel model = (MigrateModel) getWindow();
        Guid run_on_vds = null;
        boolean allRunOnSameVds = true;

        for (Object item : getSelectedItems())
        {
            VM a = (VM) item;
            if (!a.getVdsGroupId().equals(((VM) getSelectedItems().get(0)).getVdsGroupId()))
            {
                model.setVmsOnSameCluster(false);
            }
            if (run_on_vds == null)
            {
                run_on_vds = a.getRunOnVds();
            }
            else if (allRunOnSameVds && !run_on_vds.equals(a.getRunOnVds()))
            {
                allRunOnSameVds = false;
            }
        }

        model.setIsHostSelAvailable(model.getVmsOnSameCluster() && hosts.size() > 0);

        if (model.getVmsOnSameCluster() && allRunOnSameVds)
        {
            VDS runOnSameVDS = null;
            for (VDS host : hosts)
            {
                if (host.getId().equals(run_on_vds))
                {
                    runOnSameVDS = host;
                }
            }
            hosts.remove(runOnSameVDS);
        }
        if (hosts.isEmpty())
        {
            model.setIsHostSelAvailable(false);

            if (allRunOnSameVds)
            {
                model.setNoSelAvailable(true);

                UICommand tempVar = new UICommand("Cancel", this); //$NON-NLS-1$
                tempVar.setTitle(ConstantsManager.getInstance().getConstants().close());
                tempVar.setIsDefault(true);
                tempVar.setIsCancel(true);
                model.getCommands().add(tempVar);
            }
        }
        else
        {
            model.getHosts().setItems(hosts);
            model.getHosts().setSelectedItem(Linq.firstOrDefault(hosts));

            UICommand tempVar2 = new UICommand("OnMigrate", this); //$NON-NLS-1$
            tempVar2.setTitle(ConstantsManager.getInstance().getConstants().ok());
            tempVar2.setIsDefault(true);
            model.getCommands().add(tempVar2);
            UICommand tempVar3 = new UICommand("Cancel", this); //$NON-NLS-1$
            tempVar3.setTitle(ConstantsManager.getInstance().getConstants().cancel());
            tempVar3.setIsCancel(true);
            model.getCommands().add(tempVar3);
        }
    }

    private void onMigrate()
    {
        MigrateModel model = (MigrateModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        model.startProgress(null);

        if (model.getIsAutoSelect())
        {
            ArrayList<VdcActionParametersBase> list = new ArrayList<VdcActionParametersBase>();
            for (Object item : getSelectedItems())
            {
                VM a = (VM) item;
                list.add(new MigrateVmParameters(true, a.getId()));
            }

            Frontend.getInstance().runMultipleAction(VdcActionType.MigrateVm, list,
                    new IFrontendMultipleActionAsyncCallback() {
                        @Override
                        public void executed(FrontendMultipleActionAsyncResult result) {

                            MigrateModel localModel = (MigrateModel) result.getState();
                            localModel.stopProgress();
                            cancel();

                        }
                    }, model);
        }
        else
        {
            ArrayList<VdcActionParametersBase> list = new ArrayList<VdcActionParametersBase>();
            for (Object item : getSelectedItems())
            {
                VM a = (VM) item;

                if (a.getRunOnVds().equals(((VDS) model.getHosts().getSelectedItem()).getId()))
                {
                    continue;
                }

                list.add(new MigrateVmToServerParameters(true, a.getId(), ((VDS) model.getHosts()
                        .getSelectedItem()).getId()));
            }

            Frontend.getInstance().runMultipleAction(VdcActionType.MigrateVmToServer, list,
                                                     new IFrontendMultipleActionAsyncCallback() {
                                                         @Override
                                                         public void executed(FrontendMultipleActionAsyncResult result) {

                                                     MigrateModel localModel = (MigrateModel) result.getState();
                                                             localModel.stopProgress();
                                                             cancel();

                                                         }
                                                     }, model);
        }
    }

    private void powerAction(String actionName, String title, String message) {
        ConfirmationModel model = new ConfirmationModel();
        setWindow(model);
        model.setTitle(title);

        if (actionName.equals(SHUTDOWN)) {
            model.setHelpTag(HelpTag.shutdown_virtual_machine);
            model.setHashName("shutdown_virtual_machine"); //$NON-NLS-1$
        }
        else if (actionName.equals(STOP)) {
            model.setHelpTag(HelpTag.stop_virtual_machine);
            model.setHashName("stop_virtual_machine"); //$NON-NLS-1$
        }
        else if (actionName.equals(REBOOT)) {
            model.setHelpTag(HelpTag.reboot_virtual_machine);
            model.setHashName("reboot_virtual_machine"); //$NON-NLS-1$
        }

        model.setMessage(message);
        // model.Items = SelectedItems.Cast<VM>().Select(a => a.vm_name);
        ArrayList<String> items = new ArrayList<String>();
        for (Object item : getSelectedItems())
        {
            VM a = (VM) item;
            items.add(a.getName());
        }
        model.setItems(items);

        UICommand tempVar = new UICommand("On" + actionName, this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    private interface PowerActionParametersFactory<P extends VdcActionParametersBase> {
        P createActionParameters(VM vm);
    }

    private void onPowerAction(VdcActionType actionType, PowerActionParametersFactory<?> parametersFactory) {
        ConfirmationModel model = (ConfirmationModel) getWindow();

        if (model.getProgress() != null) {
            return;
        }


        ArrayList<VdcActionParametersBase> list = new ArrayList<VdcActionParametersBase>();
        for (Object item : getSelectedItems()) {
            VM vm = (VM) item;
            list.add(parametersFactory.createActionParameters(vm));
        }

        model.startProgress(null);

        Frontend.getInstance().runMultipleAction(actionType, list,
                                                 new IFrontendMultipleActionAsyncCallback() {
                                                     @Override
                                                     public void executed(FrontendMultipleActionAsyncResult result) {

                                                         ConfirmationModel localModel = (ConfirmationModel) result.getState();
                                                         localModel.stopProgress();
                                                         cancel();

                                                     }
                                                 }, model);

    }

    private void shutdown() {
        UIConstants constants = ConstantsManager.getInstance().getConstants();
        powerAction(SHUTDOWN,
                    constants.shutdownVirtualMachinesTitle(),
                    constants.areYouSureYouWantToShutDownTheFollowingVirtualMachinesMsg());
    }

    private void onShutdown() {
        onPowerAction(VdcActionType.ShutdownVm, new PowerActionParametersFactory<VdcActionParametersBase>() {
            @Override
            public VdcActionParametersBase createActionParameters(VM vm) {
                return new ShutdownVmParameters(vm.getId(), true);
            }
        });
    }

    private void stop() {
        UIConstants constants = ConstantsManager.getInstance().getConstants();
        powerAction(STOP,
                    constants.stopVirtualMachinesTitle(),
                    constants.areYouSureYouWantToStopTheFollowingVirtualMachinesMsg());
    }

    private void onStop() {
        onPowerAction(VdcActionType.StopVm, new PowerActionParametersFactory<VdcActionParametersBase>() {
            @Override
            public VdcActionParametersBase createActionParameters(VM vm) {
                return new StopVmParameters(vm.getId(), StopVmTypeEnum.NORMAL);
            }
        });
    }

    private void reboot() {
        UIConstants constants = ConstantsManager.getInstance().getConstants();
        powerAction(REBOOT,
                    constants.rebootVirtualMachinesTitle(),
                    constants.areYouSureYouWantToRebootTheFollowingVirtualMachinesMsg());
    }

    private void onReboot() {
        onPowerAction(VdcActionType.RebootVm, new PowerActionParametersFactory<VdcActionParametersBase>() {
            @Override
            public VdcActionParametersBase createActionParameters(VM vm) {
                return new VmOperationParameterBase(vm.getId());
            }
        });
    }

    private void pause()
    {
        ArrayList<VdcActionParametersBase> list = new ArrayList<VdcActionParametersBase>();
        for (Object item : getSelectedItems())
        {
            VM a = (VM) item;
            list.add(new VmOperationParameterBase(a.getId()));
        }

        Frontend.getInstance().runMultipleAction(VdcActionType.HibernateVm, list,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {

                    }
                }, null);
    }

    private void run()
    {
        ArrayList<VdcActionParametersBase> list = new ArrayList<VdcActionParametersBase>();
        for (Object item : getSelectedItems())
        {
            VM a = (VM) item;
            list.add(new RunVmParams(a.getId()));
        }

        Frontend.getInstance().runMultipleAction(VdcActionType.RunVm, list,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {

                    }
                }, null);
    }

    private void onRemove()
    {
        final ConfirmationModel model = (ConfirmationModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        final ArrayList<VdcActionParametersBase> list = new ArrayList<VdcActionParametersBase>();
        for (Entry<Guid, EntityModel> entry : vmsRemoveMap.entrySet()) {
            list.add(new RemoveVmParameters(entry.getKey(), false, (Boolean) entry.getValue().getEntity()));
        }

        model.startProgress(null);

        Frontend.getInstance().runMultipleAction(VdcActionType.RemoveVm, list,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {
                        ConfirmationModel localModel = (ConfirmationModel) result.getState();
                        localModel.stopProgress();
                        cancel();
                    }
                }, model);
    }

    private void changeCD()
    {
        VM vm = (VM) getSelectedItem();
        if (vm == null)
        {
            return;
        }

        AttachCdModel model = new AttachCdModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().changeCDTitle());
        model.setHelpTag(HelpTag.change_cd);
        model.setHashName("change_cd"); //$NON-NLS-1$

        AttachCdModel attachCdModel = (AttachCdModel) getWindow();
        ArrayList<String> images1 =
                new ArrayList<String>(Arrays.asList(new String[] { ConstantsManager.getInstance()
                        .getConstants()
                        .noCds() }));
        attachCdModel.getIsoImage().setItems(images1);
        attachCdModel.getIsoImage().setSelectedItem(Linq.firstOrDefault(images1));

        AsyncQuery getIrsImageListCallback = new AsyncQuery();
        getIrsImageListCallback.setModel(this);

        getIrsImageListCallback.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result)
            {
                VmListModel vmListModel2 = (VmListModel) model;
                AttachCdModel _attachCdModel = (AttachCdModel) vmListModel2.getWindow();
                ArrayList<String> images = (ArrayList<String>) result;
                images.add(0, ConsoleModel.getEjectLabel());
                _attachCdModel.getIsoImage().setItems(images);
                if (_attachCdModel.getIsoImage().getIsChangable())
                {
                    _attachCdModel.getIsoImage().setSelectedItem(Linq.firstOrDefault(images));
                }
            }
        };
        AsyncDataProvider.getIrsImageList(getIrsImageListCallback, vm.getStoragePoolId());

        UICommand tempVar = new UICommand("OnChangeCD", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    private void onChangeCD()
    {
        VM vm = (VM) getSelectedItem();
        if (vm == null)
        {
            cancel();
            return;
        }

        AttachCdModel model = (AttachCdModel) getWindow();
        if (model.getProgress() != null)
        {
            return;
        }

        String isoName =
                (StringHelper.stringsEqual(model.getIsoImage().getSelectedItem().toString(), ConsoleModel.getEjectLabel())) ? "" //$NON-NLS-1$
                        : model.getIsoImage().getSelectedItem().toString();

        model.startProgress(null);

        Frontend.getInstance().runAction(VdcActionType.ChangeDisk, new ChangeDiskCommandParameters(vm.getId(), isoName),
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {

                        AttachCdModel attachCdModel = (AttachCdModel) result.getState();
                        attachCdModel.stopProgress();
                        cancel();

                    }
                }, model);
    }

    private void preSave()
    {
        final UnitVmModel model = (UnitVmModel) getWindow();
        final String name = model.getName().getEntity();

        if (model.getIsNew() == false && selectedItem == null)
        {
            cancel();
            return;
        }

        setcurrentVm(model.getIsNew() ? new VM() : (VM) Cloner.clone(selectedItem));

        if (!model.validate())
        {
            return;
        }

        AsyncDataProvider.isVmNameUnique(new AsyncQuery(this, new INewAsyncCallback() {

            @Override
            public void onSuccess(Object target, Object returnValue) {
                if (!(Boolean) returnValue && name.compareToIgnoreCase(getcurrentVm().getName()) != 0) {
                    model.getName()
                            .getInvalidityReasons()
                            .add(ConstantsManager.getInstance().getConstants().nameMustBeUniqueInvalidReason());
                    model.getName().setIsValid(false);
                    model.setIsGeneralTabValid(false);
                } else {
                    model.getName()
                            .getInvalidityReasons().clear();
                    model.getName().setIsValid(true);
                    model.setIsGeneralTabValid(true);
                    onSave();
                }
            }
        }), name);

    }

    private void onSave() {
        final VmInterfaceCreatingManager defaultNetworkCreatingManager =
                new VmInterfaceCreatingManager(new VmInterfaceCreatingManager.PostVnicCreatedCallback() {
                    @Override
                    public void vnicCreated(Guid vmId) {
                        getWindow().stopProgress();
                        cancel();
                        updateActionAvailability();
                    }

                    @Override
                    public void queryFailed() {
                        getWindow().stopProgress();
                        cancel();
                    }
                });

        final UnitVmModel model = (UnitVmModel) getWindow();
        VM selectedItem = (VM) getSelectedItem();

        // Save changes.
        buildVmOnSave(model, getcurrentVm());

        getcurrentVm().setBalloonEnabled(balloonEnabled(model));

        getcurrentVm().setCpuPinning(model.getCpuPinning().getEntity());

        if (model.getCpuSharesAmount().getIsAvailable() && model.getCpuSharesAmount().getEntity() != null) {  // $NON-NLS-1$
            getcurrentVm().setCpuShares(model.getCpuSharesAmount().getEntity());
        }

        getcurrentVm().setUseHostCpuFlags(model.getHostCpu().getEntity());

        getcurrentVm().setVmInit(model.getVmInitModel().buildCloudInitParameters(model));

        if (model.getIsNew())
        {
            if (getcurrentVm().getVmtGuid().equals(Guid.Empty))
            {
                if (model.getProgress() != null)
                {
                    return;
                }

                VmInterfaceCreatingManager addVmFromScratchNetworkManager =
                        new VmInterfaceCreatingManager(new VmInterfaceCreatingManager.PostVnicCreatedCallback() {
                            @Override
                            public void vnicCreated(Guid vmId) {
                                getWindow().stopProgress();
                                cancel();
                                setGuideContext(vmId);
                                updateActionAvailability();
                                getGuideCommand().execute();
                            }

                            @Override
                            public void queryFailed() {
                                getWindow().stopProgress();
                                cancel();
                            }
                        });


                model.startProgress(null);

                AddVmFromScratchParameters parameters = new AddVmFromScratchParameters(getcurrentVm(),
                        new ArrayList<DiskImage>(),
                        Guid.Empty);
                parameters.setSoundDeviceEnabled(model.getIsSoundcardEnabled().getEntity());
                parameters.setConsoleEnabled(model.getIsConsoleDeviceEnabled().getEntity());
                parameters.setVirtioScsiEnabled(model.getIsVirtioScsiEnabled().getEntity());
                parameters.setBalloonEnabled(balloonEnabled(model));

                setVmWatchdogToParams(model, parameters);

                Frontend.getInstance().runAction(VdcActionType.AddVmFromScratch, parameters, new UnitVmModelNetworkAsyncCallback(model, addVmFromScratchNetworkManager), this);
            }
            else
            {
                if (model.getProgress() != null)
                {
                    return;
                }

                if (model.getProvisioning().getEntity())
                {
                    model.startProgress(null);

                    AsyncQuery _asyncQuery = new AsyncQuery();
                    _asyncQuery.setModel(this);
                    _asyncQuery.asyncCallback = new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object model1, Object result1)
                        {
                            VmListModel vmListModel = (VmListModel) model1;
                            UnitVmModel unitVmModel = (UnitVmModel) vmListModel.getWindow();

                            VM vm = vmListModel.getcurrentVm();
                            vm.setUseLatestVersion(constants.latestTemplateVersionName().equals(unitVmModel.getTemplate().getSelectedItem().getTemplateVersionName()));

                            AddVmFromTemplateParameters param = new AddVmFromTemplateParameters(vm,
                                    unitVmModel.getDisksAllocationModel().getImageToDestinationDomainMap(),
                                    Guid.Empty);
                            param.setSoundDeviceEnabled(model.getIsSoundcardEnabled().getEntity());
                            param.setVirtioScsiEnabled(model.getIsVirtioScsiEnabled().getEntity());
                            param.setConsoleEnabled(model.getIsConsoleDeviceEnabled().getEntity());
                            param.setBalloonEnabled(balloonEnabled(model));
                            param.setCopyTemplatePermissions(model.getCopyPermissions().getEntity());

                            Frontend.getInstance().runAction(VdcActionType.AddVmFromTemplate, param, new UnitVmModelNetworkAsyncCallback(model, defaultNetworkCreatingManager), vmListModel);
                        }
                    };
                    AsyncDataProvider.getTemplateDiskList(_asyncQuery, getcurrentVm().getVmtGuid());
                }
                else
                {
                    if (model.getProgress() != null)
                    {
                        return;
                    }

                    model.startProgress(null);

                    VM vm = getcurrentVm();
                    vm.setUseLatestVersion(constants.latestTemplateVersionName().equals(model.getTemplate().getSelectedItem().getTemplateVersionName()));

                    VmManagementParametersBase params = new VmManagementParametersBase(vm);
                    params.setDiskInfoDestinationMap(model.getDisksAllocationModel().getImageToDestinationDomainMap());
                    params.setConsoleEnabled(model.getIsConsoleDeviceEnabled().getEntity());
                    params.setBalloonEnabled(balloonEnabled(model));
                    params.setCopyTemplatePermissions(model.getCopyPermissions().getEntity());

                    ArrayList<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();
                    parameters.add(params);
                    params.setSoundDeviceEnabled(model.getIsSoundcardEnabled().getEntity());
                    params.setVirtioScsiEnabled(model.getIsVirtioScsiEnabled().getEntity());
                    setVmWatchdogToParams(model, params);

                    Frontend.getInstance().runAction(VdcActionType.AddVm, params, new UnitVmModelNetworkAsyncCallback(model, defaultNetworkCreatingManager), this);
                }
            }
        }
        else // Update existing VM -> consists of editing VM cluster, and if succeeds - editing VM:
        {
            if (model.getProgress() != null)
            {
                return;
            }

            // explicitly pass non-editable field from the original VM
            getcurrentVm().setCreatedByUserId(selectedItem.getCreatedByUserId());

            // runEditVM: should be true if Cluster hasn't changed or if
            // Cluster has changed and Editing it in the Backend has succeeded:
            Guid oldClusterID = selectedItem.getVdsGroupId();
            Guid newClusterID = model.getSelectedCluster().getId();
            if (oldClusterID.equals(newClusterID) == false)
            {
                ChangeVMClusterParameters parameters =
                        new ChangeVMClusterParameters(newClusterID, getcurrentVm().getId());

                model.startProgress(null);

                Frontend.getInstance().runAction(VdcActionType.ChangeVMCluster, parameters,
                        new IFrontendActionAsyncCallback() {
                            @Override
                            public void executed(FrontendActionAsyncResult result) {

                                final VmListModel vmListModel = (VmListModel) result.getState();
                                VdcReturnValueBase returnValueBase = result.getReturnValue();
                                if (returnValueBase != null && returnValueBase.getSucceeded())
                                {
                                    VM vm = vmListModel.getcurrentVm();
                                    vm.setUseLatestVersion(constants.latestTemplateVersionName().equals(model.getTemplate().getSelectedItem().getTemplateVersionName()));

                                    VmManagementParametersBase updateVmParams = new VmManagementParametersBase(vm);
                                    setVmWatchdogToParams(model, updateVmParams);
                                    updateVmParams.setSoundDeviceEnabled(model.getIsSoundcardEnabled()
                                            .getEntity());
                                    updateVmParams.setBalloonEnabled(balloonEnabled(model));
                                    updateVmParams.setVirtioScsiEnabled(model.getIsVirtioScsiEnabled().getEntity());

                                    Frontend.getInstance().runAction(VdcActionType.UpdateVm,
                                            updateVmParams, new UnitVmModelNetworkAsyncCallback(model, defaultNetworkCreatingManager, vm.getId()), vmListModel);
                                }
                                else
                                {
                                    vmListModel.getWindow().stopProgress();
                                }

                            }
                        },
                        this);
            }
            else
            {
                if (model.getProgress() != null)
                {
                    return;
                }

                model.startProgress(null);

                VM vm = getcurrentVm();
                vm.setUseLatestVersion(constants.latestTemplateVersionName().equals(model.getTemplate().getSelectedItem().getTemplateVersionName()));

                VmManagementParametersBase updateVmParams = new VmManagementParametersBase(vm);

                setVmWatchdogToParams(model, updateVmParams);
                updateVmParams.setSoundDeviceEnabled(model.getIsSoundcardEnabled().getEntity());
                updateVmParams.setConsoleEnabled(model.getIsConsoleDeviceEnabled().getEntity());
                updateVmParams.setBalloonEnabled(balloonEnabled(model));
                updateVmParams.setVirtioScsiEnabled(model.getIsVirtioScsiEnabled().getEntity());

                Frontend.getInstance().runAction(VdcActionType.UpdateVm, updateVmParams, new UnitVmModelNetworkAsyncCallback(model, defaultNetworkCreatingManager, getcurrentVm().getId()), this);
            }
        }
    }

    protected static void buildVmOnSave(UnitVmModel model, VM vm) {
        BuilderExecutor.build(model, vm.getStaticData(), new FullUnitToVmBaseBuilder());
        BuilderExecutor.build(model, vm, new VmSpecificUnitToVmBuilder());
    }

    private boolean balloonEnabled(UnitVmModel model) {
        return model.getMemoryBalloonDeviceEnabled().getEntity()
                && model.getSelectedCluster().getcompatibility_version().compareTo(BALLOON_DEVICE_MIN_VERSION) >= 0;
    }

    private void setVmWatchdogToParams(final UnitVmModel model, VmManagementParametersBase updateVmParams) {
        VmWatchdogType wdModel = VmWatchdogType.getByName(model.getWatchdogModel()
                .getSelectedItem());
        updateVmParams.setUpdateWatchdog(true);
        if (wdModel != null) {
            VmWatchdog vmWatchdog = new VmWatchdog();
            vmWatchdog.setAction(VmWatchdogAction.getByName(model.getWatchdogAction()
                    .getSelectedItem()));
            vmWatchdog.setModel(wdModel);
            updateVmParams.setWatchdog(vmWatchdog);
        }
    }

    private void retrieveIsoImages()
    {
        Object tempVar = getSelectedItem();
        VM vm = (VM) ((tempVar instanceof VM) ? tempVar : null);
        if (vm == null)
        {
            return;
        }

        getIsoImages().clear();

        ChangeCDModel tempVar2 = new ChangeCDModel();
        tempVar2.setTitle(ConsoleModel.getEjectLabel());
        ChangeCDModel ejectModel = tempVar2;
        ejectModel.getExecutedEvent().addListener(this);
        getIsoImages().add(ejectModel);

        ChangeCDModel tempVar4 = new ChangeCDModel();
        tempVar4.setTitle(ConstantsManager.getInstance().getConstants().noCds());
        getIsoImages().add(tempVar4);
    }

    private void changeCD(Object sender, EventArgs e)
    {
        ChangeCDModel model = (ChangeCDModel) sender;

        // TODO: Patch!
        String isoName = model.getTitle();
        if (StringHelper.stringsEqual(isoName, ConstantsManager.getInstance()
                .getConstants()
                .noCds()))
        {
            return;
        }

        Object tempVar = getSelectedItem();
        VM vm = (VM) ((tempVar instanceof VM) ? tempVar : null);
        if (vm == null)
        {
            return;
        }

        Frontend.getInstance().runMultipleAction(VdcActionType.ChangeDisk,
                new ArrayList<VdcActionParametersBase>(Arrays.asList(new VdcActionParametersBase[] { new ChangeDiskCommandParameters(vm.getId(),
                        StringHelper.stringsEqual(isoName, ConsoleModel.getEjectLabel()) ? "" : isoName) })), //$NON-NLS-1$
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {

                    }
                },
                null);
    }

    public void cancel()
    {
        Frontend.getInstance().unsubscribe();

        cancelConfirmation();

        setGuideContext(null);
        setWindow(null);

        updateActionAvailability();
    }

    private void cancelConfirmation()
    {
        setConfirmWindow(null);
    }

    @Override
    protected void onSelectedItemChanged()
    {
        super.onSelectedItemChanged();

        updateActionAvailability();
    }

    @Override
    protected void selectedItemsChanged() {
        super.selectedItemsChanged();

        updateActionAvailability();
    }

    @Override
    protected void selectedItemPropertyChanged(Object sender, PropertyChangedEventArgs e) {
        super.selectedItemPropertyChanged(sender, e);

        if (e.propertyName.equals("status")) { //$NON-NLS-1$
            updateActionAvailability();
        }
    }

    private void updateActionAvailability() {
        List items =
                getSelectedItems() != null && getSelectedItem() != null ? getSelectedItems()
                        : new ArrayList();

        getEditCommand().setIsExecutionAllowed(isEditCommandExecutionAllowed(items));
        getRemoveCommand().setIsExecutionAllowed(items.size() > 0
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.RemoveVm));
        getRunCommand().setIsExecutionAllowed(items.size() > 0
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.RunVm));
        getPauseCommand().setIsExecutionAllowed(items.size() > 0
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.HibernateVm));
        getShutdownCommand().setIsExecutionAllowed(items.size() > 0
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.ShutdownVm));
        getStopCommand().setIsExecutionAllowed(items.size() > 0
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.StopVm));
        getRebootCommand().setIsExecutionAllowed(AsyncDataProvider.isRebootCommandExecutionAllowed(items));
        getMigrateCommand().setIsExecutionAllowed(items.size() > 0
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.MigrateVm));
        getCancelMigrateCommand().setIsExecutionAllowed(items.size() > 0
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.CancelMigrateVm));
        getNewTemplateCommand().setIsExecutionAllowed(items.size() == 1
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.AddVmTemplate));
        getRunOnceCommand().setIsExecutionAllowed(items.size() == 1
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.RunVmOnce));
        getExportCommand().setIsExecutionAllowed(items.size() > 0
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.ExportVm));
        getCreateSnapshotCommand().setIsExecutionAllowed(items.size() == 1
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.CreateAllSnapshotsFromVm));
        getRetrieveIsoImagesCommand().setIsExecutionAllowed(items.size() == 1
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.ChangeDisk));
        getChangeCdCommand().setIsExecutionAllowed(items.size() == 1
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.ChangeDisk));
        getAssignTagsCommand().setIsExecutionAllowed(items.size() > 0);

        getGuideCommand().setIsExecutionAllowed(getGuideContext() != null
                || (getSelectedItem() != null && getSelectedItems() != null && getSelectedItems().size() == 1));

        getConsoleConnectCommand().setIsExecutionAllowed(isConsoleCommandsExecutionAllowed());
        getEditConsoleCommand().setIsExecutionAllowed(isConsoleEditEnabled());
    }

    private boolean isConsoleEditEnabled() {
        return getSelectedItem() != null && ((VM) getSelectedItem()).isRunningOrPaused();
    }

    private boolean isConsoleCommandsExecutionAllowed() {
        final List<VM> list = getSelectedItem() == null ? null : getSelectedItems();
        if (list == null) {
            return false;
        }

        // return true, if at least one console is available
        for (VM vm : list) {
            if (consoleModelsCache.getVmConsolesForEntity(vm).canConnectToConsole()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return true if and only if one element is selected.
     */
    private boolean isEditCommandExecutionAllowed(List items) {
        if (items == null) {
            return false;
        }
        if (items.size() != 1) {
            return false;
        }
        return true;
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (ev.matchesDefinition(ChangeCDModel.executedEventDefinition))
        {
            changeCD(sender, args);
        }
    }

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);

        if (command == getNewVmCommand())
        {
            newVm();
        }
        else if (command == getEditCommand())
        {
            edit();
        }
        else if (command == getEditConsoleCommand())
        {
            editConsole();
        }
        else if (command == getConsoleConnectCommand())
        {
            connectToConsoles();
        }
        else if (command == getRemoveCommand())
        {
            remove();
        }
        else if (command == getRunCommand())
        {
            run();
        }
        else if (command == getPauseCommand())
        {
            pause();
        }
        else if (command == getStopCommand())
        {
            stop();
        }
        else if (command == getShutdownCommand())
        {
            shutdown();
        }
        else if (command == getRebootCommand()) {
            reboot();
        }
        else if (command == getMigrateCommand())
        {
            migrate();
        }
        else if (command == getNewTemplateCommand())
        {
            newTemplate();
        }
        else if (command == getRunOnceCommand())
        {
            runOnce();
        }
        else if (command == getExportCommand())
        {
            export(ConstantsManager.getInstance().getConstants().exportVirtualMachineTitle());
        }
        else if (command == getCreateSnapshotCommand())
        {
            createSnapshot();
        }
        else if (command == getGuideCommand())
        {
            guide();
        }
        else if (command == getRetrieveIsoImagesCommand())
        {
            retrieveIsoImages();
        }
        else if (command == getChangeCdCommand())
        {
            changeCD();
        }
        else if (command == getAssignTagsCommand())
        {
            assignTags();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnAssignTags")) //$NON-NLS-1$
        {
            onAssignTags();
        }
        else if (StringHelper.stringsEqual(command.getName(), "Cancel")) //$NON-NLS-1$
        {
            cancel();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnSave")) //$NON-NLS-1$
        {
            preSave();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnRemove")) //$NON-NLS-1$
        {
            onRemove();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnExport")) //$NON-NLS-1$
        {
            onExport();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnExportNoTemplates")) //$NON-NLS-1$
        {
            onExportNoTemplates();
        }
        else if (StringHelper.stringsEqual(command.getName(), "CancelConfirmation")) //$NON-NLS-1$
        {
            cancelConfirmation();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnRunOnce")) //$NON-NLS-1$
        {
            cancel();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnNewTemplate")) //$NON-NLS-1$
        {
            onNewTemplate();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnMigrate")) //$NON-NLS-1$
        {
            onMigrate();
        }
        else if (command == getCancelMigrateCommand())
        {
            cancelMigration();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnShutdown")) //$NON-NLS-1$
        {
            onShutdown();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnStop")) //$NON-NLS-1$
        {
            onStop();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnReboot")) //$NON-NLS-1$
        {
            onReboot();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnChangeCD")) //$NON-NLS-1$
        {
            onChangeCD();
        }
        else if (command.getName().equals("closeVncInfo") || // $NON-NLS-1$
                "OnEditConsoleSave".equals(command.getName())) { //$NON-NLS-1$
            setWindow(null);
        }

    }

    private void connectToConsoles() {
        StringBuilder errorMessages = null;

        final List<VM> list = getSelectedItems();
        if (list == null || list.isEmpty()) {
            return;
        }

        for (VM vm : list) {
            try {
                consoleModelsCache.getVmConsolesForEntity(vm).connect();
            } catch (VmConsoles.ConsoleConnectException e) {
                final String errorMessage = e.getLocalizedErrorMessage();
                if (errorMessage != null) {
                    if (errorMessages == null) {
                        errorMessages = new StringBuilder();
                    } else {
                        errorMessages.append("\r\n"); //$NON-NLS-1$
                    }

                    errorMessages
                            .append(vm.getName())
                            .append(" - ") //$NON-NLS-1$
                            .append(errorMessage);
                }
            }
        }

        if (errorMessages != null) {
            errorPopupManager.show(errorMessages.toString());
        }
    }

    private SystemTreeItemModel systemTreeSelectedItem;

    @Override
    public SystemTreeItemModel getSystemTreeSelectedItem()
    {
        return systemTreeSelectedItem;
    }

    @Override
    public void setSystemTreeSelectedItem(SystemTreeItemModel value)
    {
        systemTreeSelectedItem = value;
        onPropertyChanged(new PropertyChangedEventArgs("SystemTreeSelectedItem")); //$NON-NLS-1$
    }

    @Override
    protected String getListName() {
        return "VmListModel"; //$NON-NLS-1$
    }

    @Override
    protected Guid extractStoragePoolIdNullSafe(VM entity) {
        return entity.getStoragePoolId();
    }
}
