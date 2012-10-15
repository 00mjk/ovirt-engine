package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.ovirt.engine.api.common.util.DetailHelper;
import org.ovirt.engine.api.common.util.LinkHelper;
import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Disk;
import org.ovirt.engine.api.model.Disks;
import org.ovirt.engine.api.model.Statistic;
import org.ovirt.engine.api.model.Statistics;
import org.ovirt.engine.api.model.StorageDomain;
import org.ovirt.engine.api.resource.VmDiskResource;
import org.ovirt.engine.api.resource.VmDisksResource;
import org.ovirt.engine.core.common.action.AddDiskParameters;
import org.ovirt.engine.core.common.action.AttachDettachVmDiskParameters;
import org.ovirt.engine.core.common.action.RemoveDiskParameters;
import org.ovirt.engine.core.common.action.UpdateVmDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.api.restapi.resource.AbstractBackendSubResource.ParametersProvider;

public class BackendVmDisksResource
        extends AbstractBackendDevicesResource<Disk, Disks, org.ovirt.engine.core.common.businessentities.Disk>
        implements VmDisksResource {

    private static final String SUB_COLLECTIONS = "statistics";

    public BackendVmDisksResource(Guid parentId,
                                VdcQueryType queryType,
                                VdcQueryParametersBase queryParams) {
        super(Disk.class,
              Disks.class,
              org.ovirt.engine.core.common.businessentities.Disk.class,
              parentId,
              queryType,
              queryParams,
              VdcActionType.AddDisk,
              VdcActionType.RemoveDisk,
              VdcActionType.UpdateVmDisk,
              SUB_COLLECTIONS);
    }

    @Override
    public Response add(Disk disk) {
        validateEnums(Disk.class, disk);

        if (disk.isSetId()) {
            return Response.fromResponse(attachDiskToVm(disk))
                           .entity(lookupEntity(asGuid(disk.getId())))
                           .build();
        }else {
            validateParameters(disk, "format", "interface");
            if (!disk.isSetLunStorage() || disk.getLunStorage().getLogicalUnits().isEmpty()) { // lun-disk does not
                                                                                               // require
                                                                                               // size
                validateParameters(disk, "provisionedSize|size");
            }
            return performCreation(addAction,
                    getAddParameters(map(disk), disk),
                    getEntityIdResolver(disk.getName()));
        }
    }

    @Override
    public Response remove(String id, Action action) {
        getEntity(id); //verifies that entity exists, returns 404 otherwise.
        if (action.isSetDetach() && action.isDetach()) {
            return performAction(VdcActionType.DetachDiskFromVm,
                    new AttachDettachVmDiskParameters(parentId, Guid.createGuidFromString(id), true));
        } else {
            return remove(id);
        }
    }

    @Override
    @SingleEntityResource
    public VmDiskResource getDeviceSubResource(String id) {
        return inject(new BackendVmDiskResource(id,
                                              this,
                                              updateType,
                                              getUpdateParametersProvider(),
                                              getRequiredUpdateFields(),
                                              subCollections));
    }

    @Override
    protected boolean matchEntity(org.ovirt.engine.core.common.businessentities.Disk entity, Guid id) {
        return id != null && (id.equals(entity.getId()));
    }

    @Override
    protected boolean matchEntity(org.ovirt.engine.core.common.businessentities.Disk entity, String name) {
        return false;
    }

    @Override
    protected String[] getRequiredAddFields() {
        return new String[] { "provisionedSize|size", "format", "interface" };
    }

    @Override
    protected String[] getRequiredUpdateFields() {
        return new String[0];
    }

    @Override
    protected VdcActionParametersBase getAddParameters(org.ovirt.engine.core.common.businessentities.Disk entity, Disk disk) {
        AddDiskParameters parameters = new AddDiskParameters(parentId, entity);
        if (disk.isSetStorageDomains() && disk.getStorageDomains().getStorageDomains().get(0).isSetId()) {
            parameters.setStorageDomainId(new Guid(disk.getStorageDomains().getStorageDomains().get(0).getId()));
        } else if (disk.isSetStorageDomains() && disk.getStorageDomains().getStorageDomains().get(0).isSetName()) {
            Guid storageDomainId = getStorageDomainId(disk.getStorageDomains().getStorageDomains().get(0).getName());
            if (storageDomainId == null) {
                notFound(StorageDomain.class);
            } else {
                parameters.setStorageDomainId(storageDomainId);
            }
        }
        return parameters;
    }

    private Guid getStorageDomainId(String storageDomainName) {
        List<storage_domains> storageDomains =
                getBackendCollection(storage_domains.class,
                        VdcQueryType.GetAllStorageDomains,
                        new VdcQueryParametersBase());
        for (storage_domains storageDomain : storageDomains) {
            if (storageDomain.getstorage_name().equals(storageDomainName)) {
                return storageDomain.getId();
            }
        }
        return null;
    }

    @Override
    protected VdcActionParametersBase getRemoveParameters(String id) {
        return new RemoveDiskParameters(asGuid(id));
    }

    @Override
    protected ParametersProvider<Disk, org.ovirt.engine.core.common.businessentities.Disk> getUpdateParametersProvider() {
        return new UpdateParametersProvider();
    }

    protected class UpdateParametersProvider implements ParametersProvider<Disk, org.ovirt.engine.core.common.businessentities.Disk> {
        @Override
        public VdcActionParametersBase getParameters(Disk incoming, org.ovirt.engine.core.common.businessentities.Disk entity) {
            return new UpdateVmDiskParameters(parentId, entity.getId(), map(incoming, entity));
        }
    }

    @Override
    protected Disk populate(Disk model, org.ovirt.engine.core.common.businessentities.Disk entity) {
        return addStatistics(model, entity, uriInfo, httpHeaders);
    }

    Disk addStatistics(Disk model, org.ovirt.engine.core.common.businessentities.Disk entity, UriInfo ui, HttpHeaders httpHeaders) {
        if (DetailHelper.include(httpHeaders, "statistics")) {
            model.setStatistics(new Statistics());
            DiskStatisticalQuery query = new DiskStatisticalQuery(newModel(model.getId()));
            List<Statistic> statistics = query.getStatistics(entity);
            for (Statistic statistic : statistics) {
                LinkHelper.addLinks(ui, statistic, query.getParentType());
            }
            model.getStatistics().getStatistics().addAll(statistics);
        }
        return model;
    }

    private Response attachDiskToVm(Disk disk) {
        AttachDettachVmDiskParameters params;
        if (disk.isSetActive()) {
            params = new AttachDettachVmDiskParameters(parentId, Guid.createGuidFromString(disk.getId()), disk.isActive());
        } else {
            params = new AttachDettachVmDiskParameters(parentId, Guid.createGuidFromString(disk.getId()));
        }
        return performAction(VdcActionType.AttachDiskToVm, params);
    }
}
