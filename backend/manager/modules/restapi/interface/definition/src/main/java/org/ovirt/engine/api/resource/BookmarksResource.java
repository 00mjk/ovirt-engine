package org.ovirt.engine.api.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Bookmark;
import org.ovirt.engine.api.model.Bookmarks;

@Path("/bookmarks")
@Produces({ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON})
public interface BookmarksResource {
    @GET
    Bookmarks list();

    @POST
    @Consumes({ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON})
    Response add(Bookmark bookmark);

    @Path("{id}")
    BookmarkResource getBookmarkResource(@PathParam("id") String id);
}
