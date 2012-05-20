package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VmOsType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.DataProvider;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.RangeEntityModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemType;
import org.ovirt.engine.ui.uicommonweb.models.storage.DisksAllocationModel;
import org.ovirt.engine.ui.uicommonweb.validation.AsciiOrNoneValidation;
import org.ovirt.engine.ui.uicommonweb.validation.ByteSizeValidation;
import org.ovirt.engine.ui.uicommonweb.validation.CustomPropertyValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.LengthValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicommonweb.validation.RegexValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

@SuppressWarnings("unused")
public class UnitVmModel extends Model {

    public static final int WINDOWS_VM_NAME_MAX_LIMIT = 15;
    public static final int NON_WINDOWS_VM_NAME_MAX_LIMIT = 64;

    private boolean privateIsNew;

    public boolean getIsNew()
    {
        return privateIsNew;
    }

    public void setIsNew(boolean value)
    {
        privateIsNew = value;
    }

    private VmType privateVmType = getVmType().values()[0];

    public VmType getVmType()
    {
        return privateVmType;
    }

    public void setVmType(VmType value)
    {
        privateVmType = value;
    }

    private String privateHash;

    public String getHash()
    {
        return privateHash;
    }

    public void setHash(String value)
    {
        privateHash = value;
    }

    private boolean isBlankTemplate;

    public boolean getIsBlankTemplate()
    {
        return isBlankTemplate;
    }

    public void setIsBlankTemplate(boolean value)
    {
        if (isBlankTemplate != value)
        {
            isBlankTemplate = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsBlankTemplate")); //$NON-NLS-1$
        }
    }

    private boolean isWindowsOS;

    public boolean getIsWindowsOS()
    {
        return isWindowsOS;
    }

    public void setIsWindowsOS(boolean value)
    {
        if (isWindowsOS != value)
        {
            isWindowsOS = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsWindowsOS")); //$NON-NLS-1$
        }
    }

    private boolean isLinux_Unassign_UnknownOS;

    public boolean getIsLinux_Unassign_UnknownOS()
    {
        return isLinux_Unassign_UnknownOS;
    }

    public void setIsLinux_Unassign_UnknownOS(boolean value)
    {
        if (isLinux_Unassign_UnknownOS != value)
        {
            isLinux_Unassign_UnknownOS = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsLinux_Unassign_UnknownOS")); //$NON-NLS-1$
        }
    }

    private String cpuNotification;

    public String getCPUNotification()
    {
        return cpuNotification;
    }

    public void setCPUNotification(String value)
    {
        if (!StringHelper.stringsEqual(cpuNotification, value))
        {
            cpuNotification = value;
            OnPropertyChanged(new PropertyChangedEventArgs("CPUNotification")); //$NON-NLS-1$
        }
    }

    public boolean isCPUsAmountValid;

    public boolean getIsCPUsAmountValid()
    {
        return isCPUsAmountValid;
    }

    public void setIsCPUsAmountValid(boolean value)
    {
        if (isCPUsAmountValid != value)
        {
            isCPUsAmountValid = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsCPUsAmountValid")); //$NON-NLS-1$
        }
    }

    private boolean isGeneralTabValid;

    public boolean getIsGeneralTabValid()
    {
        return isGeneralTabValid;
    }

    public void setIsGeneralTabValid(boolean value)
    {
        if (isGeneralTabValid != value)
        {
            isGeneralTabValid = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsGeneralTabValid")); //$NON-NLS-1$
        }
    }

    private boolean isFirstRunTabValid;

    public boolean getIsFirstRunTabValid()
    {
        return isFirstRunTabValid;
    }

    public void setIsFirstRunTabValid(boolean value)
    {
        if (isFirstRunTabValid != value)
        {
            isFirstRunTabValid = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsFirstRunTabValid")); //$NON-NLS-1$
        }
    }

    private boolean isDisplayTabValid;

    public boolean getIsDisplayTabValid()
    {
        return isDisplayTabValid;
    }

    public void setIsDisplayTabValid(boolean value)
    {
        if (isDisplayTabValid != value)
        {
            isDisplayTabValid = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsDisplayTabValid")); //$NON-NLS-1$
        }
    }

    private boolean isAllocationTabValid;

    public boolean getIsAllocationTabValid()
    {
        return isAllocationTabValid;
    }

    public void setIsAllocationTabValid(boolean value)
    {
        if (isAllocationTabValid != value)
        {
            isAllocationTabValid = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsAllocationTabValid")); //$NON-NLS-1$
        }
    }

    private boolean isHostTabValid;

    public boolean getIsHostTabValid()
    {
        return isHostTabValid;
    }

    public void setIsHostTabValid(boolean value)
    {
        if (isHostTabValid != value)
        {
            isHostTabValid = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsHostTabValid")); //$NON-NLS-1$
        }
    }

    private boolean isBootSequenceTabValid;

    public boolean getIsBootSequenceTabValid()
    {
        return isBootSequenceTabValid;
    }

    public void setIsBootSequenceTabValid(boolean value)
    {
        if (isBootSequenceTabValid != value)
        {
            isBootSequenceTabValid = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsBootSequenceTabValid")); //$NON-NLS-1$
        }
    }

    private boolean isCustomPropertiesTabValid;

    public boolean getIsCustomPropertiesTabValid()
    {
        return isCustomPropertiesTabValid;
    }

    public void setIsCustomPropertiesTabValid(boolean value)
    {
        if (isCustomPropertiesTabValid != value)
        {
            isCustomPropertiesTabValid = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsCustomPropertiesTabValid")); //$NON-NLS-1$
        }
    }

    private ListModel privateDataCenter;

    public ListModel getDataCenter()
    {
        return privateDataCenter;
    }

    private void setDataCenter(ListModel value)
    {
        privateDataCenter = value;
    }

    private ListModel privateStorageDomain;

    public ListModel getStorageDomain()
    {
        return privateStorageDomain;
    }

    private void setStorageDomain(ListModel value)
    {
        privateStorageDomain = value;
    }

    private ListModel privateTemplate;

    public ListModel getTemplate()
    {
        return privateTemplate;
    }

    private void setTemplate(ListModel value)
    {
        privateTemplate = value;
    }

    private EntityModel privateName;

    public EntityModel getName()
    {
        return privateName;
    }

    private void setName(EntityModel value)
    {
        privateName = value;
    }

    private ListModel privateOSType;

    public ListModel getOSType()
    {
        return privateOSType;
    }

    private void setOSType(ListModel value)
    {
        privateOSType = value;
    }

    private ListModel privateNumOfMonitors;

    public ListModel getNumOfMonitors()
    {
        return privateNumOfMonitors;
    }

    private void setNumOfMonitors(ListModel value)
    {
        privateNumOfMonitors = value;
    }

    private EntityModel privateAllowConsoleReconnect;

    public EntityModel getAllowConsoleReconnect()
    {
        return privateAllowConsoleReconnect;
    }

