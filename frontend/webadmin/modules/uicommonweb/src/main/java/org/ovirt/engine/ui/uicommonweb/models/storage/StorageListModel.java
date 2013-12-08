package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ovirt.engine.core.common.action.AddSANStorageDomainParameters;
import org.ovirt.engine.core.common.action.ExtendSANStorageDomainParameters;
import org.ovirt.engine.core.common.action.RemoveStorageDomainParameters;
import org.ovirt.engine.core.common.action.StorageDomainManagementParameter;
import org.ovirt.engine.core.common.action.StorageDomainParametersBase;
import org.ovirt.engine.core.common.action.StorageDomainPoolParametersBase;
import org.ovirt.engine.core.common.action.StorageServerConnectionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.LUNs;
import org.ovirt.engine.core.common.businessentities.NfsVersion;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainSharedStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageFormatType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.searchbackend.SearchObjects;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Cloner;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ISupportSystemTreeContext;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.ListWithDetailsModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemType;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.reports.ReportModel;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicommonweb.validation.RegexValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.ITaskTarget;
import org.ovirt.engine.ui.uicompat.NotifyCollectionChangedEventArgs;
import org.ovirt.engine.ui.uicompat.ObservableCollection;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.Task;
import org.ovirt.engine.ui.uicompat.TaskContext;
import org.ovirt.engine.ui.uicompat.UIConstants;

@SuppressWarnings("unused")
public class StorageListModel extends ListWithDetailsModel implements ITaskTarget, ISupportSystemTreeContext
{

    private UICommand privateNewDomainCommand;

    public UICommand getNewDomainCommand()
    {
        return privateNewDomainCommand;
    }

    private void setNewDomainCommand(UICommand value)
    {
        privateNewDomainCommand = value;
    }

    private UICommand privateImportDomainCommand;

    public UICommand getImportDomainCommand()
    {
        return privateImportDomainCommand;
    }

    private void setImportDomainCommand(UICommand value)
    {
        privateImportDomainCommand = value;
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

    private UICommand privateDestroyCommand;

    public UICommand getDestroyCommand()
    {
        return privateDestroyCommand;
    }

    private void setDestroyCommand(UICommand value)
    {
        privateDestroyCommand = value;
    }

    private ArrayList<String> usedLunsMessages;

    public ArrayList<String> getUsedLunsMessages() {
        return usedLunsMessages;
    }

    public void setUsedLunsMessages(ArrayList<String> usedLunsMessages) {
        this.usedLunsMessages = usedLunsMessages;
    }

    // get { return SelectedItems == null ? new object[0] : SelectedItems.Cast<storage_domains>().Select(a =>
    // a.id).Cast<object>().ToArray(); }
    protected Object[] getSelectedKeys()
    {
        if (getSelectedItems() == null)
        {
            return new Object[0];
        }
        else
        {
            ArrayList<Object> items = new ArrayList<Object>();
            for (Object item : getSelectedItems())
            {
                StorageDomain i = (StorageDomain) item;
                items.add(i.getId());
            }
            return items.toArray(new Object[] {});
        }
    }

    public StorageListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().storageTitle());

        setDefaultSearchString("Storage:"); //$NON-NLS-1$
        setSearchString(getDefaultSearchString());
        setSearchObjects(new String[] { SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME, SearchObjects.VDC_STORAGE_DOMAIN_PLU_OBJ_NAME });
        setAvailableInModes(ApplicationMode.VirtOnly);

        setNewDomainCommand(new UICommand("NewDomain", this)); //$NON-NLS-1$
        setImportDomainCommand(new UICommand("ImportDomain", this)); //$NON-NLS-1$
        setEditCommand(new UICommand("Edit", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$
        setDestroyCommand(new UICommand("Destroy", this)); //$NON-NLS-1$

        updateActionAvailability();

        getSearchNextPageCommand().setIsAvailable(true);
        getSearchPreviousPageCommand().setIsAvailable(true);
    }

    private EntityModel generalModel;
    private EntityModel vmBackupModel;
    private EntityModel templateBackupModel;
    private ListModel dcListModel;
    private ListModel vmListModel;
    private ListModel templateListModel;
    private ListModel isoListModel;
    private ListModel diskListModel;

    public StorageDomainStatic storageDomain;
    public TaskContext context;
    public IStorageModel storageModel;
    public Guid storageId;
    public StorageServerConnections fileConnection;
    public StorageServerConnections connection;
    public Guid hostId = Guid.Empty;
    public String path;
    public StorageDomainType domainType = StorageDomainType.values()[0];
    public StorageType storageType;
    public boolean removeConnection;

    @Override
    protected void initDetailModels()
    {
        super.initDetailModels();

        generalModel = new StorageGeneralModel();
        generalModel.setIsAvailable(false);

        dcListModel = new StorageDataCenterListModel();
        dcListModel.setIsAvailable(false);

        vmBackupModel = new VmBackupModel();
        vmBackupModel.setIsAvailable(false);

        templateBackupModel = new TemplateBackupModel();
        templateBackupModel.setIsAvailable(false);

        vmListModel = new StorageVmListModel();
        vmListModel.setIsAvailable(false);

        templateListModel = new StorageTemplateListModel();
        templateListModel.setIsAvailable(false);

        isoListModel = new StorageIsoListModel();
        isoListModel.setIsAvailable(false);

        diskListModel = new StorageDiskListModel();
        diskListModel.setIsAvailable(false);

        ObservableCollection<EntityModel> list = new ObservableCollection<EntityModel>();
        list.add(generalModel);
        list.add(dcListModel);
        list.add(vmBackupModel);
        list.add(templateBackupModel);
        list.add(vmListModel);
        list.add(templateListModel);
        list.add(isoListModel);
        list.add(diskListModel);
        list.add(new StorageEventListModel());
        list.add(new PermissionListModel());
        setDetailModels(list);
    }

    @Override
    public boolean isSearchStringMatch(String searchString)
    {
        return searchString.trim().toLowerCase().startsWith("storage"); //$NON-NLS-1$
    }

    @Override
    protected void syncSearch()
    {
        SearchParameters tempVar = new SearchParameters(getSearchString(), SearchType.StorageDomain, isCaseSensitiveSearch());
        tempVar.setMaxCount(getSearchPageSize());
        super.syncSearch(VdcQueryType.Search, tempVar);
    }

    private void newDomain()
    {
        if (getWindow() != null)
        {
            return;
        }

        StorageModel model = new StorageModel(new NewEditStorageModelBehavior());
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().newDomainTitle());
        model.setHashName("new_domain"); //$NON-NLS-1$
        model.setSystemTreeSelectedItem(getSystemTreeSelectedItem());

        ArrayList<IStorageModel> items = new ArrayList<IStorageModel>();
        // putting all Data domains at the beginning on purpose (so when choosing the
        // first selectable storage type/function, it will be a Data one, if relevant).

        NfsStorageModel nfsDataModel = new NfsStorageModel();
        nfsDataModel.setRole(StorageDomainType.Data);
        items.add(nfsDataModel);

        IscsiStorageModel iscsiDataModel = new IscsiStorageModel();
        iscsiDataModel.setRole(StorageDomainType.Data);
        iscsiDataModel.setIsGrouppedByTarget(true);
        items.add(iscsiDataModel);

        FcpStorageModel fcpDataModel = new FcpStorageModel();
        fcpDataModel.setRole(StorageDomainType.Data);
        items.add(fcpDataModel);

        LocalStorageModel localDataModel = new LocalStorageModel();
        localDataModel.setRole(StorageDomainType.Data);
        items.add(localDataModel);

        LocalStorageModel localIsoModel = new LocalStorageModel();
        localIsoModel.setRole(StorageDomainType.ISO);
        items.add(localIsoModel);

        PosixStorageModel posixDataModel = new PosixStorageModel();
        posixDataModel.setRole(StorageDomainType.Data);
        items.add(posixDataModel);

        GlusterStorageModel GlusterDataModel = new GlusterStorageModel();
        GlusterDataModel.setRole(StorageDomainType.Data);
        items.add(GlusterDataModel);

        PosixStorageModel posixIsoModel = new PosixStorageModel();
        posixIsoModel.setRole(StorageDomainType.ISO);
        items.add(posixIsoModel);

        NfsStorageModel nfsIsoModel = new NfsStorageModel();
        nfsIsoModel.setRole(StorageDomainType.ISO);
        items.add(nfsIsoModel);

        NfsStorageModel nfsExportModel = new NfsStorageModel();
        nfsExportModel.setRole(StorageDomainType.ImportExport);
        items.add(nfsExportModel);

        model.setItems(items);

        model.initialize();

        UICommand  command = createOKCommand("OnSave"); //$NON-NLS-1$
        model.getCommands().add(command);

        command = createCancelCommand("Cancel"); //$NON-NLS-1$
        model.getCommands().add(command);
    }

    private void edit()
    {
        StorageDomain storage = (StorageDomain) getSelectedItem();

        if (getWindow() != null)
        {
            return;
        }

        final UIConstants constants = ConstantsManager.getInstance().getConstants();
        StorageModel model = new StorageModel(new NewEditStorageModelBehavior());
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().editDomainTitle());
        model.setHashName("edit_domain"); //$NON-NLS-1$
        model.setSystemTreeSelectedItem(getSystemTreeSelectedItem());
        model.setStorage(storage);
        model.getName().setEntity(storage.getStorageName());
        model.getDescription().setEntity(storage.getDescription());
        model.getComment().setEntity(storage.getComment());
        model.setOriginalName(storage.getStorageName());

