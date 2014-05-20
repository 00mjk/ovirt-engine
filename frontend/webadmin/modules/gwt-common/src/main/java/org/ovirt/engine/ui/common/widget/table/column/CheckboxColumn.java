package org.ovirt.engine.ui.common.widget.table.column;

import org.ovirt.engine.ui.uicommonweb.models.EntityModel;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.CellPreviewEvent;

public abstract class CheckboxColumn<T> extends Column<T, Boolean> {

    private boolean isCentralized = false;
    private boolean multipleSelectionAllowed = true;

    static class EnabledDisabledCheckboxCell extends CheckboxCell implements EventHandlingCell {

        public EnabledDisabledCheckboxCell() {
            super(true, false);
        }

        @Override
        public boolean handlesEvent(CellPreviewEvent<EntityModel> event) {
            return CheckboxColumn.handlesEvent(event);
        }

    }

    static class EnabledDisabledRadioCell extends RadioboxCell implements EventHandlingCell {

        public EnabledDisabledRadioCell() {
            super(true, false);
        }

        @Override
        public boolean handlesEvent(CellPreviewEvent<EntityModel> event) {
            return CheckboxColumn.handlesEvent(event);
        }

    }

    private static boolean handlesEvent(CellPreviewEvent<EntityModel> event) {
        NativeEvent nativeEvent = event.getNativeEvent();
        if (!"click".equals(nativeEvent.getType().toLowerCase())) { //$NON-NLS-1$
            return false;
        }
        Element target = nativeEvent.getEventTarget().cast();
        return "input".equals(target.getTagName().toLowerCase()); //$NON-NLS-1$
    }

    private static final SafeHtml INPUT_CHECKBOX_DISABLED_PREFIX =
            SafeHtmlUtils.fromTrustedString("<input type=\"checkbox\" tabindex=\"-1\" disabled"); //$NON-NLS-1$
    private static final SafeHtml INPUT_RADIO_DISABLED_PREFIX =
            SafeHtmlUtils.fromTrustedString("<input type=\"radio\" tabindex=\"-1\" disabled"); //$NON-NLS-1$
    private static final SafeHtml CHECKED_ATTR = SafeHtmlUtils.fromTrustedString(" checked"); //$NON-NLS-1$
    private static final SafeHtml TITLE_ATTR_START = SafeHtmlUtils.fromTrustedString(" title=\""); //$NON-NLS-1$
    private static final SafeHtml TITLE_ATTR_END = SafeHtmlUtils.fromTrustedString("\""); //$NON-NLS-1$
    private static final SafeHtml TAG_CLOSE = SafeHtmlUtils.fromTrustedString("/>"); //$NON-NLS-1$

    public CheckboxColumn() {
        super(new EnabledDisabledCheckboxCell());
    }

    public CheckboxColumn(boolean isCentralized) {
        super(new EnabledDisabledCheckboxCell());
        this.isCentralized = isCentralized;
    }

    public CheckboxColumn(FieldUpdater<T, Boolean> fieldUpdater) {
        this();
        setFieldUpdater(fieldUpdater);
    }

    public CheckboxColumn(boolean multipleSelectionAllowed, FieldUpdater<T, Boolean> fieldUpdater) {
        super(multipleSelectionAllowed ? new EnabledDisabledCheckboxCell() : new EnabledDisabledRadioCell());
        this.multipleSelectionAllowed = multipleSelectionAllowed;
        setFieldUpdater(fieldUpdater);
    }

    @Override
    public void render(Context context, T object, SafeHtmlBuilder sb) {
        if (isCentralized) {
            sb.appendHtmlConstant("<div style='text-align: center'>"); //$NON-NLS-1$
        }

        if (!canEdit(object)) {
            sb.append(multipleSelectionAllowed ? INPUT_CHECKBOX_DISABLED_PREFIX : INPUT_RADIO_DISABLED_PREFIX);
            if (Boolean.TRUE.equals(getValue(object))) {
                sb.append(CHECKED_ATTR);
            }
            String disabledMessage = getDisabledMessage(object);
            if (disabledMessage != null && !disabledMessage.isEmpty()) {
                sb.append(TITLE_ATTR_START);
                sb.append(SafeHtmlUtils.fromString(disabledMessage));
                sb.append(TITLE_ATTR_END);
            }
            sb.append(TAG_CLOSE);
        } else {
            super.render(context, object, sb);
        }

        if (isCentralized) {
            sb.appendHtmlConstant("</div>"); //$NON-NLS-1$
        }
    }

    protected abstract boolean canEdit(T object);

    protected String getDisabledMessage(T object) {
        return null;
    }
}
