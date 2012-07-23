package org.ovirt.engine.ui.webadmin.section.main.view.popup.host.panels;

import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.common.businessentities.NetworkBootProtocol;
import org.ovirt.engine.core.common.businessentities.VdsNetworkInterface;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.BondNetworkInterfaceModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.LogicalNetworkModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkInterfaceModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkItemModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlexTable;

public class ItemInfoPopup extends DecoratedPopupPanel {

    private final FlexTable contents = new FlexTable();
    private static final EnumRenderer<NetworkBootProtocol> RENDERER = new EnumRenderer<NetworkBootProtocol>();
    private final ApplicationConstants constants = ClientGinjectorProvider.instance().getApplicationConstants();
    private final ApplicationTemplates templates = ClientGinjectorProvider.instance().getApplicationTemplates();
    final ApplicationResources resources = ClientGinjectorProvider.instance().getApplicationResources();
    SafeHtml mgmtNetworkImage = SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.mgmtNetwork()).getHTML());
    SafeHtml vmImage = SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.networkVm()).getHTML());
    SafeHtml monitorImage = SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.networkMonitor()).getHTML());
    SafeHtml unknownImage = SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.questionMarkImage()).getHTML());

    public ItemInfoPopup() {
        super(true);
        contents.setCellPadding(5);

        setWidget(contents);
        getElement().getStyle().setZIndex(1);
    }

    public void showItem(NetworkItemModel<?> item, NetworkItemPanel panel) {
        contents.clear();
        if (item instanceof LogicalNetworkModel) {
            showNetwork((LogicalNetworkModel) item);
        } else if (item instanceof NetworkInterfaceModel) {
            showNic((NetworkInterfaceModel) item);
        }
        showRelativeTo(panel);
    }

    private void addRow(String label, String value) {
        int rowCount = contents.insertRow(contents.getRowCount());
        contents.setText(rowCount, 0, label + ": " + value); //$NON-NLS-1$
    }

    private void addRow(SafeHtml value) {
        int rowCount = contents.insertRow(contents.getRowCount());
        contents.setHTML(rowCount, 0, value);
    }

    private void showNetwork(LogicalNetworkModel networkModel) {
        contents.removeAllRows();
        Network entity = networkModel.getEntity();
        addRow(templates.titleSetupNetworkTooltip(networkModel.getName()));

        if (entity.getdescription()!=null && !entity.getdescription().trim().equals("")){ //$NON-NLS-1$
               addRow(SafeHtmlUtils.fromString(entity.getdescription()));
        }

        // Usages
        boolean isDisplay = entity.getCluster() == null ? false : entity.getCluster().getis_display();
        if (entity.getCluster() == null || networkModel.isManagement() || isDisplay || entity.isVmNetwork()){
            addRow(SafeHtmlUtils.fromString(constants.usageItemInfo() + ":")); //$NON-NLS-1$

            if (entity.getCluster() == null){
                addRow(templates.imageTextSetupNetworkUsage(unknownImage, constants.unknownItemInfo()));
            }else{

                if (networkModel.isManagement()){
                    addRow(templates.imageTextSetupNetworkUsage(mgmtNetworkImage, constants.managementItemInfo()));
                }

                if (isDisplay){
                    addRow(templates.imageTextSetupNetworkUsage(monitorImage, constants.displayItemInfo()));
                }

                if (entity.isVmNetwork()){
                    addRow(templates.imageTextSetupNetworkUsage(vmImage, constants.vmItemInfo()));
                }
            }
        }

        // Mtu
        if (entity.getMtu() != 0){
            addRow(constants.mtuItemInfo(), String.valueOf(entity.getMtu()));
        }
    }

    private void showNic(NetworkInterfaceModel nic) {
        contents.removeAllRows();
        VdsNetworkInterface entity = nic.getEntity();
        NetworkBootProtocol bootProtocol = entity.getBootProtocol();
        addRow(templates.titleSetupNetworkTooltip(nic.getName()));
        addRow(constants.bootProtocolItemInfo(), RENDERER.render(bootProtocol));
        if (bootProtocol == NetworkBootProtocol.StaticIp) {
            addRow(constants.addressItemInfo(), entity.getAddress());
            addRow(constants.subnetItemInfo(), entity.getSubnet());
            if (entity.getIsManagement()) {
                addRow(constants.gatewayItemInfo(), entity.getGateway());
            }
        }
        if (nic instanceof BondNetworkInterfaceModel) {
            addRow(constants.bondOptionsItemInfo(), entity.getBondOptions());
        }
    }
}
