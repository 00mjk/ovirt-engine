package org.ovirt.engine.ui.webadmin.section.main.view.popup.networkQoS;


import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.NetworkQoSModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.NetworkQoSPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.inject.Inject;


public class NetworkQoSPopupView extends AbstractModelBoundPopupView<NetworkQoSModel>
        implements NetworkQoSPopupPresenterWidget.ViewDef {

    @UiField
    WidgetStyle style;

    @UiField(provided = true)
    @Path(value = "dataCenters.selectedItem")
    @WithElementId
    ListModelListBoxEditor<Object> dataCenterEditor;

    @UiField
    @Path(value = "name.entity")
    @WithElementId
    EntityModelTextBoxEditor nameEditor;

    @UiField(provided = true)
    @Path(value = "inboundEnabled.entity")
    @WithElementId
    EntityModelCheckBoxEditor inboundEnabled;

    @UiField(provided = true)
    @Path(value = "outboundEnabled.entity")
    @WithElementId
    EntityModelCheckBoxEditor outboundEnabled;

    @UiField
    @Path(value = "inboundAverage.entity")
    @WithElementId
    EntityModelTextBoxOnlyEditor inboundAverageEditor;

    @UiField
    @Path(value = "inboundPeak.entity")
    @WithElementId
    EntityModelTextBoxOnlyEditor inboundPeakEditor;

    @UiField
    @Path(value = "inboundBurst.entity")
    @WithElementId
    EntityModelTextBoxOnlyEditor inboundBurstEditor;

    @UiField
    @Path(value = "outboundAverage.entity")
    @WithElementId
    EntityModelTextBoxOnlyEditor outboundAverageEditor;

    @UiField
    @Path(value = "outboundPeak.entity")
    @WithElementId
    EntityModelTextBoxOnlyEditor outboundPeakEditor;

    @UiField
    @Path(value = "outboundBurst.entity")
    @WithElementId
    EntityModelTextBoxOnlyEditor outboundBurstEditor;



    private NetworkQoSModel model;

    interface Driver extends SimpleBeanEditorDriver<NetworkQoSModel, NetworkQoSPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, NetworkQoSPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<NetworkQoSPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public NetworkQoSPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initListBoxEditors();
        inboundEnabled = new EntityModelCheckBoxEditor(Align.RIGHT);
        outboundEnabled = new EntityModelCheckBoxEditor(Align.RIGHT);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);

        localize(constants);
        driver.initialize(this);
        setStyle();
    }

    private void setStyle() {
        inboundAverageEditor.setContentWidgetStyleName(style.valueWidth());
        inboundPeakEditor.setContentWidgetStyleName(style.valueWidth());
        inboundBurstEditor.setContentWidgetStyleName(style.valueWidth());
        outboundAverageEditor.setContentWidgetStyleName(style.valueWidth());
        outboundPeakEditor.setContentWidgetStyleName(style.valueWidth());
        outboundBurstEditor.setContentWidgetStyleName(style.valueWidth());
    }

    private void initListBoxEditors() {
        dataCenterEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((StoragePool) object).getName();
            }
        });
    }

    void localize(ApplicationConstants constants) {
        nameEditor.setLabel(constants.networkQoSName());
        dataCenterEditor.setLabel(constants.dataCenterNetworkQoSPopup());
        inboundEnabled.setLabel(constants.inboundLabelQoSPopup());
        outboundEnabled.setLabel(constants.outboundLabelQoSPopup());
        inboundAverageEditor.setTitle(constants.averageNetworkQoSPopup() + constants.inMegabitsNetworkQoSPopup());
        inboundPeakEditor.setTitle(constants.peakNetworkQoSPopup() + constants.inMegabitsNetworkQoSPopup());
        inboundBurstEditor.setTitle(constants.burstNetworkQoSPopup() + constants.inMegabytesNetworkQoSPopup());
        outboundAverageEditor.setTitle(constants.averageNetworkQoSPopup() + constants.inMegabitsNetworkQoSPopup());
        outboundPeakEditor.setTitle(constants.peakNetworkQoSPopup() + constants.inMegabitsNetworkQoSPopup());
        outboundBurstEditor.setTitle(constants.burstNetworkQoSPopup() + constants.inMegabytesNetworkQoSPopup());
    }

    @Override
    public void edit(NetworkQoSModel object) {
        this.model = object;
        driver.edit(object);
    }

    @Override
    public NetworkQoSModel flush() {
        return driver.flush();
    }



    interface WidgetStyle extends CssResource {
        String valueWidth();
    }
}
