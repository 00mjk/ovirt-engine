package org.ovirt.engine.ui.uicommonweb.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.EventDefinition;
import org.ovirt.engine.ui.uicompat.FrontendMultipleQueryAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleQueryAsyncCallback;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class SystemTreeModel extends SearchableListModel implements IFrontendMultipleQueryAsyncCallback {

    public static final EventDefinition resetRequestedEventDefinition;
    private Event privateResetRequestedEvent;

    public Event getResetRequestedEvent()
    {
        return privateResetRequestedEvent;
    }

    private void setResetRequestedEvent(Event value)
    {
        privateResetRequestedEvent = value;
    }

    private UICommand privateResetCommand;

    public UICommand getResetCommand()
    {
        return privateResetCommand;
    }

    private void setResetCommand(UICommand value)
    {
        privateResetCommand = value;
    }

    private UICommand privateExpandAllCommand;

    public UICommand getExpandAllCommand()
    {
        return privateExpandAllCommand;
    }

    private void setExpandAllCommand(UICommand value)
    {
        privateExpandAllCommand = value;
    }

    private UICommand privateCollapseAllCommand;

    public UICommand getCollapseAllCommand()
    {
        return privateCollapseAllCommand;
    }

    private void setCollapseAllCommand(UICommand value)
    {
        privateCollapseAllCommand = value;
    }

    @Override
    public ArrayList<SystemTreeItemModel> getItems()
    {
        return (ArrayList<SystemTreeItemModel>) super.getItems();
    }

    public void setItems(ArrayList<SystemTreeItemModel> value)
    {
        if (items != value)
        {
            itemsChanging(value, items);
            items = value;
            itemsChanged();
            getItemsChangedEvent().raise(this, EventArgs.EMPTY);
            onPropertyChanged(new PropertyChangedEventArgs("Items")); //$NON-NLS-1$
        }
    }

    private ArrayList<StoragePool> privateDataCenters;

    public ArrayList<StoragePool> getDataCenters()
    {
        return privateDataCenters;
    }

    public void setDataCenters(ArrayList<StoragePool> value)
    {
        privateDataCenters = value;
    }

    private List<Provider> privateProviders;

    private List<Provider> getProviders() {
        return privateProviders;
    }

    private void setProviders(List<Provider> value) {
        privateProviders = value;
    }

    private HashMap<Guid, ArrayList<VDSGroup>> privateClusterMap;

    public HashMap<Guid, ArrayList<VDSGroup>> getClusterMap()
    {
        return privateClusterMap;
    }

    public void setClusterMap(HashMap<Guid, ArrayList<VDSGroup>> value)
    {
        privateClusterMap = value;
    }

    private Map<Guid, List<Network>> networkMap;

    public Map<Guid, List<Network>> getNetworkMap()
    {
        return networkMap;
    }

    public void setNetworkMap(Map<Guid, List<Network>> value)
    {
        networkMap = value;
    }

    private HashMap<Guid, ArrayList<VDS>> privateHostMap;

    public HashMap<Guid, ArrayList<VDS>> getHostMap()
    {
        return privateHostMap;
    }

    public void setHostMap(HashMap<Guid, ArrayList<VDS>> value)
    {
        privateHostMap = value;
    }

    private HashMap<Guid, ArrayList<GlusterVolumeEntity>> privateVolumeMap;

    public HashMap<Guid, ArrayList<GlusterVolumeEntity>> getVolumeMap() {
        return privateVolumeMap;
    }

    public void setVolumeMap(HashMap<Guid, ArrayList<GlusterVolumeEntity>> value) {
        privateVolumeMap = value;
    }

    private Map<Guid, SystemTreeItemModel> treeItemById;

    public SystemTreeItemModel getItemById(Guid id) {
        return treeItemById.get(id);
    }

    static
    {
        resetRequestedEventDefinition = new EventDefinition("ResetRequested", SystemTreeModel.class); //$NON-NLS-1$
    }

    public SystemTreeModel()
    {
        setResetRequestedEvent(new Event(resetRequestedEventDefinition));

        setResetCommand(new UICommand("Reset", this)); //$NON-NLS-1$
        setExpandAllCommand(new UICommand("ExpandAll", this)); //$NON-NLS-1$
        setCollapseAllCommand(new UICommand("CollapseAll", this)); //$NON-NLS-1$

        setIsTimerDisabled(true);

        setItems(new ArrayList<SystemTreeItemModel>());
    }

    @Override
    protected void syncSearch()
    {
        super.syncSearch();

        final AsyncQuery dcQuery = new AsyncQuery();
        dcQuery.setModel(this);
        dcQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result)
            {
                final SystemTreeModel systemTreeModel = (SystemTreeModel) model;
                systemTreeModel.setDataCenters((ArrayList<StoragePool>) result);

                AsyncQuery clusterQuery = new AsyncQuery();
                clusterQuery.setModel(systemTreeModel);
                clusterQuery.asyncCallback = new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object model1, Object result1)
                    {
                        SystemTreeModel systemTreeModel1 = (SystemTreeModel) model1;
                        ArrayList<VDSGroup> clusters = (ArrayList<VDSGroup>) result1;

                        systemTreeModel1.setClusterMap(new HashMap<Guid, ArrayList<VDSGroup>>());
                        for (VDSGroup cluster : clusters)
                        {
                            if (cluster.getStoragePoolId() != null)
                            {
                                Guid key = cluster.getStoragePoolId();
                                if (!systemTreeModel1.getClusterMap().containsKey(key))
                                {
                                    systemTreeModel1.getClusterMap().put(key, new ArrayList<VDSGroup>());
                                }
                                ArrayList<VDSGroup> list1 = systemTreeModel1.getClusterMap().get(key);
                                list1.add(cluster);
                            }
                        }
                        AsyncQuery hostQuery = new AsyncQuery();
                        hostQuery.setModel(systemTreeModel1);
                        hostQuery.asyncCallback = new INewAsyncCallback() {
                            @Override
                            public void onSuccess(Object model2, Object result2)
                            {
                                SystemTreeModel systemTreeModel2 = (SystemTreeModel) model2;
                                ArrayList<VDS> hosts = (ArrayList<VDS>) result2;
                                systemTreeModel2.setHostMap(new HashMap<Guid, ArrayList<VDS>>());
                                for (VDS host : hosts)
                                {
                                    Guid key = host.getVdsGroupId();
                                    if (!systemTreeModel2.getHostMap().containsKey(key))
                                    {
                                        systemTreeModel2.getHostMap().put(key, new ArrayList<VDS>());
                                    }
                                    ArrayList<VDS> list = systemTreeModel2.getHostMap().get(key);
                                    list.add(host);
                                }

                                AsyncQuery volumeQuery = new AsyncQuery();
                                volumeQuery.setModel(systemTreeModel2);
                                volumeQuery.asyncCallback = new INewAsyncCallback() {
                                    @Override
                                    public void onSuccess(Object model3, Object result3)
                                    {
                                        SystemTreeModel systemTreeModel3 = (SystemTreeModel) model3;
                                        ArrayList<GlusterVolumeEntity> volumes =
                                                (ArrayList<GlusterVolumeEntity>) result3;
                                        systemTreeModel3.setVolumeMap(new HashMap<Guid, ArrayList<GlusterVolumeEntity>>());

                                        for (GlusterVolumeEntity volume : volumes)
                                        {
                                            Guid key = volume.getClusterId();
                                            if (!systemTreeModel3.getVolumeMap().containsKey(key))
                                            {
                                                systemTreeModel3.getVolumeMap().put(key,
                                                        new ArrayList<GlusterVolumeEntity>());
                                            }
                                            ArrayList<GlusterVolumeEntity> list =
                                                    systemTreeModel3.getVolumeMap().get(key);
                                            list.add(volume);
                                        }


                                        // Networks
                                        ArrayList<VdcQueryType> queryTypeList =
                                                new ArrayList<VdcQueryType>();
                                        ArrayList<VdcQueryParametersBase> queryParamList =
                                                new ArrayList<VdcQueryParametersBase>();

                                        for (StoragePool dataCenter : systemTreeModel.getDataCenters())
                                        {
                                            queryTypeList.add(VdcQueryType.GetAllNetworks);
                                            queryParamList.add(new IdQueryParameters(dataCenter.getId()));
                                        }

                                        Frontend.getInstance().runMultipleQueries(queryTypeList, queryParamList, new IFrontendMultipleQueryAsyncCallback() {

                                            @Override
                                            public void executed(FrontendMultipleQueryAsyncResult result) {

                                                systemTreeModel.setNetworkMap(new HashMap<Guid, List<Network>>());

                                                List<VdcQueryReturnValue> returnValueList = result.getReturnValues();
                                                List<Network> dcNetworkList;
                                                Guid dcId;

                                                for (int i = 0; i < returnValueList.size(); i++)
                                                {
                                                    VdcQueryReturnValue returnValue = returnValueList.get(i);
                                                    if (returnValue.getSucceeded() && returnValue.getReturnValue() != null)
                                                    {
                                                        dcNetworkList = (List<Network>) returnValue.getReturnValue();
                                                        dcId = systemTreeModel.getDataCenters().get(i).getId();
                                                        systemTreeModel.getNetworkMap().put(dcId, dcNetworkList);
                                                    }
                                                }

                                                // Storages
                                                ArrayList<VdcQueryType> queryTypeList =
                                                        new ArrayList<VdcQueryType>();
                                                ArrayList<VdcQueryParametersBase> queryParamList =
                                                        new ArrayList<VdcQueryParametersBase>();

                                                for (StoragePool dataCenter : systemTreeModel.getDataCenters())
                                                {
                                                    queryTypeList.add(VdcQueryType.GetStorageDomainsByStoragePoolId);
                                                    queryParamList.add(new IdQueryParameters(dataCenter.getId()));
                                                }
                                                if ((ApplicationModeHelper.getUiMode().getValue() & ApplicationMode.VirtOnly.getValue()) == 0) {
                                                    FrontendMultipleQueryAsyncResult dummyResult =
                                                            new FrontendMultipleQueryAsyncResult();
                                                    VdcQueryReturnValue value = new VdcQueryReturnValue();
                                                    value.setSucceeded(true);
                                                    dummyResult.getReturnValues().add(value);
                                                    SystemTreeModel.this.executed(dummyResult);
                                                } else {
                                                    Frontend.getInstance().runMultipleQueries(queryTypeList, queryParamList, systemTreeModel);
                                                }
                                            }
                                        });
                                    }
                                };
                                AsyncDataProvider.getVolumeList(volumeQuery, null);
                            }
                        };
                        AsyncDataProvider.getHostList(hostQuery);
                    }
                };
                AsyncDataProvider.getClusterList(clusterQuery);
            }
        };

        AsyncQuery providersQuery = new AsyncQuery();
        providersQuery.setModel(this);
        providersQuery.asyncCallback = new INewAsyncCallback() {

            @SuppressWarnings("unchecked")
            @Override
            public void onSuccess(Object model, Object returnValue) {
                setProviders((List<Provider>) returnValue);
                AsyncDataProvider.getDataCenterList(dcQuery);
            }
        };
        AsyncDataProvider.getAllProviders(providersQuery);
    }

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);

        if (command == getResetCommand())
        {
            reset();
        }
        else if (command == getExpandAllCommand())
        {
            expandAll();
        }
        else if (command == getCollapseAllCommand())
        {
            collapseAll();
        }
    }

    private void collapseAll()
    {
        setIsExpandedRecursively(false, getItems().get(0));
    }

    private void expandAll()
    {
        setIsExpandedRecursively(true, getItems().get(0));
    }

    private void setIsExpandedRecursively(boolean value, SystemTreeItemModel root)
    {
        root.setIsExpanded(value);

        for (SystemTreeItemModel model : root.getChildren())
        {
            setIsExpandedRecursively(value, model);
        }
    }

    private void reset()
    {
        getResetRequestedEvent().raise(this, EventArgs.EMPTY);
    }

    @Override
    public void executed(FrontendMultipleQueryAsyncResult result)
    {
        ArrayList<StorageDomain> storages = null;
        int count = -1;
        treeItemById = new HashMap<Guid, SystemTreeItemModel>();

        // Build tree items.
        SystemTreeItemModel systemItem = new SystemTreeItemModel();
        systemItem.setType(SystemTreeItemType.System);
        systemItem.setIsSelected(true);
        systemItem.setTitle(ConstantsManager.getInstance().getConstants().systemTitle());

        // Add Data Centers node under System
        SystemTreeItemModel dataCentersItem = new SystemTreeItemModel();
        dataCentersItem.setType(SystemTreeItemType.DataCenters);
        dataCentersItem.setApplicationMode(ApplicationMode.VirtOnly);
        dataCentersItem.setTitle(ConstantsManager.getInstance().getConstants().dataCentersTitle());
        systemItem.addChild(dataCentersItem);

        // Populate everything under Data Centers
        for (VdcQueryReturnValue returnValue : result.getReturnValues())
        {
            ++count;
            if (!returnValue.getSucceeded())
            {
                continue;
            }
            storages = (ArrayList<StorageDomain>) returnValue.getReturnValue();

            SystemTreeItemModel dataCenterItem = new SystemTreeItemModel();
            dataCenterItem.setType(SystemTreeItemType.DataCenter);
            dataCenterItem.setApplicationMode(ApplicationMode.VirtOnly);
            dataCenterItem.setTitle(getDataCenters().get(count).getName());
            dataCenterItem.setEntity(getDataCenters().get(count));
            dataCentersItem.addChild(dataCenterItem);

            SystemTreeItemModel storagesItem = new SystemTreeItemModel();
            storagesItem.setType(SystemTreeItemType.Storages);
            storagesItem.setApplicationMode(ApplicationMode.VirtOnly);
            storagesItem.setTitle(ConstantsManager.getInstance().getConstants().storageTitle());
            storagesItem.setEntity(getDataCenters().get(count));
            dataCenterItem.addChild(storagesItem);

            if (storages != null && storages.size() > 0)
            {
                // sort by name first
                Collections.sort(storages, new Linq.StorageDomainComparator());
                for (StorageDomain storage : storages)
                {
                    SystemTreeItemModel storageItem = new SystemTreeItemModel();
                    storageItem.setType(SystemTreeItemType.Storage);
                    storageItem.setApplicationMode(ApplicationMode.VirtOnly);
                    storageItem.setTitle(storage.getStorageName());
                    storageItem.setEntity(storage);
                    storagesItem.addChild(storageItem);
                }
            }

            SystemTreeItemModel networksItem = new SystemTreeItemModel();
            networksItem.setType(SystemTreeItemType.Networks);
            networksItem.setApplicationMode(ApplicationMode.VirtOnly);
            networksItem.setTitle(ConstantsManager.getInstance().getConstants().networksTitle());
            networksItem.setEntity(getDataCenters().get(count));
            dataCenterItem.addChild(networksItem);

            List<Network> dcNetworks = getNetworkMap().get(getDataCenters().get(count).getId());
            if (dcNetworks != null)
            {
                // sort by name first
                Collections.sort(dcNetworks, new Linq.NetworkComparator());
                for (Network network : dcNetworks)
                {
                    SystemTreeItemModel networkItem = new SystemTreeItemModel();
                    networkItem.setType(SystemTreeItemType.Network);
                    networkItem.setApplicationMode(ApplicationMode.VirtOnly);
                    networkItem.setTitle(network.getName());
                    networkItem.setEntity(network);
                    networksItem.addChild(networkItem);
                    treeItemById.put(network.getId(), networkItem);
                }
            }

            SystemTreeItemModel templatesItem = new SystemTreeItemModel();
            templatesItem.setType(SystemTreeItemType.Templates);
            templatesItem.setApplicationMode(ApplicationMode.VirtOnly);
            templatesItem.setTitle(ConstantsManager.getInstance().getConstants().templatesTitle());
            templatesItem.setEntity(getDataCenters().get(count));
            dataCenterItem.addChild(templatesItem);

            SystemTreeItemModel clustersItem = new SystemTreeItemModel();
            clustersItem.setType(SystemTreeItemType.Clusters);
            clustersItem.setTitle(ConstantsManager.getInstance().getConstants().clustersTitle());
            clustersItem.setEntity(getDataCenters().get(count));
            dataCenterItem.addChild(clustersItem);

            if (getClusterMap().containsKey(getDataCenters().get(count).getId()))
            {
                List<VDSGroup> clusters = getClusterMap().get(getDataCenters().get(count).getId());
                Collections.sort(clusters, new Linq.VDSGroupComparator());
                for (VDSGroup cluster : clusters)
                {
                    SystemTreeItemModel clusterItem = new SystemTreeItemModel();
                    clusterItem.setType(cluster.supportsGlusterService() ? SystemTreeItemType.Cluster_Gluster
                            : SystemTreeItemType.Cluster);
                    clusterItem.setTitle(cluster.getName());
                    clusterItem.setEntity(cluster);
                    clustersItem.addChild(clusterItem);

                    SystemTreeItemModel hostsItem = new SystemTreeItemModel();
                    hostsItem.setType(SystemTreeItemType.Hosts);
                    hostsItem.setTitle(ConstantsManager.getInstance().getConstants().hostsTitle());
                    hostsItem.setEntity(cluster);
                    clusterItem.addChild(hostsItem);

                    if (getHostMap().containsKey(cluster.getId()))
                    {
                        for (VDS host : getHostMap().get(cluster.getId()))
                        {
                            SystemTreeItemModel hostItem = new SystemTreeItemModel();
                            hostItem.setType(SystemTreeItemType.Host);
                            hostItem.setTitle(host.getName());
                            hostItem.setEntity(host);
                            hostsItem.addChild(hostItem);
                        }
                    }
                    if (cluster.supportsGlusterService())
                    {
                        SystemTreeItemModel volumesItem = new SystemTreeItemModel();
                        volumesItem.setType(SystemTreeItemType.Volumes);
                        volumesItem.setApplicationMode(ApplicationMode.GlusterOnly);
                        volumesItem.setTitle(ConstantsManager.getInstance().getConstants().volumesTitle());
                        volumesItem.setEntity(cluster);
                        clusterItem.addChild(volumesItem);

                        if (getVolumeMap().containsKey(cluster.getId())) {
                            for (GlusterVolumeEntity volume : getVolumeMap().get(cluster.getId())) {
                                SystemTreeItemModel volumeItem = new SystemTreeItemModel();
                                volumeItem.setType(SystemTreeItemType.Volume);
                                volumeItem.setApplicationMode(ApplicationMode.GlusterOnly);
                                volumeItem.setTitle(volume.getName());
                                volumeItem.setEntity(volume);
                                volumesItem.addChild(volumeItem);
                            }
                        }
                    }

                    if (cluster.supportsVirtService())
                    {
                        SystemTreeItemModel vmsItem = new SystemTreeItemModel();
                        vmsItem.setType(SystemTreeItemType.VMs);
                        vmsItem.setApplicationMode(ApplicationMode.VirtOnly);
                        vmsItem.setTitle(ConstantsManager.getInstance().getConstants().vmsTitle());
                        vmsItem.setEntity(cluster);
                        clusterItem.addChild(vmsItem);
                    }
                }
            }
        }

        // Add Providers node under System
        SystemTreeItemModel providersItem = new SystemTreeItemModel();
        providersItem.setType(SystemTreeItemType.Providers);
        providersItem.setApplicationMode(ApplicationMode.VirtOnly);
        providersItem.setTitle(ConstantsManager.getInstance().getConstants().externalProvidersTitle());
        systemItem.addChild(providersItem);

        // Populate with providers
        for (Provider provider : getProviders()) {
            SystemTreeItemModel providerItem = new SystemTreeItemModel();
            providerItem.setType(SystemTreeItemType.Provider);
            providerItem.setApplicationMode(ApplicationMode.VirtOnly);
            providerItem.setTitle(provider.getName());
            providerItem.setEntity(provider);
            providersItem.addChild(providerItem);
            treeItemById.put(provider.getId(), providerItem);
        }

        if (!ApplicationModeHelper.getUiMode().equals(ApplicationMode.AllModes)) {
            ApplicationModeHelper.filterSystemTreeByApplictionMode(systemItem);
        }
        setItems(new ArrayList<SystemTreeItemModel>(Arrays.asList(new SystemTreeItemModel[] { systemItem })));
    }

    @Override
    protected String getListName() {
        return "SystemTreeModel"; //$NON-NLS-1$
    }

}
