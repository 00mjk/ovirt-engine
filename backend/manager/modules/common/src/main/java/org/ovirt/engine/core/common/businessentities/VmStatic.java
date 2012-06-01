package org.ovirt.engine.core.common.businessentities;

import java.util.ArrayList;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.ovirt.engine.core.common.businessentities.OvfExportOnlyField.ExportOption;
import org.ovirt.engine.core.common.businessentities.mapping.GuidType;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.validation.annotation.ValidName;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.common.validation.group.UpdateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;

@Entity
@Table(name = "vm_static")
@TypeDef(name = "guid", typeClass = GuidType.class)
public class VmStatic extends VmBase {
    private static final long serialVersionUID = -2753306386502558044L;

    @Size(min = 1, max = BusinessEntitiesDefinitions.VM_NAME_SIZE)
    @Column(name = "vm_name")
    @ValidName(message = "ACTION_TYPE_FAILED_NAME_MAY_NOT_CONTAIN_SPECIAL_CHARS", groups = { CreateEntity.class,
            UpdateEntity.class })
    private String name = "";

    @Column(name = "vmt_guid")
    @Type(type = "guid")
    private Guid vmt_guid = new Guid();

    @Column(name = "is_initialized")
    private boolean is_initialized;

    @Column(name = "dedicated_vm_for_vds")
    @Type(type = "guid")
    private NGuid dedicated_vm_for_vds;

    @Column(name = "default_display_type")
    private DisplayType default_display_type = DisplayType.qxl;

    @OvfExportOnlyField(valueToIgnore = "MIGRATABLE", exportOption = ExportOption.EXPORT_NON_IGNORED_VALUES)
    @Column(name = "migration_support")
    private MigrationSupport migrationSupport;

    @OvfExportOnlyField(exportOption = ExportOption.EXPORT_NON_IGNORED_VALUES)
    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    @Column(name = "userdefined_properties")
    private String userDefinedProperties;

    @OvfExportOnlyField(exportOption = ExportOption.EXPORT_NON_IGNORED_VALUES)
    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    @Column(name = "predefined_properties")
    private String predefinedProperties;

    /**
     * Disk size in sectors of 512 bytes
     */
    @Transient
    private int m_nDiskSize;

    @Transient
    private int minAllocatedMemField;

    @Transient
    private String customProperties;

    @OvfExportOnlyField(exportOption = ExportOption.EXPORT_NON_IGNORED_VALUES)
    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    @Column(name = "cpu_pinning")
    private String cpuPinning;

    public VmStatic() {
        setnum_of_monitors(1);
        is_initialized = false;
        setis_auto_suspend(false);
        setnice_level(0);
        setdefault_boot_sequence(BootSequence.C);
        default_display_type = DisplayType.qxl;
        setvm_type(VmType.Desktop);
        sethypervisor_type(HypervisorType.KVM);
        setoperation_mode(OperationMode.FullVirtualized);
        migrationSupport = MigrationSupport.MIGRATABLE;
    }

    public VmStatic(VmStatic vmStatic) {
        super(vmStatic.getId(),
                vmStatic.getvds_group_id(),
                vmStatic.getos(),
                vmStatic.getcreation_date(),
                vmStatic.getdescription(),
                vmStatic.getmem_size_mb(),
                vmStatic.getnum_of_sockets(),
                vmStatic.getcpu_per_socket(),
                vmStatic.getnum_of_monitors(),
                vmStatic.getdomain(),
                vmStatic.gettime_zone(),
                vmStatic.getvm_type(),
                vmStatic.getusb_policy(),
                vmStatic.getfail_back(),
                vmStatic.getdefault_boot_sequence(),
                vmStatic.gethypervisor_type(),
                vmStatic.getoperation_mode(),
                vmStatic.getnice_level(),
                vmStatic.getis_auto_suspend(),
                vmStatic.getpriority(),
                vmStatic.getauto_startup(),
                vmStatic.getis_stateless(),
                vmStatic.getiso_path(),
                vmStatic.getorigin(),
                vmStatic.getkernel_url(),
                vmStatic.getkernel_params(),
                vmStatic.getinitrd_url(),
                vmStatic.getQuotaId());
        name = vmStatic.getvm_name();
        vmt_guid = vmStatic.getvmt_guid();
        setnum_of_monitors(vmStatic.getnum_of_monitors());
        setis_initialized(vmStatic.getis_initialized());
        setdefault_display_type(vmStatic.getdefault_display_type());
        setdedicated_vm_for_vds(vmStatic.getdedicated_vm_for_vds());
        setMigrationSupport(vmStatic.getMigrationSupport());
        setMinAllocatedMem(vmStatic.getMinAllocatedMem());
    }

