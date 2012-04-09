package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.QuotaStorage;
import org.ovirt.engine.core.common.businessentities.QuotaVdsGroup;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * <code>QuotaDAODbFacadeImpl</code> implements the calling to quota stored procedures (@see QuotaDAO).
 */
public class QuotaDAODbFacadeImpl extends BaseDAODbFacade implements QuotaDAO {

    /**
     * Save <code>Quota</code> entity with specific <code>Quota</code> storage and <code>Quota</code> vdsGroup
     * limitation list.
     */
    @Override
    public void save(Quota quota) {
        saveGlobalQuota(quota);
        saveStorageSpecificQuotas(quota);
        saveVdsGroupSpecificQuotas(quota);
    }

    /**
     * Get <code>Quota</code> by name.
     *
     * @param quotaName
     *            - The quota name to find.
     * @param storagePoolId
     *            - The storage pool id that the quota is being searched in.
     * @return The quota entity that was found.
     */
    @Override
    public Quota getQuotaByQuotaName(String quotaName) {
        MapSqlParameterSource quotaParameterSource = getCustomMapSqlParameterSource();
        quotaParameterSource.addValue("quota_name", quotaName);
        Quota quotaEntity =
                getCallsHandler().executeRead("GetQuotaByQuotaName", getQuotaFromResultSet(), quotaParameterSource);
        return quotaEntity;
    }

    /**
     * Get list of <code>Quotas</code> which are consumed by ad element id in storage pool (if not storage pool id not
     * null).
     *
     * @param adElementId
     *            - The user ID or group ID.
     * @param storagePoolId
     *            - The storage pool Id to search the quotas in (If null search all over the setup).
     * @return All quotas for user.
     */
    @Override
    public List<Quota> getQuotaByAdElementId(Guid adElementId, Guid storagePoolId) {
        MapSqlParameterSource quotaParameterSource = getCustomMapSqlParameterSource();
        quotaParameterSource.addValue("ad_element_id", adElementId);
        quotaParameterSource.addValue("storage_pool_id", storagePoolId);
        List<Quota> quotaEntityList =
                getCallsHandler().executeReadList("GetQuotaByAdElementId",
                        getQuotaMetaDataFromResultSet(),
                        quotaParameterSource);
        return quotaEntityList;
    }

    /**
     * Get specific limitation for <code>VdsGroup</code>.
     *
     * @param vdsGroupId
     *            - The vds group id, if null returns all the vds group limitations in the storage pool.
     * @param quotaId
     *            - The <code>Quota</code> id
     * @return List of QuotaStorage
     */
    @Override
    public List<QuotaVdsGroup> getQuotaVdsGroupByVdsGroupGuid(Guid vdsGroupId, Guid quotaId) {
        return getQuotaVdsGroupByVdsGroupGuid(vdsGroupId, quotaId, true);
    }

    /**
     * Get specific limitation for <code>VdsGroup</code>.
     *
     * @param vdsGroupId
     *            - The vds group id, if null returns all the vds group limitations in the storage pool.
     * @param quotaId
     *            - The <code>Quota</code> id
     * @param allowEmpty
     *            - Whether to return empty quotas or not
     * @return List of QuotaStorage
     */
    @Override
    public List<QuotaVdsGroup> getQuotaVdsGroupByVdsGroupGuid(Guid vdsGroupId, Guid quotaId, boolean allowEmpty) {
        MapSqlParameterSource parameterSource =
                createQuotaIdParameterMapper(quotaId)
                        .addValue("vds_group_id", vdsGroupId)
                        .addValue("allow_empty", allowEmpty);
        List<QuotaVdsGroup> quotaVdsGroupList = getCallsHandler().executeReadList("GetQuotaVdsGroupByVdsGroupGuid",
                getVdsGroupQuotaResultSet(),
                parameterSource);
        return quotaVdsGroupList;
    }

    /**
     * Get specific limitation for storage domain.
     *
     * @param storageId
     *            - The storage id, if null returns all the storages limitation in the storage pool.
     * @param quotaId
     *            - The quota id
     * @return List of QuotaStorage
     */
    @Override
    public List<QuotaStorage> getQuotaStorageByStorageGuid(Guid storageId, Guid quotaId) {
        return getQuotaStorageByStorageGuid(storageId, quotaId, true);
    }

