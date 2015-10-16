package org.ovirt.engine.api.restapi.resource;

import java.lang.reflect.Method;
import java.util.List;

import org.ovirt.engine.api.model.BaseDevice;
import org.ovirt.engine.api.model.BaseResources;
import org.ovirt.engine.api.model.Vm;
import org.ovirt.engine.api.resource.ReadOnlyDeviceResource;
import org.ovirt.engine.api.resource.ReadOnlyDevicesResource;
import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public abstract class AbstractBackendReadOnlyDevicesResource<D extends BaseDevice, C extends BaseResources, Q extends IVdcQueryable>
        extends AbstractBackendCollectionResource<D, Q>
        implements ReadOnlyDevicesResource<D, C> {

    protected Class<C> collectionType;
    protected Guid parentId;
    protected VdcQueryType queryType;
    protected VdcQueryParametersBase queryParams;

    public AbstractBackendReadOnlyDevicesResource(Class<D> modelType,
                                                  Class<C> collectionType,
                                                  Class<Q> entityType,
                                                  Guid parentId,
                                                  VdcQueryType queryType,
                                                  VdcQueryParametersBase queryParams,
                                                  String... subCollections) {
        super(modelType, entityType, subCollections);
        this.collectionType = collectionType;
        this.parentId = parentId;
        this.queryType = queryType;
        this.queryParams = queryParams;
    }

    @Override
    public C list() {
        return mapCollection(getBackendCollection(queryType, queryParams));
    }

    @Override
    public ReadOnlyDeviceResource<D> getDeviceResource(String id) {
        return inject(new BackendReadOnlyDeviceResource<D, C, Q>(modelType, entityType, asGuidOr404(id), this));
    }

    @Override
    public D addParents(D device) {
        // REVISIT: this can also be a template
        device.setVm(new Vm());
        device.getVm().setId(parentId.toString());
        return device;
    }

    protected C mapCollection(List<Q> entities, boolean addLinks) {
        C collection = instantiate(collectionType);
        List<D> list = getList(collection);
        if (entities != null) {
            for (Q entity : entities) {
                D candidate = populate(map(entity), entity);
                if (validate(candidate)) {
                    if (addLinks) {
                        candidate = addLinks(candidate);
                    }
                    list.add(candidate);
                }
            }
        }
        return collection;
    }

    protected C mapCollection(List<Q> entities) {
        return mapCollection(entities, true);
    }

    protected boolean validate(D device) {
        return true;
    }

    @SuppressWarnings("unchecked")
    protected List<D> getList(C collection) {
        for (Method m : collectionType.getMethods()) {
            if (m.getName().equals("get" + collectionType.getSimpleName())) {
                try {
                    return (List<D>)m.invoke(collection);
                } catch (Exception e) {
                    // simple getter shouldn't fail
                }
            }
        }
        return null;
    }

    public Q lookupEntity(Guid id) {
        for (Q entity : getBackendCollection(queryType, queryParams)) {
            if (matchEntity(entity, id)) {
                return entity;
            }
        }
        return null;
    }

    protected abstract <T> boolean matchEntity(Q entity, T id);
}
