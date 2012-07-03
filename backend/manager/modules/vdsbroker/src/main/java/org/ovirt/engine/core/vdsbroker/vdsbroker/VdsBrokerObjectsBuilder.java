package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskImageDynamic;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.InterfaceStatus;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.common.businessentities.NetworkBootProtocol;
import org.ovirt.engine.core.common.businessentities.SessionState;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSDomainsData;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VdsInterfaceType;
import org.ovirt.engine.core.common.businessentities.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.VdsNetworkStatistics;
import org.ovirt.engine.core.common.businessentities.VdsTransparentHugePagesState;
import org.ovirt.engine.core.common.businessentities.VdsVersion;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmExitStatus;
import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.VmPauseStatus;
import org.ovirt.engine.core.common.businessentities.VmStatistics;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.utils.EnumUtils;
import org.ovirt.engine.core.compat.FormatException;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.LongCompat;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.NetworkUtils;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.serialization.json.JsonObjectSerializer;
import org.ovirt.engine.core.vdsbroker.xmlrpc.XmlRpcStruct;

/**
 * This class encapsulate the knowledge of how to create objects from the VDS RPC protocol response.
 * This class has methods that receive XmlRpcStruct and construct the following Classes: VmDynamic VdsDynamic VdsStatic.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class VdsBrokerObjectsBuilder {
    private final static int VNC_START_PORT = 5900;
    private final static double NANO_SECONDS = 1000000000;

    public static VmDynamic buildVMDynamicDataFromList(XmlRpcStruct xmlRpcStruct) {
        VmDynamic vmdynamic = new VmDynamic();
        if (xmlRpcStruct.contains(VdsProperties.vm_guid)) {
            try {
                vmdynamic.setId(new Guid((String) xmlRpcStruct.getItem(VdsProperties.vm_guid)));
            } catch (FormatException e) {
                log.info("vm id is not in uuid format, ", e);
                vmdynamic.setId(new Guid());
            }
        }
        if (xmlRpcStruct.contains(VdsProperties.status)) {
            vmdynamic.setstatus(convertToVmStatus((String) xmlRpcStruct.getItem(VdsProperties.status)));
        }
        return vmdynamic;
    }

    public static VmDynamic buildVMDynamicData(XmlRpcStruct xmlRpcStruct) {
        VmDynamic vmdynamic = new VmDynamic();
        updateVMDynamicData(vmdynamic, xmlRpcStruct);
        return vmdynamic;
    }

    public static storage_pool buildStoragePool(XmlRpcStruct xmlRpcStruct) {
        storage_pool sPool = new storage_pool();
        if (xmlRpcStruct.contains("type")) {
            sPool.setstorage_pool_type(StorageType.valueOf(xmlRpcStruct.getItem("type").toString()));
        }
        sPool.setname(AssignStringValue(xmlRpcStruct, "name"));
        Integer masterVersion = AssignIntValue(xmlRpcStruct, "master_ver");
        if (masterVersion != null) {
            sPool.setmaster_domain_version(masterVersion);
        }
        return sPool;
    }

    public static VmStatistics buildVMStatisticsData(XmlRpcStruct xmlRpcStruct) {
        VmStatistics vmStatistics = new VmStatistics();
        updateVMStatisticsData(vmStatistics, xmlRpcStruct);
        return vmStatistics;
    }

    public static void updateVMDynamicData(VmDynamic vm, XmlRpcStruct xmlRpcStruct) {
        if (xmlRpcStruct.contains(VdsProperties.vm_guid)) {
            try {
                vm.setId(new Guid((String) xmlRpcStruct.getItem(VdsProperties.vm_guid)));
            } catch (FormatException e) {
                log.info("vm id is not in uuid format, ", e);
                vm.setId(new Guid());
            }
        }
        if (xmlRpcStruct.contains(VdsProperties.session)) {
            String session = (String) xmlRpcStruct.getItem(VdsProperties.session);
            try {
                vm.setsession(SessionState.valueOf(session));
            } catch (Exception e) {
                log.errorFormat("vm session value illegal : {0}", session);
            }
        }
        if (xmlRpcStruct.contains(VdsProperties.kvmEnable)) {
            vm.setkvm_enable(Boolean.parseBoolean((String) xmlRpcStruct.getItem(VdsProperties.kvmEnable)));
        }
        if (xmlRpcStruct.contains(VdsProperties.acpiEnable)) {
            vm.setacpi_enable(Boolean.parseBoolean((String) xmlRpcStruct.getItem(VdsProperties.acpiEnable)));
        }
        if (xmlRpcStruct.contains(VdsProperties.win2kHackEnable)) {
            vm.setWin2kHackEnable(Boolean.parseBoolean((String) xmlRpcStruct.getItem(VdsProperties.win2kHackEnable)));
        }
        if (xmlRpcStruct.contains(VdsProperties.status)) {
            vm.setstatus(convertToVmStatus((String) xmlRpcStruct.getItem(VdsProperties.status)));
        }
        if (xmlRpcStruct.contains(VdsProperties.display_port)) {
            try {
                vm.setdisplay(Integer.parseInt(xmlRpcStruct.getItem(VdsProperties.display_port).toString()));
            } catch (NumberFormatException e) {
                log.errorFormat("vm display_port value illegal : {0}", xmlRpcStruct.getItem(VdsProperties.display_port));
            }
        } else if (xmlRpcStruct.contains(VdsProperties.display)) {
            try {
                vm.setdisplay(VNC_START_PORT + Integer.parseInt(xmlRpcStruct.getItem(VdsProperties.display).toString()));
            } catch (NumberFormatException e) {
                log.errorFormat("vm display value illegal : {0}", xmlRpcStruct.getItem(VdsProperties.display));
            }
        }
        if (xmlRpcStruct.contains(VdsProperties.display_secure_port)) {
            try {
                vm.setdisplay_secure_port(Integer.parseInt(xmlRpcStruct.getItem(VdsProperties.display_secure_port)
                        .toString()));
            } catch (NumberFormatException e) {
                log.errorFormat("vm display_secure_port value illegal : {0}",
                        xmlRpcStruct.getItem(VdsProperties.display_secure_port));
            }
        }
        if (xmlRpcStruct.contains((VdsProperties.displayType))) {
            String displayType = xmlRpcStruct.getItem(VdsProperties.displayType).toString();
            try {
                vm.setdisplay_type(DisplayType.valueOf(displayType));

            } catch (Exception e2) {
                log.errorFormat("vm display type value illegal : {0}", displayType);
            }
        }
        if (xmlRpcStruct.contains((VdsProperties.displayIp))) {
            vm.setdisplay_ip((String) xmlRpcStruct.getItem(VdsProperties.displayIp));
        }

        if (xmlRpcStruct.contains((VdsProperties.utc_diff))) {
            String utc_diff = xmlRpcStruct.getItem(VdsProperties.utc_diff).toString();
            if (utc_diff.startsWith("+")) {
                utc_diff = utc_diff.substring(1);
            }
            try {
                vm.setutc_diff(Integer.parseInt(utc_diff));
            } catch (NumberFormatException e) {
                log.errorFormat("vm offset (utc_diff) value illegal : {0}", utc_diff);
            }
        }

        if (xmlRpcStruct.contains(VdsProperties.hash)) {
            String hash = (String) xmlRpcStruct.getItem(VdsProperties.hash);
            try {
                vm.setHash(hash);
            } catch (Exception e) {
                log.errorFormat("vm hash value illegal : {0}", hash);
            }
        }

        /**
         * vm disks
         */
        if (xmlRpcStruct.contains(VdsProperties.vm_disks)) {
            initDisks(xmlRpcStruct, vm);
        }

        // ------------- vm internal agent data
        vm.setguest_cur_user_name(AssignStringValue(xmlRpcStruct, VdsProperties.guest_cur_user_name));
        vm.setguest_last_login_time(AssignDateTImeFromEpoch(xmlRpcStruct, VdsProperties.guest_last_login_time));
        vm.setvm_host(AssignStringValue(xmlRpcStruct, VdsProperties.vm_host));

        initAppsList(xmlRpcStruct, vm);
        vm.setguest_os(AssignStringValue(xmlRpcStruct, VdsProperties.guest_os));
        vm.setvm_ip(AssignStringValue(xmlRpcStruct, VdsProperties.vm_ip));
        if (vm.getvm_ip() != null) {
            if (vm.getvm_ip().startsWith("127.0.")) {
                vm.setvm_ip(null);
            } else {
                vm.setvm_ip(vm.getvm_ip().trim());
            }
        }

        if (xmlRpcStruct.contains(VdsProperties.exit_code)) {
            String exitCodeStr = xmlRpcStruct.getItem(VdsProperties.exit_code).toString();
            vm.setExitStatus(VmExitStatus.forValue(Integer.parseInt(exitCodeStr)));
        }
        if (xmlRpcStruct.contains(VdsProperties.exit_message)) {
            String exitMsg = (String) xmlRpcStruct.getItem(VdsProperties.exit_message);
            vm.setExitMessage(exitMsg);
        }

        // if monitorResponse returns negative it means its erroneous
        if (xmlRpcStruct.contains(VdsProperties.monitorResponse)) {
            int response = Integer.parseInt(xmlRpcStruct.getItem(VdsProperties.monitorResponse).toString());
            if (response < 0) {
                vm.setstatus(VMStatus.NotResponding);
            }
        }
        if (xmlRpcStruct.contains(VdsProperties.clientIp)) {
            vm.setclient_ip(xmlRpcStruct.getItem(VdsProperties.clientIp).toString());
        }

        VmPauseStatus pauseStatus = VmPauseStatus.NONE;
        if (xmlRpcStruct.contains(VdsProperties.pauseCode)) {
            String pauseCodeStr = (String) xmlRpcStruct.getItem(VdsProperties.pauseCode);
            try {
                pauseStatus = VmPauseStatus.valueOf(pauseCodeStr);

            } catch (IllegalArgumentException ex) {
                log.error("Error in parsing vm pause status. Setting value to NONE");
                pauseStatus = VmPauseStatus.NONE;
            }
        }
        vm.setPauseStatus(pauseStatus);
    }

    public static void updateVMStatisticsData(VmStatistics vm, XmlRpcStruct xmlRpcStruct) {
        if (xmlRpcStruct.contains(VdsProperties.vm_guid)) {
            try {
                vm.setId(new Guid((String) xmlRpcStruct.getItem(VdsProperties.vm_guid)));
            } catch (FormatException e) {
                log.info("vm id is not in uuid format, ", e);
                vm.setId(new Guid());
            }
        }

        vm.setelapsed_time(AssignDoubleValue(xmlRpcStruct, VdsProperties.elapsed_time));

        // ------------- vm network statistics -----------------------
        if (xmlRpcStruct.containsKey(VdsProperties.vm_network)) {
            Map networkStruct = (Map) xmlRpcStruct.getItem(VdsProperties.vm_network);
            vm.setInterfaceStatistics(new ArrayList<VmNetworkInterface>());
            for (Object tempNic : networkStruct.values()) {
                XmlRpcStruct nic = new XmlRpcStruct((Map) tempNic);
                VmNetworkInterface stats = new VmNetworkInterface();
                vm.getInterfaceStatistics().add(stats);

                if (nic.containsKey(VdsProperties.if_name)) {
                    stats.setName((String) ((nic.getItem(VdsProperties.if_name) instanceof String) ? nic
                            .getItem(VdsProperties.if_name) : null));
                }
                Double rx_rate = AssignDoubleValue(nic, VdsProperties.rx_rate);
                Double rx_dropped = AssignDoubleValue(nic, VdsProperties.rx_dropped);
                Double tx_rate = AssignDoubleValue(nic, VdsProperties.tx_rate);
                Double tx_dropped = AssignDoubleValue(nic, VdsProperties.tx_dropped);
                stats.getStatistics().setReceiveRate(rx_rate != null ? rx_rate : 0);
                stats.getStatistics().setReceiveDropRate(rx_dropped != null ? rx_dropped : 0);
                stats.getStatistics().setTransmitRate(tx_rate != null ? tx_rate : 0);
                stats.getStatistics().setTransmitDropRate(tx_dropped != null ? tx_dropped : 0);
                stats.setMacAddress((String) ((nic.getItem(VdsProperties.mac_addr) instanceof String) ? nic
                        .getItem(VdsProperties.mac_addr) : null));
                stats.setSpeed(AssignIntValue(nic, VdsProperties.if_speed));
            }
        }

        if (xmlRpcStruct.contains(VdsProperties.VM_DISKS_USAGE)) {
            initDisksUsage(xmlRpcStruct, vm);
        }

        // ------------- vm cpu statistics -----------------------
        vm.setcpu_sys(AssignDoubleValue(xmlRpcStruct, VdsProperties.cpu_sys));
        vm.setcpu_user(AssignDoubleValue(xmlRpcStruct, VdsProperties.cpu_user));

        // ------------- vm memory statistics -----------------------
        vm.setusage_mem_percent(AssignIntValue(xmlRpcStruct, VdsProperties.vm_usage_mem_percent));

    }

    public static void updateVDSDynamicData(VDS vds, XmlRpcStruct xmlRpcStruct) {
        updateNetworkData(vds, xmlRpcStruct);

        vds.setcpu_cores(AssignIntValue(xmlRpcStruct, VdsProperties.cpu_cores));
        vds.setcpu_sockets(AssignIntValue(xmlRpcStruct, VdsProperties.cpu_sockets));
        vds.setcpu_model(AssignStringValue(xmlRpcStruct, VdsProperties.cpu_model));
        vds.setcpu_speed_mh(AssignDoubleValue(xmlRpcStruct, VdsProperties.cpu_speed_mh));
        vds.setphysical_mem_mb(AssignIntValue(xmlRpcStruct, VdsProperties.physical_mem_mb));

        vds.setkvm_enabled(AssignBoolValue(xmlRpcStruct, VdsProperties.kvm_enabled));

        vds.setreserved_mem(AssignIntValue(xmlRpcStruct, VdsProperties.reservedMem));
        Integer guestOverhead = AssignIntValue(xmlRpcStruct, VdsProperties.guestOverhead);
        vds.setguest_overhead(guestOverhead != null ? guestOverhead : 0);
        updateVdsStaticVersion(vds, xmlRpcStruct);

        vds.setcpu_flags(AssignStringValue(xmlRpcStruct, VdsProperties.cpu_flags));

        UpdatePackagesVersions(vds, xmlRpcStruct);

        vds.setsupported_cluster_levels(AssignStringValueFromArray(xmlRpcStruct, VdsProperties.supported_cluster_levels));
        vds.setsupported_engines(AssignStringValueFromArray(xmlRpcStruct, VdsProperties.supported_engines));
        vds.setIScsiInitiatorName(AssignStringValue(xmlRpcStruct, VdsProperties.iSCSIInitiatorName));

        String hooksStr = ""; // default value if hooks is not in the xml rpc struct
        if (xmlRpcStruct.containsKey(VdsProperties.hooks)) {
            hooksStr = xmlRpcStruct.getItem(VdsProperties.hooks).toString();
        }
        vds.setHooksStr(hooksStr);
    }

    private static void initDisksUsage(XmlRpcStruct vmStruct, VmStatistics vm) {
        Object[] vmDisksUsage = (Object[]) vmStruct.getItem(VdsProperties.VM_DISKS_USAGE);
        if (vmDisksUsage != null) {
            ArrayList<Object> disksUsageList = new ArrayList<Object>(Arrays.asList(vmDisksUsage));
            vm.setDisksUsage(new JsonObjectSerializer().serializeUnformattedJson(disksUsageList));
        }
    }

    private static void UpdatePackagesVersions(VDS vds, XmlRpcStruct xmlRpcStruct) {
        if (xmlRpcStruct.contains(VdsProperties.host_os)) {
            vds.sethost_os(GetPackageVersionFormated(
                    new XmlRpcStruct((Map) xmlRpcStruct.getItem(VdsProperties.host_os)), true));
        }
        if (xmlRpcStruct.contains(VdsProperties.packages)) {
            // packages is an array of xmlRpcStruct (that each is a name, ver,
            // release.. of a package)
            for (Object hostPackageMap : (Object[]) xmlRpcStruct.getItem(VdsProperties.packages)) {
                XmlRpcStruct hostPackage = new XmlRpcStruct((Map) hostPackageMap);
                String packageName = AssignStringValue(hostPackage, VdsProperties.package_name);
                if (VdsProperties.kvmPackageName.equals(packageName)) {
                    vds.setkvm_version(GetPackageVersionFormated(hostPackage, false));
                } else if (VdsProperties.spicePackageName.equals(packageName)) {
                    vds.setspice_version(GetPackageVersionFormated(hostPackage, false));
                } else if (VdsProperties.kernelPackageName.equals(packageName)) {
                    vds.setkernel_version(GetPackageVersionFormated(hostPackage, false));
                }
            }
        } else if (xmlRpcStruct.contains(VdsProperties.packages2)) {
            Map packages = (Map) xmlRpcStruct.getItem(VdsProperties.packages2);

            if (packages.containsKey(VdsProperties.qemuKvmPackageName)) {
                Map kvm = (Map) packages.get(VdsProperties.qemuKvmPackageName);
                vds.setkvm_version(getPackageVersionFormated2(kvm));
            }
            if (packages.containsKey(VdsProperties.spiceServerPackageName)) {
                Map spice = (Map) packages.get(VdsProperties.spiceServerPackageName);
                vds.setspice_version(getPackageVersionFormated2(spice));
            }
            if (packages.containsKey(VdsProperties.kernelPackageName)) {
                Map kernel = (Map) packages.get(VdsProperties.kernelPackageName);
                vds.setkernel_version(getPackageVersionFormated2(kernel));
            }
        }
    }

    // Version 2 of GetPackageVersionFormated2:
    // from 2.3 we get dictionary and not a flat list.
    // from now the packages names (of spice, kernel, qemu and libvirt) are the same as far as VDSM and ENGINE.
    // (VDSM use to report packages name of rpm so in RHEL6 when it change it broke our interface)
    private static String getPackageVersionFormated2(Map hostPackage) {

        String packageVersion = (hostPackage.get(VdsProperties.package_version) != null) ? (String) hostPackage
                .get(VdsProperties.package_version) : null;
        String packageRelease = (hostPackage.get(VdsProperties.package_release) != null) ? (String) hostPackage
                .get(VdsProperties.package_release) : null;

        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(packageVersion)) {
            sb.append(packageVersion);
        }
        if (!StringUtils.isEmpty(packageRelease)) {
            if (sb.length() > 0) {
                sb.append(String.format(" - %1$s", packageRelease));
            } else {
                sb.append(packageRelease);
            }
        }
        return sb.toString();
    }

    private static String GetPackageVersionFormated(XmlRpcStruct hostPackage, boolean getName) {
        String packageName = AssignStringValue(hostPackage, VdsProperties.package_name);
        String packageVersion = AssignStringValue(hostPackage, VdsProperties.package_version);
        String packageRelease = AssignStringValue(hostPackage, VdsProperties.package_release);
        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(packageName) && getName) {
            sb.append(packageName);
        }
        if (!StringUtils.isEmpty(packageVersion)) {
            if (sb.length() > 0) {
                sb.append(String.format(" - %1$s", packageVersion));
            } else {
                sb.append(packageVersion);
            }
        }
        if (!StringUtils.isEmpty(packageRelease)) {
            if (sb.length() > 0) {
                sb.append(String.format(" - %1$s", packageRelease));
            } else {
                sb.append(packageRelease);
            }
        }
        return sb.toString();
    }

    public static void updateVDSStatisticsData(VDS vds, XmlRpcStruct xmlRpcStruct) {
        // ------------- vds memory usage ---------------------------
        vds.setusage_mem_percent(AssignIntValue(xmlRpcStruct, VdsProperties.mem_usage));

        // ------------- vds network statistics ---------------------
        Map<String, Object> interfaces = (Map<String, Object>) ((xmlRpcStruct
                .getItem(VdsProperties.network) instanceof Map) ? xmlRpcStruct.getItem(VdsProperties.network)
                : null);
        if (interfaces != null) {
            int networkUsage = 0;
            for (String name : interfaces.keySet()) {
                VdsNetworkInterface iface = null;
                for (VdsNetworkInterface tempInterface : vds.getInterfaces()) {
                    if (tempInterface.getName().equals(name)) {
                        iface = tempInterface;
                        break;
                    }
                }
                if (iface != null) {
                    iface.setVdsId(vds.getId());
                    Map<String, Object> dictTemp =
                            (Map<String, Object>) ((interfaces.get(name) instanceof Map) ? interfaces
                                    .get(name)
                                    : null);
                    XmlRpcStruct dict = new XmlRpcStruct(dictTemp);
                    Double rx_rate = AssignDoubleValue(dict, VdsProperties.rx_rate);
                    Double rx_dropped = AssignDoubleValue(dict, VdsProperties.rx_dropped);
                    Double tx_rate = AssignDoubleValue(dict, VdsProperties.tx_rate);
                    Double tx_dropped = AssignDoubleValue(dict, VdsProperties.tx_dropped);
                    iface.getStatistics().setReceiveRate(rx_rate != null ? rx_rate : 0);
                    iface.getStatistics().setReceiveDropRate(rx_dropped != null ? rx_dropped : 0);
                    iface.getStatistics().setTransmitRate(tx_rate != null ? tx_rate : 0);
                    iface.getStatistics().setTransmitDropRate(tx_dropped != null ? tx_dropped : 0);
                    iface.setSpeed(AssignIntValue(dict, VdsProperties.if_speed));
                    iface.getStatistics().setStatus(AssignInterfaceStatusValue(dict, VdsProperties.iface_status));

                    int hold =
                            (iface.getStatistics().getTransmitRate().compareTo(iface.getStatistics().getReceiveRate()) > 0 ? iface.getStatistics()
                                    .getTransmitRate()
                                    : iface
                                            .getStatistics().getReceiveRate()).intValue();
                    if (hold > networkUsage) {
                        networkUsage = hold;
                    }
                }
            }
            vds.setusage_network_percent((networkUsage > 100) ? 100 : networkUsage);
        }

        // ----------- vds cpu statistics info ---------------------
        vds.setcpu_sys(AssignDoubleValue(xmlRpcStruct, VdsProperties.cpu_sys));
        vds.setcpu_user(AssignDoubleValue(xmlRpcStruct, VdsProperties.cpu_user));
        if (vds.getcpu_sys() != null && vds.getcpu_user() != null) {
            vds.setusage_cpu_percent((int) (vds.getcpu_sys() + vds.getcpu_user()));
            if (vds.getusage_cpu_percent() >= vds.gethigh_utilization()
                    || vds.getusage_cpu_percent() <= vds.getlow_utilization()) {
                if (vds.getcpu_over_commit_time_stamp() == null) {
                    vds.setcpu_over_commit_time_stamp(new Date());
                }
            } else {
                vds.setcpu_over_commit_time_stamp(null);
            }
        }
        // CPU load reported by VDSM is in uptime-style format, i.e. normalized
        // to unity, so that say an 8% load is reported as 0.08

        Double d = AssignDoubleValue(xmlRpcStruct, VdsProperties.cpu_load);
        d = (d != null) ? d : 0;
        vds.setcpu_load(d.doubleValue() * 100.0);
        vds.setcpu_idle(AssignDoubleValue(xmlRpcStruct, VdsProperties.cpu_idle));
        vds.setmem_available(AssignLongValue(xmlRpcStruct, VdsProperties.mem_available));
        vds.setmem_shared(AssignLongValue(xmlRpcStruct, VdsProperties.mem_shared));

        vds.setswap_free(AssignLongValue(xmlRpcStruct, VdsProperties.swap_free));
        vds.setswap_total(AssignLongValue(xmlRpcStruct, VdsProperties.swap_total));
        vds.setksm_cpu_percent(AssignIntValue(xmlRpcStruct, VdsProperties.ksm_cpu_percent));
        vds.setksm_pages(AssignLongValue(xmlRpcStruct, VdsProperties.ksm_pages));
        vds.setksm_state(AssignBoolValue(xmlRpcStruct, VdsProperties.ksm_state));

        // dynamic data got from GetVdsStats
        if (xmlRpcStruct.containsKey(VdsProperties.transparent_huge_pages_state)) {
            vds.setTransparentHugePagesState(EnumUtils.valueOf(VdsTransparentHugePagesState.class, xmlRpcStruct
                    .getItem(VdsProperties.transparent_huge_pages_state).toString(), true));
        }
        if (xmlRpcStruct.containsKey(VdsProperties.anonymous_transparent_huge_pages)) {
            vds.setAnonymousHugePages(AssignIntValue(xmlRpcStruct, VdsProperties.anonymous_transparent_huge_pages));
        }
        vds.setnet_config_dirty(AssignBoolValue(xmlRpcStruct, VdsProperties.netConfigDirty));

        vds.setImagesLastCheck(AssignDoubleValue(xmlRpcStruct, VdsProperties.images_last_check));
        vds.setImagesLastDelay(AssignDoubleValue(xmlRpcStruct, VdsProperties.images_last_delay));

        Integer vm_count = AssignIntValue(xmlRpcStruct, VdsProperties.vm_count);
        vds.setvm_count(vm_count == null ? 0 : vm_count);
        vds.setvm_active(AssignIntValue(xmlRpcStruct, VdsProperties.vm_active));
        vds.setvm_migrating(AssignIntValue(xmlRpcStruct, VdsProperties.vm_migrating));
        updateVDSDomainData(vds, xmlRpcStruct);
        updateLocalDisksUsage(vds, xmlRpcStruct);
    }

    /**
     * Update {@link VDS#setLocalDisksUsage(Map)} with map of paths usage extracted from the returned returned value. The
     * usage is reported in MB.
     *
     * @param vds
     *            The VDS object to update.
     * @param xmlRpcStruct
     *            The XML/RPC to extract the usage from.
     */
    protected static void updateLocalDisksUsage(VDS vds, XmlRpcStruct xmlRpcStruct) {
        if (xmlRpcStruct.containsKey(VdsProperties.DISK_STATS)) {
            Map<String, Object> diskStatsStruct = (Map<String, Object>) xmlRpcStruct.getItem(VdsProperties.DISK_STATS);
            Map<String, Long> diskStats = new HashMap<String, Long>();

            vds.setLocalDisksUsage(diskStats);

            for (String path : diskStatsStruct.keySet()) {
                XmlRpcStruct pathStatsStruct = new XmlRpcStruct((Map<String, Object>) diskStatsStruct.get(path));

                diskStats.put(path, AssignLongValue(pathStatsStruct, VdsProperties.DISK_STATS_FREE));
            }
        }
    }

    private static void updateVDSDomainData(VDS vds, XmlRpcStruct xmlRpcStruct) {
        if (xmlRpcStruct.containsKey(VdsProperties.domains)) {
            Map<String, Object> domains = (Map<String, Object>)
                    xmlRpcStruct.getItem(VdsProperties.domains);
            ArrayList<VDSDomainsData> domainsData = new ArrayList<VDSDomainsData>();
            for (Map.Entry<String, ?> value : domains.entrySet()) {
                try {
                    VDSDomainsData data = new VDSDomainsData();
                    data.setDomainId(new Guid(value.getKey().toString()));
                    Map<String, Object> internalValue = (Map<String, Object>) value.getValue();
                    double lastCheck = 0;
                    data.setCode((Integer) (internalValue).get(VdsProperties.code));
                    if (internalValue.containsKey(VdsProperties.lastCheck)) {
                        lastCheck = Double.parseDouble((String) internalValue.get(VdsProperties.lastCheck));
                    }
                    data.setLastCheck(lastCheck);
                    double delay = 0;
                    if (internalValue.containsKey(VdsProperties.delay)) {
                        delay = Double.parseDouble((String) internalValue.get(VdsProperties.delay));
                    }
                    data.setDelay(delay);
                    domainsData.add(data);
                } catch (Exception e) {
                    log.error("failed building domains", e);
                }
            }
            vds.setDomains(domainsData);
        }
    }

    @SuppressWarnings("null")
    private static InterfaceStatus AssignInterfaceStatusValue(XmlRpcStruct input, String name) {
        InterfaceStatus ifaceStatus = InterfaceStatus.None;
        if (input.containsKey(name)) {
            String stringValue = (String) ((input.getItem(name) instanceof String) ? input.getItem(name) : null);
            if (!StringUtils.isEmpty(stringValue)) {
                if (stringValue.toLowerCase().trim().equals("up")) {
                    ifaceStatus = InterfaceStatus.Up;
                } else {
                    ifaceStatus = InterfaceStatus.Down;
                }
            }
        }
        return ifaceStatus;
    }

    private static Double AssignDoubleValue(XmlRpcStruct input, String name) {
        Double returnValue = null;
        if (input.containsKey(name)) {
            String stringValue = (String) ((input.getItem(name) instanceof String) ? input.getItem(name) : null);
            returnValue = (stringValue == null) ? null : Double.parseDouble(stringValue);
        }
        return returnValue;
    }

    /**
     * Do the same logic as AssignDoubleValue does, but instead, in case of null we return 0.
     * @param input - the Input xml
     * @param name - The name of the field we want to cast it to double.
     * @return - the double value.
     */
    private static Double assignDoubleValueWithNullProtection(XmlRpcStruct input, String name) {
        Double doubleValue = AssignDoubleValue(input, name);
        return (doubleValue == null ? 0.0 : doubleValue);
    }

    private static Integer AssignIntValue(XmlRpcStruct input, String name) {
        if (input.containsKey(name)) {
            if (input.getItem(name) instanceof Integer) {
                return (Integer) input.getItem(name);
            }
            String stringValue = (String) input.getItem(name);
            if (!StringUtils.isEmpty(stringValue)) { // in case the input
                                                     // is decimal and we
                                                     // need int.
                stringValue = stringValue.split("[.]", -1)[0];
            }
            try {
                int intValue = Integer.parseInt(stringValue);
                return intValue;
            } catch (NumberFormatException nfe) {
                String errMsg = String.format("Failed to parse %1$s value %2$s to integer", name, stringValue);
                log.error(errMsg, nfe);
            }
        }
        return null;
    }

    @SuppressWarnings("null")
    private static Long AssignLongValue(XmlRpcStruct input, String name) {
        if (input.containsKey(name)) {
            if (input.getItem(name) instanceof Long || input.getItem(name) instanceof Integer) {
                return Long.parseLong(input.getItem(name).toString());
            }
            String stringValue = (String) ((input.getItem(name) instanceof String) ? input.getItem(name) : null);
            if (!StringUtils.isEmpty(stringValue)) { // in case the input
                                                     // is decimal and we
                                                     // need int.
                stringValue = stringValue.split("[.]", -1)[0];
            }
            final Long dec = LongCompat.tryParse(stringValue);
            if (dec == null) {
                log.errorFormat("Failed to parse {0} value {1} to long", name, stringValue);
            } else {
                return dec;
            }
        }
        return null;
    }

    private static String AssignStringValue(XmlRpcStruct input, String name) {
        if (input.containsKey(name)) {
            return (String) ((input.getItem(name) instanceof String) ? input.getItem(name) : null);
        }
        return null;
    }

    private static String AssignStringValueFromArray(XmlRpcStruct input, String name) {
        if (input.containsKey(name)) {
            String[] arr = (String[]) ((input.getItem(name) instanceof String[]) ? input.getItem(name) : null);
            if (arr == null) {
                Object[] arr2 = (Object[]) ((input.getItem(name) instanceof Object[]) ? input.getItem(name) : null);
                if (arr2 != null) {
                    arr = new String[arr2.length];
                    for (int i = 0; i < arr2.length; i++)
                        arr[i] = arr2[i].toString();
                }
            }
            if (arr != null) {
                return StringUtils.join(arr, ',');
            }
        }
        return null;
    }

    private static Date AssignDateTImeFromEpoch(XmlRpcStruct input, String name) {
        Date retval = null;
        try {
            if (input.containsKey(name)) {
                Double secsSinceEpoch = (Double) input.getItem(name);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(secsSinceEpoch.longValue());
                retval = calendar.getTime();
            }
        } catch (RuntimeException ex) {
            String msg = String.format("VdsBroker::AssignDateTImeFromEpoch - failed to convert field %1$s to dateTime",
                    name);
            log.warn(msg, ex);
            retval = null;
        }
        return retval;
    }

    private static Boolean AssignBoolValue(XmlRpcStruct input, String name) {
        if (input.containsKey(name)) {
            if (input.getItem(name) instanceof Boolean) {
                return (Boolean) input.getItem(name);
            }
            return Boolean.parseBoolean(input.getItem(name).toString());
        }
        return null;
    }

    private static void initDisks(XmlRpcStruct vmStruct, VmDynamic vm) {
        Map disks = (Map) vmStruct.getItem(VdsProperties.vm_disks);
        ArrayList<DiskImageDynamic> disksData = new ArrayList<DiskImageDynamic>();
        List<Disk> vmDisksFromDb = DbFacade.getInstance().getDiskDao().getAllForVm(vm.getId());
        for (Object diskAsObj : disks.values()) {
            XmlRpcStruct disk = new XmlRpcStruct((Map) diskAsObj);
            DiskImageDynamic diskData = new DiskImageDynamic();
            String imageGroupIdString = AssignStringValue(disk, VdsProperties.image_group_id);
            if (!StringUtils.isEmpty(imageGroupIdString)) {
                Guid imageGroupIdGuid = new Guid(imageGroupIdString);
                DiskImage vmCurrentDisk = null;
                for (Disk vmDisk : vmDisksFromDb) {
                    if (vmDisk.getId() != null
                            && imageGroupIdGuid.equals(vmDisk.getId().getValue())
                            && vmDisk.getDiskStorageType() == DiskStorageType.IMAGE) {
                        vmCurrentDisk = (DiskImage) vmDisk;
                        break;
                    }
                }
                if (vmCurrentDisk != null) {
                    diskData.setId(vmCurrentDisk.getImageId());
                    diskData.setread_rate(AssignIntValue(disk, VdsProperties.vm_disk_read_rate));
                    diskData.setwrite_rate(AssignIntValue(disk, VdsProperties.vm_disk_write_rate));

                    if (disk.contains(VdsProperties.disk_actual_size)) {
                        Long size = AssignLongValue(disk, VdsProperties.disk_actual_size);
                        diskData.setactual_size(size != null ? size * 512 : 0);
                    } else if (disk.contains(VdsProperties.disk_true_size)) {
                        Long size = AssignLongValue(disk, VdsProperties.disk_true_size);
                        diskData.setactual_size(size != null ? size : 0);
                    }
                    if (disk.contains(VdsProperties.vm_disk_read_latency)) {
                        diskData.setReadLatency(assignDoubleValueWithNullProtection(disk,
                                VdsProperties.vm_disk_read_latency) / NANO_SECONDS);
                    }
                    if (disk.contains(VdsProperties.vm_disk_write_latency)) {
                        diskData.setWriteLatency(assignDoubleValueWithNullProtection(disk,
                                VdsProperties.vm_disk_write_latency) / NANO_SECONDS);
                    }
                    if (disk.contains(VdsProperties.vm_disk_flush_latency)) {
                        diskData.setFlushLatency(assignDoubleValueWithNullProtection(disk,
                                VdsProperties.vm_disk_flush_latency) / NANO_SECONDS);
                    }
                    disksData.add(diskData);
                }
            }
        }
        vm.setDisks(disksData);
    }

    private static void initAppsList(XmlRpcStruct vmStruct, VmDynamic vm) {
        if (vmStruct.contains(VdsProperties.app_list)) {
            Object tempAppsList = vmStruct.getItem(VdsProperties.app_list);
            if (tempAppsList instanceof Object[]) {
                Object[] apps = (Object[]) tempAppsList;
                StringBuilder builder = new StringBuilder();
                boolean firstTime = true;
                for (Object app : apps) {
                    String appString = (String) ((app instanceof String) ? app : null);
                    if (app == null) {
                        log.warnFormat("Failed to convert app: {0} to string", app);
                    }
                    if (!firstTime) {
                        builder.append(",");
                    } else {
                        firstTime = false;
                    }
                    builder.append(appString);
                }
                vm.setapp_list(builder.toString());
            } else {
                vm.setapp_list("");
            }
        }
    }

    private static void updateVdsStaticVersion(VDS vds, XmlRpcStruct xmlRpcStruct) {
        VdsVersion version = new VdsVersion();
        version.setVersionName(AssignStringValue(xmlRpcStruct, "version_name"));
        version.setSoftwareVersion(AssignStringValue(xmlRpcStruct, "software_version"));
        version.setSoftwareRevision(AssignStringValue(xmlRpcStruct, "software_revision"));
        version.setBuildName(AssignStringValue(xmlRpcStruct, "build_name"));
        try {
            version.parseFullVersion();
        } catch (RuntimeException e) {
            log.infoFormat("Couldn't parse vds version: {0} , {1} for Host {2}, {3}",
                    version.getSoftwareVersion(),
                    version.getSoftwareRevision(),
                    vds.getId(),
                    vds.getvds_name());
        }
        vds.setVersion(version);
    }

    private static VMStatus convertToVmStatus(String statusName) {
        VMStatus status = VMStatus.Unassigned;
        if ("Running".equals(statusName) || "Unknown".equals(statusName)) {
            status = VMStatus.Up;
        }
        else if ("Migration Source".equals(statusName)) {
            status = VMStatus.MigratingFrom;
        }
        else if ("Migration Destination".equals(statusName)) {
            status = VMStatus.MigratingTo;
        } else {
            try {
                statusName = statusName.replace(" ", "");
                status = EnumUtils.valueOf(VMStatus.class, statusName, true);
            } catch (Exception e) {
                log.errorFormat("Vm status: {0} illegal", statusName);
            }
        }
        return status;
    }

    public static void updateNetworkData(VDS vds, XmlRpcStruct xmlRpcStruct) {
        List<VdsNetworkInterface> oldInterfaces =
                DbFacade.getInstance().getInterfaceDAO().getAllInterfacesForVds(vds.getId());
        vds.getInterfaces().clear();

        // Interfaces list
        Map<String, Object> nics = (Map<String, Object>) xmlRpcStruct.getItem(VdsProperties.network_nics);
        if (nics != null) {
            for (String key : nics.keySet()) {
                VdsNetworkInterface iface = new VdsNetworkInterface();
                VdsNetworkStatistics iStats = new VdsNetworkStatistics();
                iface.setStatistics(iStats);
                iStats.setId(Guid.NewGuid());
                iface.setId(iStats.getId());

                iface.setName(key);
                iface.setVdsId(vds.getId());

                // name value of nic property, i.e.: speed = 1000
                Map<String, Object> nic = (Map<String, Object>) nics.get(key);
                if (nic != null) {
                    if (nic.get("speed") != null) {
                        Object speed = nic.get("speed");
                        iface.setSpeed((Integer) speed);
                    }
                    iface.setAddress((String) nic.get("addr"));
                    iface.setSubnet((String) nic.get("netmask"));
                    iface.setMacAddress((String) nic.get("hwaddr"));
                    // if we get "permhwaddr", we are a part of a bond and we use that as the mac address
                    if (nic.get("permhwaddr") != null) {
                        iface.setMacAddress((String) nic.get("permhwaddr"));
                    }
                    if (StringUtils.isNotBlank((String) nic.get(VdsProperties.mtu))) {
                        iface.setMtu(Integer.parseInt((String) nic.get(VdsProperties.mtu)));
                    }

                }

                iStats.setVdsId(vds.getId());

                vds.getInterfaces().add(iface);
            }
        }

        // interface to vlan map
        Map<String, Integer> currVlans = new HashMap<String, Integer>();

        // vlans
        Map<String, Object> vlans = (Map<String, Object>) xmlRpcStruct.getItem(VdsProperties.network_vlans);
        if (vlans != null) {
            for (String key : vlans.keySet()) {
                VdsNetworkInterface iface = new VdsNetworkInterface();
                VdsNetworkStatistics iStats = new VdsNetworkStatistics();
                iface.setStatistics(iStats);
                iStats.setId(Guid.NewGuid());
                iface.setId(iStats.getId());

                iface.setName(key);
                iface.setVdsId(vds.getId());

                if (key.contains(".")) {
                    String[] names = key.split("[.]", -1);
                    String vlan = names[1];
                    iface.setVlanId(Integer.parseInt(vlan));
                    currVlans.put(key, iface.getVlanId());
                }

                Map<String, Object> vlan = (Map<String, Object>) vlans.get(key);

                iface.setAddress((String) vlan.get("addr"));
                iface.setSubnet((String) vlan.get("netmask"));
                if (StringUtils.isNotBlank((String) vlan.get(VdsProperties.mtu))) {
                    iface.setMtu(Integer.parseInt((String) vlan.get(VdsProperties.mtu)));
                }

                iStats.setVdsId(vds.getId());

                vds.getInterfaces().add(iface);
            }
        }

        // bonds
        Map<String, Object> bonds = (Map<String, Object>) xmlRpcStruct.getItem(VdsProperties.network_bondings);
        if (bonds != null) {
            for (String key : bonds.keySet()) {
                VdsNetworkInterface iface = new VdsNetworkInterface();
                VdsNetworkStatistics iStats = new VdsNetworkStatistics();
                iface.setStatistics(iStats);
                iStats.setId(Guid.NewGuid());
                iface.setId(iStats.getId());

                iface.setName(key);
                iface.setVdsId(vds.getId());
                iface.setBonded(true);

                Map<String, Object> bond = (Map<String, Object>) bonds.get(key);
                if (bond != null) {
                    iface.setMacAddress((String) bond.get("hwaddr"));
                    iface.setAddress((String) bond.get("addr"));
                    iface.setSubnet((String) bond.get("netmask"));
                    iface.setGateway((String) bond.get(VdsProperties.GLOBAL_GATEWAY));
                    if (bond.get("slaves") != null) {
                        Object[] interfaces = (Object[]) bond.get("slaves");
                        iStats.setVdsId(vds.getId());
                        AddBond(vds, iface, interfaces);
                    }
                    if (StringUtils.isNotBlank((String) bond.get(VdsProperties.mtu))) {
                        iface.setMtu(Integer.parseInt((String) bond.get(VdsProperties.mtu)));
                    }

                    XmlRpcStruct config =
                            (bond.get("cfg") instanceof Map) ? new XmlRpcStruct((Map<String, Object>) bond.get("cfg"))
                                    : null;

                    if (config != null && config.getItem("BONDING_OPTS") != null) {
                        iface.setBondOptions(config.getItem("BONDING_OPTS").toString());
                    }
                    AddBootProtocol(config, iface);
                }
            }
        }
        // network to vlan map
        Map<String, Integer> networkVlans = new HashMap<String, Integer>();

        // Networks collection (name point to list of nics or bonds)
        Map<String, Object> networks = (Map<String, Object>) xmlRpcStruct.getItem(VdsProperties.network_networks);
        if (networks != null) {
            vds.getNetworks().clear();
            for (String key : networks.keySet()) {
                Map<String, Object> network = (Map<String, Object>) networks.get(key);
                if (network != null) {
                    Network net = new Network();
                    net.setname(key);

                    net.setaddr((String) network.get("addr"));
                    net.setsubnet((String) network.get("netmask"));
                    net.setgateway((String) network.get(VdsProperties.GLOBAL_GATEWAY));
                    if (StringUtils.isNotBlank((String) network.get(VdsProperties.mtu))) {
                        net.setMtu(Integer.parseInt((String) network.get(VdsProperties.mtu)));
                    }

                    // map interface to network
                    if (network.get("interface") != null) {
                        updateNetwrokDetailsInInterface(vds,
                                currVlans,
                                networkVlans,
                                network,
                                net,
                                network.get("interface").toString());
                    } else {
                        Object[] ports = (Object[]) network.get("ports");
                        if (ports != null) {
                            for (Object port : ports) {
                                updateNetwrokDetailsInInterface(vds,
                                        currVlans,
                                        networkVlans,
                                        network,
                                        net,
                                        port.toString());
                            }
                        }
                    }
                    vds.getNetworks().add(net);
                }
            }
        }

        // Check vlans are line with Clusters vlans
        checkClusterVlans(vds, networkVlans);

        // set bonding options
        setBondingOptions(vds, oldInterfaces);

        // This information was added in 3.1, so don't use it if it's not there.
        if (xmlRpcStruct.containsKey(VdsProperties.netConfigDirty)) {
            vds.setnet_config_dirty(AssignBoolValue(xmlRpcStruct, VdsProperties.netConfigDirty));
        }
    }

    /**
     * Update the network details on the given iface.
     *
     * @param vds
     *            The host (for getting the interface from).
     * @param currVlans
     *            Used for checking the VLANs later.
     * @param networkVlans
     *            Used for checking the VLANs later.
     * @param network
     *            Network struct to get details from.
     * @param net
     *            Network to get details from.
     * @param ifaceName
     *            The name of the interface to update.
     */
    private static void updateNetwrokDetailsInInterface(VDS vds,
            Map<String, Integer> currVlans,
            Map<String, Integer> networkVlans,
            Map<String, Object> network,
            Network net,
            String ifaceName) {
        VdsNetworkInterface iface = null;
        for (VdsNetworkInterface tempInterface : vds.getInterfaces()) {
            if (tempInterface.getName().equals(ifaceName)) {
                iface = tempInterface;
                break;
            }
        }
        if (iface != null) {
            iface.setNetworkName(net.getname());

            if (currVlans.containsKey(iface.getName())) {
                networkVlans.put(net.getname(), currVlans.get(iface.getName()));
            }
            iface.setAddress(net.getaddr());

            // if vdsm doesn't report the 'bridged' property, we assumes bridge-less network isn't supported
            boolean bridged = true;
            if (network.get("bridged") != null) {
                bridged = Boolean.parseBoolean(network.get("bridged").toString());
            }
            iface.setBridged(bridged);

            // set the management ip
            if (StringUtils.equals(iface.getNetworkName(), NetworkUtils.getEngineNetwork())) {
                iface.setType(iface.getType() | VdsInterfaceType.Management.getValue());
            }
            iface.setSubnet(net.getsubnet());
            iface.setGateway(net.getgateway());
            Map<String, Object> networkConfigAsMap =
                    (Map<String, Object>) ((network.get("cfg") instanceof Map) ? network.get("cfg") : null);
            XmlRpcStruct networkConfig = networkConfigAsMap == null ? null : new XmlRpcStruct(
                    networkConfigAsMap);
            AddBootProtocol(networkConfig, iface);
        }
    }

    // we check for old bonding options,
    // if we had value for the bonding options, i.e. the user set it by the UI
    // and we have host that is not returning it's bonding options(host below 2.2.4) we override
    // the "new" bonding options with the old one only if we have the new one as null and the old one is not
    private static void setBondingOptions(VDS vds, List<VdsNetworkInterface> oldInterfaces) {
        for (VdsNetworkInterface iface : oldInterfaces) {
            if (iface.getBondOptions() != null) {
                for (VdsNetworkInterface newIface : vds.getInterfaces()) {
                    if (iface.getName().equals(newIface.getName()) && newIface.getBondOptions() == null) {
                        newIface.setBondOptions(iface.getBondOptions());
                        break;
                    }
                }
            }
        }
    }

    private static void checkClusterVlans(VDS vds, Map<String, Integer> hostVlans) {
        List<Network> clusterNetworks = DbFacade.getInstance().getNetworkDAO()
                .getAllForCluster(vds.getvds_group_id());
        for (Network net : clusterNetworks) {
            if (net.getvlan_id() != null) {
                if (hostVlans.containsKey(net.getname())) {
                    if (!hostVlans.get(net.getname()).equals(net.getvlan_id())) {
                        // error wrong vlan
                        AuditLogableBase logable = new AuditLogableBase();
                        logable.setVdsId(vds.getId());
                        logable.AddCustomValue("VlanIdHost", hostVlans.get(net.getname()).toString());
                        logable.AddCustomValue("VlanIdCluster", net.getvlan_id().toString());
                        AuditLogDirector.log(logable, AuditLogType.NETWORK_HOST_USING_WRONG_CLUSER_VLAN);
                    }
                } else {
                    // error no vlan
                    AuditLogableBase logable = new AuditLogableBase();
                    logable.setVdsId(vds.getId());
                    logable.AddCustomValue("VlanIdCluster", net.getvlan_id().toString());
                    AuditLogDirector.log(logable, AuditLogType.NETWORK_HOST_MISSING_CLUSER_VLAN);
                }
            }
        }
    }

    private static void AddBootProtocol(XmlRpcStruct cfg, VdsNetworkInterface iface) {
        if (cfg != null) {
            if (cfg.getItem("BOOTPROTO") != null) {
                if (cfg.getItem("BOOTPROTO").toString().toLowerCase().equals("dhcp")) {
                    iface.setBootProtocol(NetworkBootProtocol.Dhcp);
                } else {
                    iface.setBootProtocol(NetworkBootProtocol.None);
                }
            } else if (cfg.containsKey("IPADDR") && !StringUtils.isEmpty(cfg.getItem("IPADDR").toString())) {
                iface.setBootProtocol(NetworkBootProtocol.StaticIp);
                if (cfg.containsKey(VdsProperties.gateway)) {
                    Object gateway = cfg.getItem(VdsProperties.gateway);
                    if (gateway != null && !StringUtils.isEmpty(gateway.toString())) {
                        iface.setGateway(gateway.toString());
                    }
                }
            } else {
                iface.setBootProtocol(NetworkBootProtocol.None);
            }
        }
    }

    private static void AddBond(VDS vds, VdsNetworkInterface iface, Object[] interfaces) {
        vds.getInterfaces().add(iface);
        if (interfaces != null) {
            for (Object name : interfaces) {
                VdsNetworkInterface nic = null;
                for (VdsNetworkInterface tempInterface : vds.getInterfaces()) {
                    if (tempInterface.getName().equals(name.toString())) {
                        nic = tempInterface;
                        break;
                    }
                }

                if (nic != null) {
                    nic.setBondName(iface.getName());
                }
            }
        }
    }

    private static final Log log = LogFactory.getLog(VdsBrokerObjectsBuilder.class);
}
