package org.ovirt.engine.ui.common.widget.label;

import com.google.gwt.user.client.ui.ValueLabel;
import org.ovirt.engine.ui.common.widget.renderer.DiskSizeRenderer;
import org.ovirt.engine.ui.uicommonweb.models.SizeConverter;

public class StorageSizeLabel<T extends Number> extends ValueLabel<T> {

    public StorageSizeLabel() {
        super(new DiskSizeRenderer<T>(SizeConverter.SizeUnit.GB) {
            @Override
            protected boolean isUnavailable(T size) {
                return size == null || size.longValue() == 0;
            }
        });
    }

}