    private void setAllowConsoleReconnect(EntityModel value)
    {
        privateAllowConsoleReconnect = value;
    }

    private EntityModel privateDescription;

    public EntityModel getDescription()
    {
        return privateDescription;
    }

    private void setDescription(EntityModel value)
    {
        privateDescription = value;
    }

    private ListModel privateDomain;

    public ListModel getDomain()
    {
        return privateDomain;
    }

    private void setDomain(ListModel value)
    {
        privateDomain = value;
    }

    private EntityModel privateMemSize;

    public EntityModel getMemSize()
    {
        return privateMemSize;
    }

    private void setMemSize(EntityModel value)
    {
        privateMemSize = value;
    }

    private EntityModel privateMinAllocatedMemory;

    public EntityModel getMinAllocatedMemory()
    {
        return privateMinAllocatedMemory;
    }

    private void setMinAllocatedMemory(EntityModel value)
    {
        privateMinAllocatedMemory = value;
    }

    private ListModel privateQuota;

    public ListModel getQuota()
    {
        return privateQuota;
    }

    private void setQuota(ListModel value)
    {
        privateQuota = value;
    }

    private ListModel privateCluster;

    public ListModel getCluster()
    {
        return privateCluster;
    }

    private void setCluster(ListModel value)
    {
        privateCluster = value;
    }

    private ListModel privateUsbPolicy;

    public ListModel getUsbPolicy()
    {
        return privateUsbPolicy;
    }

    private void setUsbPolicy(ListModel value)
    {
        privateUsbPolicy = value;
    }

    private ListModel privateTimeZone;

    public ListModel getTimeZone()
    {
        return privateTimeZone;
    }

    private void setTimeZone(ListModel value)
    {
        privateTimeZone = value;
    }

    private RangeEntityModel privateNumOfSockets;

    public RangeEntityModel getNumOfSockets()
    {
        return privateNumOfSockets;
    }

    private void setNumOfSockets(RangeEntityModel value)
    {
        privateNumOfSockets = value;
    }

    private RangeEntityModel privateTotalCPUCores;

    public RangeEntityModel getTotalCPUCores()
    {
        return privateTotalCPUCores;
    }

    private void setTotalCPUCores(RangeEntityModel value)
    {
        privateTotalCPUCores = value;
    }

    private ListModel privateDefaultHost;

    public ListModel getDefaultHost()
    {
        return privateDefaultHost;
    }

    private void setDefaultHost(ListModel value)
    {
        privateDefaultHost = value;
    }

    private EntityModel privateIsStateless;

    public EntityModel getIsStateless()
    {
        return privateIsStateless;
    }

    private void setIsStateless(EntityModel value)
    {
        privateIsStateless = value;
    }

    private ListModel privateDisplayProtocol;

    public ListModel getDisplayProtocol()
    {
        return privateDisplayProtocol;
    }

    private void setDisplayProtocol(ListModel value)
    {
        privateDisplayProtocol = value;
    }

    private EntityModel privateProvisioning;

    public EntityModel getProvisioning()
    {
        return privateProvisioning;
    }

    private void setProvisioning(EntityModel value)
    {
        privateProvisioning = value;
    }

    private EntityModel privateProvisioningThin_IsSelected;

    public EntityModel getProvisioningThin_IsSelected()
    {
        return privateProvisioningThin_IsSelected;
    }

    public void setProvisioningThin_IsSelected(EntityModel value)
    {
        privateProvisioningThin_IsSelected = value;
    }

    private EntityModel privateProvisioningClone_IsSelected;

    public EntityModel getProvisioningClone_IsSelected()
    {
        return privateProvisioningClone_IsSelected;
    }

    public void setProvisioningClone_IsSelected(EntityModel value)
    {
        privateProvisioningClone_IsSelected = value;
    }

    private ListModel privatePriority;

    public ListModel getPriority()
    {
        return privatePriority;
    }

    private void setPriority(ListModel value)
    {
        privatePriority = value;
    }

    private EntityModel privateIsHighlyAvailable;

    public EntityModel getIsHighlyAvailable()
    {
        return privateIsHighlyAvailable;
    }

    private void setIsHighlyAvailable(EntityModel value)
    {
        privateIsHighlyAvailable = value;
    }

    private ListModel privateFirstBootDevice;

    public ListModel getFirstBootDevice()
    {
        return privateFirstBootDevice;
    }

    private void setFirstBootDevice(ListModel value)
    {
        privateFirstBootDevice = value;
    }

    private ListModel privateSecondBootDevice;

    public ListModel getSecondBootDevice()
    {
        return privateSecondBootDevice;
    }

    private void setSecondBootDevice(ListModel value)
    {
        privateSecondBootDevice = value;
    }

    private ListModel privateCdImage;

    public ListModel getCdImage()
    {
        return privateCdImage;
    }

    private void setCdImage(ListModel value)
    {
        privateCdImage = value;
    }

    private EntityModel cdAttached;

    public EntityModel getCdAttached() {
        return cdAttached;
    }

    public void setCdAttached(EntityModel value) {
        cdAttached = value;
    }

    private EntityModel privateInitrd_path;

    public EntityModel getInitrd_path()
    {
        return privateInitrd_path;
    }

    private void setInitrd_path(EntityModel value)
    {
        privateInitrd_path = value;
    }

    private EntityModel privateKernel_path;

    public EntityModel getKernel_path()
    {
        return privateKernel_path;
    }

    private void setKernel_path(EntityModel value)
    {
        privateKernel_path = value;
    }

    private EntityModel privateKernel_parameters;

    public EntityModel getKernel_parameters()
    {
        return privateKernel_parameters;
    }

    private void setKernel_parameters(EntityModel value)
    {
        privateKernel_parameters = value;
    }

    private EntityModel privateCustomProperties;

    public EntityModel getCustomProperties()
    {
        return privateCustomProperties;
    }

    private void setCustomProperties(EntityModel value)
    {
        privateCustomProperties = value;
    }

    private HashMap<Version, ArrayList<String>> privateCustomPropertiesKeysList;

    public HashMap<Version, ArrayList<String>> getCustomPropertiesKeysList()
    {
        return privateCustomPropertiesKeysList;
    }

    public void setCustomPropertiesKeysList(HashMap<Version, ArrayList<String>> value)
    {
        privateCustomPropertiesKeysList = value;
    }

    private EntityModel privateIsAutoAssign;

    public EntityModel getIsAutoAssign()
    {
        return privateIsAutoAssign;
    }

    public void setIsAutoAssign(EntityModel value)
    {
        privateIsAutoAssign = value;
    }

    private EntityModel privateRunVMOnSpecificHost;

    public EntityModel getRunVMOnSpecificHost()
    {
        return privateRunVMOnSpecificHost;
    }

    public void setRunVMOnSpecificHost(EntityModel value)
    {
        privateRunVMOnSpecificHost = value;
    }

