package org.ovirt.engine.ui.common.widget.editor;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.ovirt.engine.ui.common.widget.editor.ListModelTypeAheadListBoxEditor.SuggestBoxRenderer;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle.MultiWordSuggestion;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

/**
 * SuggestBox widget that adapts to UiCommon list model items and looks like a list box. The suggestion content can be rich (html).
 * <p>
 * Accepts any objects as soon as the provided renderer can render them.
 */
public class ListModelTypeAheadListBox<T> extends BaseListModelSuggestBox<T> {

    @UiField(provided = true)
    SuggestBox suggestBox;

    @UiField
    Image dropDownImage;

    @UiField
    FlowPanel mainPanel;

    @UiField
    Style style;

    private final SuggestBoxRenderer<T> renderer;

    private Collection<T> acceptableValues = new ArrayList<T>();

    private HandlerRegistration eventHandler;

    interface Style extends CssResource {

        String enabledMainPanel();

        String disabledMainPanel();
    }

    interface ViewUiBinder extends UiBinder<FlowPanel, ListModelTypeAheadListBox> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    public ListModelTypeAheadListBox(SuggestBoxRenderer<T> renderer) {
        super(new RenderableSuggestOracle<T>(renderer));
        this.renderer = renderer;

        suggestBox = asSuggestBox();

        // this needs to be handled by focus on text box and clicks on drop down image
        setAutoHideEnabled(false);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        registerListeners();
    }

    private void registerListeners() {
        SuggestBoxFocusHandler handlers = new SuggestBoxFocusHandler();
        suggestBox.getValueBox().addBlurHandler(handlers);
        suggestBox.getValueBox().addFocusHandler(handlers);

        // not listening to focus because it would show the suggestions also after the whole browser
        // gets the focus back (after loosing it) if this was the last element with focus
        suggestBox.getValueBox().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                switchSuggestions();
            }
        });

        dropDownImage.addMouseDownHandler(new FocusHandlerEnablingMouseHandlers(handlers));
        dropDownImage.addMouseUpHandler(new FocusHandlerEnablingMouseHandlers(handlers) {
            @Override
            public void onMouseUp(MouseUpEvent event) {
                super.onMouseUp(event);
                switchSuggestions();
            }
        });

        getSuggestionMenu().addDomHandler(new FocusHandlerEnablingMouseHandlers(handlers), MouseDownEvent.getType());

        // no need to do additional switchSuggestions() - it is processed by MenuBar itself
        getSuggestionMenu().addDomHandler(new FocusHandlerEnablingMouseHandlers(handlers), MouseUpEvent.getType());

        asSuggestBox().addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                // in case of other way changed the value (like clicking somewhere else when there is a correct value)
                // hide the suggest box
                if(isSuggestionListShowing()) {
                    hideSuggestions();
                }
            }
        });

        addValueChangeHandler(new ValueChangeHandler<T>() {
            @Override
            public void onValueChange(ValueChangeEvent<T> event) {
                // if the value has been changed using the mouse
                setValue(event.getValue());
            }
        });

    }

    private void switchSuggestions() {
        if (!isEnabled()) {
            return;
        }

        if (isSuggestionListShowing()) {
            hideSuggestions();
            adjustSelectedValue();
        } else {
            // show all the suggestions even if there is already something filled
            // otherwise it is not obvious that there are more options
            suggestBox.setText(null);
            suggestBox.showSuggestionList();

            selectInMenu(getValue());

            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                public void execute () {
                    setFocus(true);
                }
            });
        }
    }

    private void selectInMenu(T toSelect) {
        if (!(getSuggestionMenu() instanceof MenuBar)) {
            // can not select if the it is not a menu bar
            return;
        }

        MenuBar menuBar = (MenuBar) getSuggestionMenu();
        List<MenuItem> items = getItems(menuBar);
        if (items == null) {
            return;
        }

        String selectedReplacementString = renderer.getReplacementString(toSelect);
        if (selectedReplacementString == null) {
            return;
        }

        int selectedItemIndex = -1;
        for (T acceptableValue : acceptableValues) {
            selectedItemIndex ++;
            String acceptableValueReplacement = renderer.getReplacementString(acceptableValue);
            if (acceptableValueReplacement != null && acceptableValueReplacement.equals(selectedReplacementString)) {
                if (items.size() > selectedItemIndex) {
                    menuBar.selectItem(items.get(selectedItemIndex));
                }

                break;
            }
        }
    }

    // extremely ugly - there is just no better way how to find the items as MenuItems
    private native List<MenuItem> getItems(MenuBar menuBar) /*-{
        return menuBar.@com.google.gwt.user.client.ui.MenuBar::getItems()();
    }-*/;

    private void adjustSelectedValue() {
        if (acceptableValues.size() == 0) {
            return;
        }

        // empty suggest box
        String providedText = asSuggestBox().getText();
        if (providedText == null || "".equals(providedText)) {
            if (getValue() != null) {
                // something has been there, deleted on click inside and than hidden the box - restoring
                asSuggestBox().setText(renderer.getReplacementString(getValue()));
            } else {
                // nothing has been there, selecting the first accpetable value
                setValue(acceptableValues.iterator().next());
            }
        } else {
            // something has been typed inside - validate
            T newData = asEntity(providedText);
            if (newData != null) {
                // correct provided - use it
                setValue(newData);
            } else {
                // incorrect - return to previous one
                asSuggestBox().setText(renderer.getReplacementString(getValue()));
            }
        }
    }

    @Override
    protected T asEntity(String provided) {
        if (provided == null) {
            return null;
        }

        for (T data : acceptableValues) {
            if (data == null) {
                continue;
            }

            String expected = renderer.getReplacementString(data);
            if (expected == null) {
                continue;
            }

            if (expected.equals(provided)) {
                return data;
            }
        }

        return null;
    }

    public void setValue(T value) {
        addToValidValuesIfNeeded(value);
        super.setValue(value);
    }

    public void setValue(T value, boolean fireEvents) {
        addToValidValuesIfNeeded(value);
        super.setValue(value, fireEvents);
    }

    @Override
    public T getValue() {
        if (acceptableValues.contains(super.getValue())) {
            return super.getValue();
        }

        return null;
    }

    private void addToValidValuesIfNeeded(T value) {
        if (!acceptableValues.contains(value)) {
            acceptableValues.add(value);
        }

    }

    @Override
    public void setAcceptableValues(Collection<T> acceptableValues) {
        this.acceptableValues = acceptableValues;
        T selected = getValue();
        addToValidValuesIfNeeded(selected);
        RenderableSuggestOracle<T> suggestOracle = (RenderableSuggestOracle<T>) suggestBox.getSuggestOracle();
        suggestOracle.setData(acceptableValues);
    }

    @Override
    protected void render(T value, boolean fireEvents) {
        asSuggestBox().setValue(renderer.getReplacementString(value), fireEvents);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            mainPanel.getElement().replaceClassName(style.disabledMainPanel(), style.enabledMainPanel());
        } else {
            mainPanel.getElement().replaceClassName(style.enabledMainPanel(), style.disabledMainPanel());
        }
    }

    class SuggestBoxFocusHandler implements FocusHandler, BlurHandler {

        private boolean enabled = true;

        @Override
        public void onBlur(BlurEvent blurEvent) {
            if (eventHandler != null) {
                eventHandler.removeHandler();
            }

            // process only if it will not be processed by other handlers
            if (enabled) {
                // first give the opportunity to the click handler on the menu to process the event, than we can hide it
                hideSuggestions();
                adjustSelectedValue();
            }
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void onFocus(FocusEvent event) {
            eventHandler =
                    Event.addNativePreviewHandler(new EnterIgnoringNativePreviewHandler<T>(ListModelTypeAheadListBox.this));
        }
    }

    class FocusHandlerEnablingMouseHandlers implements MouseDownHandler, MouseUpHandler {

        private SuggestBoxFocusHandler focusHandler;

        public FocusHandlerEnablingMouseHandlers(SuggestBoxFocusHandler focusHandler) {
            this.focusHandler = focusHandler;
        }

        @Override
        public void onMouseDown(MouseDownEvent event) {
            focusHandler.setEnabled(false);
        }

        @Override
        public void onMouseUp(MouseUpEvent event) {
            focusHandler.setEnabled(true);
        }
    }

}