    /**
     * Get specific limitation for storage domain.
     *
     * @param storageId
     *            - The storage id, if null returns all the storages limitation in the storage pool.
     * @param quotaId
     *            - The quota id
     * @param allowEmpty
     *            - Whether to return empty quotas or not
     * @return List of QuotaStorage
     */
    @Override
    public List<QuotaStorage> getQuotaStorageByStorageGuid(Guid storageId, Guid quotaId, boolean allowEmpty) {
        MapSqlParameterSource parameterSource =
                createQuotaIdParameterMapper(quotaId).addValue("storage_id", storageId).addValue("allow_empty",
                        allowEmpty);
        List<QuotaStorage> quotaStorageList = getCallsHandler().executeReadList("GetQuotaStorageByStorageGuid",
                getQuotaStorageResultSet(),
                parameterSource);
        return quotaStorageList;
    }

    /**
     * Returns all the Quota storages in the storage pool if v_storage_id is null, if v_storage_id is not null then a
     * specific quota storage will be returned.
     */
    @Override
    public List<Quota> getQuotaByStoragePoolGuid(Guid storagePoolId) {
        MapSqlParameterSource parameterSource =
                getCustomMapSqlParameterSource().addValue("storage_pool_id", storagePoolId);
        List<Quota> quotaList = getCallsHandler().executeReadList("GetQuotaByStoragePoolGuid",
                getQuotaFromResultSet(),
                parameterSource);
        return quotaList;
    }

    /**
     * Get full <code>Quota</code> entity.
     */
    @Override
    public Quota getById(Guid quotaId) {
        MapSqlParameterSource parameterSource = createQuotaIdParameterMapper(quotaId);

        Quota quotaEntity =
                getCallsHandler().executeRead("GetQuotaByQuotaGuid", getQuotaFromResultSet(), parameterSource);

        if (quotaEntity != null) {
            quotaEntity.setQuotaVdsGroups(getQuotaVdsGroupByQuotaGuid(quotaId));
            quotaEntity.setQuotaStorages(getQuotaStorageByQuotaGuid(quotaId));
        }
        return quotaEntity;
    }

    /**
     * Get all quota storages which belong to quota with quotaId.
     */
    @Override
    public List<QuotaStorage> getQuotaStorageByQuotaGuid(Guid quotaId) {
        MapSqlParameterSource parameterSource = createQuotaIdParameterMapper(quotaId);
        return getCallsHandler().executeReadList("GetQuotaStorageByQuotaGuid",
                getQuotaStorageResultSet(),
                parameterSource);
    }

    /**
     * Get all quota storages which belong to quota with quotaId.
     */
    @Override
    public List<QuotaStorage> getQuotaStorageByQuotaGuidWithGeneralDefault(Guid quotaId) {
        return getQuotaStorageByStorageGuid(null, quotaId, false);
    }

    /**
     * Get all quota Vds groups, which belong to quota with quotaId.
     */
    @Override
    public List<QuotaVdsGroup> getQuotaVdsGroupByQuotaGuid(Guid quotaId) {
        MapSqlParameterSource parameterSource = createQuotaIdParameterMapper(quotaId);
        return getCallsHandler().executeReadList("GetQuotaVdsGroupByQuotaGuid",
                getVdsGroupQuotaResultSet(),
                parameterSource);
    }

    /**
     * Get all quota Vds groups, which belong to quota with quotaId.
     * In case no quota Vds Groups are returned, a fictitious QuotaVdsGroup is returned,
     * with an {@link Guid.Empty} Vds Id and a <code>null</code> name.
     */
    @Override
    public List<QuotaVdsGroup> getQuotaVdsGroupByQuotaGuidWithGeneralDefault(Guid quotaId) {
        return getQuotaVdsGroupByVdsGroupGuid(null, quotaId, false);
    }

    @Override
    public Quota getDefaultQuotaByStoragePoolId(Guid storagePoolId) {
        MapSqlParameterSource quotaParameterSource = getCustomMapSqlParameterSource();
        quotaParameterSource.addValue("storage_pool_id", storagePoolId);
        Quota quotaEntity =
                getCallsHandler().executeRead("getDefaultQuotaByStoragePoolId",
                        getQuotaFromResultSet(),
                        quotaParameterSource);
        return quotaEntity;
    }

