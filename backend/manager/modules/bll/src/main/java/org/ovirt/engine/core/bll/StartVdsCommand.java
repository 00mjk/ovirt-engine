package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.FenceVdsActionParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBllMessages;

/**
 * Send a Start action to a power control device.
 *
 * This command should be run mutually exclusive from other fence actions to prevent same action or other fence actions
 * to clear the VMs and start them.
 *
 * @see RestartVdsCommand
 * @see FenceVdsBaseCommand#restartVdsVms()
 */
@LockIdNameAttribute
@NonTransactiveCommandAttribute
public class StartVdsCommand<T extends FenceVdsActionParameters> extends FenceVdsBaseCommand<T> {
    public StartVdsCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected boolean canDoAction() {
        boolean retValue = super.canDoAction();
        VDS vds = getVds();
        if (vds != null) {
            VDSStatus vdsStatus = vds.getStatus();
            if (vdsStatus == VDSStatus.Connecting) {
                retValue = false;
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VDS_INTERMITENT_CONNECTIVITY);

            } else if (!legalStatusForStartingVds(vdsStatus)) {
                addCanDoActionMessage(VdcBllMessages.VDS_STATUS_NOT_VALID_FOR_START);
                retValue = false;
                log.errorFormat("VDS status for vds {0}:{1} is {2}", vds.getId(), vds.getName(), vdsStatus);
            }
        }
        return retValue;
    }

    protected boolean legalStatusForStartingVds(VDSStatus status) {
        return status == VDSStatus.Down || status == VDSStatus.NonResponsive || status == VDSStatus.Reboot || status == VDSStatus.Maintenance;
    }

    @Override
    protected void setStatus() {
        setStatus(VDSStatus.NonResponsive);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__START);
    }

    @Override
    protected void handleError() {
        log.errorFormat("Failed to run StartVdsCommand on vds :{0}", getVdsName());
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        addCanDoActionMessage(VdcBllMessages.VDS_FENCE_OPERATION_FAILED);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__HOST);
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__START);

        return getSucceeded() ? AuditLogType.USER_VDS_START : AuditLogType.USER_FAILED_VDS_START;
    }

    @Override
    protected void handleSpecificCommandActions() {
        restartVdsVms();
    }

    @Override
    protected int getRerties() {
        return Config.<Integer> GetValue(ConfigValues.FenceStartStatusRetries);
    }

    @Override
    protected int getDelayInSeconds() {
        return Config.<Integer> GetValue(ConfigValues.FenceStartStatusDelayBetweenRetriesInSec);
    }

    @Override
    protected void freeLock() {
        if (getParameters().getParentCommand() != VdcActionType.RestartVds) {
            super.freeLock();
        }
    }
}
