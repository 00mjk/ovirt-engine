package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.SessionState;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmExitStatus;
import org.ovirt.engine.core.common.businessentities.VmPauseStatus;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.dal.dbbroker.DbFacadeUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class VmDynamicDAODbFacadeImpl extends MassOperationsGenericDaoDbFacade<VmDynamic, Guid>
        implements VmDynamicDAO {

    public VmDynamicDAODbFacadeImpl() {
        super("VmDynamic");
        setProcedureNameForGet("GetVmDynamicByVmGuid");
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<VmDynamic> getAllRunningForVds(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vds_id", id);

        RowMapper<VmDynamic> mapper = createEntityRowMapper();

        return getCallsHandler().executeReadList("GetVmsDynamicRunningOnVds", mapper, parameterSource);
    }

    @Override
    public void updateStatus(Guid vmGuid, VMStatus status) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_guid", vmGuid)
                .addValue("status", status);

        getCallsHandler().executeModification("UpdateVmDynamicStatus", parameterSource);
    }

    @Override
    public boolean updateConsoleUserWithOptimisticLocking(VmDynamic vm) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_guid", vm.getId())
                .addValue("console_user_id", vm.getConsoleUserId())
                .addValue("guest_cur_user_name", vm.getGuestCurrentUserName())
                .addValue("console_cur_user_name", vm.getConsoleCurrentUserName());

        Map<String, Object> results = getCallsHandler().executeModification("UpdateConsoleUserWithOptimisticLocking", parameterSource);

        return (Boolean) results.get("updated");
    }

    @Override
    public List<VmDynamic> getAll() {
        throw new NotImplementedException();
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("vm_guid", id);
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(VmDynamic vm) {
        return createIdParameterMapper(vm.getId())
                .addValue("app_list", vm.getAppList())
                .addValue("guest_cur_user_name", vm.getGuestCurrentUserName())
                .addValue("console_cur_user_name", vm.getConsoleCurrentUserName())
                .addValue("console_user_id", vm.getConsoleUserId())
                .addValue("guest_last_login_time",
                        vm.getGuestLastLoginTime())
                .addValue("guest_last_logout_time",
                        vm.getGuestLastLogoutTime())
                .addValue("guest_os", vm.getGuestOs())
                .addValue("migrating_to_vds", vm.getMigratingToVds())
                .addValue("run_on_vds", vm.getRunOnVds())
                .addValue("status", vm.getStatus())
                .addValue("vm_host", vm.getVmHost())
                .addValue("vm_ip", vm.getVmIp())
                .addValue("last_start_time", vm.getLastStartTime())
                .addValue("vm_pid", vm.getVmPid())
                .addValue("display", vm.getDisplay())
                .addValue("acpi_enable", vm.getAcpiEnable())
                .addValue("session", vm.getSession())
                .addValue("display_ip", vm.getDisplayIp())
                .addValue("display_type", vm.getDisplayType())
                .addValue("kvm_enable", vm.getKvmEnable())
                .addValue("boot_sequence", vm.getBootSequence())
                .addValue("display_secure_port", vm.getDisplaySecurePort())
                .addValue("utc_diff", vm.getUtcDiff())
                .addValue("last_vds_run_on", vm.getLastVdsRunOn())
                .addValue("client_ip", vm.getClientIp())
                .addValue("guest_requested_memory",
                        vm.getGuestRequestedMemory())
                .addValue("hibernation_vol_handle",
                        vm.getHibernationVolHandle())
                .addValue("exit_status", vm.getExitStatus().getValue())
                .addValue("pause_status", vm.getPauseStatus().getValue())
                .addValue("exit_message", vm.getExitMessage())
                .addValue("hash", vm.getHash())
                .addValue("guest_agent_nics_hash", vm.getGuestAgentNicsHash())
                .addValue("last_watchdog_event", vm.getLastWatchdogEvent())
                .addValue("last_watchdog_action", vm.getLastWatchdogAction());
    }

    @Override
    protected RowMapper<VmDynamic> createEntityRowMapper() {
        return new RowMapper<VmDynamic>() {
            @Override
            public VmDynamic mapRow(ResultSet rs, int rowNum)
                    throws SQLException {
                VmDynamic entity = new VmDynamic();
                entity.setAppList(rs.getString("app_list"));
                entity.setGuestCurrentUserName(rs
                        .getString("guest_cur_user_name"));
                entity.setConsoleCurrentUserName(rs
                        .getString("console_cur_user_name"));
                entity.setConsoleUserId(NGuid.createGuidFromString(rs.getString("console_user_id")));
                entity.setGuestLastLoginTime(DbFacadeUtils.fromDate(rs
                        .getTimestamp("guest_last_login_time")));
                entity.setGuestLastLogoutTime(DbFacadeUtils.fromDate(rs
                        .getTimestamp("guest_last_logout_time")));
                entity.setGuestOs(rs.getString("guest_os"));
                entity.setMigratingToVds(NGuid.createGuidFromString(rs
                        .getString("migrating_to_vds")));
                entity.setRunOnVds(NGuid.createGuidFromString(rs
                        .getString("run_on_vds")));
                entity.setStatus(VMStatus.forValue(rs.getInt("status")));
                entity.setId(Guid.createGuidFromString(rs
                        .getString("vm_guid")));
                entity.setVmHost(rs.getString("vm_host"));
                entity.setVmIp(rs.getString("vm_ip"));
                entity.setLastStartTime(DbFacadeUtils.fromDate(rs
                        .getTimestamp("last_start_time")));
                entity.setVmPid((Integer) rs.getObject("vm_pid"));
                entity.setDisplay((Integer) rs.getObject("display"));
                entity.setAcpiEnable((Boolean) rs.getObject("acpi_enable"));
                entity.setSession(SessionState.forValue(rs.getInt("session")));
                entity.setDisplayIp(rs.getString("display_ip"));
                entity.setDisplayType(DisplayType.forValue(rs
                        .getInt("display_type")));
                entity.setKvmEnable((Boolean) rs.getObject("kvm_enable"));
                entity.setBootSequence(BootSequence.forValue(rs
                        .getInt("boot_sequence")));
                entity.setDisplaySecurePort((Integer) rs
                        .getObject("display_secure_port"));
                entity.setUtcDiff((Integer) rs.getObject("utc_diff"));
                entity.setLastVdsRunOn(NGuid.createGuidFromString(rs
                        .getString("last_vds_run_on")));
                entity.setClientIp(rs.getString("client_ip"));
                entity.setGuestRequestedMemory((Integer) rs
                        .getObject("guest_requested_memory"));
                entity.setHibernationVolHandle(rs
                        .getString("hibernation_vol_handle"));
                VmExitStatus exitStatus = VmExitStatus.forValue(rs
                        .getInt("exit_status"));
                VmPauseStatus pauseStatus = VmPauseStatus.forValue(rs
                        .getInt("pause_status"));
                entity.setExitMessage(rs.getString("exit_message"));
                entity.setExitStatus(exitStatus);
                entity.setPauseStatus(pauseStatus);
                entity.setHash(rs.getString("hash"));
                entity.setGuestAgentNicsHash(rs.getInt("guest_agent_nics_hash"));
                entity.setLastWatchdogEvent(getLong(rs, "last_watchdog_event"));
                entity.setLastWatchdogAction(rs.getString("last_watchdog_action"));
                return entity;
            }
        };
    }
}
