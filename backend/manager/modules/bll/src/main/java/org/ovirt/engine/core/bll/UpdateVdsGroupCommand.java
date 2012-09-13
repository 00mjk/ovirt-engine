package org.ovirt.engine.core.bll;

import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.storage.StorageHandlingCommandBase;
import org.ovirt.engine.core.bll.utils.VersionSupport;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.action.VdsGroupOperationParameters;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.common.businessentities.NetworkStatus;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VdsSelectionAlgorithm;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.businessentities.network_cluster;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.validation.group.UpdateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.NetworkDAO;
import org.ovirt.engine.core.dao.VdsStaticDAO;

@SuppressWarnings("serial")
public class UpdateVdsGroupCommand<T extends VdsGroupOperationParameters> extends
        VdsGroupOperationCommandBase<T> {
    public UpdateVdsGroupCommand(T parameters) {
        super(parameters);
    }

    protected UpdateVdsGroupCommand(Guid commandId) {
        super(commandId);
    }

    @Override
    protected void executeCommand() {
        // TODO: This code should be revisited and proper compensation logic should be introduced here
        VDSGroup oldGroup = getVdsGroupDAO().get(getParameters().getVdsGroup().getId());
        CheckMaxMemoryOverCommitValue();
        getVdsGroupDAO().update(getParameters().getVdsGroup());

        if (oldGroup.getstorage_pool_id() != null
                && !oldGroup.getstorage_pool_id().equals(getVdsGroup().getstorage_pool_id())
                || oldGroup.getstorage_pool_id() == null
                && getVdsGroup().getstorage_pool_id() != null) {
            for (VdsStatic vds : getVdsStaticDAO().getAllForVdsGroup(oldGroup.getId())) {
                VdsActionParameters parameters = new VdsActionParameters(vds.getId());
                if (oldGroup.getstorage_pool_id() != null) {
                    VdcReturnValueBase removeVdsSpmIdReturn =
                            getBackend().runInternalAction(VdcActionType.RemoveVdsSpmId,
                                    parameters);
                    if (!removeVdsSpmIdReturn.getSucceeded()) {
                        setSucceeded(false);
                        getReturnValue().setFault(removeVdsSpmIdReturn.getFault());
                        return;
                    }
                }
                if (getVdsGroup().getstorage_pool_id() != null) {
                    VdcReturnValueBase addVdsSpmIdReturn =
                            getBackend().runInternalAction(VdcActionType.AddVdsSpmId, parameters);
                    if (!addVdsSpmIdReturn.getSucceeded()) {
                        setSucceeded(false);
                        getReturnValue().setFault(addVdsSpmIdReturn.getFault());
                        return;
                    }
                }
            }
        }

        // when changing data center we check that default networks exists in
        // cluster
        List<Network> networks = getNetworkDAO()
                .getAllForCluster(getVdsGroup().getId());
        boolean exists = false;
        String managementNetwork = Config.<String> GetValue(ConfigValues.ManagementNetwork);
        for (Network net : networks) {
            if (StringUtils.equals(net.getname(), managementNetwork)) {
                exists = true;
            }
        }
        if (!exists) {
            if (getVdsGroup().getstorage_pool_id() != null) {
                List<Network> storagePoolNets =
                        getNetworkDAO()
                                .getAllForDataCenter(
                                        getVdsGroup().getstorage_pool_id()
                                                .getValue());
                for (Network net : storagePoolNets) {
                    if (StringUtils.equals(net.getname(), managementNetwork)) {
                        getNetworkClusterDAO().save(new network_cluster(getVdsGroup().getId(), net.getId(),
                                NetworkStatus.Operational, true, true));
                    }
                }
            }
        }

        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getParameters().getIsInternalCommand()) {
            return getSucceeded() ? AuditLogType.SYSTEM_UPDATE_VDS_GROUP
                    : AuditLogType.SYSTEM_UPDATE_VDS_GROUP_FAILED;
        }

        return getSucceeded() ? AuditLogType.USER_UPDATE_VDS_GROUP
                : AuditLogType.USER_UPDATE_VDS_GROUP_FAILED;
    }

    @SuppressWarnings("null")
    @Override
    protected boolean canDoAction() {
        boolean result = super.canDoAction();
        boolean hasVms = false;
        getReturnValue().getCanDoActionMessages()
                .add(VdcBllMessages.VAR__ACTION__UPDATE.toString());
        VDSGroup oldGroup = getVdsGroupDAO().get(getVdsGroup().getId());
        // check that if name was changed, it was done to the same cluster
        VDSGroup groupWithName = getVdsGroupDAO().getByName(getVdsGroup().getname());
        if (oldGroup != null && !StringUtils.equals(oldGroup.getname(), getVdsGroup().getname())) {
            if (groupWithName != null && !groupWithName.getId().equals(getVdsGroup().getId())) {
                addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_DO_ACTION_NAME_IN_USE);
                result = false;
            }
        }
        if (oldGroup == null) {
            addCanDoActionMessage(VdcBllMessages.VDS_CLUSTER_IS_NOT_VALID);
            result = false;
        }
        // If both original Cpu and new Cpu are null, don't check Cpu validity
        if (result && (oldGroup.getcpu_name() != null || getVdsGroup().getcpu_name() != null)) {
            // Check that cpu exist
            if (!checkIfCpusExist()) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_CPU_NOT_FOUND);

                addCanDoActionMessage(VdcBllMessages.VAR__TYPE__CLUSTER);
                result = false;
            } else {
                // if cpu changed from intel to amd (or backwards) and there are
                // vds in this cluster, cannot update
                if (!StringUtils.isEmpty(oldGroup.getcpu_name())
                        && !checkIfCpusSameManufacture(oldGroup)
                        && getVdsStaticDAO().getAllForVdsGroup(getVdsGroup().getId()).size() > 0) {
                    addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_UPDATE_CPU_ILLEGAL);
                    result = false;
                }
            }
        }
        if (result && !VersionSupport.checkVersionSupported(getVdsGroup()
                .getcompatibility_version())) {
            addCanDoActionMessage(VersionSupport.getUnsupportedVersionMessage());
            result = false;
        }
        // decreasing of compatibility version is not allowed
        if (result && getVdsGroup().getcompatibility_version().compareTo(oldGroup.getcompatibility_version()) < 0) {
            result = false;
            getReturnValue()
                    .getCanDoActionMessages()
                    .add(VdcBllMessages.ACTION_TYPE_FAILED_CANNOT_DECREASE_COMPATIBILITY_VERSION
                            .toString());
        }
        if (result) {
            SearchParameters p = new SearchParameters(MessageFormat.format(
                    StorageHandlingCommandBase.UpVdssInCluster, oldGroup.getname()),
                    SearchType.VDS);
            p.setMaxCount(Integer.MAX_VALUE);
            @SuppressWarnings("unchecked")
            Iterable<VDS> clusterUpVdss =
                    (Iterable<VDS>) getBackend().runInternalQuery(VdcQueryType.Search, p).getReturnValue();
            for (VDS vds : clusterUpVdss) {
                if (!VersionSupport.checkClusterVersionSupported(
                        getVdsGroup().getcompatibility_version(), vds)) {
                    result = false;
                    getReturnValue()
                            .getCanDoActionMessages()
                            .add(VdcBllMessages.VDS_GROUP_CANNOT_UPDATE_COMPATIBILITY_VERSION_WITH_LOWER_HOSTS
                                    .toString());
                    break;
                } else if (missingServerCpuFlags(vds) != null) {
                    getReturnValue().getCanDoActionMessages().add(
                            VdcBllMessages.VDS_GROUP_CANNOT_UPDATE_CPU_WITH_LOWER_HOSTS
                                    .toString());
                    result = false;
                    break;
                }
            }
        }

        if (result && (oldGroup.getstorage_pool_id() != null
                && !oldGroup.getstorage_pool_id().equals(getVdsGroup().getstorage_pool_id()))) {
            addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_CHANGE_STORAGE_POOL);
            result = false;
        }
        if (result) {
            SearchParameters searchParams = new SearchParameters("vms: cluster = "
                    + oldGroup.getname(), SearchType.VM);
            searchParams.setMaxCount(Integer.MAX_VALUE);

            @SuppressWarnings("unchecked")
            List<VM> vmList =
                    (List<VM>) getBackend().runInternalQuery(VdcQueryType.Search, searchParams).getReturnValue();
            if (vmList.size() > 0) {
                hasVms = true;
            }
            int notDownVms = 0;
            int suspendedVms = 0;
            for (VM vm : vmList) {
                // the search can return vm from cluster with similar name
                // so it's critical to check that
                // the vm cluster id is the same as the cluster.id
                if (!vm.getvds_group_id().equals(oldGroup.getId())) {
                    continue;
                }
                VMStatus vmStatus = vm.getstatus();
                if (vmStatus == VMStatus.Suspended) {
                    suspendedVms++;
                }
                if (vmStatus != VMStatus.Down) {
                    notDownVms++;
                }
            }

            boolean sameCpuNames = StringUtils.equals(oldGroup.getcpu_name(), getVdsGroup().getcpu_name());
            if (result && !sameCpuNames) {
                if (suspendedVms > 0) {
                    addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_UPDATE_CPU_WITH_SUSPENDED_VMS);
                    result = false;
                } else if (notDownVms > 0) {
                    int compareResult = compareCpuLevels(oldGroup);
                    if (compareResult < 0) {
                        addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_LOWER_CPU_LEVEL);
                        result = false;
                    } else if (compareResult > 0) {// Upgrade of CPU in same compability level is allowed if there
                        // are running VMs - but we should warn they
                        // cannot not be hibernated
                        AuditLogableBase logable = new AuditLogableBase();
                        logable.AddCustomValue("VdsGroup", getParameters().getVdsGroup().getname());
                        AuditLogDirector.log(logable,
                                AuditLogType.CANNOT_HIBERNATE_RUNNING_VMS_AFTER_CLUSTER_CPU_UPGRADE);
                    }
                }
            }
        }
        if (result && getVdsGroup().getstorage_pool_id() != null) {
            storage_pool storagePool = getStoragePoolDAO().get(getVdsGroup().getstorage_pool_id().getValue());
            if (oldGroup.getstorage_pool_id() == null && storagePool.getstorage_pool_type() == StorageType.LOCALFS) {
                // we allow only one cluster in localfs data center
                if (!getVdsGroupDAO().getAllForStoragePool(getVdsGroup().getstorage_pool_id().getValue()).isEmpty()) {
                    getReturnValue()
                            .getCanDoActionMessages()
                            .add(VdcBllMessages.VDS_GROUP_CANNOT_ADD_MORE_THEN_ONE_HOST_TO_LOCAL_STORAGE
                                    .toString());
                    result = false;
                }
                // selection algorithm must be set to none in localfs
                else if (getVdsGroup().getselection_algorithm() != VdsSelectionAlgorithm.None) {
                    getReturnValue()
                            .getCanDoActionMessages()
                            .add(VdcBllMessages.VDS_GROUP_SELECTION_ALGORITHM_MUST_BE_SET_TO_NONE_ON_LOCAL_STORAGE
                                    .toString());
                    result = false;
                }
                else if (VDSGroup.DEFAULT_VDS_GROUP_ID.equals(getVdsGroup().getId())) {
                    addCanDoActionMessage(VdcBllMessages.DEFAULT_CLUSTER_CANNOT_BE_ON_LOCALFS);
                    result = false;
                }
            }
        }
        if (result) {
            result = validateMetrics();
        }
        if (result) {
            if (!(getVdsGroup().supportsGlusterService() || getVdsGroup().supportsVirtService())) {
                addCanDoActionMessage(VdcBllMessages.VDS_GROUP_AT_LEAST_ONE_SERVICE_MUST_BE_ENABLED);
                result = false;
            }
            else if (getVdsGroup().supportsGlusterService() && getVdsGroup().supportsVirtService()
                    && !isAllowClusterWithVirtGluster()) {
                addCanDoActionMessage(VdcBllMessages.VDS_GROUP_ENABLING_BOTH_VIRT_AND_GLUSTER_SERVICES_NOT_ALLOWED);
                result = false;
            }
        }
        if (result && hasVms && !getVdsGroup().supportsVirtService()) {
            addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_DISABLE_VIRT_WHEN_CLUSTER_CONTAINS_VMS);
            result = false;
        }
        if (result && !getVdsGroup().supportsGlusterService()) {
            List<GlusterVolumeEntity> volumes = getGlusterVolumeDao().getByClusterId(getVdsGroup().getId());
            if (volumes != null && volumes.size() > 0) {
                addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_DISABLE_GLUSTER_WHEN_CLUSTER_CONTAINS_VOLUMES);
                result = false;
            }
        }
        return result;
    }

    protected boolean checkIfCpusSameManufacture(VDSGroup group) {
        return CpuFlagsManagerHandler.CheckIfCpusSameManufacture(group.getcpu_name(),
                getVdsGroup().getcpu_name(),
                getVdsGroup().getcompatibility_version());
    }

    protected boolean checkIfCpusExist() {
        return CpuFlagsManagerHandler.CheckIfCpusExist(getVdsGroup().getcpu_name(),
                getVdsGroup().getcompatibility_version());
    }

    protected List<String> missingServerCpuFlags(VDS vds) {
        return CpuFlagsManagerHandler.missingServerCpuFlags(
                getVdsGroup().getcpu_name(),
                vds.getcpu_flags(),
                getVdsGroup().getcompatibility_version());
    }

    protected int compareCpuLevels(VDSGroup otherGroup) {
        return CpuFlagsManagerHandler.compareCpuLevels(getVdsGroup().getcpu_name(),
                otherGroup.getcpu_name(),
                otherGroup.getcompatibility_version());
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(UpdateEntity.class);
        return super.getValidationGroups();
    }

    protected VdsStaticDAO getVdsStaticDAO() {
        return getDbFacade().getVdsStaticDAO();
    }

    @Override
    protected NetworkDAO getNetworkDAO() {
        return getDbFacade().getNetworkDAO();
    }
}