    private EntityModel privateDontMigrateVM;

    public EntityModel getDontMigrateVM()
    {
        return privateDontMigrateVM;
    }

    public void setDontMigrateVM(EntityModel value)
    {
        privateDontMigrateVM = value;
    }

    private EntityModel privateIsTemplatePublic;

    public EntityModel getIsTemplatePublic()
    {
        return privateIsTemplatePublic;
    }

    private void setIsTemplatePublic(EntityModel value)
    {
        privateIsTemplatePublic = value;
    }

    private EntityModel privateIsTemplatePrivate;

    public EntityModel getIsTemplatePrivate()
    {
        return privateIsTemplatePrivate;
    }

    private void setIsTemplatePrivate(EntityModel value)
    {
        privateIsTemplatePrivate = value;
    }

    private boolean privateIsFirstRun;

    public boolean getIsFirstRun()
    {
        return privateIsFirstRun;
    }

    public void setIsFirstRun(boolean value)
    {
        privateIsFirstRun = value;
    }

    private List<DiskModel> disks;

    public List<DiskModel> getDisks()
    {
        return disks;
    }

    public void setDisks(List<DiskModel> value)
    {
        if (disks != value)
        {
            disks = value;
            OnPropertyChanged(new PropertyChangedEventArgs("Disks")); //$NON-NLS-1$
        }
    }

    private DisksAllocationModel disksAllocationModel;

    public DisksAllocationModel getDisksAllocationModel()
    {
        return disksAllocationModel;
    }

    private void setDisksAllocationModel(DisksAllocationModel value)
    {
        disksAllocationModel = value;
    }

    private boolean isDisksAvailable;

    public boolean getIsDisksAvailable()
    {
        return isDisksAvailable;
    }

    public void setIsDisksAvailable(boolean value)
    {
        if (isDisksAvailable != value)
        {
            isDisksAvailable = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsDisksAvailable")); //$NON-NLS-1$
        }
    }

    private boolean isCustomPropertiesAvailable;

    public boolean getIsCustomPropertiesAvailable()
    {
        return isCustomPropertiesAvailable;
    }

    public void setIsCustomPropertiesAvailable(boolean value)
    {
        if (isCustomPropertiesAvailable != value)
        {
            isCustomPropertiesAvailable = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsCustomPropertiesAvailable")); //$NON-NLS-1$

            if (value == false)
            {
                getCustomProperties().setEntity(""); //$NON-NLS-1$
            }
        }
    }

    private boolean isHostAvailable;

    public boolean getIsHostAvailable()
    {
        return isHostAvailable;
    }

    public void setIsHostAvailable(boolean value)
    {
        if (isHostAvailable != value)
        {
            isHostAvailable = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsHostAvailable")); //$NON-NLS-1$
        }
    }

    private Boolean isDatacenterAvailable;

    public Boolean getIsDatacenterAvailable()
    {
        return isDatacenterAvailable;
    }

    public void setIsDatacenterAvailable(Boolean value)
    {
        if (isDatacenterAvailable == null && value == null)
        {
            return;
        }
        if (isDatacenterAvailable == null || !isDatacenterAvailable.equals(value))
        {
            isDatacenterAvailable = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsDatacenterAvailable")); //$NON-NLS-1$
        }
    }

    private final VmModelBehaviorBase behavior;

    public VmModelBehaviorBase getBehavior() {
        return behavior;
    }

    private void setBehavior(VmModelBehaviorBase value) {
    }

    private int _minMemSize = 1;

    public int get_MinMemSize()
    {
        return _minMemSize;
    }

    public void set_MinMemSize(int value)
    {
        _minMemSize = value;
    }

    private int _maxMemSize32 = 20480;

    public int get_MaxMemSize32()
    {
        return _maxMemSize32;
    }

    public void set_MaxMemSize32(int value)
    {
        _maxMemSize32 = value;
    }

    private int _maxMemSize64 = 262144;

    public int get_MaxMemSize64()
    {
        return _maxMemSize64;
    }

    public void set_MaxMemSize64(int value)
    {
        _maxMemSize64 = value;
    }

