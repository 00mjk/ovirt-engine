package org.ovirt.engine.api.restapi.resource;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.common.util.QueryHelper;
import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Vm;
import org.ovirt.engine.api.model.Vms;
import org.ovirt.engine.api.resource.ActionResource;
import org.ovirt.engine.api.resource.StorageDomainContentDisksResource;
import org.ovirt.engine.api.resource.StorageDomainContentResource;
import org.ovirt.engine.core.common.action.ImportVmParameters;
import org.ovirt.engine.core.common.action.RemoveVmFromImportExportParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendStorageDomainVmResource
    extends AbstractBackendStorageDomainContentResource<Vms, Vm, org.ovirt.engine.core.common.businessentities.VM>
    implements StorageDomainContentResource<Vm> {

    public static final String COLLAPSE_SNAPSHOTS = "collapse_snapshots";

    org.ovirt.engine.core.common.businessentities.VM vm;

    public BackendStorageDomainVmResource(BackendStorageDomainVmsResource parent, String vmId) {
        super(vmId, parent, Vm.class, org.ovirt.engine.core.common.businessentities.VM.class, "disks");
    }

    @Override
    protected Vm getFromDataDomain() {
        return performGet(VdcQueryType.GetVmByVmId, new IdQueryParameters(guid));
    }

    @Override
    protected Vm getFromExportDomain() {
        org.ovirt.engine.core.common.businessentities.VM entity = getEntity();
        return addLinks(populate(map(entity, null), entity), null, new String[0]);
    }

    @Override
    public Response register(Action action) {
        validateParameters(action, "cluster.id|name");
        ImportVmParameters params = new ImportVmParameters();
        params.setContainerId(guid);
        params.setStorageDomainId(parent.getStorageDomainId());
        params.setVdsGroupId(getClusterId(action));
        params.setImagesExistOnTargetStorageDomain(true);

        if (action.isSetClone()) {
            params.setImportAsNewEntity(action.isClone());
            if (action.isSetVm() && action.getVm().isSetName()) {
                params.getVm().setName(action.getVm().getName());
            }
        }

        return doAction(VdcActionType.ImportVmFromConfiguration, params, action);
    }

    @Override
    public Response doImport(Action action) {
        validateParameters(action, "cluster.id|name", "storageDomain.id|name");
        Guid destStorageDomainId = getDestStorageDomainId(action);

        ImportVmParameters params = new ImportVmParameters(getEntity(),
                parent.getStorageDomainId(),
                destStorageDomainId,
                parent.getDataCenterId(destStorageDomainId),
                getClusterId(action));
        params.setImageToDestinationDomainMap(getDiskToDestinationMap(action));
        params.setForceOverride(action.isSetExclusive() ? action.isExclusive() : false);

        boolean collapseSnapshots = QueryHelper.getBooleanMatrixParameter(uriInfo, COLLAPSE_SNAPSHOTS, true, false);
        if (collapseSnapshots) {
            params.setCopyCollapse(collapseSnapshots);
        }

        if (action.isSetClone()) {
            params.setImportAsNewEntity(action.isClone());
            if (action.isSetVm() && action.getVm().isSetName()) {
                params.getVm().setName(action.getVm().getName());
            }
        }

        return doAction(VdcActionType.ImportVm, params, action);
    }

    @Override
    public ActionResource getActionResource(String action, String ids) {
        return inject(new BackendActionResource(action, ids));
    }

    @Override
    public StorageDomainContentDisksResource getDisksResource() {
        return inject(new BackendExportDomainDisksResource(this));
    }

    @Override
    protected Vm addParents(Vm vm) {
        vm.setStorageDomain(parent.getStorageDomainModel());
        return vm;
    }

    protected org.ovirt.engine.core.common.businessentities.VM getEntity() {
        if (vm != null) {
            return vm;
        }
        for (org.ovirt.engine.core.common.businessentities.VM entity : parent.getEntitiesFromExportDomain()) {
            if (guid.equals(entity.getId())) {
                vm = entity;
                return entity;
            }
        }
        return entityNotFound();
    }

    @Override
    public java.util.Map<Guid, Disk> getDiskMap() {
        return getEntity().getDiskMap();
    }

    @Override
    public Response remove() {
        get();
        RemoveVmFromImportExportParameters params = new RemoveVmFromImportExportParameters(
                guid,
                parent.storageDomainId,
                getDataCenterId(parent.storageDomainId));
        return performAction(VdcActionType.RemoveVmFromImportExport, params);
    }

}
