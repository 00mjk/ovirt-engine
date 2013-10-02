package org.ovirt.engine.ui.common.widget.uicommon.popup.networkinterface;

import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.dialog.AdvancedParametersExpander;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelRadioButtonEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.profile.ProfileEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractModelBoundPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmInterfaceModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;

public class NetworkInterfacePopupWidget extends AbstractModelBoundPopupWidget<VmInterfaceModel> {

    interface Driver extends SimpleBeanEditorDriver<VmInterfaceModel, NetworkInterfacePopupWidget> {
    }

    interface ViewUiBinder extends UiBinder<FlowPanel, NetworkInterfacePopupWidget> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<NetworkInterfacePopupWidget> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    protected interface Style extends CssResource {
        String cardStatusEditorContent();

        String cardStatusRadioContent();

        String linkStateEditorContent();

        String linkStateRadioContent();

        String checkBox();
    }

    @UiField
    protected Style style;

    @UiField
    @Path("name.entity")
    @WithElementId("name")
    EntityModelTextBoxEditor nameEditor;

    @UiField
    @Path(value = "profile.selectedItem")
    @WithElementId("profile")
    public ProfileEditor profileEditor;

    @UiField(provided = true)
    @Path("nicType.selectedItem")
    @WithElementId("nicType")
    ListModelListBoxEditor<Object> nicTypeEditor;

    @UiField(provided = true)
    @Path("enableMac.entity")
    @WithElementId("enableManualMac")
    EntityModelCheckBoxEditor enableManualMacCheckbox;

    @UiField
    @Path("MAC.entity")
    @WithElementId("mac")
    EntityModelTextBoxEditor MACEditor;

    @UiField
    @Ignore
    Label macExample;

    @UiField
    protected HorizontalPanel cardStatusSelectionPanel;

    @UiField
    @Path(value = "plugged.entity")
    public ListModelListBoxEditor<Object> cardStatusEditor;

    @UiField(provided = true)
    @Path(value = "plugged_IsSelected.entity")
    public EntityModelRadioButtonEditor pluggedEditor;

    @UiField(provided = true)
    @Path(value = "unplugged_IsSelected.entity")
    public EntityModelRadioButtonEditor unpluggedEditor;

    @UiField
    protected HorizontalPanel linkStateSelectionPanel;

    @UiField
    @Path(value = "linked.entity")
    public ListModelListBoxEditor<Object> linkStateEditor;

    @UiField(provided = true)
    @Path(value = "linked_IsSelected.entity")
    public EntityModelRadioButtonEditor linkedEditor;

    @UiField(provided = true)
    @Path(value = "unlinked_IsSelected.entity")
    public EntityModelRadioButtonEditor unlinkedEditor;

    public final static CommonApplicationMessages messages = GWT.create(CommonApplicationMessages.class);
    public final static CommonApplicationTemplates templates = GWT.create(CommonApplicationTemplates.class);
    public final static CommonApplicationResources resources = GWT.create(CommonApplicationResources.class);

    @UiField
    @Ignore
    public AdvancedParametersExpander expander;

    @UiField
    @Ignore
    public Panel expanderContent;

    private final Driver driver = GWT.create(Driver.class);

    public NetworkInterfacePopupWidget(EventBus eventBus, CommonApplicationConstants constants) {
        initManualWidgets();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        expander.initWithContent(expanderContent.getElement());
        localize(constants);
        applyStyles();
        ViewIdHandler.idHandler.generateAndSetIds(this);
        driver.initialize(this);
    }

    private void localize(CommonApplicationConstants constants) {
        nameEditor.setLabel(constants.nameNetworkIntefacePopup());
        profileEditor.setLabel(constants.profileNetworkIntefacePopup());
        nicTypeEditor.setLabel(constants.typeNetworkIntefacePopup());
        enableManualMacCheckbox.setLabel(constants.customMacNetworkIntefacePopup());

        cardStatusEditor.setLabel(constants.cardStatusNetworkInteface());
        pluggedEditor.asRadioButton()
                .setHTML(templates.imageTextCardStatus(SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.pluggedNetworkImage())
                        .getHTML()),
                        constants.pluggedNetworkInteface()));
        unpluggedEditor.asRadioButton()
                .setHTML(templates.imageTextCardStatus(SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.unpluggedNetworkImage())
                        .getHTML()),
                        constants.unpluggedNetworkInteface()));

        linkStateEditor.setLabel(constants.linkStateNetworkInteface());
        linkedEditor.asRadioButton()
                .setHTML(templates.imageTextCardStatus(SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.linkedNetworkImage())
                        .getHTML()),
                        constants.linkedNetworkInteface()));
        unlinkedEditor.asRadioButton()
                .setHTML(templates.imageTextCardStatus(SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.unlinkedNetworkImage())
                        .getHTML()),
                        constants.unlinkedNetworkInteface()));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void initManualWidgets() {
        nicTypeEditor = new ListModelListBoxEditor<Object>(new EnumRenderer());

        pluggedEditor = new EntityModelRadioButtonEditor("cardStatus"); //$NON-NLS-1$
        unpluggedEditor = new EntityModelRadioButtonEditor("cardStatus"); //$NON-NLS-1$

        linkedEditor = new EntityModelRadioButtonEditor("linkState"); //$NON-NLS-1$
        unlinkedEditor = new EntityModelRadioButtonEditor("linkState"); //$NON-NLS-1$

        enableManualMacCheckbox = new EntityModelCheckBoxEditor(Align.RIGHT);
    }

    @Override
    public void focusInput() {
        nameEditor.setFocus(true);
    }

    @Override
    public void edit(final VmInterfaceModel iface) {
        driver.edit(iface);

        hideMacWhenNotEnabled(iface);
        iface.getMAC().getPropertyChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if ("IsAvailable".equals(((PropertyChangedEventArgs) args).PropertyName)) { //$NON-NLS-1$
                    hideMacWhenNotEnabled(iface);
                }
            }
        });
    }

    private void hideMacWhenNotEnabled(VmInterfaceModel iface) {
        if (!iface.getMAC().getIsAvailable()) {
            enableManualMacCheckbox.setVisible(false);
            MACEditor.setVisible(false);
            macExample.setVisible(false);
        }
    }

    @Override
    public VmInterfaceModel flush() {
        return driver.flush();
    }

    private void applyStyles() {
        cardStatusEditor.addContentWidgetStyleName(style.cardStatusEditorContent());
        pluggedEditor.addContentWidgetStyleName(style.cardStatusRadioContent());
        unpluggedEditor.addContentWidgetStyleName(style.cardStatusRadioContent());

        linkStateEditor.addContentWidgetStyleName(style.linkStateEditorContent());
        linkedEditor.addContentWidgetStyleName(style.linkStateRadioContent());
        unlinkedEditor.addContentWidgetStyleName(style.linkStateRadioContent());

        enableManualMacCheckbox.addContentWidgetStyleName(style.checkBox());
    }
}
