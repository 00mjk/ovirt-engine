package org.ovirt.engine.api.restapi.resource;

import org.ovirt.engine.api.model.Disk;
import org.ovirt.engine.api.resource.CreationResource;
import org.ovirt.engine.api.resource.DiskResource;
import org.ovirt.engine.api.resource.StatisticsResource;
import org.ovirt.engine.core.common.queries.GetDiskByDiskIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendDiskResource extends AbstractBackendSubResource<Disk, org.ovirt.engine.core.common.businessentities.Disk> implements DiskResource {

    protected BackendDiskResource(String id) {
        super(id, Disk.class, org.ovirt.engine.core.common.businessentities.Disk.class);
    }

    @Override
    public CreationResource getCreationSubresource(String ids) {
        return inject(new BackendCreationResource(ids));
    }

    @Override
    public StatisticsResource getStatisticsResource() {
        QueryIdResolver resolver = new QueryIdResolver(VdcQueryType.GetDiskByDiskId, GetDiskByDiskIdParameters.class);
        DiskStatisticalQuery query = new DiskStatisticalQuery(resolver, newModel(id));
        return inject(new BackendStatisticsResource<Disk, org.ovirt.engine.core.common.businessentities.Disk>(entityType, guid, query));
    }

    @Override
    public Disk get() {
        return performGet(VdcQueryType.GetDiskByDiskId, new GetDiskByDiskIdParameters(guid));
    }
}