    @Override
    public List<Quota> getAllRelevantQuotasForStorage(Guid storageId) {
        MapSqlParameterSource quotaParameterSource = getCustomMapSqlParameterSource();
        quotaParameterSource.addValue("storage_id", storageId);
        List<Quota> quotas =
                getCallsHandler().executeReadList("getAllThinQuotasByStorageId",
                        getQuotaMetaDataFromResultSet(),
                        quotaParameterSource);
        return quotas;
    }

    @Override
    public List<Quota> getAllRelevantQuotasForVdsGroup(Guid vdsGroupId) {
        MapSqlParameterSource quotaParameterSource = getCustomMapSqlParameterSource();
        quotaParameterSource.addValue("vds_group_id", vdsGroupId);
        List<Quota> quotas =
                getCallsHandler().executeReadList("getAllThinQuotasByVDSGroupId",
                        getQuotaMetaDataFromResultSet(),
                        quotaParameterSource);
        return quotas;
    }

    /**
     * Remove quota with quota id.
     */
    @Override
    public void remove(Guid id) {
        getCallsHandler().executeModification("DeleteQuotaByQuotaGuid",
                createQuotaIdParameterMapper(id));
    }

    /**
     * Update <Code>quota</Code>, by updating the quota meta data and remove all its limitations and add the limitations
     * from the quota parameter.
     */
    @Override
    public void update(Quota quota) {
        getCallsHandler().executeModification("UpdateQuotaMetaData",
                createQuotaMetaDataParameterMapper(quota));
        getCallsHandler().executeModification("DeleteQuotaLimitationByQuotaGuid",
                createQuotaIdParameterMapper(quota.getId()));
        getCallsHandler().executeModification("InsertQuotaLimitation", getFullQuotaParameterMap(quota));
        saveStorageSpecificQuotas(quota);
        saveVdsGroupSpecificQuotas(quota);
    }

    /**
     * Return initialized entity with quota Vds group result set.
     */
    private ParameterizedRowMapper<QuotaVdsGroup> getVdsGroupQuotaResultSet() {
        ParameterizedRowMapper<QuotaVdsGroup> mapperQuotaLimitation = new ParameterizedRowMapper<QuotaVdsGroup>() {
            @Override
            public QuotaVdsGroup mapRow(ResultSet rs, int rowNum)
                    throws SQLException {
                QuotaVdsGroup entity = new QuotaVdsGroup();
                entity.setQuotaId(Guid.createGuidFromString(rs.getString("quota_id")));
                entity.setQuotaVdsGroupId(Guid.createGuidFromString(rs.getString("quota_vds_group_id")));
                entity.setVdsGroupId(Guid.createGuidFromString(rs.getString("vds_group_id")));
                entity.setVdsGroupName(rs.getString("vds_group_name"));
                entity.setMemSizeMB((Long) rs.getObject("mem_size_mb"));
                entity.setMemSizeMBUsage((Long) rs.getObject("mem_size_mb_usage"));
                entity.setVirtualCpu((Integer) rs.getObject("virtual_cpu"));
                entity.setVirtualCpuUsage((Integer) rs.getObject("virtual_cpu_usage"));

                return entity;
            }
        };
        return mapperQuotaLimitation;
    }

    /**
     * Returns initialized entity with quota Storage result set.
     */
    private ParameterizedRowMapper<QuotaStorage> getQuotaStorageResultSet() {
        ParameterizedRowMapper<QuotaStorage> mapperQuotaLimitation = new ParameterizedRowMapper<QuotaStorage>() {
            @Override
            public QuotaStorage mapRow(ResultSet rs, int rowNum)
                    throws SQLException {
                QuotaStorage entity = new QuotaStorage();
                entity.setQuotaId(Guid.createGuidFromString(rs.getString("quota_id")));
                entity.setQuotaStorageId(Guid.createGuidFromString(rs.getString("quota_storage_id")));
                entity.setStorageId(Guid.createGuidFromString(rs.getString("storage_id")));
                entity.setStorageName(rs.getString("storage_name"));
                entity.setStorageSizeGB((Long) rs.getObject("storage_size_gb"));
                entity.setStorageSizeGBUsage((Double) rs.getObject("storage_size_gb_usage"));
                return entity;
            }
        };
        return mapperQuotaLimitation;
    }