    public UnitVmModel(VmModelBehaviorBase behavior)
    {
        Frontend.getQueryStartedEvent().addListener(this);
        Frontend.getQueryCompleteEvent().addListener(this);

        Frontend.Subscribe(new VdcQueryType[] { VdcQueryType.GetStorageDomainsByStoragePoolId,
                VdcQueryType.GetAllIsoImagesList, VdcQueryType.GetTimeZones, VdcQueryType.GetDefualtTimeZone,
                VdcQueryType.GetDomainList, VdcQueryType.GetConfigurationValue,
                VdcQueryType.GetVdsGroupsByStoragePoolId, VdcQueryType.GetVmTemplatesByStoragePoolId,
                VdcQueryType.GetVmTemplatesDisks, VdcQueryType.GetStorageDomainsByVmTemplateId,
                VdcQueryType.GetStorageDomainById, VdcQueryType.GetDataCentersWithPermittedActionOnClusters,
                VdcQueryType.GetClustersWithPermittedAction, VdcQueryType.GetVmTemplatesWithPermittedAction,
                VdcQueryType.GetVdsGroupById, VdcQueryType.GetStoragePoolById, VdcQueryType.GetAllDisksByVmId,
                VdcQueryType.GetVmTemplate, VdcQueryType.GetVmConfigurationBySnapshot,
                VdcQueryType.GetPermittedStorageDomainsByStoragePoolId, VdcQueryType.Search });

        this.behavior = behavior;
        this.behavior.setModel(this);

        setStorageDomain(new ListModel());
        setName(new EntityModel());
        setNumOfMonitors(new ListModel());
        setAllowConsoleReconnect(new EntityModel());
        setDescription(new EntityModel());
        setDomain(new ListModel());
        setMinAllocatedMemory(new EntityModel());
        setUsbPolicy(new ListModel());
        setIsStateless(new EntityModel());

        setCdImage(new ListModel());
        getCdImage().setIsChangable(false);

        setCdAttached(new EntityModel());
        getCdAttached().getEntityChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {

                getCdImage().setIsChangable((Boolean) getCdAttached().getEntity());
            }
        });
        getCdAttached().setEntity(false);

        setIsHighlyAvailable(new EntityModel());
        setDontMigrateVM(new EntityModel());
        setIsTemplatePublic(new EntityModel());
        setIsTemplatePrivate(new EntityModel());
        setKernel_parameters(new EntityModel());
        setKernel_path(new EntityModel());
        setInitrd_path(new EntityModel());
        setCustomProperties(new EntityModel());
        setDisplayProtocol(new ListModel());
        setSecondBootDevice(new ListModel());
        setPriority(new ListModel());
        setTotalCPUCores(new RangeEntityModel());

        setDataCenter(new ListModel());
        getDataCenter().getSelectedItemChangedEvent().addListener(this);

        setTemplate(new ListModel());
        getTemplate().getSelectedItemChangedEvent().addListener(this);

        setQuota(new ListModel());
        getQuota().setIsAvailable(false);

        setCluster(new ListModel());
        getCluster().getSelectedItemChangedEvent().addListener(this);

        setTimeZone(new ListModel());
        getTimeZone().getSelectedItemChangedEvent().addListener(this);

        setDefaultHost(new ListModel());
        getDefaultHost().getSelectedItemChangedEvent().addListener(this);

        setOSType(new ListModel());
        getOSType().getSelectedItemChangedEvent().addListener(this);

        setFirstBootDevice(new ListModel());
        getFirstBootDevice().getSelectedItemChangedEvent().addListener(this);

        setProvisioning(new EntityModel());
        getProvisioning().getEntityChangedEvent().addListener(this);

        setMemSize(new EntityModel());
        getMemSize().getEntityChangedEvent().addListener(this);

        setNumOfSockets(new RangeEntityModel());
        getNumOfSockets().getEntityChangedEvent().addListener(this);

        setRunVMOnSpecificHost(new EntityModel());
        getRunVMOnSpecificHost().getEntityChangedEvent().addListener(this);

        setIsAutoAssign(new EntityModel());
        getIsAutoAssign().getEntityChangedEvent().addListener(this);

        setIsTemplatePrivate(new EntityModel());
        getIsTemplatePrivate().getEntityChangedEvent().addListener(this);

        setIsHostTabValid(true);
        setIsCustomPropertiesTabValid(getIsHostTabValid());
        setIsBootSequenceTabValid(getIsCustomPropertiesTabValid());
        setIsAllocationTabValid(getIsBootSequenceTabValid());
        setIsDisplayTabValid(getIsAllocationTabValid());
        setIsFirstRunTabValid(getIsDisplayTabValid());
        setIsGeneralTabValid(getIsFirstRunTabValid());

        // NOTE: This is because currently the auto generated view code tries to register events of pooltype for
        // VM/Template views as this model is shared across VM/Template/Pool models
        setPoolType(new ListModel());

        setNumOfDesktops(new EntityModel());
        getNumOfDesktops().setEntity(0);
        getNumOfDesktops().setIsAvailable(false);

        setAssignedVms(new EntityModel());
        getAssignedVms().setEntity(0);
        getAssignedVms().setIsAvailable(false);
        // Assigned VMs count is always read-only.
        getAssignedVms().setIsChangable(false);

        setPrestartedVms(new EntityModel());
        getPrestartedVms().setEntity(0);
        getPrestartedVms().setIsAvailable(false);


        setDisksAllocationModel(new DisksAllocationModel());

        setProvisioningClone_IsSelected(new EntityModel());
        getProvisioningClone_IsSelected().getEntityChangedEvent().addListener(this);

        setProvisioningThin_IsSelected(new EntityModel());
        getProvisioningThin_IsSelected().getEntityChangedEvent().addListener(this);
    }

    public void Initialize(SystemTreeItemModel SystemTreeSelectedItem)
    {
        super.Initialize();

        setHash(getHashName() + new Date());

        getMemSize().setEntity(256);
        getMinAllocatedMemory().setEntity(256);
        getIsStateless().setEntity(false);
        getIsHighlyAvailable().setEntity(false);
        getDontMigrateVM().setEntity(false);
        getIsAutoAssign().setEntity(true);
        getIsTemplatePublic().setEntity(false);
        getIsTemplatePrivate().setEntity(true);

        getRunVMOnSpecificHost().setEntity(false);
        getRunVMOnSpecificHost().setIsChangable(false);

        getCdImage().setIsChangable(false);

        InitUsbPolicy();
        InitOSType();
        InitDisplayProtocol();
        InitFirstBootDevice();
        InitNumOfMonitors();
        InitAllowConsoleReconnect();
        InitMinimalVmMemSize();
        InitMaximalVmMemSize32OS();

        behavior.Initialize(SystemTreeSelectedItem);
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (ev.equals(Frontend.QueryStartedEventDefinition)
                && StringHelper.stringsEqual(Frontend.getCurrentContext(), getHash()))
        {
            Frontend_QueryStarted();
        }
        else if (ev.equals(Frontend.QueryCompleteEventDefinition)
                && StringHelper.stringsEqual(Frontend.getCurrentContext(), getHash()))
        {
            Frontend_QueryComplete();
        }
        else if (ev.equals(ListModel.SelectedItemChangedEventDefinition))
        {
            if (sender == getDataCenter())
            {
                DataCenter_SelectedItemChanged(sender, args);
            }
            else if (sender == getTemplate())
            {
                Template_SelectedItemChanged(sender, args);
            }
            else if (sender == getCluster())
            {
                Cluster_SelectedItemChanged(sender, args);
            }
            else if (sender == getTimeZone())
            {
                TimeZone_SelectedItemChanged(sender, args);
            }
            else if (sender == getDefaultHost())
            {
                DefaultHost_SelectedItemChanged(sender, args);
            }
            else if (sender == getOSType())
            {
                OSType_SelectedItemChanged(sender, args);
            }
            else if (sender == getFirstBootDevice())
            {
                FirstBootDevice_SelectedItemChanged(sender, args);
            }
            else if (sender == getDisplayProtocol())
            {
                DisplayProtocol_SelectedItemChanged(sender, args);
            }
        }
        else if (ev.equals(EntityModel.EntityChangedEventDefinition))
        {
            if (sender == getMemSize())
            {
                MemSize_EntityChanged(sender, args);
            }
            else if (sender == getNumOfSockets())
            {
                NumOfSockets_EntityChanged(sender, args);
            }
            else if (sender == getRunVMOnSpecificHost())
            {
                RunVMOnSpecificHost_EntityChanged(sender, args);
            }
            else if (sender == getIsAutoAssign())
            {
                IsAutoAssign_EntityChanged(sender, args);
            }
            else if (sender == getIsTemplatePrivate())
            {
                IsTemplatePrivate_EntityChanged(sender, args);
            }

            else if (sender == getProvisioning())
            {
                Provisioning_SelectedItemChanged(sender, args);
            }
            else if (sender == getProvisioningThin_IsSelected())
            {
                if ((Boolean) getProvisioningThin_IsSelected().getEntity()) {
                    getProvisioning().setEntity(false);
                }
            }
            else if (sender == getProvisioningClone_IsSelected())
            {
                if ((Boolean) getProvisioningClone_IsSelected().getEntity()) {
                    getProvisioning().setEntity(true);
                }
            }
        }
    }

    private int queryCounter;

    private void Frontend_QueryStarted()
    {
        queryCounter++;
        if (getProgress() == null)
        {
            StartProgress(null);
        }
    }

    private void Frontend_QueryComplete()
    {
        queryCounter--;
        if (queryCounter == 0)
        {
            StopProgress();
        }
    }

    protected void InitNumOfMonitors()
    {
        if (getVmType() == VmType.Desktop)
        {
            AsyncDataProvider.GetNumOfMonitorList(new AsyncQuery(this,
                    new INewAsyncCallback() {
                        @Override
                        public void OnSuccess(Object target, Object returnValue) {

                            UnitVmModel model = (UnitVmModel) target;
                            Integer oldNumOfMonitors = null;
                            if (model.getNumOfMonitors().getSelectedItem() != null)
                            {
                                oldNumOfMonitors = (Integer) model.getNumOfMonitors().getSelectedItem();
                            }
                            ArrayList<Integer> numOfMonitors = (ArrayList<Integer>) returnValue;
                            model.getNumOfMonitors().setItems(numOfMonitors);
                            if (oldNumOfMonitors != null)
                            {
                                model.getNumOfMonitors().setSelectedItem(oldNumOfMonitors);
                            }

                        }
                    }, getHash()));
        }
        else
        {
            getNumOfMonitors().setItems(new ArrayList<Integer>(Arrays.asList(new Integer[] {1})));
            getNumOfMonitors().setSelectedItem(1);
        }
    }

    protected void InitAllowConsoleReconnect()
    {
        getAllowConsoleReconnect().setEntity(getVmType() == VmType.Server);
    }

    private void InitOSType()
    {
        getOSType().setItems(DataProvider.GetOSList());
        getOSType().setSelectedItem(VmOsType.Unassigned);
    }

    private void InitUsbPolicy()
    {
        getUsbPolicy().setItems(DataProvider.GetUsbPolicyList());
    }

    private void InitMinimalVmMemSize()
    {
        AsyncDataProvider.GetMinimalVmMemSize(new AsyncQuery(this,
            new INewAsyncCallback() {
                @Override
                public void OnSuccess(Object target, Object returnValue) {

                    UnitVmModel vmModel = (UnitVmModel) target;
                    vmModel.set_MinMemSize((Integer) returnValue);

                }
            }, getHash()));
    }

    private void InitMaximalVmMemSize32OS()
    {
        AsyncDataProvider.GetMaximalVmMemSize32OS(new AsyncQuery(this,
            new INewAsyncCallback() {
                @Override
                public void OnSuccess(Object target, Object returnValue) {

                    UnitVmModel vmModel = (UnitVmModel) target;
                    vmModel.set_MaxMemSize32((Integer) returnValue);

                }
            }, getHash()));
    }

    private void UpdateMaximalVmMemSize()
    {
        VDSGroup cluster = (VDSGroup) getCluster().getSelectedItem();

        if (cluster != null)
        {
            AsyncDataProvider.GetMaximalVmMemSize64OS(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object target, Object returnValue) {

                        UnitVmModel vmModel = (UnitVmModel) target;
                        vmModel.set_MaxMemSize64((Integer) returnValue);

                    }
                }, getHash()), cluster.getcompatibility_version().toString());
        }
    }

    private void InitDisplayProtocol()
    {
        ArrayList<EntityModel> displayProtocolOptions = new ArrayList<EntityModel>();

        EntityModel spiceProtocol = new EntityModel();
        spiceProtocol.setTitle(ConstantsManager.getInstance().getConstants().spiceTitle());
        spiceProtocol.setEntity(DisplayType.qxl);

        EntityModel vncProtocol = new EntityModel();
        vncProtocol.setTitle(ConstantsManager.getInstance().getConstants().VNCTitle());
        vncProtocol.setEntity(DisplayType.vnc);

        displayProtocolOptions.add(spiceProtocol);
        displayProtocolOptions.add(vncProtocol);
        getDisplayProtocol().setItems(displayProtocolOptions);

        getDisplayProtocol().getSelectedItemChangedEvent().addListener(this);
    }

    private void InitFirstBootDevice()
    {
        EntityModel tempVar = new EntityModel();
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().hardDiskTitle());
        tempVar.setEntity(BootSequence.C);
        EntityModel hardDiskOption = tempVar;

        ArrayList<EntityModel> firstBootDeviceItems = new ArrayList<EntityModel>();
        firstBootDeviceItems.add(hardDiskOption);
        EntityModel tempVar2 = new EntityModel();
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cdromTitle());
        tempVar2.setEntity(BootSequence.D);
        firstBootDeviceItems.add(tempVar2);
        EntityModel tempVar3 = new EntityModel();
        tempVar3.setTitle(ConstantsManager.getInstance().getConstants().networkPXETitle());
        tempVar3.setEntity(BootSequence.N);
        firstBootDeviceItems.add(tempVar3);
        getFirstBootDevice().setItems(firstBootDeviceItems);
        getFirstBootDevice().setSelectedItem(hardDiskOption);
    }

    private void DataCenter_SelectedItemChanged(Object sender, EventArgs args)
    {
        behavior.DataCenter_SelectedItemChanged();

        storage_pool dataCenter = (storage_pool) getDataCenter().getSelectedItem();
        if (dataCenter != null) {
            getDisksAllocationModel().setQuotaEnforcementType(dataCenter.getQuotaEnforcementType());
        }
    }

    private void Template_SelectedItemChanged(Object sender, EventArgs args)
    {
        behavior.Template_SelectedItemChanged();
    }

    private void Cluster_SelectedItemChanged(Object sender, EventArgs args)
    {
        behavior.Cluster_SelectedItemChanged();
    }

    private void TimeZone_SelectedItemChanged(Object sender, EventArgs args)
    {
    }

    private void DefaultHost_SelectedItemChanged(Object sender, EventArgs args)
    {
        behavior.DefaultHost_SelectedItemChanged();
    }

    private void OSType_SelectedItemChanged(Object sender, EventArgs args)
    {
        VmOsType osType = (VmOsType) getOSType().getSelectedItem();

        setIsWindowsOS(DataProvider.IsWindowsOsType(osType));
        setIsLinux_Unassign_UnknownOS(DataProvider.IsLinuxOsType(osType) || osType == VmOsType.Unassigned
            || osType == VmOsType.Other);

        getInitrd_path().setIsChangable(getIsLinux_Unassign_UnknownOS());
        getInitrd_path().setIsAvailable(getIsLinux_Unassign_UnknownOS());

        getKernel_path().setIsChangable(getIsLinux_Unassign_UnknownOS());
        getKernel_path().setIsAvailable(getIsLinux_Unassign_UnknownOS());

        getKernel_parameters().setIsChangable(getIsLinux_Unassign_UnknownOS());
        getKernel_parameters().setIsAvailable(getIsLinux_Unassign_UnknownOS());

        getDomain().setIsChangable(getIsWindowsOS());
        getDomain().setIsAvailable(getIsWindowsOS());

        getTimeZone().setIsChangable(getIsWindowsOS());
        getTimeZone().setIsAvailable(getIsWindowsOS());
    }

    private void FirstBootDevice_SelectedItemChanged(Object sender, EventArgs args)
    {
        EntityModel entityModel = (EntityModel) getFirstBootDevice().getSelectedItem();
        BootSequence firstDevice = (BootSequence) entityModel.getEntity();

        ArrayList<EntityModel> list = new ArrayList<EntityModel>();
        for (Object item : getFirstBootDevice().getItems())
        {
            EntityModel a = (EntityModel) item;
            if ((BootSequence) a.getEntity() != firstDevice)
            {
                list.add(a);
            }
        }

        EntityModel tempVar = new EntityModel();
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().noneTitle());
        EntityModel noneOption = tempVar;

        list.add(0, noneOption);

        getSecondBootDevice().setItems(list);
        getSecondBootDevice().setSelectedItem(noneOption);
    }

    private void Provisioning_SelectedItemChanged(Object sender, EventArgs args)
    {
        behavior.Provisioning_SelectedItemChanged();
    }

    private void DisplayProtocol_SelectedItemChanged(Object sender, EventArgs args)
    {
        EntityModel entityModel = (EntityModel) getDisplayProtocol().getSelectedItem();
        if (entityModel == null)
        {
            return;
        }
        DisplayType type = (DisplayType) entityModel.getEntity();

        if (type == DisplayType.vnc)
        {
            getUsbPolicy().setSelectedItem(org.ovirt.engine.core.common.businessentities.UsbPolicy.Disabled);
        }

        getUsbPolicy().setIsChangable(type == DisplayType.qxl);

        UpdateNumOfMonitors();
    }

    private void MemSize_EntityChanged(Object sender, EventArgs args)
    {
        behavior.UpdateMinAllocatedMemory();
    }

    private void NumOfSockets_EntityChanged(Object sender, EventArgs args)
    {
        behavior.UpdateTotalCpus();
    }

    private void RunVMOnSpecificHost_EntityChanged(Object sender, EventArgs args)
    {
        if ((Boolean) getRunVMOnSpecificHost().getEntity() == true)
        {
            getDontMigrateVM().setEntity(true);
            getDontMigrateVM().setIsChangable(false);
        }
        else
        {
            getDontMigrateVM().setIsChangable(true);
        }
    }

    private void IsAutoAssign_EntityChanged(Object sender, EventArgs args)
    {
        if ((Boolean) getIsAutoAssign().getEntity() == true)
        {
            getRunVMOnSpecificHost().setEntity(false);
            getRunVMOnSpecificHost().setIsChangable(false);
        }
        else
        {
            getRunVMOnSpecificHost().setIsChangable(true);
        }
    }

    private void IsTemplatePrivate_EntityChanged(Object sender, EventArgs args)
    {
        getIsTemplatePublic().setEntity(!(Boolean) getIsTemplatePrivate().getEntity());
    }

    private void UpdateNumOfMonitors()
    {
        boolean isVnc = false;

        if (getDisplayProtocol().getSelectedItem() != null)
        {
            DisplayType displayType = (DisplayType) ((EntityModel) getDisplayProtocol().getSelectedItem()).getEntity();
            isVnc = displayType == DisplayType.vnc;
        }

        if (isVnc)
        {
            getNumOfMonitors().setSelectedItem(1);
            getNumOfMonitors().setIsChangable(false);
        } else {
            getNumOfMonitors().setIsChangable(true);
        }
    }

    public BootSequence getBootSequence()
    {
        EntityModel firstSelectedItem = (EntityModel) getFirstBootDevice().getSelectedItem();
        EntityModel secondSelectedItem = (EntityModel) getSecondBootDevice().getSelectedItem();

        String firstSelectedString =
                firstSelectedItem.getEntity() == null ? "" : firstSelectedItem.getEntity().toString(); //$NON-NLS-1$
        String secondSelectedString =
                secondSelectedItem.getEntity() == null ? "" : secondSelectedItem.getEntity().toString(); //$NON-NLS-1$

        return BootSequence.valueOf(firstSelectedString + secondSelectedString);
    }

    public void setBootSequence(BootSequence value)
    {
        ArrayList<BootSequence> items = new ArrayList<BootSequence>();
        for (char a : value.toString().toCharArray())
        {
            items.add(BootSequence.valueOf((new Character(a)).toString()));
        }

        Object firstBootDevice = null;
        for (Object item : getFirstBootDevice().getItems())
        {
            EntityModel a = (EntityModel) item;
            if ((BootSequence) a.getEntity() == Linq.FirstOrDefault(items))
            {
                firstBootDevice = a;
            }
        }
        getFirstBootDevice().setSelectedItem(firstBootDevice);

        ArrayList<EntityModel> secondDeviceOptions =
                Linq.<EntityModel> Cast(getSecondBootDevice().getItems());

        if (items.size() > 1)
        {
            BootSequence last = items.get(items.size() - 1);
            for (EntityModel a : secondDeviceOptions)
            {
                if (a.getEntity() != null && (BootSequence) a.getEntity() == last)
                {
                    getSecondBootDevice().setSelectedItem(a);
                    break;
                }
            }
        }
        else
        {
            for (EntityModel a : secondDeviceOptions)
            {
                if (a.getEntity() == null)
                {
                    getSecondBootDevice().setSelectedItem(a);
                    break;
                }
            }
        }
    }

    public void SetDataCenter(UnitVmModel model, ArrayList<storage_pool> list)
    {
        if (model.getBehavior().getSystemTreeSelectedItem() != null
                && model.getBehavior().getSystemTreeSelectedItem().getType() != SystemTreeItemType.System)
        {
            switch (model.getBehavior().getSystemTreeSelectedItem().getType())
            {
            case DataCenter:
                storage_pool selectDataCenter = (storage_pool) model.getBehavior().getSystemTreeSelectedItem().getEntity();
                model.getDataCenter().setItems(new ArrayList<storage_pool>(Arrays.asList(new storage_pool[] {selectDataCenter})));
                model.getDataCenter().setSelectedItem(selectDataCenter);
                model.getDataCenter().setIsChangable(false);
                model.getDataCenter().setInfo("Cannot choose Data Center in tree context"); //$NON-NLS-1$
                break;
            case Cluster:
            case VMs:
                VDSGroup cluster = (VDSGroup) model.getBehavior().getSystemTreeSelectedItem().getEntity();
                for (storage_pool dc : list)
                {
                    if (dc.getId().equals(cluster.getstorage_pool_id()))
                    {
                        model.getDataCenter().setItems(new ArrayList<storage_pool>(Arrays.asList(new storage_pool[] {dc})));
                        model.getDataCenter().setSelectedItem(dc);
                        break;
                    }
                }
                model.getDataCenter().setIsChangable(false);
                model.getDataCenter().setInfo("Cannot choose Data Center in tree context"); //$NON-NLS-1$
                break;
            case Host:
                VDS host = (VDS) model.getBehavior().getSystemTreeSelectedItem().getEntity();
                for (storage_pool dc : list)
                {
                    if (dc.getId().equals(host.getstorage_pool_id()))
                    {
                        model.getDataCenter().setItems(new ArrayList<storage_pool>(Arrays.asList(new storage_pool[] {dc})));
                        model.getDataCenter().setSelectedItem(dc);
                        model.getDataCenter().setIsChangable(false);
                        model.getDataCenter().setInfo("Cannot choose Data Center in tree context"); //$NON-NLS-1$
                        break;
                    }
                }
                break;
            case Storage:
                storage_domains storage = (storage_domains) model.getBehavior().getSystemTreeSelectedItem().getEntity();
                for (storage_pool dc : list)
                {
                    if (dc.getId().equals(storage.getstorage_pool_id()))
                    {
                        model.getDataCenter().setItems(new ArrayList<storage_pool>(Arrays.asList(new storage_pool[] {dc})));
                        model.getDataCenter().setSelectedItem(dc);
                        model.getDataCenter().setIsChangable(false);
                        model.getDataCenter().setInfo("Cannot choose Data Center in tree context"); //$NON-NLS-1$
                        break;
                    }
                }
                break;
            default:
                break;
            }
        }
        else
        {
            model.getDataCenter().setItems(list);
            model.getDataCenter().setSelectedItem(Linq.FirstOrDefault(list));
        }
    }

    public void SetClusters(UnitVmModel model, ArrayList<VDSGroup> clusters, NGuid clusterGuid)
    {
        VmModelBehaviorBase behavior = model.getBehavior();
        if (behavior.getSystemTreeSelectedItem() != null
                && behavior.getSystemTreeSelectedItem().getType() != SystemTreeItemType.System)
        {
            switch (model.getBehavior().getSystemTreeSelectedItem().getType())
            {
            case Cluster:
            case VMs:
                VDSGroup cluster = (VDSGroup) behavior.getSystemTreeSelectedItem().getEntity();
                model.getCluster().setItems(new ArrayList<VDSGroup>(Arrays.asList(new VDSGroup[] {cluster})));
                model.getCluster().setSelectedItem(cluster);
                model.getCluster().setIsChangable(false);
                model.getCluster().setInfo("Cannot choose Cluster in tree context"); //$NON-NLS-1$
                break;
            case Host:
                VDS host = (VDS) behavior.getSystemTreeSelectedItem().getEntity();
                for (VDSGroup iterCluster : clusters)
                {
                    if (iterCluster.getId().equals(host.getvds_group_id()))
                    {
                        model.getCluster().setItems(new ArrayList<VDSGroup>(Arrays.asList(new VDSGroup[] {iterCluster})));
                        model.getCluster().setSelectedItem(iterCluster);
                        model.getCluster().setIsChangable(false);
                        model.getCluster().setInfo("Cannot choose Cluster in tree context"); //$NON-NLS-1$
                        break;
                    }
                }
                break;
            default:
                model.getCluster().setItems(clusters);
                if (clusterGuid == null)
                {
                    model.getCluster().setSelectedItem(Linq.FirstOrDefault(clusters));
                }
                else
                {
                    model.getCluster().setSelectedItem(Linq.FirstOrDefault(clusters,
                            new Linq.ClusterPredicate((Guid) clusterGuid)));
                }
                break;
            }
        }
        else
        {

            model.getCluster().setItems(clusters);
            if (clusterGuid == null)
            {
                model.getCluster().setSelectedItem(Linq.FirstOrDefault(clusters));
            }
            else
            {
                model.getCluster().setSelectedItem(Linq.FirstOrDefault(clusters,
                        new Linq.ClusterPredicate((Guid) clusterGuid)));
            }
        }
    }

    private LengthValidation createNameLengthValidation(VmOsType osType) {
        // for template dialog, name max length is 40 chars
        if (this.getBehavior() instanceof TemplateVmModelBehavior) {
            return new LengthValidation(40);
        }

        // for VM it depends on the OS type
        return new LengthValidation(DataProvider.IsWindowsOsType(osType) ? WINDOWS_VM_NAME_MAX_LIMIT
                : NON_WINDOWS_VM_NAME_MAX_LIMIT);
    }

    public boolean Validate()
    {
        getDataCenter().ValidateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        getCluster().ValidateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        getMemSize().ValidateEntity(new IValidation[] { new ByteSizeValidation() });
        getMinAllocatedMemory().ValidateEntity(new IValidation[] { new ByteSizeValidation() });
        getOSType().ValidateSelectedItem(new NotEmptyValidation[] { new NotEmptyValidation() });

        getDescription().ValidateEntity(new IValidation[] { new AsciiOrNoneValidation() });
        if (getOSType().getIsValid())
        {
            VmOsType osType = (VmOsType) getOSType().getSelectedItem();

            String nameExpr;
            String nameMsg;
            if (DataProvider.IsWindowsOsType(osType))
            {
                nameExpr = "^[0-9a-zA-Z-_]{1," + WINDOWS_VM_NAME_MAX_LIMIT + "}$"; //$NON-NLS-1$ //$NON-NLS-2$
                nameMsg =
                        ConstantsManager.getInstance()
                                .getMessages()
                                .nameMustConataionOnlyAlphanumericChars(WINDOWS_VM_NAME_MAX_LIMIT);
            }
            else
            {
                nameExpr = "^[-\\w]{1," + NON_WINDOWS_VM_NAME_MAX_LIMIT + "}$"; //$NON-NLS-1$ //$NON-NLS-2$
                nameMsg =
                        ConstantsManager.getInstance()
                                .getMessages()
                                .nameCannotContainBlankOrSpecialChars(NON_WINDOWS_VM_NAME_MAX_LIMIT);
            }

            getName().ValidateEntity(
                new IValidation[] {
                    new NotEmptyValidation(),
                    new LengthValidation(this.getBehavior() instanceof TemplateVmModelBehavior ? 40 : 64),
                    new RegexValidation(nameExpr, nameMsg)
                });

            getDescription().ValidateEntity(
                new IValidation[] {
                    new LengthValidation(255)
                });

            boolean is64OsType =
                    (osType == VmOsType.Other || osType == VmOsType.OtherLinux || DataProvider.Is64bitOsType(osType));
            int maxMemSize = is64OsType ? get_MaxMemSize64() : get_MaxMemSize32();

            ValidateMemorySize(getMemSize(), maxMemSize, _minMemSize);
            if (!(this.getBehavior() instanceof TemplateVmModelBehavior))
            {
                // Minimum 'Physical Memory Guaranteed' is 1MB
                ValidateMemorySize(getMinAllocatedMemory(), (Integer) getMemSize().getEntity(), 1);
            }
        }

        if ((Boolean) getIsAutoAssign().getEntity() == false)
        {
            getDefaultHost().ValidateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        }
        else
        {
            getDefaultHost().setIsValid(true);
        }

        VmTemplate template = (VmTemplate) getTemplate().getSelectedItem();
        storage_domains storageDomain = (storage_domains) getStorageDomain().getSelectedItem();

        getTemplate().ValidateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        getDisksAllocationModel().ValidateEntity(new IValidation[] {});

        getCdImage().setIsValid(true);
        if (getCdImage().getIsChangable())
        {
            getCdImage().ValidateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        }

        getKernel_path().setIsValid(true);
        getKernel_parameters().setIsValid(true);
        getInitrd_path().setIsValid(true);
        if (getKernel_path().getEntity() == null)
        {
            getKernel_path().setEntity(""); //$NON-NLS-1$
        }
        if (getKernel_parameters().getEntity() == null)
        {
            getKernel_parameters().setEntity(""); //$NON-NLS-1$
        }
        if (getInitrd_path().getEntity() == null)
        {
            getInitrd_path().setEntity(""); //$NON-NLS-1$
        }

        if (isLinux_Unassign_UnknownOS
                && ((((String) getKernel_parameters().getEntity()).length() > 0 || ((String) getInitrd_path().getEntity()).length() > 0) && ((String) getKernel_path().getEntity()).length() == 0))
        {
            boolean kernelParamInvalid = false;
            boolean inetdPathInvalid = false;
            if (((String) getKernel_parameters().getEntity()).length() > 0)
            {
                getKernel_parameters().setIsValid(false);
                kernelParamInvalid = true;
            }
            if (((String) getInitrd_path().getEntity()).length() > 0)
            {
                getInitrd_path().setIsValid(false);
                inetdPathInvalid = true;
            }

            String msg =
                    ConstantsManager.getInstance()
                            .getMessages()
                            .invalidPath(kernelParamInvalid ? ConstantsManager.getInstance()
                                    .getConstants()
                                    .kernelInvalid() : "", //$NON-NLS-1$
                                    kernelParamInvalid && inetdPathInvalid ? ConstantsManager.getInstance()
                                            .getConstants()
                                            .or() : "", //$NON-NLS-1$
                                    inetdPathInvalid ? ConstantsManager.getInstance()
                                            .getConstants()
                                            .inetdInvalid() : ""); //$NON-NLS-1$

            getKernel_path().setIsValid(false);
            getInitrd_path().getInvalidityReasons().add(msg);
            getKernel_parameters().getInvalidityReasons().add(msg);
            getKernel_path().getInvalidityReasons().add(msg);
        }

        if (getCluster().getSelectedItem() != null && getCustomProperties().getEntity() != null)
        {
            getCustomProperties().ValidateEntity(new IValidation[] {new CustomPropertyValidation(getCustomPropertiesKeysList().get(((VDSGroup) getCluster().getSelectedItem()).getcompatibility_version()))});
        }

        setIsBootSequenceTabValid(true);
        setIsAllocationTabValid(getIsBootSequenceTabValid());
        setIsDisplayTabValid(getIsAllocationTabValid());
        setIsFirstRunTabValid(getIsDisplayTabValid());
        setIsGeneralTabValid(getIsFirstRunTabValid());

        setIsGeneralTabValid(getName().getIsValid() && getDescription().getIsValid() && getDataCenter().getIsValid()
                && getTemplate().getIsValid() && getCluster().getIsValid() && getMemSize().getIsValid()
                && getMinAllocatedMemory().getIsValid());

        setIsFirstRunTabValid(getDomain().getIsValid() && getTimeZone().getIsValid());
        setIsDisplayTabValid(getUsbPolicy().getIsValid() && getNumOfMonitors().getIsValid());
        setIsHostTabValid(getDefaultHost().getIsValid());
        setIsAllocationTabValid(getDisksAllocationModel().getIsValid() && getMinAllocatedMemory().getIsValid());
        setIsBootSequenceTabValid(getCdImage().getIsValid() && getKernel_path().getIsValid());
        setIsCustomPropertiesTabValid(getCustomProperties().getIsValid());

        return getName().getIsValid() && getDescription().getIsValid() && getDataCenter().getIsValid()
                && getDisksAllocationModel().getIsValid() && getTemplate().getIsValid() && getCluster().getIsValid()
                && getDefaultHost().getIsValid() && getMemSize().getIsValid() && getMinAllocatedMemory().getIsValid()
                && getNumOfMonitors().getIsValid() && getDomain().getIsValid() && getUsbPolicy().getIsValid()
                && getTimeZone().getIsValid() && getOSType().getIsValid() && getCdImage().getIsValid()
                && getKernel_path().getIsValid() && getCustomProperties().getIsValid() && behavior.Validate();
    }

    private void ValidateMemorySize(EntityModel model, int maxMemSize, int minMemSize)
    {
        boolean isValid = false;

        int memSize = (Integer) model.getEntity();

        if (memSize == 0)
        {
            model.getInvalidityReasons().add(ConstantsManager.getInstance()
                    .getMessages()
                    .memSizeBetween(minMemSize, maxMemSize));
        }
        else if (memSize > maxMemSize)
        {
            model.getInvalidityReasons().add(ConstantsManager.getInstance()
                    .getMessages()
                    .maxMemSizeIs(maxMemSize));
        }
        else if (memSize < minMemSize)
        {
            model.getInvalidityReasons().add(ConstantsManager.getInstance()
                    .getMessages()
                    .minMemSizeIs(minMemSize));
        }
        else
        {
            isValid = true;
        }

        model.setIsValid(isValid);
    }

    private ListModel poolType;

    public ListModel getPoolType()
    {
        return poolType;
    }

    protected void setPoolType(ListModel value)
    {
        poolType = value;
    }

    private EntityModel numOfDesktops;

    public EntityModel getNumOfDesktops()
    {
        return numOfDesktops;
    }

    protected void setNumOfDesktops(EntityModel value)
    {
        numOfDesktops = value;
    }

    private EntityModel assignedVms;

    public EntityModel getAssignedVms()
    {
        return assignedVms;
    }

    public void setAssignedVms(EntityModel value)
    {
        assignedVms = value;
    }

    private boolean isPoolTabValid;

    public boolean getIsPoolTabValid()
    {
        return isPoolTabValid;
    }

    public void setIsPoolTabValid(boolean value)
    {
        if (isPoolTabValid != value)
        {
            isPoolTabValid = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsPoolTabValid")); //$NON-NLS-1$
        }
    }

    private EntityModel prestartedVms;

    public EntityModel getPrestartedVms() {
        return prestartedVms;
    }

    protected void setPrestartedVms(EntityModel value) {
        prestartedVms = value;
    }

    private String prestartedVmsHint;

    public String getPrestartedVmsHint() {
        return prestartedVmsHint;
    }

    public void setPrestartedVmsHint(String value) {
        if (prestartedVmsHint != value) {
            prestartedVmsHint = value;
            OnPropertyChanged(new PropertyChangedEventArgs("PrestartedVmsHint")); //$NON-NLS-1$
        }
    }
}
