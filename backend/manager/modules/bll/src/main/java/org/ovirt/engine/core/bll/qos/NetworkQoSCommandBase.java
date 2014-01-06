package org.ovirt.engine.core.bll.qos;

import org.ovirt.engine.core.bll.CommandBase;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.NetworkQoSParametersBase;
import org.ovirt.engine.core.common.businessentities.network.NetworkQoS;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.dao.network.NetworkQoSDao;

import java.util.Collections;
import java.util.List;


public abstract class NetworkQoSCommandBase extends CommandBase<NetworkQoSParametersBase> {

    private NetworkQoS networkQoS;

    public NetworkQoSCommandBase(NetworkQoSParametersBase parameters) {
        super(parameters);
        if (getNetworkQoS() != null) {
            setStoragePoolId(getNetworkQoS().getStoragePoolId());
            addCustomValue("NetworkQoSName", getNetworkQoS().getName());
        }
        getParameters().setShouldBeLogged(true);
    }

    public NetworkQoS getNetworkQoS() {
        if (networkQoS == null) {
            if (getParameters().getNetworkQoS() == null) {
                if (getParameters().getNetworkQoSGuid() != null) {
                    getNetworkQoSDao().get(getParameters().getNetworkQoSGuid());
                }
            } else {
                networkQoS = getParameters().getNetworkQoS();
            }
        }
        return networkQoS;
    }

    protected boolean validateParameters() {
        if (getNetworkQoS() == null) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_NETWORK_QOS_NOT_FOUND);
        }
        return true;
    }

    protected boolean validateValues() {
        if(missingValues()) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_NETWORK_QOS_MISSING_VALUES);
        }

        if (peakLowerThanAverage()) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_NETWORK_QOS_PEAK_LOWER_THAN_AVERAGE);
        }

        return true;
    }

    protected  boolean peakLowerThanAverage() {
        return (getNetworkQoS().getInboundPeak() != null
                && getNetworkQoS().getInboundPeak() < getNetworkQoS().getInboundAverage())
                || (getNetworkQoS().getOutboundPeak() != null
                && getNetworkQoS().getOutboundPeak() < getNetworkQoS().getOutboundAverage());
    }

    protected boolean missingValues() {
        return missingValue(getNetworkQoS().getInboundAverage(),
                getNetworkQoS().getInboundPeak(),
                getNetworkQoS().getInboundBurst())
                || missingValue(getNetworkQoS().getOutboundAverage(),
                getNetworkQoS().getOutboundPeak(),
                getNetworkQoS().getOutboundBurst());
    }

    private boolean missingValue(Integer average, Integer peak, Integer burst) {
        return (average != null || peak != null || burst != null)
                && (average == null || peak == null || burst == null);
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(getStoragePoolId(),
                VdcObjectType.StoragePool, getActionType().getActionGroup()));
    }

    protected NetworkQoSDao getNetworkQoSDao() {
        return getDbFacade().getQosDao();
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__NETWORK_QOS);
    }
}