    public VmStatic(String description, int mem_size_mb, VmOsType os, Guid vds_group_id, Guid vm_guid, String vm_name,
            Guid vmt_guid, String domain, java.util.Date creation_date, int num_of_monitors, boolean is_initialized,
            boolean is_auto_suspend,
            Guid dedicated_vm_for_vds,
            int num_of_sockets,
            int cpu_per_socket,
            int numOfMonitors,
            UsbPolicy usb_policy, String time_zone, boolean auto_startup, boolean is_stateless, boolean fail_back,
            BootSequence default_boot_sequence, VmType vm_type, HypervisorType hypervisor_type,
            OperationMode operation_mode, int minAllocatedMem, Guid quotaGuid) {
        super(vm_guid,
                vds_group_id,
                os,
                creation_date,
                description,
                mem_size_mb,
                num_of_sockets,
                cpu_per_socket,
                numOfMonitors,
                domain,
                time_zone,
                vm_type,
                usb_policy,
                fail_back,
                default_boot_sequence,
                hypervisor_type,
                operation_mode,
                0,
                is_auto_suspend,
                0,
                auto_startup,
                is_stateless,
                null,
                OriginType.valueOf(Config.<String> GetValue(ConfigValues.OriginType)),
                null,
                null,
                null,
                quotaGuid);

        this.name = vm_name;
        this.vmt_guid = vmt_guid;
        this.setnum_of_monitors(num_of_monitors);
        this.setis_initialized(is_initialized);
        this.setusb_policy(usb_policy);
        this.setdedicated_vm_for_vds(dedicated_vm_for_vds);
        this.setMinAllocatedMem(minAllocatedMem);
    }

    public String getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(String customProperties) {
        this.customProperties = customProperties;
    }

    public String getPredefinedProperties() {
        return predefinedProperties;
    }

    public void setPredefinedProperties(String predefinedProperties) {
        this.predefinedProperties = predefinedProperties;
    }

    public String getUserDefinedProperties() {
        return userDefinedProperties;
    }

    public void setUserDefinedProperties(String userDefinedProperties) {
        this.userDefinedProperties = userDefinedProperties;
    }

    public int getDiskSize() {
        return m_nDiskSize;
    }

    public void setDiskSize(int value) {
        m_nDiskSize = value;
    }

    public boolean getIsFirstRun() {
        return !getis_initialized();
    }

    public MigrationSupport getMigrationSupport() {
        return migrationSupport;
    }

    public void setMigrationSupport(MigrationSupport migrationSupport) {
        this.migrationSupport = migrationSupport;
    }

    public String getvm_name() {
        return this.name;
    }

    public void setvm_name(String value) {
        this.name = value;
    }

    public Guid getvmt_guid() {
        return this.vmt_guid;
    }

    public void setvmt_guid(Guid value) {
        this.vmt_guid = value;
    }

    public boolean getis_initialized() {
        return is_initialized;
    }

    public void setis_initialized(boolean value) {
        is_initialized = value;
    }

    public int getnum_of_cpus() {
        return getcpu_per_socket() * getnum_of_sockets();
    }

    public NGuid getdedicated_vm_for_vds() {
        return dedicated_vm_for_vds;
    }

    public void setdedicated_vm_for_vds(NGuid value) {
        dedicated_vm_for_vds = value;
    }

    public DisplayType getdefault_display_type() {
        return default_display_type;
    }

    public void setdefault_display_type(DisplayType value) {
        default_display_type = value;
    }

    public int getMinAllocatedMem() {
        if (minAllocatedMemField > 0) {
            return minAllocatedMemField;
        }
        return getmem_size_mb();
    }

    public void setMinAllocatedMem(int value) {
        minAllocatedMemField = value;
    }

    @Override
    public Object getQueryableId() {
        return getId();
    }

    @Override
    public ArrayList<String> getChangeablePropertiesList() {
        // Actual implementation is TBD
        return null;
    }

    public String getCpuPinning() {
        return cpuPinning;
    }

    public void setCpuPinning(String cpuPinning) {
        this.cpuPinning = cpuPinning;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((dedicated_vm_for_vds == null) ? 0 : dedicated_vm_for_vds.hashCode());
        result = prime * result + ((default_display_type == null) ? 0 : default_display_type.hashCode());
        result = prime * result + (is_initialized ? 1231 : 1237);
        result = prime * result + m_nDiskSize;
        result = prime * result + ((migrationSupport == null) ? 0 : migrationSupport.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((predefinedProperties == null) ? 0 : predefinedProperties.hashCode());
        result = prime * result + ((userDefinedProperties == null) ? 0 : userDefinedProperties.hashCode());
        result = prime * result + ((vmt_guid == null) ? 0 : vmt_guid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof VmStatic)) {
            return false;
        }
        VmStatic other = (VmStatic) obj;
        if (dedicated_vm_for_vds == null) {
            if (other.dedicated_vm_for_vds != null) {
                return false;
            }
        } else if (!dedicated_vm_for_vds.equals(other.dedicated_vm_for_vds)) {
            return false;
        }
        if (default_display_type != other.default_display_type) {
            return false;
        }
        if (is_initialized != other.is_initialized) {
            return false;
        }
        if (m_nDiskSize != other.m_nDiskSize) {
            return false;
        }
        if (migrationSupport != other.migrationSupport) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (predefinedProperties == null) {
            if (other.predefinedProperties != null) {
                return false;
            }
        } else if (!predefinedProperties.equals(other.predefinedProperties)) {
            return false;
        }
        if (userDefinedProperties == null) {
            if (other.userDefinedProperties != null) {
                return false;
            }
        } else if (!userDefinedProperties.equals(other.userDefinedProperties)) {
            return false;
        }
        if (vmt_guid == null) {
            if (other.vmt_guid != null) {
                return false;
            }
        } else if (!vmt_guid.equals(other.vmt_guid)) {
            return false;
        }
        return true;
    }

}
