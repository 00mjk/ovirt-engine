package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Disk;
import org.ovirt.engine.api.model.Disks;
import org.ovirt.engine.api.model.StorageDomain;
import org.ovirt.engine.api.model.StorageType;
import org.ovirt.engine.api.resource.DiskResource;
import org.ovirt.engine.api.resource.DisksResource;
import org.ovirt.engine.api.restapi.resource.utils.DiskResourceUtils;
import org.ovirt.engine.core.common.action.AddDiskParameters;
import org.ovirt.engine.core.common.action.RemoveDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.GetDiskByDiskIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendDisksResource extends AbstractBackendCollectionResource<Disk, org.ovirt.engine.core.common.businessentities.Disk> implements DisksResource{

    private static final String SUB_COLLECTIONS = "statistics";
    public BackendDisksResource() {
        super(Disk.class, org.ovirt.engine.core.common.businessentities.Disk.class, SUB_COLLECTIONS);
    }

    @Override
    public Response add(Disk disk) {
        validateDiskForCreation(disk);
        AddDiskParameters params = new AddDiskParameters();
        params.setDiskInfo(getMapper(Disk.class, org.ovirt.engine.core.common.businessentities.Disk.class).map(disk, null));
        if (disk.isSetStorageDomains() && disk.getStorageDomains().isSetStorageDomains() && disk.getStorageDomains().getStorageDomains().get(0).isSetId()) {
            params.setStorageDomainId(Guid.createGuidFromString(disk.getStorageDomains().getStorageDomains().get(0).getId()));
        } else if (disk.isSetStorageDomains() && disk.getStorageDomains().getStorageDomains().get(0).isSetName()) {
            Guid storageDomainId = getStorageDomainId(disk.getStorageDomains().getStorageDomains().get(0).getName());
            if (storageDomainId == null) {
                notFound(StorageDomain.class);
            } else {
                params.setStorageDomainId(storageDomainId);
            }
        }
        return performCreation(VdcActionType.AddDisk, params,
                new QueryIdResolver(VdcQueryType.GetDiskByDiskId, GetDiskByDiskIdParameters.class));
    }

    protected void validateDiskForCreation(Disk disk) {
        validateParameters(disk, "format", "interface");
        if (DiskResourceUtils.isLunDisk(disk)) {
            validateParameters(disk.getLunStorage(), "type"); // when creating a LUN disk, user must specify type.
            StorageType storageType = StorageType.fromValue(disk.getLunStorage().getType());
            if (storageType!=null && storageType==StorageType.ISCSI) {
                    validateParameters(disk.getLunStorage().getLogicalUnits().get(0), "address", "target", "port");
            }
        } else {
            validateParameters(disk, "provisionedSize|size"); // Non lun disks require size
        }
        validateEnums(Disk.class, disk);
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
    public Disks list() {
        return mapCollection(getBackendCollection(SearchType.Disk));
    }

    @Override
    public DiskResource getDeviceSubResource(String id) {
        return inject(new BackendDiskResource(id));
    }

    @Override
    protected Response performRemove(String id) {
        return performAction(VdcActionType.RemoveDisk, new RemoveDiskParameters(Guid.createGuidFromString(id)));
    }

    protected Disks mapCollection(List<org.ovirt.engine.core.common.businessentities.Disk> entities) {
        Disks collection = new Disks();
        for (org.ovirt.engine.core.common.businessentities.Disk disk : entities) {
            collection.getDisks().add(addLinks(map(disk)));
        }
        return collection;
    }
}
