package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;

import org.ovirt.engine.core.common.action.AddDiskParameters;
import org.ovirt.engine.core.common.action.AddVmInterfaceParameters;
import org.ovirt.engine.core.common.action.AttachDettachVmDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.LUNs;
import org.ovirt.engine.core.common.businessentities.LunDisk;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.common.businessentities.NetworkStatus;
import org.ovirt.engine.core.common.businessentities.PropagateErrors;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.VmOsType;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.DataProvider;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.GuideModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

@SuppressWarnings("unused")
public class VmGuideModel extends GuideModel
{
    public final String VmConfigureNetworkInterfacesAction = ConstantsManager.getInstance()
            .getConstants()
            .vmConfigureNetworkInterfacesAction();
    public final String VmAddAnotherNetworkInterfaceAction = ConstantsManager.getInstance()
            .getConstants()
            .vmAddAnotherNetworkInterfaceAction();
    public final String VmConfigureVirtualDisksAction = ConstantsManager.getInstance()
            .getConstants()
            .vmConfigureVirtualDisksAction();
    public final String VmAddAnotherVirtualDiskAction = ConstantsManager.getInstance()
            .getConstants()
            .vmAddAnotherVirtualDiskAction();

    private ArrayList<VmNetworkInterface> nics;
    private ArrayList<Disk> disks;
    private ArrayList<Network> networks;
    private storage_domains storage;
    private VDSGroup cluster;
    private boolean isHotPlugSupported;

    @Override
    public VM getEntity()
    {
        return (VM) super.getEntity();
    }

    protected void updateIsHotPlugAvailable()
    {
        if (getEntity() == null)
        {
            return;
        }
        VM vm = getEntity();
        Version clusterCompatibilityVersion = vm.getvds_group_compatibility_version() != null
                ? vm.getvds_group_compatibility_version() : new Version();

        isHotPlugSupported =
                (Boolean) AsyncDataProvider.GetConfigValuePreConverted(ConfigurationValues.HotPlugEnabled,
                        clusterCompatibilityVersion.toString());
    }

    @Override
    public void setEntity(Object value) {
        super.setEntity(value);
        updateIsHotPlugAvailable();
    }

    @Override
    protected void OnEntityChanged()
    {
        super.OnEntityChanged();
        UpdateOptions();
    }

