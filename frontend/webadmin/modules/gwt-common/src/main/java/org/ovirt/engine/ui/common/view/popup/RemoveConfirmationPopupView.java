package org.ovirt.engine.ui.common.view.popup;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.presenter.popup.RemoveConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class RemoveConfirmationPopupView extends AbstractConfirmationPopupView implements RemoveConfirmationPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<ConfirmationModel, RemoveConfirmationPopupView> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, RemoveConfirmationPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<RemoveConfirmationPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private final CommonApplicationMessages messages;
    private final CommonApplicationConstants constants;

    @UiField
    protected FlowPanel itemPanel;

    @UiField(provided = true)
    @Path(value = "latch.entity")
    @WithElementId
    protected EntityModelCheckBoxEditor latch;

    @UiField
    @Ignore
    protected Label noteLabel;

    @Inject

    public RemoveConfirmationPopupView(EventBus eventBus,
            CommonApplicationResources resources,
            CommonApplicationMessages messages,
            CommonApplicationConstants constants) {
        super(eventBus, resources);
        latch = new EntityModelCheckBoxEditor(Align.RIGHT);
        latch.setLabel(constants.approveOperation());
        this.constants= constants;
        this.messages = messages;
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize(constants);
        Driver.driver.initialize(this);
    }

    @Override
    public void setMessage(String message) {
        if (getHashName() != null && getHashName().startsWith("remove_")) { //$NON-NLS-1$
            super.setMessage(messages.removeConfirmationPopupMessage(message));
        } else {
            super.setMessage(message);
        }
    }

    @Override
    public void setItems(Iterable<?> items) {
        if (items != null) {
            for (Object i : items) {
                itemPanel.add(new Label(getItemText(i)));
            }
        } else {
            itemPanel.clear();
        }
    }

    void setNote(String note) {
        if (note != null) {
            noteLabel.getElement().setInnerHTML(note.replace("\n", "<br/>")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    String getItemText(Object item) {
        return "- " + String.valueOf(item); //$NON-NLS-1$
    }

    @Override
    public void edit(ConfirmationModel object) {
        Driver.driver.edit(object);

        // Bind "Latch.IsAvailable"
        object.getLatch().getPropertyChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if ("IsAvailable".equals(((PropertyChangedEventArgs) args).PropertyName)) { //$NON-NLS-1$
                    EntityModel entity = (EntityModel) sender;
                    if (entity.getIsAvailable()) {
                        latch.setVisible(true);
                    }
                }
            }
        });

        // Bind "Note"
        object.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if ("Note".equals(((PropertyChangedEventArgs) args).PropertyName)) { //$NON-NLS-1$
                    ConfirmationModel entity = (ConfirmationModel) sender;
                    setNote(entity.getNote());
                }
            }
        });
    }

    protected void localize(CommonApplicationConstants constants) {
        latch.setLabel(constants.latchApproveOperationLabel());
    }

    @Override
    public ConfirmationModel flush() {
        return Driver.driver.flush();
    }

}
