package org.ovirt.engine.core.common.action;

import java.io.Serializable;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.compat.Guid;

public class VmTemplateParametersBase extends VdcActionParametersBase implements Serializable {
    private static final long serialVersionUID = -8930994274659598061L;
    private boolean removeTemplateFromDb;
    private Guid vmTemplateId;
    private Guid quotaId;
    private boolean privateCheckDisksExists;
    private VmWatchdog watchdog;
    private Boolean virtioScsiEnabled;
    private boolean balloonEnabled;

    /*
     * see VmManagementParametersBase#updateWatchdog for details
     */
    private boolean updateWatchdog;


    public boolean getCheckDisksExists() {
        return privateCheckDisksExists;
    }

    public void setCheckDisksExists(boolean value) {
        privateCheckDisksExists = value;
    }

    public VmTemplateParametersBase(Guid vmTemplateId) {
        this.vmTemplateId = vmTemplateId;
    }

    public Guid getVmTemplateId() {
        return vmTemplateId;
    }

    private List<Guid> privateStorageDomainsList;

    public List<Guid> getStorageDomainsList() {
        return privateStorageDomainsList;
    }

    public void setStorageDomainsList(List<Guid> value) {
        privateStorageDomainsList = value;
    }

    public VmTemplateParametersBase() {
        vmTemplateId = Guid.Empty;
    }

    public void setRemoveTemplateFromDb(boolean removeTemplateFromDb) {
        this.removeTemplateFromDb = removeTemplateFromDb;
    }

    public boolean isRemoveTemplateFromDb() {
        return removeTemplateFromDb;
    }

    public Guid getQuotaId() {
        return quotaId;
    }

    public void setQuotaId(Guid value) {
        quotaId = value;
    }

    public VmWatchdog getWatchdog() {
        return watchdog;
    }

    public void setWatchdog(VmWatchdog watchdog) {
        this.watchdog = watchdog;
    }

    public boolean isUpdateWatchdog() {
        return updateWatchdog;
    }

    public void setUpdateWatchdog(boolean updateWatchdog) {
        this.updateWatchdog = updateWatchdog;
    }

    public Boolean isVirtioScsiEnabled() {
        return virtioScsiEnabled;
    }

    public void setVirtioScsiEnabled(Boolean virtioScsiEnabled) {
        this.virtioScsiEnabled = virtioScsiEnabled;
    }

    public boolean isBalloonEnabled() {
        return balloonEnabled;
    }

    public void setBalloonEnabled(boolean balloonEnabled) {
        this.balloonEnabled = balloonEnabled;
    }
}
