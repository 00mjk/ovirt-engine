package org.ovirt.engine.ui.uicommonweb.models.networks;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ovirt.engine.core.common.action.AddNetworkStoragePoolParameters;
import org.ovirt.engine.core.common.action.AttachNetworkToVdsGroupParameter;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VnicProfileParameters;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.comparators.NameableComparator;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.common.businessentities.network.VnicProfile;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.providers.ExternalNetwork;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;

public class ImportNetworksModel extends Model {

    private static final String CMD_IMPORT = "OnImport"; //$NON-NLS-1$
    private static final String CMD_CANCEL = "Cancel"; //$NON-NLS-1$

    private final SearchableListModel sourceListModel;

    private final ListModel providers = new ListModel();
    private final ListModel providerNetworks = new ListModel();
    private final ListModel importedNetworks = new ListModel();

    private UICommand addImportCommand = new UICommand(null, this);
    private UICommand cancelImportCommand = new UICommand(null, this);

    private Map<Guid, Collection<VDSGroup>> dcClusters;

    public ListModel getProviderNetworks() {
        return providerNetworks;
    }

    public ListModel getImportedNetworks() {
        return importedNetworks;
    }

    public ListModel getProviders() {
        return providers;
    }

    public UICommand getAddImportCommand() {
        return addImportCommand;
    }

    public UICommand getCancelImportCommand() {
        return cancelImportCommand;
    }

    public ImportNetworksModel(SearchableListModel sourceListModel) {
        this.sourceListModel = sourceListModel;

        setTitle(ConstantsManager.getInstance().getConstants().importNetworksTitle());
        setHashName("import_networks"); //$NON-NLS-1$

        UICommand importCommand = new UICommand(CMD_IMPORT, this);
        importCommand.setIsExecutionAllowed(false);
        importCommand.setTitle(ConstantsManager.getInstance().getConstants().importNetworksButton());
        importCommand.setIsDefault(true);
        getCommands().add(importCommand);
        UICommand cancelCommand = new UICommand(CMD_CANCEL, this);
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);
        getCommands().add(cancelCommand);

