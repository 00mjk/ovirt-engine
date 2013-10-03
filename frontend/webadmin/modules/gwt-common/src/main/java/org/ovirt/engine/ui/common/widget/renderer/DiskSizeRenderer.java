package org.ovirt.engine.ui.common.widget.renderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.text.shared.AbstractRenderer;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.uicommonweb.models.SizeConverter;

public class DiskSizeRenderer<T extends Number> extends AbstractRenderer<T> {

    private final SizeConverter.SizeUnit unit;

    private static final CommonApplicationConstants CONSTANTS = GWT.create(CommonApplicationConstants.class);

    public DiskSizeRenderer(SizeConverter.SizeUnit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("The unit can not be null!"); //$NON-NLS-1$
        }

        this.unit = unit;
    }

    protected boolean isUnavailable(T size) {
        return size == null;
    }

    @Override
    public String render(T size) {
        if (isUnavailable(size)) {
            return CONSTANTS.unAvailablePropertyLabel();
        }

        long sizeInGB = SizeConverter.convert(size.longValue(), unit, SizeConverter.SizeUnit.GB).longValue();
        return sizeInGB >= 1 ? sizeInGB + " GB" : "< 1 GB"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