class RenderableSuggestion<T> extends MultiWordSuggestion {

    public RenderableSuggestion(T row, SuggestBoxRenderer<T> renderer) {
        super(renderer.getReplacementString(row), renderer.getDisplayString(row));
    }

    public boolean matches(String query) {
        String replacementString = getReplacementString();
        if (replacementString == null || query == null) {
            return false;
        }

        return replacementString.toLowerCase().startsWith(query.toLowerCase());
    }
}

class RenderableSuggestOracle<T> extends MultiWordSuggestOracle {

    private SuggestBoxRenderer<T> renderer;

    // inited to avoid null checks
    private Collection<T> data = new ArrayList<T>();

    public RenderableSuggestOracle(SuggestBoxRenderer<T> renderer) {
        this.renderer = renderer;
    }

    @Override
    public void requestSuggestions(Request request, Callback callback) {
        List<RenderableSuggestion<T>> suggestions = new ArrayList<RenderableSuggestion<T>>();

        String query = request.getQuery();
        for (T row : data) {
            RenderableSuggestion<T> suggestionCandidate = new RenderableSuggestion<T>(row, renderer);
            if (suggestionCandidate.matches(query)) {
                suggestions.add(suggestionCandidate);
            }
        }

        callback.onSuggestionsReady(request, new Response(suggestions));
    }

    @Override
    public void requestDefaultSuggestions(Request request, Callback callback) {
        List<RenderableSuggestion<T>> suggestions = new ArrayList<RenderableSuggestion<T>>();

        for (T row : data) {
            suggestions.add(new RenderableSuggestion<T>(row, renderer));
        }

        callback.onSuggestionsReady(request, new Response(suggestions));
    }

    public void setData(Collection<T> data) {
        this.data = data;
    }

}

class EnterIgnoringNativePreviewHandler<T> implements NativePreviewHandler {

    private final ListModelTypeAheadListBox<T> listModelTypeAheadListBox;

    public EnterIgnoringNativePreviewHandler(ListModelTypeAheadListBox<T> listModelTypeAheadListBox) {
        this.listModelTypeAheadListBox = listModelTypeAheadListBox;
    }

    @Override
    public void onPreviewNativeEvent(NativePreviewEvent event) {
        NativeEvent nativeEvent = event.getNativeEvent();
        if (nativeEvent.getKeyCode() == KeyCodes.KEY_ENTER && event.getTypeInt() == Event.ONKEYPRESS && !event.isCanceled()) {
            // swallow the enter key otherwise the whole dialog would get submitted
            nativeEvent.preventDefault();
            nativeEvent.stopPropagation();
            event.cancel();

            // process the event here directly
            Suggestion currentSelection = listModelTypeAheadListBox.getCurrentSelection();
            if (currentSelection != null) {
                String replacementString = currentSelection.getReplacementString();
                listModelTypeAheadListBox.setValue(listModelTypeAheadListBox.asEntity(replacementString), true);
            }

            listModelTypeAheadListBox.hideSuggestions();
        }
    }

}