        providers.getSelectedItemChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                onProviderChosen();
            }
        });

        initProviderList();
    }

    protected void initProviderList() {
        startProgress(null);
        AsyncDataProvider.getAllNetworkProviders(new AsyncQuery(this, new INewAsyncCallback() {

            @Override
            public void onSuccess(Object model, Object returnValue) {
                stopProgress();
                List<Provider> providers = (List<Provider>) returnValue;
                providers.add(0, null);
                getProviders().setItems(providers);
            }
        }));
    }

    private void onProviderChosen() {
        final Provider provider = (Provider) providers.getSelectedItem();
        if (provider == null) {
            return;
        }

        final List<StoragePool> dataCenters = new LinkedList<StoragePool>();

        final AsyncQuery networkQuery = new AsyncQuery();
        networkQuery.asyncCallback = new INewAsyncCallback() {

            @Override
            public void onSuccess(Object model, Object returnValue) {
                Map<Network, Set<Guid>> externalNetworkToDataCenters = (Map<Network, Set<Guid>>) returnValue;
                List<ExternalNetwork> items = new LinkedList<ExternalNetwork>();
                for (Network network : externalNetworkToDataCenters.keySet()) {
                    ExternalNetwork externalNetwork = new ExternalNetwork();
                    externalNetwork.setNetwork(network);
                    externalNetwork.setDisplayName(network.getName());
                    externalNetwork.setPublicUse(true);

                    Set<Guid> attachedDataCenters = externalNetworkToDataCenters.get(network);
                    List<StoragePool> availableDataCenters = new LinkedList<StoragePool>();
                    for (StoragePool dc : dataCenters) {
                        if (!attachedDataCenters.contains(dc.getId())) {
                            availableDataCenters.add(dc);
                        }
                    }
                    externalNetwork.getDataCenters().setItems(availableDataCenters);
                    externalNetwork.getDataCenters().setSelectedItem(Linq.firstOrDefault(availableDataCenters));

                    items.add(externalNetwork);
                }
                Collections.sort(items, new Linq.ExternalNetworkComparator());
                providerNetworks.setItems(items);
                importedNetworks.setItems(new LinkedList<ExternalNetwork>());

                stopProgress();
            }
        };

        final AsyncQuery dcQuery = new AsyncQuery();
        dcQuery.asyncCallback = new INewAsyncCallback() {

            @Override
            public void onSuccess(Object model, Object returnValue) {
                dataCenters.addAll((Collection<StoragePool>) returnValue);
                Collections.sort(dataCenters, new NameableComparator());

                AsyncDataProvider.getExternalNetworkMap(networkQuery, provider.getId());
            }
        };

        startProgress(null);
        AsyncDataProvider.getDataCenterList(dcQuery);
    }

    public void cancel() {
        sourceListModel.setWindow(null);
    }

    public void onImport() {
        List<VdcActionParametersBase> multipleActionParameters =
                new LinkedList<VdcActionParametersBase>();
        List<IFrontendActionAsyncCallback> callbacks = new LinkedList<IFrontendActionAsyncCallback>();
        dcClusters = new HashMap<Guid, Collection<VDSGroup>>();

        for (final ExternalNetwork externalNetwork : (Iterable<ExternalNetwork>) importedNetworks.getItems()) {
            final Network network = externalNetwork.getNetwork();
            final Guid dcId = ((StoragePool) externalNetwork.getDataCenters().getSelectedItem()).getId();
            network.setName(externalNetwork.getDisplayName());
            network.setDataCenterId(dcId);
            AddNetworkStoragePoolParameters params =
                    new AddNetworkStoragePoolParameters(dcId, network);
            params.setVnicProfileRequired(false);
            multipleActionParameters.add(params);
            callbacks.add(new IFrontendActionAsyncCallback() {

                @Override
                public void executed(FrontendActionAsyncResult result) {
                    network.setId((Guid) result.getReturnValue().getActionReturnValue());

                    // Perform sequentially: first fetch clusters, then attach network, then create VNIC profile
                    fetchDcClusters(dcId, network, externalNetwork.isPublicUse());
                }
            });
        }

        Frontend.RunMultipleActions(VdcActionType.AddNetwork, multipleActionParameters, callbacks);
        cancel();
    }

    private void fetchDcClusters(final Guid dcId, final Network network, final boolean publicUse) {
        if (dcClusters.containsKey(dcId)) {
            attachNetworkToClusters(network, dcClusters.get(dcId), publicUse);
        } else {
            AsyncDataProvider.getClusterList(new AsyncQuery(this, new INewAsyncCallback() {

                @Override
                public void onSuccess(Object model, Object returnValue) {
                    Collection<VDSGroup> clusters = (Collection<VDSGroup>) returnValue;
                    dcClusters.put(dcId, clusters);
                    attachNetworkToClusters(network, clusters, publicUse);
                }
            }), dcId);
        }
    }

    private void attachNetworkToClusters(final Network network, Collection<VDSGroup> clusters, final boolean publicUse) {
        NetworkCluster networkCluster = new NetworkCluster();
        networkCluster.setRequired(false);
        network.setCluster(networkCluster);
        List<VdcActionParametersBase> parameters = new LinkedList<VdcActionParametersBase>();
        for (VDSGroup cluster : clusters) {
            parameters.add(new AttachNetworkToVdsGroupParameter(cluster, network));
        }

        Frontend.RunMultipleActions(VdcActionType.AttachNetworkToVdsGroup,
                parameters,
                new IFrontendActionAsyncCallback() {

                    @Override
                    public void executed(FrontendActionAsyncResult result) {
                        addVnicProfile(network, publicUse);
                    }
                });
    }

    private void addVnicProfile(Network network, boolean publicUse) {
        VnicProfile vnicProfile = new VnicProfile();
        vnicProfile.setName(network.getName());
        vnicProfile.setNetworkId(network.getId());
        VnicProfileParameters parameters = new VnicProfileParameters(vnicProfile);
        parameters.setPublicUse(publicUse);
        Frontend.RunAction(VdcActionType.AddVnicProfile, parameters, new IFrontendActionAsyncCallback() {

            @Override
            public void executed(FrontendActionAsyncResult result) {
                sourceListModel.getSearchCommand().execute();
            }
        });
    }

    private void addImport() {
        getDefaultCommand().setIsExecutionAllowed(true);
    }

    private void cancelImport() {
        List<ExternalNetwork> selectedNetworks = (List<ExternalNetwork>) getImportedNetworks().getSelectedItems();
        Collection<ExternalNetwork> importedNetworks = (Collection<ExternalNetwork>) getImportedNetworks().getItems();
        getDefaultCommand().setIsExecutionAllowed(selectedNetworks.size() < importedNetworks.size());

        for (ExternalNetwork externalNetwork : selectedNetworks) {
            externalNetwork.setDisplayName(externalNetwork.getNetwork().getName());
        }
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (StringHelper.stringsEqual(command.getName(), CMD_IMPORT)) {
            onImport();
        } else if (StringHelper.stringsEqual(command.getName(), CMD_CANCEL)) {
            cancel();
        } else if (getAddImportCommand().equals(command)) {
            addImport();
        } else if (getCancelImportCommand().equals(command)) {
            cancelImport();
        }
    }

}
