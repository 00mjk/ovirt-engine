package org.ovirt.engine.core.bll.network;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transaction;

import org.ovirt.engine.core.bll.context.CompensationContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.VmDAO;
import org.ovirt.engine.core.dao.network.VmNetworkInterfaceDao;
import org.ovirt.engine.core.dao.network.VmNetworkStatisticsDao;
import org.ovirt.engine.core.dao.network.VmNicDao;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

/**
 * Helper class to use for adding/removing {@link VmNic}s.
 */
public class VmInterfaceManager {

    private Log log = LogFactory.getLog(getClass());

    /**
     * Add a {@link VmNic} to the VM. Allocates a MAC from the {@link MacPoolManager} if necessary, otherwise, if
     * {@code ConfigValues.HotPlugEnabled} is true, forces adding the MAC address to the {@link MacPoolManager}. If
     * HotPlug is not enabled tries to add the {@link VmNic}'s MAC address to the {@link MacPoolManager}, and throws a
     * {@link VdcBllException} if it fails.
     *
     * @param iface
     *            The interface to save.
     * @param compensationContext
     *            Used to snapshot the saved entities.
     * @param clusterCompatibilityVersion
     *            the compatibility version of the cluster
     * @return <code>true</code> if the MAC wasn't used, <code>false</code> if it was.
     */
    public void add(final VmNic iface,
            CompensationContext compensationContext,
            boolean allocateMac,
            int osId,
            Version clusterCompatibilityVersion) {

        if (allocateMac) {
            iface.setMacAddress(getMacPoolManager().allocateNewMac());
        } else if (FeatureSupported.hotPlug(clusterCompatibilityVersion)
                && getOsRepository().hasNicHotplugSupport(osId, clusterCompatibilityVersion)) {
            getMacPoolManager().forceAddMac(iface.getMacAddress());
        } else if (!getMacPoolManager().addMac(iface.getMacAddress())) {
            auditLogMacInUse(iface);
            throw new VdcBLLException(VdcBllErrors.MAC_ADDRESS_IS_IN_USE);
        }

        getVmNicDao().save(iface);
        getVmNetworkStatisticsDao().save(iface.getStatistics());
        compensationContext.snapshotNewEntity(iface);
        compensationContext.snapshotNewEntity(iface.getStatistics());
    }

    public OsRepository getOsRepository() {
        return SimpleDependecyInjector.getInstance().get(OsRepository.class);
    }

    public void auditLogMacInUse(final VmNic iface) {
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                AuditLogableBase logable = createAuditLog(iface);
                log(logable, AuditLogType.MAC_ADDRESS_IS_IN_USE);
                log.warnFormat("Network Interface {0} has MAC address {1} which is in use, " +
                        "therefore the action for VM {2} failed.", iface.getName(), iface.getMacAddress(),
                        iface.getVmId());
                return null;
            }
        });
    }

    public void auditLogMacInUseUnplug(final VmNic iface) {
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                AuditLogableBase logable = createAuditLog(iface);
                log(logable, AuditLogType.MAC_ADDRESS_IS_IN_USE_UNPLUG);
                log.warnFormat("Network Interface {0} has MAC address {1} which is in use, " +
                        "therefore it is being unplugged from VM {2}.", iface.getName(), iface.getMacAddress(),
                        iface.getVmId());
                return null;
            }
        });
    }

    /**
     * Remove all {@link VmNic}s from the VM, and remove the Mac addresses from {@link MacPoolManager}.
     *
     * @param vmId
     *            The ID of the VM to remove from.
     */
    public void removeAll(Guid vmId) {
        List<VmNic> interfaces = getVmNicDao().getAllForVm(vmId);
        if (interfaces != null) {
            removeFromExternalNetworks(interfaces);

            for (VmNic iface : interfaces) {
                getMacPoolManager().freeMac(iface.getMacAddress());
                getVmNicDao().remove(iface.getId());
                getVmNetworkStatisticsDao().remove(iface.getId());
            }
        }
    }

    protected void removeFromExternalNetworks(List<VmNic> interfaces) {
        Transaction transaction = TransactionSupport.suspend();
        for (VmNic iface : interfaces) {
            new ExternalNetworkManager(iface).deallocateIfExternal();
        }

        TransactionSupport.resume(transaction);
    }

    /**
     * Finds active VMs which uses a network from a given networks list
     *
     * @param vdsId
     *            The host id on which VMs are running
     * @param networks
     *            the networks to check if used
     * @return A list of VM names which uses the networks
     */
    public List<String> findActiveVmsUsingNetworks(Guid vdsId, List<String> networks) {
        List<VM> runningVms = getVmDAO().getAllRunningForVds(vdsId);
        List<String> vmNames = new ArrayList<String>();
        for (VM vm : runningVms) {
            List<VmNetworkInterface> vmInterfaces = getVmNetworkInterfaceDao().getAllForVm(vm.getId());
            for (VmNetworkInterface vmNic : vmInterfaces) {
                if (vmNic.getNetworkName() != null && networks.contains(vmNic.getNetworkName())) {
                    vmNames.add(vm.getName());
                    break;
                }
            }
        }
        return vmNames;
    }

    /***
     * Returns whether or not there is a plugged network interface with the same MAC address as the given interface
     *
     * @param interfaceToPlug
     *            the network interface that needs to be plugged
     * @return <code>true</code> if the MAC is used by another plugged network interface, <code>false</code> otherwise.
     */
    public boolean existsPluggedInterfaceWithSameMac(VmNic interfaceToPlug) {
        List<VmNic> vmNetworkIntrefaces = getVmNicDao().getPluggedForMac(interfaceToPlug.getMacAddress());
        for (VmNic vmNetworkInterface : vmNetworkIntrefaces) {
            if (!interfaceToPlug.getId().equals(vmNetworkInterface.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Log the given loggable & message to the {@link AuditLogDirector}.
     *
     * @param logable
     * @param auditLogType
     */
    protected void log(AuditLogableBase logable, AuditLogType auditLogType) {
        AuditLogDirector.log(logable, auditLogType);
    }

    protected MacPoolManager getMacPoolManager() {
        return MacPoolManager.getInstance();
    }

    protected VmNetworkStatisticsDao getVmNetworkStatisticsDao() {
        return DbFacade.getInstance().getVmNetworkStatisticsDao();
    }

    protected VmNetworkInterfaceDao getVmNetworkInterfaceDao() {
        return DbFacade.getInstance().getVmNetworkInterfaceDao();
    }

    protected VmNicDao getVmNicDao() {
        return DbFacade.getInstance().getVmNicDao();
    }

    protected VmDAO getVmDAO() {
        return DbFacade.getInstance().getVmDao();
    }

    private AuditLogableBase createAuditLog(final VmNic iface) {
        AuditLogableBase logable = new AuditLogableBase();
        logable.setVmId(iface.getVmId());
        logable.addCustomValue("MACAddr", iface.getMacAddress());
        logable.addCustomValue("IfaceName", iface.getName());
        return logable;
    }
}
