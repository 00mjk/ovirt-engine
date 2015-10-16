package org.ovirt.engine.api.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.DiskProfile;
import org.ovirt.engine.api.model.DiskProfiles;

@Path("/diskprofiles")
@Produces({ ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON })
public interface DiskProfilesResource {
    @GET
    DiskProfiles list();

    @POST
    @Consumes({ ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON })
    Response add(DiskProfile diskProfile);

    @Path("{id}")
    DiskProfileResource getDiskProfileResource(@PathParam("id") String id);
}
