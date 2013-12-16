package org.ovirt.engine.ui.webadmin.section.main.view.popup.host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.dialog.InfoIcon;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostSetupNetworksModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.LogicalNetworkModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkInterfaceModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkItemModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkOperation;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.OperationCadidateEventArgs;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.host.HostSetupNetworksPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.host.panels.ExternalNetworkPanel;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.host.panels.ExternalNetworksPanel;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.host.panels.NetworkGroup;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.host.panels.NetworkPanel;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.host.panels.InternalNetworkPanel;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.host.panels.NetworkPanelsStyle;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.host.panels.InternalNetworksPanel;
import org.ovirt.engine.ui.webadmin.widget.editor.AnimatedVerticalPanel;
import org.ovirt.engine.ui.webadmin.widget.footer.StatusLabel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public class HostSetupNetworksPopupView extends AbstractModelBoundPopupView<HostSetupNetworksModel> implements HostSetupNetworksPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<HostSetupNetworksModel, HostSetupNetworksPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, HostSetupNetworksPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    private static ApplicationConstants constants = ClientGinjectorProvider.getApplicationConstants();
    private static final String EMPTY_STATUS = constants.dragToMakeChangesSetupNetwork();

    @UiField
    InternalNetworksPanel internalNetworkList;

    @UiField
    ExternalNetworksPanel externalNetworkList;

    @UiField(provided = true)
    InfoIcon externalNetworksInfo;

    @UiField
    AnimatedVerticalPanel nicList;

    @UiField
    SimplePanel statusPanel;

    @UiField(provided = true)
    @Ignore
    StatusLabel status;

    @UiField
    NetworkPanelsStyle style;

    @UiField(provided = true)
    @Path(value = "checkConnectivity.entity")
    EntityModelCheckBoxEditor checkConnectivity;

    @UiField(provided = true)
    @Path(value = "commitChanges.entity")
    EntityModelCheckBoxEditor commitChanges;

    @UiField(provided = true)
    InfoIcon checkConnInfo;

    @UiField(provided = true)
    InfoIcon commitChangesInfo;

    private final Driver driver = GWT.create(Driver.class);

    private boolean rendered = false;

    @Inject
    public HostSetupNetworksPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants, ApplicationTemplates templates) {
        super(eventBus, resources);
        status = new StatusLabel(EMPTY_STATUS);
        checkConnectivity = new EntityModelCheckBoxEditor(Align.RIGHT);
        commitChanges = new EntityModelCheckBoxEditor(Align.RIGHT);
        externalNetworksInfo = new InfoIcon(templates.italicText(constants.externalNetworksInfo()), resources);
        checkConnInfo = new InfoIcon(templates.italicTwoLines(constants.checkConnectivityInfoPart1(), constants.checkConnectivityInfoPart2()), resources);
        commitChangesInfo = new InfoIcon(templates.italicTwoLines(constants.commitChangesInfoPart1(), constants.commitChangesInfoPart2()), resources);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        setStatusStyle(true);
        checkConnectivity.setContentWidgetStyleName(style.checkCon());
        commitChanges.setContentWidgetStyleName(style.commitChanges());
        initUnassignedNetworksPanel();
        localize();
        driver.initialize(this);
    }

    private void initUnassignedNetworksPanel() {
        internalNetworkList.setStyle(style);
        externalNetworkList.setStyle(style);
    }

    private void localize(){
        checkConnectivity.setLabel(constants.checkConHostPopup()); //$NON-NLS-1$
        commitChanges.setLabel(constants.saveNetConfigHostPopup());
    }

    private void setStatusStyle(boolean valid) {
        if (valid) {
            statusPanel.setStylePrimaryName(style.statusPanel());
            status.setStylePrimaryName(style.statusLabel());
        } else {
            statusPanel.setStylePrimaryName(style.errorPanel());
            status.setStylePrimaryName(style.errorLabel());
        }
    }

    @Override
    public void edit(HostSetupNetworksModel uicommonModel) {
        driver.edit(uicommonModel);
        uicommonModel.getNicsChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                // this is called after both networks and nics were retrieved
                HostSetupNetworksModel model = (HostSetupNetworksModel) sender;
                List<LogicalNetworkModel> networks = model.getNetworks();
                List<NetworkInterfaceModel> nics = model.getNics();
                status.setText(EMPTY_STATUS);
                updateNetworks(networks);
                updateNics(nics);
                // mark as rendered
                rendered = true;
            }
        });

        uicommonModel.getOperationCandidateEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                OperationCadidateEventArgs evtArgs = (OperationCadidateEventArgs) args;
                NetworkOperation candidate = evtArgs.getCandidate();
                NetworkItemModel<?> op1 = evtArgs.getOp1();
                NetworkItemModel<?> op2 = evtArgs.getOp2();
                boolean drop = evtArgs.isDrop();
                if (!drop) {
                    status.setFadeText(candidate != null ? candidate.getMessage(op1, op2) : constants.noValidActionSetupNetwork());
                }
                setStatusStyle(!drop || !candidate.isNullOperation() || candidate == NetworkOperation.NULL_OPERATION);
            }
        });

        internalNetworkList.setSetupModel(uicommonModel);
        externalNetworkList.setSetupModel(uicommonModel);
    }

    @Override
    public HostSetupNetworksModel flush() {
        return driver.flush();
    }

    private void updateNetworks(List<LogicalNetworkModel> allNetworks) {
        internalNetworkList.clear();
        externalNetworkList.clear();
        Collections.sort(allNetworks);
        List<NetworkPanel> staticNetworkPanels = new ArrayList<NetworkPanel>();
        List<NetworkPanel> dynamicNetworkPanels = new ArrayList<NetworkPanel>();
        for (LogicalNetworkModel network : allNetworks) {
            if (network.getEntity().isExternal()) {
                dynamicNetworkPanels.add(new ExternalNetworkPanel(network, style));
            } else if (!network.isAttached()) {
                staticNetworkPanels.add(new InternalNetworkPanel(network, style));
            }
        }
        internalNetworkList.addAll(staticNetworkPanels, !rendered);
        externalNetworkList.addAll(dynamicNetworkPanels, !rendered);
    }

    private void updateNics(List<NetworkInterfaceModel> nics) {
        nicList.clear();
        Collections.sort(nics);
        List<NetworkGroup> groups = new ArrayList<NetworkGroup>();
        for (NetworkInterfaceModel nic : nics) {
            groups.add(new NetworkGroup(nic, style));
        }
        nicList.addAll(groups, !rendered);
    }

}
