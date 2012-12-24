package org.ovirt.engine.api.restapi.resource;

import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Hook;
import org.ovirt.engine.api.model.Hooks;
import org.ovirt.engine.api.resource.HostHookResource;
import org.ovirt.engine.api.resource.HostHooksResource;
import org.ovirt.engine.core.common.queries.GetVdsHooksByIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendHostHooksResource extends AbstractBackendCollectionResource<Hook, Object> implements HostHooksResource {

    public BackendHostHooksResource(String hostId) {
        super(Hook.class, Object.class);
        this.hostId = hostId;
    }

    private String hostId;

    @Override
    @GET
    public Hooks list() {
        @SuppressWarnings("unchecked")
        HashMap<String, HashMap<String, HashMap<String, String>>> hooksMap =
                getEntity(HashMap.class, VdcQueryType.GetVdsHooksById2,
                        new GetVdsHooksByIdParameters(asGuid(hostId)), null);
        return mapCollection(hooksMap);
    }

    private Hooks mapCollection(HashMap<String, HashMap<String, HashMap<String, String>>> hooksMap) {
        Hooks hooks = mappingLocator.getMapper(HashMap.class, Hooks.class).map(hooksMap, null);
        return hooks;
    }

    @Override
    @Path("{id}")
    public HostHookResource getHookSubResource(@PathParam("id") String id) {
        return inject(new BackendHostHookResource(id, this));
    }

    @Override
    protected Response performRemove(String id) {
        // not in use
        return null;
    }

}
