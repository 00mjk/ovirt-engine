package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.compat.Guid;

public class StorageDomainParametersBase extends StoragePoolParametersBase {
    private static final long serialVersionUID = -3426166529161499883L;

    private Guid storageDomainId = new Guid();
    private boolean isInternal;

    public StorageDomainParametersBase() {
    }

    public StorageDomainParametersBase(Guid storageDomainId) {
        super(Guid.Empty);
        setStorageDomainId(storageDomainId);
    }

    public Guid getStorageDomainId() {
        return storageDomainId;
    }

    public void setStorageDomainId(Guid value) {
        storageDomainId = value;
    }

    public boolean getIsInternal() {
        return isInternal;
    }

    public void setIsInternal(boolean value) {
        isInternal = value;
    }
}