    private void UpdateOptionsData() {
        nics = null;
        disks = null;
        AsyncDataProvider.GetVmNicList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object target, Object returnValue) {
                        VmGuideModel vmGuideModel = (VmGuideModel) target;
                        ArrayList<VmNetworkInterface> nics =
                                (ArrayList<VmNetworkInterface>) returnValue;
                        vmGuideModel.nics = nics;
                        vmGuideModel.UpdateOptionsPostData();
                    }
                }), getEntity().getId());

        AsyncDataProvider.GetVmDiskList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object target, Object returnValue) {
                        VmGuideModel vmGuideModel = (VmGuideModel) target;
                        ArrayList<Disk> disks = (ArrayList<Disk>) returnValue;
                        vmGuideModel.disks = disks;
                        vmGuideModel.UpdateOptionsPostData();
                    }
                }), getEntity().getId());
    }

    private void UpdateOptionsPostData() {
        if (nics == null || disks == null) {
            return;
        }

        // Add NIC action.
        UICommand addNicAction = new UICommand("AddNetwork", this); //$NON-NLS-1$

        if (nics.isEmpty())
        {
            addNicAction.setTitle(VmConfigureNetworkInterfacesAction);
            getCompulsoryActions().add(addNicAction);
        }
        else
        {
            addNicAction.setTitle(VmAddAnotherNetworkInterfaceAction);
            getOptionalActions().add(addNicAction);
        }

        // Add disk action.
        UICommand addDiskAction = new UICommand("AddDisk", this); //$NON-NLS-1$

        if (disks.isEmpty())
        {
            addDiskAction.setTitle(VmConfigureVirtualDisksAction);
            getCompulsoryActions().add(addDiskAction);
        }
        else
        {
            int ideDiskCount = 0;
            for (Disk a : disks)
            {
                if (a.getDiskInterface() == DiskInterface.IDE)
                {
                    ideDiskCount++;
                }

            }
            if (!(getEntity().getvm_os() == VmOsType.WindowsXP && ideDiskCount > 2))
            {
                addDiskAction.setTitle(VmAddAnotherVirtualDiskAction);
                getOptionalActions().add(addDiskAction);
            }
        }

        StopProgress();
    }

    private void UpdateOptions()
    {
        getCompulsoryActions().clear();
        getOptionalActions().clear();

        if (getEntity() != null)
        {
            StartProgress(null);

            UpdateOptionsData();
        }
    }

    private void ResetData() {
        nics = null;
        disks = null;
        networks = null;
        storage = null;
        cluster = null;
    }

    private void AddNetworkUpdateData() {
        nics = null;
        networks = null;
        AsyncDataProvider.GetVmNicList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object target, Object returnValue) {
                        VmGuideModel vmGuideModel = (VmGuideModel) target;
                        ArrayList<VmNetworkInterface> nics =
                                (ArrayList<VmNetworkInterface>) returnValue;
                        vmGuideModel.nics = nics;
                        vmGuideModel.AddNetworkPostData();
                    }
                }), getEntity().getId());

        AsyncDataProvider.GetClusterNetworkList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object target, Object returnValue) {
                        VmGuideModel vmGuideModel = (VmGuideModel) target;
                        ArrayList<Network> networks = (ArrayList<Network>) returnValue;
                        vmGuideModel.networks = networks;
                        vmGuideModel.AddNetworkPostData();
                    }
                }), getEntity().getvds_group_id());
    }

    private void AddNetworkPostData() {
        if (nics == null || networks == null) {
            return;
        }

        int nicCount = nics.size();
        String newNicName = DataProvider.GetNewNicName(nics);

        ArrayList<Network> operationalNetworks = new ArrayList<Network>();
        for (Network a : networks)
        {
            if (a.getCluster().getstatus() == NetworkStatus.Operational && a.isVmNetwork())
            {
                operationalNetworks.add(a);
            }
        }

        VmInterfaceModel model = new VmInterfaceModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().newNetworkInterfaceTitle());
        model.setHashName("new_network_interface_vms_guide"); //$NON-NLS-1$
        model.setIsNew(true);
        model.getNetwork().setItems(operationalNetworks);
        model.getNetwork().setSelectedItem(operationalNetworks.size() > 0 ? operationalNetworks.get(0) : null);
        model.getNicType().setItems(DataProvider.GetNicTypeList(getEntity().getvm_os(), false));
        model.getNicType().setSelectedItem(DataProvider.GetDefaultNicType(getEntity().getvm_os()));
        model.getName().setEntity(newNicName);
        model.getMAC().setIsChangable(false);

        model.getPlugged().setIsChangable(isHotPlugSupported);
        model.getPlugged().setEntity(true);

        Version v31 = new Version(3, 1);
        boolean isLessThan31 = getEntity().getvds_group_compatibility_version().compareTo(v31) < 0;

        model.getPortMirroring().setIsChangable(!isLessThan31);
        model.getPortMirroring().setEntity(false);

        UICommand tempVar = new UICommand("OnAddNetwork", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);

        StopProgress();
    }

    public void AddNetwork()
    {
        if (getEntity() != null)
        {
            StartProgress(null);

            AddNetworkUpdateData();
        }
    }

    private void OnAddNetwork()
    {
        if (getEntity() != null)
        {
            VmInterfaceModel model = (VmInterfaceModel) getWindow();

            if (model.getProgress() != null)
            {
                return;
            }

            if (!model.Validate())
            {
                return;
            }

            // Save changes.
            Integer _type;
            if (model.getNicType().getSelectedItem() == null)
            {
                _type = null;
            }
            else
            {
                _type = ((VmInterfaceType) model.getNicType().getSelectedItem()).getValue();
            }

            VmNetworkInterface vmNetworkInterface = new VmNetworkInterface();
            vmNetworkInterface.setName((String) model.getName().getEntity());
            vmNetworkInterface.setNetworkName(((Network) model.getNetwork().getSelectedItem()).getname());
            vmNetworkInterface.setType(_type);
            vmNetworkInterface.setMacAddress(model.getMAC().getIsChangable() ? (model.getMAC().getEntity() == null ? null
                    : ((String) (model.getMAC().getEntity())).toLowerCase())
                    : ""); //$NON-NLS-1$

            vmNetworkInterface.setActive((Boolean) model.getPlugged().getEntity());
            vmNetworkInterface.setPortMirroring((Boolean) model.getPortMirroring().getEntity());

            AddVmInterfaceParameters parameters =
                    new AddVmInterfaceParameters(getEntity().getId(), vmNetworkInterface);

            model.StartProgress(null);

            Frontend.RunAction(VdcActionType.AddVmInterface, parameters,
                    new IFrontendActionAsyncCallback() {
                        @Override
                        public void Executed(FrontendActionAsyncResult result) {

                            VmGuideModel vmGuideModel = (VmGuideModel) result.getState();
                            vmGuideModel.getWindow().StopProgress();
                            VdcReturnValueBase returnValueBase = result.getReturnValue();
                            if (returnValueBase != null && returnValueBase.getSucceeded())
                            {
                                vmGuideModel.Cancel();
                                vmGuideModel.PostAction();
                            }

                        }
                    }, this);
        }
        else
        {
            Cancel();
        }
    }

    private void AddDiskUpdateData() {
        AsyncDataProvider.GetVmDiskList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object target, Object returnValue) {
                        VmGuideModel vmGuideModel = (VmGuideModel) target;
                        ArrayList<Disk> disks = (ArrayList<Disk>) returnValue;
                        vmGuideModel.disks = disks;

                        vmGuideModel.AddDiskPostData();
                    }
                }), getEntity().getId());
    }

    private void AddDiskPostData() {
        if (disks == null) {
            return;
        }

        final DiskModel model = new DiskModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().newVirtualDiskTitle());
        model.setHashName("new_virtual_disk"); //$NON-NLS-1$
        model.setIsNew(true);
        model.setDatacenterId(getEntity().getstorage_pool_id());
        model.getIsInVm().setEntity(true);
        model.getIsInternal().setEntity(true);
        model.setVmId(getEntity().getId());

        boolean hasBootableDisk = false;
        for (Disk a : disks)
        {
            if (a.isBoot())
            {
                hasBootableDisk = true;
                break;
            }
        }
        model.getIsBootable().setEntity(!hasBootableDisk);
        if (hasBootableDisk)
        {
            model.getIsBootable().setIsChangable(false);
            model.getIsBootable().getChangeProhibitionReasons().add("There can be only one bootable disk defined."); //$NON-NLS-1$
        }

        AsyncDataProvider.GetNextAvailableDiskAliasNameByVMId(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object model, Object returnValue) {
                        String suggestedDiskName = (String) returnValue;
                        VmGuideModel vmGuideModel = (VmGuideModel) model;
                        vmGuideModel.StopProgress();

                        DiskModel diskModel = (DiskModel) vmGuideModel.getWindow();
                        diskModel.getAlias().setEntity(suggestedDiskName);

                        UICommand tempVar2 = new UICommand("OnAddDisk", vmGuideModel); //$NON-NLS-1$
                        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().ok());
                        tempVar2.setIsDefault(true);
                        diskModel.getCommands().add(tempVar2);

                        UICommand tempVar3 = new UICommand("Cancel", vmGuideModel); //$NON-NLS-1$
                        tempVar3.setTitle(ConstantsManager.getInstance().getConstants().cancel());
                        tempVar3.setIsCancel(true);
                        diskModel.getCommands().add(tempVar3);
                    }
                }), getEntity().getId());
    }

    public void AddDisk()
    {
        if (getEntity() != null)
        {
            StartProgress(null);
            disks = null;

            AddDiskUpdateData();
        }
    }

    public void OnAddDisk()
    {
        if (getEntity() != null)
        {
            DiskModel model = (DiskModel) getWindow();

            if (model.getProgress() != null)
            {
                return;
            }

            if (!model.Validate())
            {
                return;
            }

            if ((Boolean) model.getAttachDisk().getEntity())
            {
                OnAttachDisks();
                return;
            }

            // Save changes.
            storage_domains storageDomain = (storage_domains) model.getStorageDomain().getSelectedItem();

            Disk disk;
            if ((Boolean) model.getIsInternal().getEntity()) {
                DiskImage diskImage = new DiskImage();
                diskImage.setSizeInGigabytes(Integer.parseInt(model.getSize().getEntity().toString()));
                diskImage.setvolume_type((VolumeType) model.getVolumeType().getSelectedItem());
                diskImage.setvolume_format(model.getVolumeFormat());
                if (model.getQuota().getIsAvailable()) {
                    diskImage.setQuotaId(((Quota) model.getQuota().getSelectedItem()).getId());
                }

                disk = diskImage;
            }
            else {
                LunDisk lunDisk = new LunDisk();
                lunDisk.setLun((LUNs) model.getSanStorageModel().getAddedLuns().get(0).getEntity());

                disk = lunDisk;
            }

            disk.setDiskAlias((String) model.getAlias().getEntity());
            disk.setDiskDescription((String) model.getDescription().getEntity());
            disk.setDiskInterface((DiskInterface) model.getInterface().getSelectedItem());
            disk.setWipeAfterDelete((Boolean) model.getWipeAfterDelete().getEntity());
            disk.setBoot((Boolean) model.getIsBootable().getEntity());
            disk.setShareable((Boolean) model.getIsShareable().getEntity());
            disk.setPlugged((Boolean) model.getIsPlugged().getEntity());
            disk.setPropagateErrors(PropagateErrors.Off);

            model.StartProgress(null);

            AddDiskParameters tempVar2 = new AddDiskParameters(getEntity().getId(), disk);
            tempVar2.setStorageDomainId(storageDomain.getId());
            Frontend.RunAction(VdcActionType.AddDisk, tempVar2,
                    new IFrontendActionAsyncCallback() {
                        @Override
                        public void Executed(FrontendActionAsyncResult result) {

                            VmGuideModel vmGuideModel = (VmGuideModel) result.getState();
                            vmGuideModel.getWindow().StopProgress();
                            VdcReturnValueBase returnValueBase = result.getReturnValue();
                            if (returnValueBase != null && returnValueBase.getSucceeded())
                            {
                                vmGuideModel.Cancel();
                                vmGuideModel.PostAction();
                            }

                        }
                    }, this);
        }
        else
        {
            Cancel();
        }
    }

    private void OnAttachDisks()
    {
        VM vm = getEntity();
        DiskModel model = (DiskModel) getWindow();
        ArrayList<VdcActionParametersBase> paramerterList = new ArrayList<VdcActionParametersBase>();

        ArrayList<EntityModel> disksToAttach = (Boolean) model.getIsInternal().getEntity() ?
                (ArrayList<EntityModel>) model.getInternalAttachableDisks().getSelectedItems() :
                (ArrayList<EntityModel>) model.getExternalAttachableDisks().getSelectedItems();

        for (EntityModel item : disksToAttach)
        {
            DiskModel disk = (DiskModel) item.getEntity();
            AttachDettachVmDiskParameters parameters = new AttachDettachVmDiskParameters(
                    vm.getId(), disk.getDisk().getId(), (Boolean) model.getIsPlugged().getEntity());
            paramerterList.add(parameters);
        }

        model.StartProgress(null);

        Frontend.RunMultipleAction(VdcActionType.AttachDiskToVm, paramerterList,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void Executed(FrontendMultipleActionAsyncResult result) {
                        VmGuideModel localModel = (VmGuideModel) result.getState();
                        localModel.getWindow().StopProgress();
                        Cancel();
                    }
                },
                this);
    }

    public void PostAction()
    {
        ResetData();
        UpdateOptions();
    }

    public void Cancel()
    {
        ResetData();
        setWindow(null);
    }

    @Override
    public void ExecuteCommand(UICommand command)
    {
        super.ExecuteCommand(command);

        if (StringHelper.stringsEqual(command.getName(), "AddNetwork")) //$NON-NLS-1$
        {
            AddNetwork();
        }
        if (StringHelper.stringsEqual(command.getName(), "AddDisk")) //$NON-NLS-1$
        {
            AddDisk();
        }
        if (StringHelper.stringsEqual(command.getName(), "OnAddNetwork")) //$NON-NLS-1$
        {
            OnAddNetwork();
        }
        if (StringHelper.stringsEqual(command.getName(), "OnAddDisk")) //$NON-NLS-1$
        {
            OnAddDisk();
        }
        if (StringHelper.stringsEqual(command.getName(), "Cancel")) //$NON-NLS-1$
        {
            Cancel();
        }
    }
}
