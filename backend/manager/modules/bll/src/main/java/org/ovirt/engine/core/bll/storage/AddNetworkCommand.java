package org.ovirt.engine.core.bll.storage;

import java.util.List;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.AddNetworkStoragePoolParameters;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;

public class AddNetworkCommand<T extends AddNetworkStoragePoolParameters> extends NetworkCommon<T> {
    public AddNetworkCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void executeCommand() {
        getParameters().getNetwork().setId(Guid.NewGuid());
        DbFacade.getInstance().getNetworkDao().save(getParameters().getNetwork());
        getReturnValue().setActionReturnValue(getParameters().getNetwork().getId());
        setSucceeded(true);
    }

    @Override
    protected boolean canDoAction() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ADD);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__NETWORK);

        if (!validateVmNetwork()) {
            return false;
        }

        if (!validateStpProperty()) {
            return false;
        }

        if (!validateMTUOverrideSupport()) {
            return false;
        }

        // check that network name not start with 'bond'
        if (getParameters().getNetwork().getname().toLowerCase().startsWith("bond")) {
            addCanDoActionMessage(VdcBllMessages.NETWORK_CANNOT_CONTAIN_BOND_NAME);
            return false;
        }

        // we return ok only if the network not exists
        List<Network> all;
        if (getParameters().getNetwork().getstorage_pool_id() != null
                && !getParameters().getNetwork().getstorage_pool_id().getValue().equals(Guid.Empty)) {
            all = DbFacade.getInstance().getNetworkDao().getAllForDataCenter(
                    getParameters().getNetwork().getstorage_pool_id().getValue());
        } else {
            all = DbFacade.getInstance().getNetworkDao().getAll();
        }
        boolean exists = null != LinqUtils.firstOrNull(all, new Predicate<Network>() {
            @Override
            public boolean eval(Network net) {
                return net.getname().equals(getParameters().getNetwork().getname());
            }
        });
        if (exists) {
            getReturnValue().getCanDoActionMessages()
                    .add(VdcBllMessages.NETWORK_NAME_ALREADY_EXISTS.toString());
            return false;
        }

        if (!validateVlanId(all)) {
            return false;
        }

        return true;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.NETWORK_ADD_NETWORK : AuditLogType.NETWORK_ADD_NETWORK_FAILED;
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(CreateEntity.class);
        return super.getValidationGroups();
    }
}