        model.getDataCenter().setIsChangable(false);
        model.getFormat().setIsChangable(false);

        boolean isStorageEditable = model.isStorageActive() || model.isNewStorage();
        model.getHost().setIsChangable(false);
        model.getName().setIsChangable(isStorageEditable);
        model.getDescription().setIsChangable(isStorageEditable);
        model.getComment().setIsChangable(isStorageEditable);
        //set the field domain type to non editable
        model.getAvailableStorageItems().setIsChangable(false);
        model.setIsChangable(isStorageEditable);

        boolean isPathEditable = isPathEditable(storage);
        isStorageEditable = isStorageEditable || isPathEditable;

        IStorageModel item = null;
        switch (storage.getStorageType()) {
            case NFS:
                item = prepareNfsStorageForEdit(storage);
                //when storage is active, only SPM can perform actions on it, thus it is set above that host is not changeable.
                //If storage is editable but not active (maintenance) - any host can perform the edit so the changeable here is set based on that
                model.getHost().setIsChangable(isPathEditable);
                break;

            case FCP:
                item = prepareFcpStorageForEdit(storage);
                break;

            case ISCSI:
                item = prepareIscsiStorageForEdit(storage);
                break;

            case LOCALFS:
                item = prepareLocalStorageForEdit(storage);
                model.getHost().setIsChangable(isPathEditable);
                break;

            case POSIXFS:
                item = preparePosixStorageForEdit(storage);
                //when storage is active, only SPM can perform actions on it, thus it is set above that host is not changeable.
                //If storage is editable but not active (maintenance) - any host can perform the edit so the changeable here is set based on that
                model.getHost().setIsChangable(isPathEditable);
                break;

            case GLUSTERFS:
                item = prepareGlusterStorageForEdit(storage);
                break;
        }

        model.setItems(new ArrayList<IStorageModel>(Arrays.asList(new IStorageModel[] {item})));
        model.setSelectedItem(item);

        model.initialize();

        if (getSystemTreeSelectedItem() != null && getSystemTreeSelectedItem().getType() == SystemTreeItemType.Storage)
        {
            model.getName().setIsChangable(false);
            model.getName().setChangeProhibitionReason(constants.cannotEditNameInTreeContext());
        }


