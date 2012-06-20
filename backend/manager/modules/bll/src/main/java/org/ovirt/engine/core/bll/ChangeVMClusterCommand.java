package org.ovirt.engine.core.bll;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ChangeVMClusterParameters;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.ObjectIdentityChecker;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;

@SuppressWarnings("serial")
public class ChangeVMClusterCommand<T extends ChangeVMClusterParameters> extends VmCommand<T> {

    private VDSGroup targetCluster;
    private boolean dedicatedHostWasCleared;

    public ChangeVMClusterCommand(T params) {
        super(params);
    }

    @Override
    protected boolean canDoAction() {
        // Set parameters for messeging.
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__UPDATE);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM__CLUSTER);

        VM vm = getVm();
        if (vm == null) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_EXIST);
            return false;
        } else {
            if (ObjectIdentityChecker.CanUpdateField(vm, "vds_group_id", vm.getstatus())) {
                targetCluster = DbFacade.getInstance().getVdsGroupDAO().get(getParameters().getClusterId());
                if (targetCluster == null) {
                    addCanDoActionMessage(VdcBllMessages.VM_CLUSTER_IS_NOT_VALID);
                    return false;
                }

                // Check that the target cluster is in the same data center.
                if (!targetCluster.getstorage_pool_id().equals(vm.getstorage_pool_id())) {
                    addCanDoActionMessage(VdcBllMessages.VM_CANNOT_MOVE_TO_CLUSTER_IN_OTHER_STORAGE_POOL);
                    return false;
                }

                List<VmNetworkInterface> interfaces = DbFacade.getInstance().getVmNetworkInterfaceDAO()
                .getAllForVm(getParameters().getVmId());

                // Get if the cluster chosen got limit of nics.
                boolean limitNumOfNics = Config.<Boolean> GetValue(ConfigValues.LimitNumberOfNetworkInterfaces,
                                                                   targetCluster.getcompatibility_version()
                                                                                .getValue()
                                                                                .toString());

                // If so , check if nic count has exceeded and print appropriate
                // message.
                if (limitNumOfNics) {
                    // Check that the number of interfaces does not exceed
                    // limit.
                    // Necessary only for version 2.2.
                    boolean numOfNicsLegal = validateNumberOfNics(interfaces, null);
                    if (!numOfNicsLegal) {
                        addCanDoActionMessage(VdcBllMessages.NETWORK_INTERFACE_EXITED_MAX_INTERFACES);
                        return false;
                    }
                }

                // Check the destination cluster have all the networks that the VM use
                List<Network> networks = DbFacade.getInstance().getNetworkDAO().getAllForCluster(getParameters().getClusterId());
                StringBuilder missingNets = new StringBuilder();
                for (VmNetworkInterface iface: interfaces) {
                    String netName = iface.getNetworkName();
                    if (isNotEmpty(netName)) {
                        boolean exists = false;
                        for (Network net: networks) {
                            if (net.getname().equals(netName)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            if (missingNets.length() > 0) {
                                missingNets.append(", ");
                            }
                            missingNets.append(netName);
                        }
                    }
                }
                if (missingNets.length() > 0) {
                    addCanDoActionMessage(VdcBllMessages.MOVE_VM_CLUSTER_MISSING_NETWORK);
                    addCanDoActionMessage(String.format("$networks %1$s", missingNets.toString()));
                    return false;
                }

                // Check if VM static parameters are compatible for new cluster.
                boolean isCpuSocketsValid = AddVmCommand.CheckCpuSockets(
                                                                         vm.getStaticData().getnum_of_sockets(),
                                                                         vm.getStaticData().getcpu_per_socket(),
                                                                         targetCluster.getcompatibility_version()
                                                                                      .getValue(),
                                                                         getReturnValue().getCanDoActionMessages());
                if (!isCpuSocketsValid) {
                    return false;
                }

                // Check that the USB policy is legal
                if (!VmHandler.isUsbPolicyLegal(vm.getusb_policy(), vm.getos(), targetCluster, getReturnValue().getCanDoActionMessages())) {
                    return false;
                }
            } else {
                addCanDoActionMessage(VdcBllMessages.VM_STATUS_NOT_VALID_FOR_UPDATE);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void executeCommand() {
        // check that the cluster are not the same
        VM vm = getVm();
        if (vm.getvds_group_id().equals(getParameters().getClusterId())) {
            setSucceeded(true);
            return;
        }

        // update vm interfaces
        List<Network> networks = DbFacade.getInstance().getNetworkDAO()
                .getAllForCluster(getParameters().getClusterId());
        List<VmNetworkInterface> interfaces = DbFacade.getInstance().getVmNetworkInterfaceDAO()
                .getAllForVm(getParameters().getVmId());

        for (final VmNetworkInterface iface : interfaces) {
            Network net = LinqUtils.firstOrNull(networks, new Predicate<Network>() {
                @Override
                public boolean eval(Network n) {
                    return iface.getNetworkName().equals(n.getname());
                }
            });
            // if network not exists in cluster we remove the network to
            // interface connection
            if (net == null) {
                iface.setNetworkName(null);
                DbFacade.getInstance().getVmNetworkInterfaceDAO().update(iface);
            }
        }

        if (vm.getdedicated_vm_for_vds() != null) {
            vm.setdedicated_vm_for_vds(null);
            dedicatedHostWasCleared = true;
        }

        vm.setvds_group_id(getParameters().getClusterId());
        DbFacade.getInstance().getVmStaticDAO().update(vm.getStaticData());
        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ?
                dedicatedHostWasCleared ?
                        AuditLogType.USER_UPDATE_VM_CLUSTER_DEFAULT_HOST_CLEARED
                        : AuditLogType.USER_UPDATE_VM
                : AuditLogType.USER_FAILED_UPDATE_VM;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = new ArrayList<PermissionSubject>();
        permissionList.add(new PermissionSubject(getParameters().getVmId(), VdcObjectType.VM, getActionType().getActionGroup()));
        permissionList.add(new PermissionSubject(getParameters().getClusterId(), VdcObjectType.VdsGroups, getActionType().getActionGroup()));
        return permissionList;
    }
}
