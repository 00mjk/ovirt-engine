package org.ovirt.engine.core.common.action;

import java.io.Serializable;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.ovirt.engine.core.common.businessentities.VmBackup;
import org.ovirt.engine.core.compat.Guid;

public class VmBackupParameters extends VmOperationParameterBase implements Serializable {
    private static final long serialVersionUID = -3821623510049174551L;

    @Valid
    @NotNull
    private VmBackup vmBackup;
    private boolean backupInitiated;
    private Guid toCheckpointId;

    public VmBackupParameters() {
    }

    public VmBackupParameters(VmBackup vmBackup) {
        this.vmBackup = vmBackup;
    }

    public VmBackup getVmBackup() {
        return vmBackup;
    }

    public void setVmBackup(VmBackup value) {
        vmBackup = value;
    }

    public boolean isBackupInitiated() {
        return backupInitiated;
    }

    public void setBackupInitiated(boolean backupInitiated) {
        this.backupInitiated = backupInitiated;
    }

    public Guid getToCheckpointId() {
        return toCheckpointId;
    }

    public void setToCheckpointId(Guid toCheckpointId) {
        this.toCheckpointId = toCheckpointId;
    }

    @Override
    public Guid getVmId() {
        return getVmBackup() != null ? getVmBackup().getVmId() : null;
    }
}
