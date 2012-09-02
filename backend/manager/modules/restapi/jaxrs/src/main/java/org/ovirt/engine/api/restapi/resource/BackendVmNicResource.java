package org.ovirt.engine.api.restapi.resource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Fault;
import org.ovirt.engine.api.model.NIC;
import org.ovirt.engine.api.model.Nics;
import org.ovirt.engine.api.resource.ActionResource;
import org.ovirt.engine.api.resource.VmNicResource;
import org.ovirt.engine.core.common.action.ActivateDeactivateVmNicParameters;
import org.ovirt.engine.core.common.action.PlugAction;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.api.model.Network;
import org.ovirt.engine.api.model.Networks;
import org.ovirt.engine.api.model.PortMirroring;

public class BackendVmNicResource extends BackendNicResource implements VmNicResource {

    protected BackendVmNicResource(String id,
                                   AbstractBackendReadOnlyDevicesResource<NIC, Nics, VmNetworkInterface> collection,
                                   VdcActionType updateType,
                                   ParametersProvider<NIC, VmNetworkInterface> updateParametersProvider,
                                   String[] requiredUpdateFields,
                                   String[] subCollections) {
        super(id, collection, updateType, updateParametersProvider, requiredUpdateFields, subCollections);
    }

    @Override
    protected NIC populate(NIC model, VmNetworkInterface entity) {
        BackendVmNicsResource parent = (BackendVmNicsResource)collection;
        Guid clusterId = parent.getClusterId();
        org.ovirt.engine.core.common.businessentities.Network network = parent.getClusterNetwork(clusterId, null, model.getNetwork().getName());
        String networkId = network == null ? null : network.getId().toString();
        model.getNetwork().setId(networkId);
        model.getNetwork().setName(null);
        if (entity.isPortMirroring()) {
            PortMirroring portMirroring = new PortMirroring();
            Networks networks = new Networks();
            Network net = new Network();
            net.setId(networkId);
            net.setName(network.getname());
            portMirroring.setNetworks(networks);
            portMirroring.getNetworks().getNetworks().add(net);
            model.setPortMirroring(portMirroring);
        }
        return parent.addStatistics(model, entity, uriInfo, httpHeaders);
    }

    @Override
    public NIC update(NIC device) {
        //TODO: this is temporary mapping between engine boolean port mirroring parameter, and REST
        //      port mirroring network collection, next engine version will support the network collection
        //      in port mirroring
        validateEnums(NIC.class, device);
        boolean fault = false;
        String faultString = "The port mirroring network must match the Network set on the NIC";
        boolean isPortMirroring = device.isSetPortMirroring() && device.getPortMirroring().isSetNetworks();
        boolean isPortMirroringExceeded =
                isPortMirroring && device.getPortMirroring().getNetworks().getNetworks().size() > 1;
        isPortMirroring = isPortMirroring && device.getPortMirroring().getNetworks().getNetworks().size() == 1;
        if (isPortMirroringExceeded) {
            fault = true;
            faultString = "Cannot set more than one network in port mirroring mode";
        }
        String networkId =
                (device.isSetNetwork() && device.getNetwork().isSetId()) ? device.getNetwork().getId() : null;
        String networkName =
                (device.isSetNetwork() && device.getNetwork().isSetName()) ? device.getNetwork().getName() : null;
        if (!fault && isPortMirroring) {
            Network pmNetwork = device.getPortMirroring().getNetworks().getNetworks().get(0);
            String pmNetworkId = (pmNetwork.isSetId() ? pmNetwork.getId() : null);
            String pmNetworkName = (pmNetwork.isSetName() ? pmNetwork.getName() : null);
            if (pmNetworkId != null) {
                if (networkId == null) {
                    networkId = (networkName != null) ? getNetworkId(networkName) : get().getNetwork().getId();
                }
                fault = (!pmNetworkId.equals(networkId));
            } else if (pmNetworkName != null) {
                if (networkName == null) {
                    if (networkId == null) {
                        networkId = get().getNetwork().getId();
                    }
                    pmNetworkId = getNetworkId(pmNetworkName);
                    fault = (!networkId.equals(pmNetworkId));
                }
                fault = fault || (!pmNetworkName.equals(networkName));
            } else {
                fault = true;
                faultString = "Network must have name or id property for port mirroring";
            }
        }
        if (fault) {
            Fault f = new Fault();
            f.setReason(faultString);
            Response response = Response.status(Response.Status.BAD_REQUEST).entity(f).build();
            throw new WebApplicationException(response);
        }
        return super.update(device);
    }

    private String getNetworkId(String networkName) {
        BackendVmNicsResource parent = (BackendVmNicsResource) collection;
        Guid clusterId = parent.getClusterId();
        org.ovirt.engine.core.common.businessentities.Network n =
                parent.getClusterNetwork(clusterId, null, networkName);
        if (n != null) {
            return n.getId().toString();
        }
        return null;
    }

    @Override
    public ActionResource getActionSubresource(String action, String oid) {
        return null;
    }

    @Override
    public Response activate(Action action) {
        ActivateDeactivateVmNicParameters params = new ActivateDeactivateVmNicParameters(guid, PlugAction.PLUG);
        BackendNicsResource parent = (BackendNicsResource) collection;
        params.setVmId(parent.parentId);
        return doAction(VdcActionType.ActivateDeactivateVmNic, params, action);
    }

    @Override
    public Response deactivate(Action action) {
        ActivateDeactivateVmNicParameters params = new ActivateDeactivateVmNicParameters(guid, PlugAction.UNPLUG);
        params.setVmId(((BackendNicsResource) collection).parentId);
        return doAction(VdcActionType.ActivateDeactivateVmNic, params, action);
    }

    @Override
    public NIC get() {
        return super.get();//explicit call solves REST-Easy confusion
    }
}