    /**
     * Returns initialized entity with quota result set.
     */
    private ParameterizedRowMapper<Quota> getQuotaFromResultSet() {
        ParameterizedRowMapper<Quota> mapper = new ParameterizedRowMapper<Quota>() {
            @Override
            public Quota mapRow(ResultSet rs, int rowNum)
                    throws SQLException {
                Quota entity = getQuotaMetaDataFromResultSet(rs);

                // Check if memory size is not null, this is an indication if global limitation for vds group exists or
                // not, since global limitation must be for all the quota vds group parameters.
                if (rs.getObject("mem_size_mb") != null) {
                    // Set global vds group quota.
                    QuotaVdsGroup vdsGroupEntity = new QuotaVdsGroup();
                    vdsGroupEntity.setMemSizeMB((Long) rs.getObject("mem_size_mb"));
                    vdsGroupEntity.setMemSizeMBUsage((Long) rs.getObject("mem_size_mb_usage"));
                    vdsGroupEntity.setVirtualCpu((Integer) rs.getObject("virtual_cpu"));
                    vdsGroupEntity.setVirtualCpuUsage((Integer) rs.getObject("virtual_cpu_usage"));
                    entity.setGlobalQuotaVdsGroup(vdsGroupEntity);
                }

                // Check if storage limit size is not null, this is an indication if global limitation for storage
                // exists or
                // not.
                if (rs.getObject("storage_size_gb") != null) {
                    // Set global storage quota.
                    QuotaStorage storageEntity = new QuotaStorage();
                    storageEntity.setStorageSizeGB((Long) rs.getObject("storage_size_gb"));
                    storageEntity.setStorageSizeGBUsage((Double) rs.getObject("storage_size_gb_usage"));
                    entity.setGlobalQuotaStorage(storageEntity);
                }

                return entity;
            }
        };
        return mapper;
    }

    /**
     * Returns initialized entity with quota meta data result set.
     */
    private ParameterizedRowMapper<Quota> getQuotaMetaDataFromResultSet() {
        ParameterizedRowMapper<Quota> mapper = new ParameterizedRowMapper<Quota>() {
            @Override
            public Quota mapRow(ResultSet rs, int rowNum)
                    throws SQLException {
                return getQuotaMetaDataFromResultSet(rs);
            }
        };
        return mapper;
    }

    private Quota getQuotaMetaDataFromResultSet(ResultSet rs) throws SQLException {
        Quota entity = new Quota();
        entity.setId(Guid.createGuidFromString(rs.getString("quota_id")));
        entity.setStoragePoolId(Guid.createGuidFromString(rs.getString("storage_pool_id")));
        entity.setStoragePoolName(rs.getString("storage_pool_name"));
        entity.setQuotaName((String) rs.getObject("quota_name"));
        entity.setDescription((String) rs.getObject("description"));
        entity.setThresholdVdsGroupPercentage((Integer) rs.getObject("threshold_vds_group_percentage"));
        entity.setThresholdStoragePercentage((Integer) rs.getObject("threshold_storage_percentage"));
        entity.setGraceVdsGroupPercentage((Integer) rs.getObject("grace_vds_group_percentage"));
        entity.setGraceStoragePercentage((Integer) rs.getObject("grace_storage_percentage"));
        entity.setQuotaEnforcementType(QuotaEnforcementTypeEnum.forValue(rs.getInt("quota_enforcement_type")));
        entity.setIsDefaultQuota(rs.getBoolean("is_default_quota"));
        return entity;
    }

    private MapSqlParameterSource createQuotaIdParameterMapper(Guid quotaId) {
        MapSqlParameterSource quotaParameterSource = getCustomMapSqlParameterSource()
                .addValue("id", quotaId);
        return quotaParameterSource;
    }

    /**
     * Build quota storage parameter map, for quota limitation table, to indicate specific limitation on storage domain.
     *
     * @param quotaId
     *            - The global quota id which the storage is referencing to
     * @param quotaStorage
     *            - The business entity which reflects the limitation on the specific storage.
     * @return - Parameter Map
     */
    private MapSqlParameterSource getQuotaStorageParameterMap(Guid quotaId, QuotaStorage quotaStorage) {
        MapSqlParameterSource storageQuotaParameterMap =
                createQuotaIdParameterMapper(quotaStorage.getQuotaStorageId()).addValue("quota_id",
                        quotaId)
                        .addValue("storage_id", quotaStorage.getStorageId())
                        .addValue("vds_group_id", null)
                        .addValue("storage_size_gb", quotaStorage.getStorageSizeGB())
                        .addValue("virtual_cpu", null)
                        .addValue("mem_size_mb", null);
        return storageQuotaParameterMap;
    }

