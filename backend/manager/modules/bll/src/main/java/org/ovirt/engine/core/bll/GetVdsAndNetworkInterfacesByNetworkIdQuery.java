package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.Entities;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VdsNetworkInterface;
import org.ovirt.engine.core.common.queries.NetworkIdParameters;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;

/**
 * A query to retrieve all Host-Network Interface pairs that the given Network is attached to.
 */
public class GetVdsAndNetworkInterfacesByNetworkIdQuery<P extends NetworkIdParameters> extends QueriesCommandBase<P> {

    public GetVdsAndNetworkInterfacesByNetworkIdQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        List<VDS> vdsList = getDbFacade().getVdsDao()
                .getAllForNetwork(getParameters().getNetworkId());
        List<VdsNetworkInterface> vdsNetworkInterfaceList = getDbFacade().getInterfaceDao()
                .getVdsInterfacesByNetworkId(getParameters().getNetworkId());

        final Map<Guid, VDS> vdsById = Entities.businessEntitiesById(vdsList);

        List<Pair<VdsNetworkInterface, VDS>> vdsInterfaceVdsPairs = new ArrayList<Pair<VdsNetworkInterface, VDS>>();
        for (VdsNetworkInterface vdsNetworkInterface : vdsNetworkInterfaceList) {
            vdsInterfaceVdsPairs.add(new Pair(vdsNetworkInterface, vdsById.get(vdsNetworkInterface.getVdsId())));
        }

        getQueryReturnValue().setReturnValue(vdsInterfaceVdsPairs);
    }

}
