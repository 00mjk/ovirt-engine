package org.ovirt.engine.ui.userportal.section.main.view.tab.extended.vm;

import org.ovirt.engine.ui.common.view.AbstractSubTabFormView;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalItemModel;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmMonitorModel;
import org.ovirt.engine.ui.userportal.ApplicationConstants;
import org.ovirt.engine.ui.userportal.section.main.presenter.tab.extended.vm.SubTabExtendedVmMonitorPresenter;
import org.ovirt.engine.ui.userportal.uicommon.model.vm.VmMonitorModelProvider;
import org.ovirt.engine.ui.userportal.widget.PercentageProgressBar;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class SubTabExtendedVmMonitorView extends AbstractSubTabFormView<UserPortalItemModel, UserPortalListModel, VmMonitorModel>
        implements SubTabExtendedVmMonitorPresenter.ViewDef {

    interface ViewUiBinder extends UiBinder<Widget, SubTabExtendedVmMonitorView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    Label cpuUsageLabel;

    @UiField
    PercentageProgressBar cpuUsageProgressBar;

    @UiField
    Label memoryUsageLabel;

    @UiField
    PercentageProgressBar memoryUsageProgressBar;

    @UiField
    Label networkUsageLabel;

    @UiField
    PercentageProgressBar networkUsageProgressBar;

    @Inject
    public SubTabExtendedVmMonitorView(VmMonitorModelProvider modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        localize(constants);
    }

    void localize(ApplicationConstants constants) {
        cpuUsageLabel.setText(constants.vmMonitorCpuUsageLabel());
        memoryUsageLabel.setText(constants.vmMonitorMemoryUsageLabel());
        networkUsageLabel.setText(constants.vmMonitorNetworkUsageLabel());
    }

    @Override
    public void setMainTabSelectedItem(UserPortalItemModel selectedItem) {
        update();
    }

    @Override
    public void update() {
        cpuUsageProgressBar.setValue(getDetailModel().getCpuUsage());
        memoryUsageProgressBar.setValue(getDetailModel().getMemoryUsage());
        networkUsageProgressBar.setValue(getDetailModel().getNetworkUsage());
    }

}