    /**
     * Build quota vds group parameter map, for quota limitation table, to indicate specific limitation on specific
     * <code>VdsGroup</code>.
     *
     * @param quotaId
     *            - The global quota id which the <code>VdsGroup</code> is referencing to
     * @param quotaVdsGroup
     *            - The business entity which reflects the limitation on the specific vdsGroup.
     * @return - <code>VdsGroup</code> Parameter Map
     */
    private MapSqlParameterSource getQuotaVdsGroupParameterMap(Guid quotaId, QuotaVdsGroup quotaVdsGroup) {
        MapSqlParameterSource vdsGroupQuotaParameterMap =
                createQuotaIdParameterMapper(quotaVdsGroup.getQuotaVdsGroupId()).addValue("quota_id", quotaId)
                        .addValue("vds_group_id", quotaVdsGroup.getVdsGroupId())
                        .addValue("storage_id", null)
                        .addValue("storage_size_gb", null)
                        .addValue("virtual_cpu", quotaVdsGroup.getVirtualCpu())
                        .addValue("mem_size_mb", quotaVdsGroup.getMemSizeMB());
        return vdsGroupQuotaParameterMap;
    }

    /**
     * Build parameter map, for quota limitation table, to indicate global limitation on <code>StoragePool</code>.
     *
     * @param quota
     *            - The global quota.
     * @return - Global quota Parameter Map.
     */
    private MapSqlParameterSource getFullQuotaParameterMap(Quota quota) {
        MapSqlParameterSource quotaParameterMap =
                getCustomMapSqlParameterSource()
                        .addValue("id", quota.getId())
                        .addValue("quota_id", quota.getId())
                        .addValue("vds_group_id", null)
                        .addValue("storage_id", null)
                        .addValue("storage_size_gb",
                                quota.getGlobalQuotaStorage() != null ? quota.getGlobalQuotaStorage()
                                        .getStorageSizeGB() : null)
                        .addValue("virtual_cpu",
                                quota.getGlobalQuotaVdsGroup() != null ? quota.getGlobalQuotaVdsGroup().getVirtualCpu()
                                        : null)
                        .addValue("mem_size_mb",
                                quota.getGlobalQuotaVdsGroup() != null ? quota.getGlobalQuotaVdsGroup().getMemSizeMB()
                                        : null);
        return quotaParameterMap;
    }

    private MapSqlParameterSource createQuotaMetaDataParameterMapper(Quota quota) {
        return createQuotaIdParameterMapper(quota.getId()).addValue("storage_pool_id", quota.getStoragePoolId())
                .addValue("quota_name", quota.getQuotaName())
                .addValue("description", quota.getDescription())
                .addValue("threshold_vds_group_percentage", quota.getThresholdVdsGroupPercentage())
                .addValue("threshold_storage_percentage", quota.getThresholdStoragePercentage())
                .addValue("grace_vds_group_percentage", quota.getGraceVdsGroupPercentage())
                .addValue("grace_storage_percentage", quota.getGraceStoragePercentage())
                .addValue("is_default_quota", quota.getIsDefaultQuota());
    }

    private void saveGlobalQuota(Quota quota) {
        getCallsHandler().executeModification("InsertQuota", createQuotaMetaDataParameterMapper(quota));
        getCallsHandler().executeModification("InsertQuotaLimitation", getFullQuotaParameterMap(quota));
    }

    private void saveVdsGroupSpecificQuotas(Quota quota) {
        // Add quota specific vds group limitations.
        for (QuotaVdsGroup quotaVdsGroup : quota.getQuotaVdsGroups()) {
            getCallsHandler().executeModification("InsertQuotaLimitation",
                    getQuotaVdsGroupParameterMap(quota.getId(), quotaVdsGroup));
        }
    }

    private void saveStorageSpecificQuotas(Quota quota) {
        // Add quota specific storage domains limitations.
        for (QuotaStorage quotaStorage : quota.getQuotaStorages()) {
            getCallsHandler().executeModification("InsertQuotaLimitation",
                    getQuotaStorageParameterMap(quota.getId(), quotaStorage));
        }
    }

    @Override
    public List<Quota> getAllWithQuery(String query) {
        return new SimpleJdbcTemplate(jdbcTemplate).query(query, getQuotaFromResultSet());
    }
}
