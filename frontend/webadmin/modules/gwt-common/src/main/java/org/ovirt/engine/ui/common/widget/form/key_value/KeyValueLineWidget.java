package org.ovirt.engine.ui.common.widget.form.key_value;

import org.ovirt.engine.ui.common.widget.HasEditorDriver;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.uicommonweb.models.vms.key_value.KeyValueLineModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

public class KeyValueLineWidget extends Composite implements HasEditorDriver<KeyValueLineModel>, HasEnabled {

    interface WidgetUiBinder extends UiBinder<Widget, KeyValueLineWidget> {
        WidgetUiBinder uiBinder = GWT.create(WidgetUiBinder.class);
    }

    interface Driver extends SimpleBeanEditorDriver<KeyValueLineModel, KeyValueLineWidget> {
    }

    private boolean enabled = true;

    @UiField
    WidgetStyle style;

    @UiField
    @Path(value = "keys.selectedItem")
    ListModelListBoxEditor<Object> keyField;

    @UiField
    @Path(value = "value.entity")
    EntityModelTextBoxEditor valueField;

    @UiField
    @Path(value = "values.selectedItem")
    ListModelListBoxEditor<Object> valuesField;

    @UiField
    @Ignore
    PushButton plusButton;

    @UiField
    @Ignore
    PushButton minusButton;

    private final Driver driver = GWT.create(Driver.class);

    KeyValueLineWidget() {
        initWidget(WidgetUiBinder.uiBinder.createAndBindUi(this));
        driver.initialize(this);
        addStyles();
    }

    private void addStyles() {
        keyField.addContentWidgetStyleName(style.fieldWidth());
        valueField.addContentWidgetStyleName(style.fieldWidth());
        valuesField.addContentWidgetStyleName(style.fieldWidth());
        hideLabels();
    }

    private void hideLabels() {
        keyField.hideLabel();
        valueField.hideLabel();
        valuesField.hideLabel();
    }

    @Override
    public void edit(final KeyValueLineModel object) {
        //        plusButton.setCommand(object.getAddLine());
        plusButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                object.getAddLine().execute();
            }
        });
        minusButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                object.getRemoveLine().execute();
            }
        });
        updateKeyTitle(object);
        object.getKeys().getSelectedItemChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                updateKeyTitle(object);
            }
        });
        driver.edit(object);
    }

    /**
     * set dropdown with selected key tooltip.
     */
    private void updateKeyTitle(KeyValueLineModel object) {
        String selectedKey = (String) object.getKeys().getSelectedItem();
        // Setting the title to null results in the string "null" being displayed on some browsers.
        if (selectedKey == null) {
            selectedKey = "";
        }
        keyField.getElement().setTitle(selectedKey);
    }

    @Override
    public KeyValueLineModel flush() {
        return driver.flush();
    }

    interface WidgetStyle extends CssResource {
        String fieldWidth();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        keyField.setEnabled(enabled);
        valueField.setEnabled(enabled);
        valuesField.setEnabled(enabled);
        plusButton.setEnabled(enabled);
        minusButton.setEnabled(enabled);
        this.enabled = enabled;
    }

    public void setPlusButtonEnabled(boolean enabled) {
        plusButton.setEnabled(enabled);
    }

    public void setMinusButtonEnabled(boolean enabled) {
        minusButton.setEnabled(enabled);
    }

}
