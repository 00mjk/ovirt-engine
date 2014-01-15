package org.ovirt.engine.ui.common.widget.editor.generic;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import java.util.Collection;
import org.ovirt.engine.ui.common.widget.editor.BaseListModelSuggestBox;

/**
 * SuggestBox widget that adapts to UiCommon list model items. Expects all of it's items to be non null Strings
 */
public class ListModelSuggestBox extends BaseListModelSuggestBox<String> {

    public ListModelSuggestBox() {
        super(new MultiWordSuggestOracle());
        initWidget(asSuggestBox());

        asSuggestBox().getTextBox().addFocusHandler(new FocusHandler() {

            @Override
            public void onFocus(FocusEvent event) {
                asSuggestBox().showSuggestionList();
            }
        });
        addKeyPressHandler(new KeyPressHandler() {

            @Override
            public void onKeyPress(KeyPressEvent event) {
                Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                    @Override
                    public void execute() {
                        ValueChangeEvent.fire(asSuggestBox(), asSuggestBox().getText());
                    }
                });
            }
        });
    }

    @Override
    public void setAcceptableValues(Collection<String> values) {
        MultiWordSuggestOracle suggestOracle = (MultiWordSuggestOracle) asSuggestBox().getSuggestOracle();
        suggestOracle.clear();
        suggestOracle.addAll(values);
        suggestOracle.setDefaultSuggestionsFromText(values);
    }

    @Override
    protected void render(String value, boolean fireEvents) {
        asSuggestBox().setValue(value, fireEvents);
    }

    @Override
    protected String asEntity(String value) {
        return value;
    }

}