        UICommand command;
        if (isStorageEditable) {
            command = createOKCommand("OnSave"); //$NON-NLS-1$
            model.getCommands().add(command);

            command = createCancelCommand("Cancel"); //$NON-NLS-1$
            model.getCommands().add(command);
        }
        else {
            // close is created the same as cancel, but with a different title
            // thus most of creation code can be reused.
            command = createCancelCommand("Cancel"); //$NON-NLS-1$
            command.setTitle(ConstantsManager.getInstance().getConstants().close());
            model.getCommands().add(command);
        }
    }

    private IStorageModel prepareNfsStorageForEdit(StorageDomain storage)
    {
        final NfsStorageModel model = new NfsStorageModel();
        model.setRole(storage.getStorageDomainType());

        boolean isNfsPathEditable = isPathEditable(storage);

        model.getPath().setIsChangable(isNfsPathEditable);
        model.getOverride().setIsChangable(isNfsPathEditable);

        AsyncDataProvider.getStorageConnectionById(new AsyncQuery(null, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {

                StorageServerConnections connection = (StorageServerConnections) returnValue;
                model.getPath().setEntity(connection.getconnection());
                model.getRetransmissions().setEntity(connection.getNfsRetrans());
                model.getTimeout().setEntity(connection.getNfsTimeo());

                for (Object item : model.getVersion().getItems()) {
                    EntityModel itemModel = (EntityModel) item;
                    boolean noNfsVersion = itemModel.getEntity() == null && connection.getNfsVersion() == null;
                    boolean foundNfsVersion = itemModel.getEntity() != null &&
                            itemModel.getEntity().equals(connection.getNfsVersion());

                    if (noNfsVersion || foundNfsVersion) {
                        model.getVersion().setSelectedItem(item);
                        break;
                    }
                }

                // If any settings were overridden, reflect this in the override checkbox
                model.getOverride().setEntity(
                        connection.getNfsVersion() != null ||
                        connection.getNfsRetrans() != null ||
                        connection.getNfsTimeo() != null);

            }
        }), storage.getStorage(), true);

        return model;
    }

    private boolean isPathEditable(StorageDomain storage) {
        if (storage.getStorageType().isFileDomain() && !storage.getStorageType().equals(StorageType.GLUSTERFS)) {
            return ((storage.getStorageDomainType() == StorageDomainType.Data || storage.getStorageDomainType() == StorageDomainType.Master) && (storage.getStatus() == StorageDomainStatus.Maintenance || storage.getStorageDomainSharedStatus() ==  StorageDomainSharedStatus.Unattached));
        }
        return false;
    }

    private IStorageModel prepareLocalStorageForEdit(StorageDomain storage)
    {
        LocalStorageModel model = new LocalStorageModel();
        model.setRole(storage.getStorageDomainType());
        boolean isPathEditable = isPathEditable(storage);
        model.getPath().setIsChangable(isPathEditable);

        AsyncDataProvider.getStorageConnectionById(new AsyncQuery(model, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                LocalStorageModel localStorageModel = (LocalStorageModel) target;
                StorageServerConnections connection = (StorageServerConnections) returnValue;
                localStorageModel.getPath().setEntity(connection.getconnection());

            }
        }), storage.getStorage(), true);

        return model;
    }

    private IStorageModel preparePosixStorageForEdit(StorageDomain storage) {

        final PosixStorageModel model = new PosixStorageModel();
        model.setRole(storage.getStorageDomainType());

        boolean isPathEditable = isPathEditable(storage);
        model.getPath().setIsChangable(isPathEditable);
        model.getVfsType().setIsChangable(isPathEditable);
        model.getMountOptions().setIsChangable(isPathEditable);

        AsyncDataProvider.getStorageConnectionById(new AsyncQuery(null, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {

                StorageServerConnections connection = (StorageServerConnections) returnValue;
                model.getPath().setEntity(connection.getconnection());
                model.getVfsType().setEntity(connection.getVfsType());
                model.getMountOptions().setEntity(connection.getMountOptions());

            }
        }), storage.getStorage(), true);

        return model;
    }

    private IStorageModel prepareIscsiStorageForEdit(StorageDomain storage)
    {
        IscsiStorageModel model = new IscsiStorageModel();
        model.setRole(storage.getStorageDomainType());

        prepareSanStorageForEdit(model);

        return model;
    }

    private IStorageModel prepareFcpStorageForEdit(StorageDomain storage)
    {
        FcpStorageModel model = new FcpStorageModel();
        model.setRole(storage.getStorageDomainType());

        prepareSanStorageForEdit(model);

        return model;
    }

    private IStorageModel prepareGlusterStorageForEdit(StorageDomain storage) {

        final GlusterStorageModel model = new GlusterStorageModel();
        model.setRole(storage.getStorageDomainType());
        model.getPath().setIsChangable(true);
        model.getVfsType().setIsChangable(false);
        model.getMountOptions().setIsChangable(false);

        AsyncDataProvider.getStorageConnectionById(new AsyncQuery(null, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {

                StorageServerConnections connection = (StorageServerConnections) returnValue;
                model.getPath().setEntity(connection.getconnection());
                model.getVfsType().setEntity(connection.getVfsType());
                model.getMountOptions().setEntity(connection.getMountOptions());

            }
        }), storage.getStorage(), true);

        return model;
    }

    private void prepareSanStorageForEdit(final SanStorageModel model)
    {
        StorageModel storageModel = (StorageModel) getWindow();
        boolean isStorageEditable = storageModel.isStorageActive() || storageModel.isNewStorage();

        if (isStorageEditable) {
            storageModel.getHost().getSelectedItemChangedEvent().addListener(new IEventListener() {
                @Override
                public void eventRaised(Event ev, Object sender, EventArgs args) {
                    postPrepareSanStorageForEdit(model, true);
                }
            });
        }
        else {
            postPrepareSanStorageForEdit(model, false);
        }
    }

    private void postPrepareSanStorageForEdit(SanStorageModel model, boolean isStorageActive)
    {
        StorageModel storageModel = (StorageModel) getWindow();
        StorageDomain storage = (StorageDomain) getSelectedItem();
        model.setStorageDomain(storage);

        VDS host = (VDS) storageModel.getHost().getSelectedItem();
        Guid hostId = host != null && isStorageActive ? host.getId() : null;

        AsyncDataProvider.getLunsByVgId(new AsyncQuery(model, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                SanStorageModel sanStorageModel = (SanStorageModel) target;
                ArrayList<LUNs> lunList = (ArrayList<LUNs>) returnValue;
                sanStorageModel.applyData(lunList, true);
            }
        }, storageModel.getHash()), storage.getStorage(), hostId);
    }

    private void importDomain()
    {
        if (getWindow() != null)
        {
            return;
        }

        StorageModel model = new StorageModel(new ImportStorageModelBehavior());
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().importPreConfiguredDomainTitle());
        model.setHashName("import_pre-configured_domain"); //$NON-NLS-1$
        model.setSystemTreeSelectedItem(getSystemTreeSelectedItem());
        model.getName().setIsAvailable(false);
        model.getDescription().setIsAvailable(false);
        model.getComment().setIsAvailable(false);
        model.getFormat().setIsAvailable(false);

        ArrayList<IStorageModel> items = new ArrayList<IStorageModel>();
        NfsStorageModel tempVar = new NfsStorageModel();
        tempVar.setRole(StorageDomainType.ISO);
        items.add(tempVar);
        LocalStorageModel localIsoModel = new LocalStorageModel();
        localIsoModel.setRole(StorageDomainType.ISO);
        items.add(localIsoModel);
        PosixStorageModel posixIsoModel = new PosixStorageModel();
        posixIsoModel.setRole(StorageDomainType.ISO);
        items.add(posixIsoModel);
        NfsStorageModel tempVar2 = new NfsStorageModel();
        tempVar2.setRole(StorageDomainType.ImportExport);
        items.add(tempVar2);

        model.setItems(items);

        model.initialize();

        UICommand command;
        command = createOKCommand("OnImport"); //$NON-NLS-1$
        model.getCommands().add(command);

        command = createCancelCommand("Cancel"); //$NON-NLS-1$
        model.getCommands().add(command);
    }

    private void onImport()
    {
        StorageModel model = (StorageModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        if (!model.validate())
        {
            return;
        }

        model.startProgress(ConstantsManager.getInstance().getConstants().importingStorageDomainProgress());

        VDS host = (VDS) model.getHost().getSelectedItem();

        // Save changes.
        if (model.getSelectedItem() instanceof NfsStorageModel)
        {
            NfsStorageModel nfsModel = (NfsStorageModel) model.getSelectedItem();
            nfsModel.setMessage(null);

            Task.create(this,
                    new ArrayList<Object>(Arrays.asList(new Object[] { "ImportFile", //$NON-NLS-1$
                            host.getId(), nfsModel.getPath().getEntity(), nfsModel.getRole(), StorageType.NFS }))).Run();
        }
        else if (model.getSelectedItem() instanceof LocalStorageModel)
        {
            LocalStorageModel localModel = (LocalStorageModel) model.getSelectedItem();
            localModel.setMessage(null);

            Task.create(this,
                    new ArrayList<Object>(Arrays.asList(new Object[] { "ImportFile", //$NON-NLS-1$
                            host.getId(), localModel.getPath().getEntity(), localModel.getRole(), StorageType.LOCALFS }))).Run();
        }
        else if (model.getSelectedItem() instanceof PosixStorageModel)
        {
            PosixStorageModel posixModel = (PosixStorageModel) model.getSelectedItem();
            posixModel.setMessage(null);

            Task.create(this,
                    new ArrayList<Object>(Arrays.asList(new Object[] { "ImportFile", //$NON-NLS-1$
                            host.getId(), posixModel.getPath().getEntity(), posixModel.getRole(), StorageType.POSIXFS}))).Run();
        }
        else
        {
            Task.create(this,
                    new ArrayList<Object>(Arrays.asList(new Object[] { "ImportSan", //$NON-NLS-1$
                            host.getId() }))).Run();
        }
    }

    public void storageNameValidation()
    {
        StorageModel model = (StorageModel) getWindow();
        String name = (String) model.getName().getEntity();
        model.getName().setIsValid(true);

        AsyncDataProvider.isStorageDomainNameUnique(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {

                StorageListModel storageListModel = (StorageListModel) target;
                StorageModel storageModel = (StorageModel) storageListModel.getWindow();

                String name1 = (String) storageModel.getName().getEntity();
                String tempVar = storageModel.getOriginalName();
                String originalName = (tempVar != null) ? tempVar : ""; //$NON-NLS-1$
                boolean isNameUnique = (Boolean) returnValue;

                if (!isNameUnique && name1.compareToIgnoreCase(originalName) != 0) {
                    storageModel.getName()
                        .getInvalidityReasons()
                        .add(ConstantsManager.getInstance().getConstants().nameMustBeUniqueInvalidReason());
                    storageModel.getName().setIsValid(false);
                    storageListModel.postStorageNameValidation();
                } else {

                    AsyncDataProvider.getStorageDomainMaxNameLength(new AsyncQuery(storageListModel, new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object target1, Object returnValue1) {

                            StorageListModel storageListModel1 = (StorageListModel) target1;
                            StorageModel storageModel1 = (StorageModel) storageListModel1.getWindow();
                            int nameMaxLength = (Integer) returnValue1;
                            RegexValidation tempVar2 = new RegexValidation();
                            tempVar2.setExpression("^[A-Za-z0-9_-]{1," + nameMaxLength + "}$"); //$NON-NLS-1$ //$NON-NLS-2$
                            tempVar2.setMessage(ConstantsManager.getInstance().getMessages()
                                .nameCanContainOnlyMsg(nameMaxLength));
                            storageModel1.getName().validateEntity(new IValidation[] {
                                new NotEmptyValidation(), tempVar2});
                            storageListModel1.postStorageNameValidation();

                        }
                    }));
                }

            }
        }),
            name);
    }

    public void postStorageNameValidation()
    {
        if (getLastExecutedCommand().getName().equals("OnSave")) //$NON-NLS-1$
        {
            onSavePostNameValidation();
        }
    }

    private void cleanConnection(StorageServerConnections connection, Guid hostId) {
        // if create connection command was the one to fail and didn't create a connection
        // then the id of connection will be empty, and there's nothing to delete.
        if (connection.getid() != null && !connection.getid().equals("")) {  //$NON-NLS-1$
            Frontend.getInstance().runAction(VdcActionType.RemoveStorageServerConnection, new StorageServerConnectionParametersBase(connection, hostId),
                null, this);
        }

    }

    private void remove()
    {
        if (getWindow() != null)
        {
            return;
        }

        RemoveStorageModel model = new RemoveStorageModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().removeStoragesTitle());
        model.setHashName("remove_storage"); //$NON-NLS-1$
        model.getFormat().setIsAvailable(false);

        StorageDomain storage = (StorageDomain) getSelectedItem();
        boolean localFsOnly = storage.getStorageType() == StorageType.LOCALFS;

        AsyncDataProvider.getHostsForStorageOperation(new AsyncQuery(new Object[]{this, model}, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {

                Object[] array = (Object[]) target;
                StorageListModel storageListModel = (StorageListModel) array[0];
                RemoveStorageModel removeStorageModel = (RemoveStorageModel) array[1];
                StorageDomain storage = (StorageDomain) storageListModel.getSelectedItem();
                ArrayList<VDS> hosts = (ArrayList<VDS>) returnValue;
                removeStorageModel.getHostList().setItems(hosts);
                removeStorageModel.getHostList().setSelectedItem(Linq.firstOrDefault(hosts));
                removeStorageModel.getFormat()
                        .setIsAvailable(storage.getStorageDomainType() == StorageDomainType.ISO
                                || storage.getStorageDomainType() == StorageDomainType.ImportExport);

                if (hosts.isEmpty()) {
                    UICommand tempVar = createCancelCommand("Cancel"); //$NON-NLS-1$
                    tempVar.setIsDefault(true);
                    removeStorageModel.getCommands().add(tempVar);
                } else {

                    UICommand command;
                    command = createOKCommand("OnRemove"); //$NON-NLS-1$
                    removeStorageModel.getCommands().add(command);

                    command = createCancelCommand("Cancel"); //$NON-NLS-1$
                    removeStorageModel.getCommands().add(command);
                }

            }
        }), null, localFsOnly);
    }

    private void onRemove()
    {
        if (getSelectedItem() != null)
        {
            StorageDomain storage = (StorageDomain) getSelectedItem();
            RemoveStorageModel model = (RemoveStorageModel) getWindow();

            if (!model.validate())
            {
                return;
            }

            VDS host = (VDS) model.getHostList().getSelectedItem();

            RemoveStorageDomainParameters tempVar = new RemoveStorageDomainParameters(storage.getId());
            tempVar.setVdsId(host.getId());
            tempVar.setDoFormat((storage.getStorageDomainType() == StorageDomainType.Data || storage.getStorageDomainType() == StorageDomainType.Master) ? true
                : (Boolean) model.getFormat().getEntity());

            Frontend.getInstance().runAction(VdcActionType.RemoveStorageDomain, tempVar, null, this);
        }

        cancel();
    }

    private void destroy()
    {
        ConfirmationModel model = new ConfirmationModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().destroyStorageDomainTitle());
        model.setHashName("destroy_storage_domain"); //$NON-NLS-1$
        ArrayList<String> items = new ArrayList<String>();
        items.add(((StorageDomain) getSelectedItem()).getStorageName());
        model.setItems(items);

        model.getLatch().setIsAvailable(true);
        model.getLatch().setIsChangable(true);


        UICommand command;
        command = createOKCommand("OnDestroy"); //$NON-NLS-1$
        model.getCommands().add(command);

        command = createCancelCommand("Cancel"); //$NON-NLS-1$
        model.getCommands().add(command);
    }

    private void onDestroy()
    {
        ConfirmationModel model = (ConfirmationModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        if (!model.validate())
        {
            return;
        }

        StorageDomain storageDomain = (StorageDomain) getSelectedItem();

        model.startProgress(null);

        Frontend.getInstance().runMultipleAction(VdcActionType.ForceRemoveStorageDomain,
                new ArrayList<VdcActionParametersBase>(Arrays.asList(new VdcActionParametersBase[]{new StorageDomainParametersBase(storageDomain.getId())})),
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {

                        ConfirmationModel localModel = (ConfirmationModel) result.getState();
                        localModel.stopProgress();
                        cancel();

                    }
                },
                model);
    }

    private void onSave()
    {
        storageNameValidation();
    }

    private void onSavePostNameValidation()
    {
        StorageModel model = (StorageModel) getWindow();

        if (!model.validate())
        {
            return;
        }

        if (model.getSelectedItem() instanceof NfsStorageModel)
        {
            saveNfsStorage();
        }
        else if (model.getSelectedItem() instanceof LocalStorageModel)
        {
            saveLocalStorage();
        }
        else if (model.getSelectedItem() instanceof PosixStorageModel)
        {
            savePosixStorage();
        }
        else if (model.getSelectedItem() instanceof GlusterStorageModel)
        {
            saveGlusterStorage();
        }
        else
        {
            saveSanStorage();
        }
    }

    private void saveLocalStorage()
    {
        if (getWindow().getProgress() != null)
        {
            return;
        }

        getWindow().startProgress(null);

        Task.create(this, new ArrayList<Object>(Arrays.asList(new Object[] { "SaveLocal" }))).Run(); //$NON-NLS-1$
    }

    private void saveNfsStorage()
    {
        if (getWindow().getProgress() != null)
        {
            return;
        }

        getWindow().startProgress(null);

        Task.create(this, new ArrayList<Object>(Arrays.asList(new Object[] { "SaveNfs" }))).Run(); //$NON-NLS-1$
    }

    private void savePosixStorage() {

        if (getWindow().getProgress() != null) {
            return;
        }

        getWindow().startProgress(null);

        Task.create(this, new ArrayList<Object>(Arrays.asList(new Object[] {"SavePosix"}))).Run(); //$NON-NLS-1$
    }

    private void saveGlusterStorage() {

        if (getWindow().getProgress() != null) {
            return;
        }

        getWindow().startProgress(null);

        Task.create(this, new ArrayList<Object>(Arrays.asList(new Object[] {"SaveGluster"}))).Run(); //$NON-NLS-1$
    }

    private void saveSanStorage()
    {
        StorageModel storageModel = (StorageModel) getWindow();
        SanStorageModel sanStorageModel = (SanStorageModel) storageModel.getSelectedItem();
        ArrayList<String> usedLunsMessages = sanStorageModel.getUsedLunsMessages();

        if (usedLunsMessages.isEmpty()) {
            onSaveSanStorage();
        }
        else {
            forceCreationWarning(usedLunsMessages);
        }
    }

    private void onSaveSanStorage()
    {
        ConfirmationModel confirmationModel = (ConfirmationModel) getConfirmWindow();

        if (confirmationModel != null && !confirmationModel.validate())
        {
            return;
        }

        cancelConfirm();
        getWindow().startProgress(null);

        Task.create(this, new ArrayList<Object>(Arrays.asList(new Object[] { "SaveSan" }))).Run(); //$NON-NLS-1$
    }

    private void forceCreationWarning(ArrayList<String> usedLunsMessages) {
        StorageModel storageModel = (StorageModel) getWindow();
        SanStorageModel sanStorageModel = (SanStorageModel) storageModel.getSelectedItem();
        sanStorageModel.setForce(true);

        ConfirmationModel model = new ConfirmationModel();
        setConfirmWindow(model);

        model.setTitle(ConstantsManager.getInstance().getConstants().forceStorageDomainCreation());
        model.setMessage(ConstantsManager.getInstance().getConstants().lunsAlreadyInUse());
        model.setHashName("force_storage_domain_creation"); //$NON-NLS-1$
        model.setItems(usedLunsMessages);

        UICommand command = createOKCommand("OnSaveSanStorage"); //$NON-NLS-1$
        model.getCommands().add(command);

        command = createCancelCommand("CancelConfirm"); //$NON-NLS-1$
        model.getCommands().add(command);
    }

    private void cancelConfirm()
    {
        setConfirmWindow(null);
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
    protected void itemsCollectionChanged(Object sender, NotifyCollectionChangedEventArgs e)
    {
        super.itemsCollectionChanged(sender, e);

        // Try to select an item corresponding to the system tree selection.
        if (getSystemTreeSelectedItem() != null && getSystemTreeSelectedItem().getType() == SystemTreeItemType.Storage)
        {
            StorageDomain storage = (StorageDomain) getSystemTreeSelectedItem().getEntity();

            setSelectedItem(Linq.firstOrDefault(Linq.<StorageDomain> cast(getItems()),
                    new Linq.StoragePredicate(storage.getId())));
        }
    }

    @Override
    protected void updateDetailsAvailability()
    {
        if (getSelectedItem() != null)
        {
            StorageDomain storage = (StorageDomain) getSelectedItem();
            boolean isBackupStorage = storage.getStorageDomainType() == StorageDomainType.ImportExport;
            boolean isDataStorage =
                    storage.getStorageDomainType() == StorageDomainType.Data
                            || storage.getStorageDomainType() == StorageDomainType.Master;
            boolean isImageStorage =
                     storage.getStorageDomainType() == StorageDomainType.Image ||
                     storage.getStorageDomainType() == StorageDomainType.ISO;
            boolean isDataCenterAvailable = storage.getStorageType() != StorageType.GLANCE;
            boolean isGeneralAvailable = storage.getStorageType() != StorageType.GLANCE;

            generalModel.setIsAvailable(isGeneralAvailable);
            dcListModel.setIsAvailable(isDataCenterAvailable);

            vmBackupModel.setIsAvailable(isBackupStorage);
            templateBackupModel.setIsAvailable(isBackupStorage);

            vmListModel.setIsAvailable(isDataStorage);
            templateListModel.setIsAvailable(isDataStorage);
            diskListModel.setIsAvailable(isDataStorage);

            isoListModel.setIsAvailable(isImageStorage);
        }
    }

    @Override
    protected void selectedItemsChanged()
    {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemPropertyChanged(Object sender, PropertyChangedEventArgs e)
    {
        super.selectedItemPropertyChanged(sender, e);

        if (e.PropertyName.equals("storage_domain_shared_status")) //$NON-NLS-1$
        {
            updateActionAvailability();
        }
    }

    private void updateActionAvailability()
    {
        ArrayList<StorageDomain> items =
                getSelectedItems() != null ? Linq.<StorageDomain> cast(getSelectedItems())
                        : new ArrayList<StorageDomain>();

        StorageDomain item = (StorageDomain) getSelectedItem();

        getNewDomainCommand().setIsAvailable(true);

        getEditCommand().setIsExecutionAllowed(items.size() == 1 && isEditAvailable(item));

        getRemoveCommand().setIsExecutionAllowed(items.size() == 1
                && items.get(0).getStorageType() != StorageType.GLANCE
                && Linq.findAllStorageDomainsBySharedStatus(items, StorageDomainSharedStatus.Unattached).size() == items.size());

        getDestroyCommand().setIsExecutionAllowed(item != null && items.size() == 1
                && items.get(0).getStorageType() != StorageType.GLANCE
                && item.getStatus() != StorageDomainStatus.Active);

        // System tree dependent actions.
        boolean isAvailable =
                !(getSystemTreeSelectedItem() != null && getSystemTreeSelectedItem().getType() == SystemTreeItemType.Storage);

        getNewDomainCommand().setIsAvailable(isAvailable);
        getRemoveCommand().setIsAvailable(isAvailable);
        getDestroyCommand().setIsAvailable(isAvailable);
    }

    private boolean isEditAvailable(StorageDomain storageDomain) {
        if (storageDomain == null) {
            return false;
        }

        boolean isEditAvailable;
        boolean isActive = storageDomain.getStorageDomainSharedStatus() == StorageDomainSharedStatus.Active
                || storageDomain.getStorageDomainSharedStatus() == StorageDomainSharedStatus.Mixed;
        boolean isInMaintenance = (storageDomain.getStatus() == StorageDomainStatus.Maintenance);
        boolean isUnattached = (storageDomain.getStorageDomainSharedStatus() == StorageDomainSharedStatus.Unattached);
        boolean isDataDomain = (storageDomain.getStorageDomainType() == StorageDomainType.Data) || (storageDomain.getStorageDomainType() == StorageDomainType.Master);
        boolean isBlockStorage = storageDomain.getStorageType().isBlockDomain();

        isEditAvailable = isActive || isBlockStorage || ((isInMaintenance || isUnattached) && isDataDomain);
        return isEditAvailable;
    }

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);

        if (command == getNewDomainCommand())
        {
            newDomain();
        }
        else if (command == getImportDomainCommand())
        {
            importDomain();
        }
        else if (command == getEditCommand())
        {
            edit();
        }
        else if (command == getRemoveCommand())
        {
            remove();
        }
        else if (command == getDestroyCommand())
        {
            destroy();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnSave")) //$NON-NLS-1$
        {
            onSave();
        }
        else if (StringHelper.stringsEqual(command.getName(), "Cancel")) //$NON-NLS-1$
        {
            cancel();
        }
        else if (StringHelper.stringsEqual(command.getName(), "CancelConfirm")) //$NON-NLS-1$
        {
            cancelConfirm();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnImport")) //$NON-NLS-1$
        {
            onImport();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnRemove")) //$NON-NLS-1$
        {
            onRemove();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnDestroy")) //$NON-NLS-1$
        {
            onDestroy();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnSaveSanStorage")) //$NON-NLS-1$
        {
            onSaveSanStorage();
        }
    }

    private void savePosixStorage(TaskContext context) {

        this.context = context;

        StorageDomain selectedItem = (StorageDomain) getSelectedItem();
        StorageModel model = (StorageModel) getWindow();
        boolean isNew = model.getStorage() == null;
        storageModel = model.getSelectedItem();
        PosixStorageModel posixModel = (PosixStorageModel) storageModel;
        path = (String) posixModel.getPath().getEntity();

        storageDomain = isNew ? new StorageDomainStatic() : (StorageDomainStatic) Cloner.clone(selectedItem.getStorageStaticData());
        storageDomain.setStorageType(isNew ? storageModel.getType() : storageDomain.getStorageType());
        storageDomain.setStorageDomainType(isNew ? storageModel.getRole() : storageDomain.getStorageDomainType());
        storageDomain.setStorageName((String) model.getName().getEntity());
        storageDomain.setDescription((String) model.getDescription().getEntity());
        storageDomain.setComment((String) model.getComment().getEntity());
        storageDomain.setStorageFormat((StorageFormatType) model.getFormat().getSelectedItem());

        if (isNew) {
            AsyncDataProvider.getStorageDomainsByConnection(new AsyncQuery(this, new INewAsyncCallback() {
                @Override
                public void onSuccess(Object target, Object returnValue) {

                    StorageListModel storageListModel = (StorageListModel) target;
                    ArrayList<StorageDomain> storages = (ArrayList<StorageDomain>) returnValue;

                    if (storages != null && storages.size() > 0) {
                        handleDomainAlreadyExists(storageListModel, storages);
                    } else {
                        storageListModel.saveNewPosixStorage();
                    }
                }
            }), null, path);
        } else {
            StorageDomain storageDomain = (StorageDomain) getSelectedItem();
            if (isPathEditable(storageDomain)) {
                updatePath();
            }
            else {
                updateStorageDomain();
            }
        }
    }

    private void updateStorageDomain() {
        Frontend.getInstance().runAction(VdcActionType.UpdateStorageDomain, new StorageDomainManagementParameter(this.storageDomain), new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {
                StorageListModel storageListModel = (StorageListModel) result.getState();
                storageListModel.onFinish(storageListModel.context, true, storageListModel.storageModel);
            }
        }, this);
    }

    public void saveNewPosixStorage() {

        StorageModel model = (StorageModel) getWindow();
        PosixStorageModel posixModel = (PosixStorageModel) model.getSelectedItem();
        VDS host = (VDS) model.getHost().getSelectedItem();
        hostId = host.getId();

        // Create storage connection.
        StorageServerConnections connection = new StorageServerConnections();
        connection.setconnection(path);
        connection.setstorage_type(posixModel.getType());
        connection.setVfsType((String) posixModel.getVfsType().getEntity());
        connection.setMountOptions((String) posixModel.getMountOptions().getEntity());
        this.connection = connection;

        ArrayList<VdcActionType> actionTypes = new ArrayList<VdcActionType>();
        ArrayList<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();

        actionTypes.add(VdcActionType.AddStorageServerConnection);
        actionTypes.add(VdcActionType.AddPosixFsStorageDomain);

        parameters.add(new StorageServerConnectionParametersBase(this.connection, host.getId()));
        StorageDomainManagementParameter parameter = new StorageDomainManagementParameter(storageDomain);
        parameter.setVdsId(host.getId());
        parameters.add(parameter);

        IFrontendActionAsyncCallback callback1 = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();
                VdcReturnValueBase vdcReturnValueBase = result.getReturnValue();
                storageListModel.storageDomain.setStorage((String) vdcReturnValueBase.getActionReturnValue());
                storageListModel.connection.setid((String)vdcReturnValueBase.getActionReturnValue());

            }
        };

        IFrontendActionAsyncCallback callback2 = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();
                VdcReturnValueBase vdcReturnValueBase = result.getReturnValue();
                storageListModel.storageId = (Guid) vdcReturnValueBase.getActionReturnValue();

                // Attach storage to data center as necessary.
                StorageModel storageModel = (StorageModel) storageListModel.getWindow();
                StoragePool dataCenter = (StoragePool) storageModel.getDataCenter().getSelectedItem();
                if (!dataCenter.getId().equals(StorageModel.UnassignedDataCenterId)) {
                    storageListModel.attachStorageToDataCenter(storageListModel.storageId, dataCenter.getId());
                }

                storageListModel.onFinish(storageListModel.context, true, storageListModel.storageModel);
            }
        };

        IFrontendActionAsyncCallback failureCallback = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();
                storageListModel.cleanConnection(storageListModel.connection, storageListModel.hostId);
                storageListModel.onFinish(storageListModel.context, false, storageListModel.storageModel);
            }
        };

        Frontend.getInstance().runMultipleActions(actionTypes,
            parameters,
            new ArrayList<IFrontendActionAsyncCallback>(Arrays.asList(new IFrontendActionAsyncCallback[] {
                        callback1, callback2 })),
            failureCallback,
            this);
    }

    private void saveGlusterStorage(TaskContext context) {

        this.context = context;

        StorageDomain selectedItem = (StorageDomain) getSelectedItem();
        StorageModel model = (StorageModel) getWindow();
        boolean isNew = model.getStorage() == null;
        storageModel = model.getSelectedItem();
        GlusterStorageModel glusterModel = (GlusterStorageModel) storageModel;
        path = (String) glusterModel.getPath().getEntity();

        storageDomain = isNew ? new StorageDomainStatic() : (StorageDomainStatic) Cloner.clone(selectedItem.getStorageStaticData());
        storageDomain.setStorageType(isNew ? storageModel.getType() : storageDomain.getStorageType());
        storageDomain.setStorageDomainType(isNew ? storageModel.getRole() : storageDomain.getStorageDomainType());
        storageDomain.setStorageName((String) model.getName().getEntity());
        storageDomain.setStorageFormat((StorageFormatType) model.getFormat().getSelectedItem());

        if (isNew) {
            AsyncDataProvider.getStorageDomainsByConnection(new AsyncQuery(this, new INewAsyncCallback() {
                @Override
                public void onSuccess(Object target, Object returnValue) {

                    StorageListModel storageListModel = (StorageListModel) target;
                    ArrayList<StorageDomain> storages = (ArrayList<StorageDomain>) returnValue;

                    if (storages != null && storages.size() > 0) {
                        handleDomainAlreadyExists(storageListModel, storages);
                    } else {
                        storageListModel.saveNewGlusterStorage();
                    }
                }
            }), null, path);
        } else {

            updateStorageDomain();
        }
    }

    public void saveNewGlusterStorage() {

        StorageModel model = (StorageModel) getWindow();
        GlusterStorageModel glusterModel = (GlusterStorageModel) model.getSelectedItem();
        VDS host = (VDS) model.getHost().getSelectedItem();
        hostId = host.getId();

        // Create storage connection.
        StorageServerConnections connection = new StorageServerConnections();
        connection.setconnection(path);
        connection.setstorage_type(glusterModel.getType());
        connection.setVfsType((String) glusterModel.getVfsType().getEntity());
        connection.setMountOptions((String) glusterModel.getMountOptions().getEntity());
        this.connection = connection;

        ArrayList<VdcActionType> actionTypes = new ArrayList<VdcActionType>();
        ArrayList<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();

        actionTypes.add(VdcActionType.AddStorageServerConnection);
        actionTypes.add(VdcActionType.AddGlusterFsStorageDomain);

        parameters.add(new StorageServerConnectionParametersBase(this.connection, host.getId()));
        StorageDomainManagementParameter parameter = new StorageDomainManagementParameter(storageDomain);
        parameter.setVdsId(host.getId());
        parameters.add(parameter);

        IFrontendActionAsyncCallback callback1 = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();
                VdcReturnValueBase vdcReturnValueBase = result.getReturnValue();
                storageListModel.storageDomain.setStorage((String) vdcReturnValueBase.getActionReturnValue());
                storageListModel.connection.setid((String) vdcReturnValueBase.getActionReturnValue());
            }
        };

        IFrontendActionAsyncCallback callback2 = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();
                VdcReturnValueBase vdcReturnValueBase = result.getReturnValue();
                storageListModel.storageId = (Guid) vdcReturnValueBase.getActionReturnValue();

                // Attach storage to data center as necessary.
                StorageModel storageModel = (StorageModel) storageListModel.getWindow();
                StoragePool dataCenter = (StoragePool) storageModel.getDataCenter().getSelectedItem();
                if (!dataCenter.getId().equals(StorageModel.UnassignedDataCenterId)) {
                    storageListModel.attachStorageToDataCenter(storageListModel.storageId, dataCenter.getId());
                }

                storageListModel.onFinish(storageListModel.context, true, storageListModel.storageModel);
            }
        };

        IFrontendActionAsyncCallback failureCallback = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();
                storageListModel.cleanConnection(storageListModel.connection, storageListModel.hostId);
                storageListModel.onFinish(storageListModel.context, false, storageListModel.storageModel);
            }
        };

        Frontend.getInstance().runMultipleActions(actionTypes,
            parameters,
            new ArrayList<IFrontendActionAsyncCallback>(Arrays.asList(new IFrontendActionAsyncCallback[] {
                        callback1, callback2 })),
            failureCallback,
            this);
    }


    private void saveNfsStorage(TaskContext context)
    {
        this.context = context;

        StorageDomain selectedItem = (StorageDomain) getSelectedItem();
        StorageModel model = (StorageModel) getWindow();
        boolean isNew = model.getStorage() == null;
        storageModel = model.getSelectedItem();
        NfsStorageModel nfsModel = (NfsStorageModel) storageModel;
        path = (String) nfsModel.getPath().getEntity();

        storageDomain =
                isNew ? new StorageDomainStatic()
                        : (StorageDomainStatic) Cloner.clone(selectedItem.getStorageStaticData());

        storageDomain.setStorageType(isNew ? storageModel.getType() : storageDomain.getStorageType());

        storageDomain.setStorageDomainType(isNew ? storageModel.getRole() : storageDomain.getStorageDomainType());

        storageDomain.setStorageName((String) model.getName().getEntity());
        storageDomain.setDescription((String) model.getDescription().getEntity());
        storageDomain.setComment((String) model.getComment().getEntity());
        storageDomain.setStorageFormat((StorageFormatType) model.getFormat().getSelectedItem());

        if (isNew)
        {
            AsyncDataProvider.getStorageDomainsByConnection(new AsyncQuery(this, new INewAsyncCallback() {
                @Override
                public void onSuccess(Object target, Object returnValue) {

                    StorageListModel storageListModel = (StorageListModel) target;
                    ArrayList<StorageDomain> storages = (ArrayList<StorageDomain>) returnValue;
                    if (storages != null && storages.size() > 0) {
                        handleDomainAlreadyExists(storageListModel, storages);
                    } else {
                        storageListModel.saveNewNfsStorage();
                    }
                }
            }), null, path);
        }
        else
        {
            StorageDomain storageDomain = (StorageDomain) getSelectedItem();
            if (isPathEditable(storageDomain)) {
                updatePath();
            }
            else {
                updateStorageDomain();
            }
        }
    }


    private void updatePath() {
        StorageModel model = (StorageModel) getWindow();
        VDS host = (VDS) model.getHost().getSelectedItem();

        Guid hostId = Guid.Empty;
        if (host != null) {
            hostId = host.getId();
        }
        IStorageModel storageModel = model.getSelectedItem();
        connection = new StorageServerConnections();
        connection.setid(storageDomain.getStorage());
        connection.setconnection(path);
        connection.setstorage_type(storageModel.getType());

        if (storageModel.getType().equals(StorageType.NFS)) {
            updateNFSProperties(storageModel);
        }
        else if (storageModel.getType().equals(StorageType.POSIXFS)) {
            updatePosixProperties(storageModel);
        }

        StorageServerConnectionParametersBase parameters =
                new StorageServerConnectionParametersBase(connection, hostId);
        Frontend.getInstance().runAction(VdcActionType.UpdateStorageServerConnection, parameters,
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {
                        StorageListModel storageListModel = (StorageListModel) result.getState();
                        storageListModel.onFinish(storageListModel.context, true, storageListModel.storageModel);

                    }
                }, this);
    }

    private void updateNFSProperties(IStorageModel storageModel) {
        NfsStorageModel nfsModel = (NfsStorageModel) storageModel;
        if ((Boolean) nfsModel.getOverride().getEntity()) {
            connection.setNfsVersion((NfsVersion) ((EntityModel) nfsModel.getVersion().getSelectedItem()).getEntity());
            connection.setNfsRetrans(nfsModel.getRetransmissions().asConvertible().nullableShort());
            connection.setNfsTimeo(nfsModel.getTimeout().asConvertible().nullableShort());
        }

    }

    private void updatePosixProperties(IStorageModel storageModel) {
        PosixStorageModel posixModel = (PosixStorageModel) storageModel;
        connection.setVfsType(posixModel.getVfsType().getEntity().toString());
        if (posixModel.getMountOptions().getEntity() != null) {
            connection.setMountOptions(posixModel.getMountOptions().getEntity().toString());
        }

    }

    public void saveNewNfsStorage()
    {
        StorageModel model = (StorageModel) getWindow();
        NfsStorageModel nfsModel = (NfsStorageModel) model.getSelectedItem();
        VDS host = (VDS) model.getHost().getSelectedItem();
        hostId = host.getId();

        // Create storage connection.
        StorageServerConnections tempVar = new StorageServerConnections();
        tempVar.setconnection(path);
        tempVar.setstorage_type(nfsModel.getType());
        if ((Boolean) nfsModel.getOverride().getEntity()) {
            tempVar.setNfsVersion((NfsVersion) ((EntityModel) nfsModel.getVersion().getSelectedItem()).getEntity());
            tempVar.setNfsRetrans(nfsModel.getRetransmissions().asConvertible().nullableShort());
            tempVar.setNfsTimeo(nfsModel.getTimeout().asConvertible().nullableShort());
        }
        connection = tempVar;

        ArrayList<VdcActionType> actionTypes = new ArrayList<VdcActionType>();
        ArrayList<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();

        actionTypes.add(VdcActionType.AddStorageServerConnection);
        actionTypes.add(VdcActionType.AddNFSStorageDomain);
        actionTypes.add(VdcActionType.DisconnectStorageServerConnection);

        parameters.add(new StorageServerConnectionParametersBase(connection, host.getId()));
        StorageDomainManagementParameter tempVar2 = new StorageDomainManagementParameter(storageDomain);
        tempVar2.setVdsId(host.getId());
        parameters.add(tempVar2);
        parameters.add(new StorageServerConnectionParametersBase(connection, host.getId()));

        IFrontendActionAsyncCallback callback1 = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();
                VdcReturnValueBase vdcReturnValueBase = result.getReturnValue();
                storageListModel.storageDomain.setStorage((String) vdcReturnValueBase.getActionReturnValue());
                storageListModel.connection.setid((String)vdcReturnValueBase.getActionReturnValue());

            }
        };
        IFrontendActionAsyncCallback callback2 = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();
                VdcReturnValueBase vdcReturnValueBase = result.getReturnValue();
                storageListModel.storageId = (Guid) vdcReturnValueBase.getActionReturnValue();

            }
        };
        IFrontendActionAsyncCallback callback3 = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();
                StorageModel storageModel = (StorageModel) storageListModel.getWindow();

                // Attach storage to data center as necessary.
                StoragePool dataCenter = (StoragePool) storageModel.getDataCenter().getSelectedItem();
                if (!dataCenter.getId().equals(StorageModel.UnassignedDataCenterId))
                {
                    storageListModel.attachStorageToDataCenter(storageListModel.storageId, dataCenter.getId());
                }

                storageListModel.onFinish(storageListModel.context, true, storageListModel.storageModel);

            }
        };
        IFrontendActionAsyncCallback failureCallback = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();
                storageListModel.cleanConnection(storageListModel.connection, storageListModel.hostId);
                storageListModel.onFinish(storageListModel.context, false, storageListModel.storageModel);

            }
        };
        Frontend.getInstance().runMultipleActions(actionTypes,
                parameters,
                new ArrayList<IFrontendActionAsyncCallback>(Arrays.asList(new IFrontendActionAsyncCallback[] {
                        callback1, callback2, callback3 })),
                failureCallback,
                this);
    }

    public void saveNewSanStorage()
    {
        StorageModel model = (StorageModel) getWindow();
        SanStorageModel sanModel = (SanStorageModel) model.getSelectedItem();
        VDS host = (VDS) model.getHost().getSelectedItem();
        boolean force = sanModel.isForce();

        ArrayList<String> lunIds = new ArrayList<String>();
        for (LunModel lun : sanModel.getAddedLuns())
        {
            lunIds.add(lun.getLunId());
        }

        AddSANStorageDomainParameters params = new AddSANStorageDomainParameters(storageDomain);
        params.setVdsId(host.getId());
        params.setLunIds(lunIds);
        params.setForce(force);
        Frontend.getInstance().runAction(VdcActionType.AddSANStorageDomain, params,
            new IFrontendActionAsyncCallback() {
                @Override
                public void executed(FrontendActionAsyncResult result) {
                        StorageListModel storageListModel = (StorageListModel) result.getState();
                        StorageModel storageModel = (StorageModel) storageListModel.getWindow();
                        storageListModel.storageModel = storageModel.getSelectedItem();
                        if (!result.getReturnValue().getSucceeded()) {
                            storageListModel.onFinish(storageListModel.context, false, storageListModel.storageModel);
                            return;
                        }

                        StoragePool dataCenter = (StoragePool) storageModel.getDataCenter().getSelectedItem();
                        if (!dataCenter.getId().equals(StorageModel.UnassignedDataCenterId)) {
                            VdcReturnValueBase returnValue = result.getReturnValue();
                            Guid storageId = (Guid) returnValue.getActionReturnValue();
                            storageListModel.attachStorageToDataCenter(storageId, dataCenter.getId());
                        }

                    storageListModel.onFinish(storageListModel.context, true, storageListModel.storageModel);
                }
            }, this);
    }

    private void saveLocalStorage(TaskContext context)
    {
        this.context = context;

        StorageDomain selectedItem = (StorageDomain) getSelectedItem();
        StorageModel model = (StorageModel) getWindow();
        VDS host = (VDS) model.getHost().getSelectedItem();
        boolean isNew = model.getStorage() == null;
        storageModel = model.getSelectedItem();
        LocalStorageModel localModel = (LocalStorageModel) storageModel;
        path = (String) localModel.getPath().getEntity();

        storageDomain =
                isNew ? new StorageDomainStatic()
                        : (StorageDomainStatic) Cloner.clone(selectedItem.getStorageStaticData());

        storageDomain.setStorageType(isNew ? storageModel.getType() : storageDomain.getStorageType());

        storageDomain.setStorageDomainType(isNew ? storageModel.getRole() : storageDomain.getStorageDomainType());

        storageDomain.setStorageName((String) model.getName().getEntity());
        storageDomain.setDescription((String) model.getDescription().getEntity());
        storageDomain.setComment((String) model.getComment().getEntity());

        if (isNew)
        {
            AsyncDataProvider.getStorageDomainsByConnection(new AsyncQuery(this, new INewAsyncCallback() {
                @Override
                public void onSuccess(Object target, Object returnValue) {

                    StorageListModel storageListModel = (StorageListModel) target;
                    ArrayList<StorageDomain> storages = (ArrayList<StorageDomain>) returnValue;
                    if (storages != null && storages.size() > 0) {
                        handleDomainAlreadyExists(storageListModel, storages);
                    } else {
                        storageListModel.saveNewLocalStorage();
                    }

                }
            }), host.getStoragePoolId(), path);
        }
        else
        {
            StorageDomain storageDomain = (StorageDomain) getSelectedItem();
            if (isPathEditable(storageDomain)) {
                updatePath();
            }
            else {
                updateStorageDomain();
            }
        }
    }

    private void handleDomainAlreadyExists(StorageListModel storageListModel, ArrayList<StorageDomain> storages) {
        String storageName = storages.get(0).getStorageName();

        onFinish(storageListModel.context,
            false,
            storageListModel.storageModel,
            ConstantsManager.getInstance().getMessages().createFailedDomainAlreadyExistStorageMsg(storageName));
    }

    public void saveNewLocalStorage()
    {
        StorageModel model = (StorageModel) getWindow();
        LocalStorageModel localModel = (LocalStorageModel) model.getSelectedItem();
        VDS host = (VDS) model.getHost().getSelectedItem();
        hostId = host.getId();

        // Create storage connection.
        StorageServerConnections tempVar = new StorageServerConnections();
        tempVar.setconnection(path);
        tempVar.setstorage_type(localModel.getType());
        connection = tempVar;

        ArrayList<VdcActionType> actionTypes = new ArrayList<VdcActionType>();
        ArrayList<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();

        actionTypes.add(VdcActionType.AddStorageServerConnection);
        actionTypes.add(VdcActionType.AddLocalStorageDomain);

        parameters.add(new StorageServerConnectionParametersBase(connection, host.getId()));
        StorageDomainManagementParameter tempVar2 = new StorageDomainManagementParameter(storageDomain);
        tempVar2.setVdsId(host.getId());
        parameters.add(tempVar2);

        IFrontendActionAsyncCallback callback1 = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();
                storageListModel.removeConnection = true;

                VdcReturnValueBase vdcReturnValueBase = result.getReturnValue();
                storageListModel.storageDomain.setStorage((String) vdcReturnValueBase.getActionReturnValue());
                storageListModel.connection.setid((String)vdcReturnValueBase.getActionReturnValue());

            }
        };
        IFrontendActionAsyncCallback callback2 = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();
                storageListModel.removeConnection = false;

                storageListModel.onFinish(storageListModel.context, true, storageListModel.storageModel);

            }
        };
        IFrontendActionAsyncCallback failureCallback = new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                StorageListModel storageListModel = (StorageListModel) result.getState();

                if (storageListModel.removeConnection)
                {
                    storageListModel.cleanConnection(storageListModel.connection, storageListModel.hostId);
                    storageListModel.removeConnection = false;
                }

                storageListModel.onFinish(storageListModel.context, false, storageListModel.storageModel);

            }
        };
        Frontend.getInstance().runMultipleActions(actionTypes,
                parameters,
                new ArrayList<IFrontendActionAsyncCallback>(Arrays.asList(new IFrontendActionAsyncCallback[] {
                        callback1, callback2 })),
                failureCallback,
                this);
    }

    public void onFinish(TaskContext context, boolean isSucceeded, IStorageModel model)
    {
        onFinish(context, isSucceeded, model, null);
    }

    public void onFinish(TaskContext context, boolean isSucceeded, IStorageModel model, String message)
    {
        context.InvokeUIThread(this,
                new ArrayList<Object>(Arrays.asList(new Object[] { "Finish", isSucceeded, model, //$NON-NLS-1$
                        message })));
    }

    private void saveSanStorage(TaskContext context)
    {
        this.context = context;

        StorageModel model = (StorageModel) getWindow();
        SanStorageModel sanModel = (SanStorageModel) model.getSelectedItem();
        StorageDomain storage = (StorageDomain) getSelectedItem();

        boolean isNew = model.getStorage() == null;

        storageDomain =
                isNew ? new StorageDomainStatic()
                        : (StorageDomainStatic) Cloner.clone(storage.getStorageStaticData());

        storageDomain.setStorageType(isNew ? sanModel.getType() : storageDomain.getStorageType());

        storageDomain.setStorageDomainType(isNew ? sanModel.getRole() : storageDomain.getStorageDomainType());

        storageDomain.setStorageFormat(isNew ? (StorageFormatType) sanModel.getContainer()
                .getFormat()
                .getSelectedItem() : storageDomain.getStorageFormat());

        storageDomain.setStorageName((String) model.getName().getEntity());
        storageDomain.setDescription((String) model.getDescription().getEntity());
        storageDomain.setComment((String) model.getComment().getEntity());

        if (isNew)
        {
            saveNewSanStorage();
        }
        else
        {
            Frontend.getInstance().runAction(VdcActionType.UpdateStorageDomain, new StorageDomainManagementParameter(storageDomain), new IFrontendActionAsyncCallback() {
                @Override
                public void executed(FrontendActionAsyncResult result) {

                    StorageListModel storageListModel = (StorageListModel) result.getState();
                    StorageModel storageModel = (StorageModel) getWindow();
                    SanStorageModel sanStorageModel = (SanStorageModel) storageModel.getSelectedItem();
                    boolean force = sanStorageModel.isForce();
                    StorageDomain storageDomain1 = (StorageDomain) storageListModel.getSelectedItem();
                    ArrayList<String> lunIds = new ArrayList<String>();

                    for (LunModel lun : sanStorageModel.getAddedLuns()) {
                        lunIds.add(lun.getLunId());
                    }

                    if (lunIds.size() > 0) {
                        Frontend.getInstance().runAction(VdcActionType.ExtendSANStorageDomain,
                            new ExtendSANStorageDomainParameters(storageDomain1.getId(), lunIds, force),
                            null, this);
                    }
                    storageListModel.onFinish(storageListModel.context, true, storageListModel.storageModel);
                }
            }, this);
        }
    }

    private void attachStorageToDataCenter(Guid storageId, Guid dataCenterId)
    {
        Frontend.getInstance().runAction(VdcActionType.AttachStorageDomainToPool, new StorageDomainPoolParametersBase(storageId,
            dataCenterId), null, this);
    }

    private void importFileStorage(TaskContext context)
    {
        this.context = context;

        ArrayList<Object> data = (ArrayList<Object>) context.getState();
        StorageModel model = (StorageModel) getWindow();

        storageModel = model.getSelectedItem();
        hostId = (Guid) data.get(1);
        path = (String) data.get(2);
        domainType = (StorageDomainType) data.get(3);
        storageType = (StorageType) data.get(4);

        importFileStorageInit();
    }

    public void importFileStorageInit()
    {
        if (fileConnection != null)
        {
            // Clean nfs connection
            Frontend.getInstance().runAction(VdcActionType.DisconnectStorageServerConnection,
                new StorageServerConnectionParametersBase(fileConnection, hostId),
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {

                        StorageListModel storageListModel = (StorageListModel) result.getState();
                        VdcReturnValueBase returnVal = result.getReturnValue();
                        boolean success = returnVal != null && returnVal.getSucceeded();
                        if (success) {
                            storageListModel.fileConnection = null;
                        }
                        storageListModel.importFileStoragePostInit();

                    }
                },
                this);
        }
        else
        {
            importFileStoragePostInit();
        }
    }

    public void importFileStoragePostInit()
    {
        // Check storage domain existence
        AsyncDataProvider.getStorageDomainsByConnection(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {

                StorageListModel storageListModel = (StorageListModel) target;
                ArrayList<StorageDomain> storages = (ArrayList<StorageDomain>) returnValue;

                if (storages != null && storages.size() > 0) {

                    String storageName = storages.get(0).getStorageName();
                    onFinish(storageListModel.context,
                        false,
                        storageListModel.storageModel,
                        ConstantsManager.getInstance().getMessages().importFailedDomainAlreadyExistStorageMsg(storageName));
                } else {
                    StorageServerConnections tempVar = new StorageServerConnections();
                    storageModel = storageListModel.storageModel;
                    tempVar.setconnection(storageListModel.path);
                    tempVar.setstorage_type(storageListModel.storageType);
                    if (storageModel instanceof NfsStorageModel) {
                        NfsStorageModel nfsModel = (NfsStorageModel) storageModel;
                        if ((Boolean) nfsModel.getOverride().getEntity()) {
                            tempVar.setNfsVersion((NfsVersion) ((EntityModel) nfsModel.getVersion().getSelectedItem()).getEntity());
                            tempVar.setNfsRetrans(nfsModel.getRetransmissions().asConvertible().nullableShort());
                            tempVar.setNfsTimeo(nfsModel.getTimeout().asConvertible().nullableShort());
                        }
                    }
                    if (storageModel instanceof PosixStorageModel) {
                        PosixStorageModel posixModel = (PosixStorageModel) storageModel;
                        tempVar.setVfsType((String) posixModel.getVfsType().getEntity());
                        tempVar.setMountOptions((String) posixModel.getMountOptions().getEntity());
                    }
                    storageListModel.fileConnection = tempVar;
                    storageListModel.importFileStorageConnect();
                }
            }
        }), null, path);
    }

    public void importFileStorageConnect()
    {
        Frontend.getInstance().runAction(VdcActionType.AddStorageServerConnection, new StorageServerConnectionParametersBase(fileConnection, hostId),
            new IFrontendActionAsyncCallback() {
                @Override
                public void executed(FrontendActionAsyncResult result) {

                    StorageListModel storageListModel = (StorageListModel) result.getState();
                        VdcReturnValueBase returnVal = result.getReturnValue();
                        boolean success = returnVal != null && returnVal.getSucceeded();
                        if (success)
                        {
                            storageListModel.fileConnection.setid((String) returnVal.getActionReturnValue());
                            AsyncDataProvider.getExistingStorageDomainList(new AsyncQuery(storageListModel,
                                    new INewAsyncCallback() {
                                        @Override
                                        public void onSuccess(Object target, Object returnValue) {

                                            StorageListModel storageListModel1 = (StorageListModel) target;
                                            ArrayList<StorageDomain> domains = (ArrayList<StorageDomain>) returnValue;
                                            if (domains != null && !domains.isEmpty()) {
                                                storageListModel1.importFileStorageAddDomain(domains);
                                            }
                                            else {
                                                String errorMessage = domains == null ?
                                                        ConstantsManager.getInstance().getConstants()
                                                                .failedToRetrieveExistingStorageDomainInformationMsg() :
                                                        ConstantsManager.getInstance().getConstants()
                                                                .thereIsNoStorageDomainUnderTheSpecifiedPathMsg();

                                                postImportFileStorage(storageListModel1.context,
                                                        false,
                                                        storageListModel1.storageModel,
                                                        errorMessage);

                                                storageListModel1.cleanConnection(storageListModel1.fileConnection, storageListModel1.hostId);
                                            }
                                        }
                                    }),
                                    hostId,
                                    domainType,
                                    storageType,
                                    path);
                        }
                        else
                        {
                            postImportFileStorage(storageListModel.context,
                                    false,
                                    storageListModel.storageModel,
                                    ConstantsManager.getInstance()
                                            .getConstants()
                                            .failedToRetrieveExistingStorageDomainInformationMsg());
                        }

                    }
                },
                this);
    }

    public void importFileStorageAddDomain(ArrayList<StorageDomain> domains)
    {
        StorageDomain sdToAdd = Linq.firstOrDefault(domains);
        StorageDomainStatic sdsToAdd = sdToAdd.getStorageStaticData();

        StorageDomainManagementParameter params = new StorageDomainManagementParameter(sdsToAdd);
        params.setVdsId(hostId);
        Frontend.getInstance().runAction(VdcActionType.AddExistingFileStorageDomain, params, new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                Object[] array = (Object[]) result.getState();
                StorageListModel storageListModel = (StorageListModel) array[0];
                StorageDomain sdToAdd1 = (StorageDomain) array[1];
                VdcReturnValueBase returnVal = result.getReturnValue();

                boolean success = returnVal != null && returnVal.getSucceeded();
                if (success) {

                    StorageModel model = (StorageModel) storageListModel.getWindow();
                    StoragePool dataCenter = (StoragePool) model.getDataCenter().getSelectedItem();
                    if (!dataCenter.getId().equals(StorageModel.UnassignedDataCenterId)) {
                        storageListModel.attachStorageToDataCenter(sdToAdd1.getId(), dataCenter.getId());
                        onFinish(storageListModel.context, true, storageListModel.storageModel, null);
                    } else {
                        postImportFileStorage(storageListModel.context, true, storageListModel.storageModel, null);
                    }

                } else {
                    postImportFileStorage(storageListModel.context, false, storageListModel.storageModel, ""); //$NON-NLS-1$
                }
            }
        }, new Object[] {this, sdToAdd});
    }

    public void postImportFileStorage(TaskContext context, boolean isSucceeded, IStorageModel model, String message)
    {
        Frontend.getInstance().runAction(VdcActionType.DisconnectStorageServerConnection,
            new StorageServerConnectionParametersBase(fileConnection, hostId),
            new IFrontendActionAsyncCallback() {
                @Override
                public void executed(FrontendActionAsyncResult result) {

                    VdcReturnValueBase returnValue = result.getReturnValue();
                    boolean success = returnValue != null && returnValue.getSucceeded();
                    if (success) {
                        fileConnection = null;
                    }
                    Object[] array = (Object[]) result.getState();
                    onFinish((TaskContext) array[0],
                        (Boolean) array[1],
                        (IStorageModel) array[2],
                        (String) array[3]);

                }
            },
            new Object[] {context, isSucceeded, model, message});
    }


    @Override
    public void run(TaskContext context)
    {
        ArrayList<Object> data = (ArrayList<Object>) context.getState();
        String key = (String) data.get(0);

        if (StringHelper.stringsEqual(key, "SaveNfs")) //$NON-NLS-1$
        {
            saveNfsStorage(context);
        }
        else if (StringHelper.stringsEqual(key, "SaveLocal")) //$NON-NLS-1$
        {
            saveLocalStorage(context);
        }
        else if (StringHelper.stringsEqual(key, "SavePosix")) //$NON-NLS-1$
        {
            savePosixStorage(context);
        }
        else if (StringHelper.stringsEqual(key, "SaveGluster")) //$NON-NLS-1$
        {
            saveGlusterStorage(context);
        }
        else if (StringHelper.stringsEqual(key, "SaveSan")) //$NON-NLS-1$
        {
            saveSanStorage(context);
        }
        else if (StringHelper.stringsEqual(key, "ImportFile")) //$NON-NLS-1$
        {
            importFileStorage(context);
        }
        else if (StringHelper.stringsEqual(key, "Finish")) //$NON-NLS-1$
        {
            getWindow().stopProgress();

            if ((Boolean) data.get(1))
            {
                cancel();
            }
            else
            {
                ((Model) data.get(2)).setMessage((String) data.get(3));
            }
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
        if (systemTreeSelectedItem != value)
        {
            systemTreeSelectedItem = value;
            onSystemTreeSelectedItemChanged();
        }
    }

    private void onSystemTreeSelectedItemChanged()
    {
        updateActionAvailability();
    }

    @Override
    protected String getListName() {
        return "StorageListModel"; //$NON-NLS-1$
    }

    @Override
    protected void openReport() {

        final ReportModel reportModel = super.createReportModel();

        List<StorageDomain> items =
                getSelectedItems() != null && getSelectedItem() != null ? getSelectedItems()
                        : new ArrayList<StorageDomain>();
        StorageDomain storage = items.iterator().next();

        AsyncDataProvider.getDataCentersByStorageDomain(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {

                List<StoragePool> dataCenters = (List<StoragePool>) returnValue;
                for (StoragePool dataCenter : dataCenters) {
                    reportModel.addDataCenterID(dataCenter.getId().toString());
                }

                if (reportModel == null) {
                    return;
                }

                setWidgetModel(reportModel);
            }
        }), storage.getId());
    }

    @Override
    protected void setReportModelResourceId(ReportModel reportModel, String idParamName, boolean isMultiple) {
        ArrayList<StorageDomain> items =
                getSelectedItems() != null ? Linq.<StorageDomain> cast(getSelectedItems())
                        : new ArrayList<StorageDomain>();

        if (idParamName != null) {
            for (StorageDomain item : items) {
                if (isMultiple) {
                    reportModel.addResourceId(idParamName, item.getId().toString());
                } else {
                    reportModel.setResourceId(idParamName, item.getId().toString());
                }
            }
        }
    }

    private UICommand createCancelCommand(String commandName) {
        UICommand command;
        command = new UICommand(commandName, this);
        command.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        command.setIsCancel(true);
        return command;
    }

    private UICommand createOKCommand(String commandName) {
        UICommand command = new UICommand(commandName, this);
        command.setTitle(ConstantsManager.getInstance().getConstants().ok());
        command.setIsDefault(true);
        return command;
    }
}
