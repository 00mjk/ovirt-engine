package org.ovirt.engine.core.bll.quota;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.QuotaStorage;
import org.ovirt.engine.core.common.businessentities.QuotaVdsGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.QuotaDAO;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class QuotaManager {
    private static final QuotaManager INSTANCE = new QuotaManager();
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Log log = LogFactory.getLog(QuotaManager.class);
    private static final HashMap<Guid, Map<Guid, Quota>> storagePoolQuotaMap = new HashMap<Guid, Map<Guid, Quota>>();

    private static final QuotaManagerAuditLogger quotaManagerAuditLogger = new QuotaManagerAuditLogger();

    public static QuotaManager getInstance() {
        return INSTANCE;
    }

    protected QuotaManagerAuditLogger getQuotaManagerAuditLogger() {
        return quotaManagerAuditLogger;
    }

    /**
     * This method is protected for testing use only
     */
    protected QuotaDAO getQuotaDAO() {
        return DbFacade.getInstance().getQuotaDao();
    }

    public void rollbackQuota(Guid storagePoolId, List<Guid> quotaList) {
        lock.writeLock().lock();
        try {
            if (!storagePoolQuotaMap.containsKey(storagePoolId)) {
                return;
            }
            synchronized (storagePoolQuotaMap.get(storagePoolId)) {
                Map<Guid, Quota> map = storagePoolQuotaMap.get(storagePoolId);
                for (Guid quotaId : quotaList) {
                    map.remove(quotaId);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean validateAndSetStorageQuotaHelper(QuotaConsumptionParametersWrapper parameters,
            Pair<AuditLogType, AuditLogableBase> auditLogPair) {
        Map<Guid, Quota> quotaMap = storagePoolQuotaMap.get(parameters.getStoragePoolId());
        Map<Guid, Map<Guid, Double>> desiredStorageSizeQuotaMap = new HashMap<Guid, Map<Guid, Double>>();

        Map<Guid, Double> newUsedGlobalStorageSize = new HashMap<Guid, Double>();
        Map<Guid, Map<Guid, Double>> newUsedSpecificStorageSize = new HashMap<Guid, Map<Guid, Double>>();

        generateDesiredStorageSizeQuotaMap(parameters, desiredStorageSizeQuotaMap);

        for (Guid quotaId : desiredStorageSizeQuotaMap.keySet()) {
            Quota quota = quotaMap.get(quotaId);
            if (quota.getGlobalQuotaStorage() != null) {
                if (!checkConsumptionForGlobalStorageQuota(parameters,
                        desiredStorageSizeQuotaMap,
                        newUsedGlobalStorageSize,
                        quotaId,
                        quota,
                        auditLogPair))
                    return false;
            } else {
                if (!checkConsumptionForSpecificStorageQuota(parameters,
                        desiredStorageSizeQuotaMap,
                        newUsedSpecificStorageSize,
                        quotaId,
                        quota,
                        auditLogPair))
                    return false;
            }
        }
        saveNewConsumptionValues(quotaMap, newUsedGlobalStorageSize, newUsedSpecificStorageSize);
        return true;
    }

    private boolean checkConsumptionForSpecificStorageQuota(QuotaConsumptionParametersWrapper parameters,
            Map<Guid, Map<Guid, Double>> desiredStorageSizeQuotaMap,
            Map<Guid, Map<Guid, Double>> newUsedSpecificStorageSize,
            Guid quotaId,
            Quota quota,
            Pair<AuditLogType, AuditLogableBase> auditLogPair) {
        newUsedSpecificStorageSize.put(quotaId, new HashMap<Guid, Double>());
        for (Guid storageId : desiredStorageSizeQuotaMap.get(quotaId).keySet()) {
            boolean hasStorageId = false;
            for (QuotaStorage quotaStorage : quota.getQuotaStorages()) {
                if (quotaStorage.getStorageId().equals(storageId)) {
                    hasStorageId = true;
                    if (!QuotaStorage.UNLIMITED.equals(quotaStorage.getStorageSizeGB())) {
                        double storageUsagePercentage = quotaStorage.getStorageSizeGBUsage()
                                / quotaStorage.getStorageSizeGB() * 100;
                        double storageRequestPercentage =
                                desiredStorageSizeQuotaMap.get(quotaId)
                                        .get(storageId)
                                        / quotaStorage.getStorageSizeGB() * 100;

                        if (!checkQuotaStorageLimits(parameters.getAuditLogable().getStoragePool().getQuotaEnforcementType(),
                                quota,
                                quotaStorage.getStorageSizeGB(),
                                storageUsagePercentage, storageRequestPercentage,
                                parameters.getCanDoActionMessages(),
                                auditLogPair)) {
                            return false;
                        }
                        newUsedSpecificStorageSize.get(quotaId).put(storageId,
                                quotaStorage.getStorageSizeGBUsage()
                                        + desiredStorageSizeQuotaMap.get(quotaId).get(storageId));
                    }
                }
            }
            if (!hasStorageId) {
                parameters.getCanDoActionMessages()
                        .add(VdcBllMessages.ACTION_TYPE_FAILED_NO_QUOTA_SET_FOR_DOMAIN.toString());
                return false;
            }
        }
        return true;
    }

    private boolean checkConsumptionForGlobalStorageQuota(QuotaConsumptionParametersWrapper parameters,
            Map<Guid, Map<Guid, Double>> desiredStorageSizeQuotaMap,
            Map<Guid, Double> newUsedGlobalStorageSize,
            Guid quotaId,
            Quota quota,
            Pair<AuditLogType, AuditLogableBase> auditLogPair) {
        if (!QuotaStorage.UNLIMITED.equals(quota.getGlobalQuotaStorage().getStorageSizeGB())) {
            double sum = 0.0;
            for (Double size : desiredStorageSizeQuotaMap.get(quotaId).values()) {
                sum += size;
            }

            double storageUsagePercentage = quota.getGlobalQuotaStorage().getStorageSizeGBUsage()
                    / quota.getGlobalQuotaStorage().getStorageSizeGB() * 100;
            double storageRequestPercentage = sum
                    / quota.getGlobalQuotaStorage().getStorageSizeGB() * 100;

            if (!checkQuotaStorageLimits(parameters.getAuditLogable().getStoragePool().getQuotaEnforcementType(),
                    quota,
                    quota.getGlobalQuotaStorage().getStorageSizeGB(),
                    storageUsagePercentage, storageRequestPercentage,
                    parameters.getCanDoActionMessages(),
                    auditLogPair)) {
                return false;
            }
            newUsedGlobalStorageSize.put(quotaId, sum
                    + quota.getGlobalQuotaStorage().getStorageSizeGBUsage());
        }
        return true;
    }

    private void saveNewConsumptionValues(Map<Guid, Quota> quotaMap,
            Map<Guid, Double> newUsedGlobalStorageSize,
            Map<Guid, Map<Guid, Double>> newUsedSpecificStorageSize) {
        // cache new storage size.
        for (Guid quotaId : newUsedGlobalStorageSize.keySet()) {
            Quota quota = quotaMap.get(quotaId);
            double value = newUsedGlobalStorageSize.get(quotaId);
            if (value < 0) {
                log.errorFormat("Quota id {0} cached storage size is negative, removing from cache", quotaId);
                quotaMap.remove(quotaId);
                continue;
            }
            quota.getGlobalQuotaStorage().setStorageSizeGBUsage(value);
        }
        for (Guid quotaId : newUsedSpecificStorageSize.keySet()) {
            Quota quota = quotaMap.get(quotaId);
            for (QuotaStorage quotaStorage : quota.getQuotaStorages()) {
                if (newUsedSpecificStorageSize.get(quotaId).containsKey(quotaStorage.getStorageId())) {
                    double value = newUsedSpecificStorageSize.get(quotaId)
                            .get(quotaStorage.getStorageId());
                    if (value < 0) {
                        log.errorFormat("Quota id {0} cached storage size is negative, removing from cache",
                                quotaId);
                        quotaMap.remove(quotaId);
                        continue;
                    }
                    quotaStorage.setStorageSizeGBUsage(value);
                }
            }
        }
    }

    private void generateDesiredStorageSizeQuotaMap(QuotaConsumptionParametersWrapper parameters,
            Map<Guid, Map<Guid, Double>> desiredStorageSizeQuotaMap) {

        for (QuotaConsumptionParameter param : parameters.getParameters()) {
            QuotaStorageConsumptionParameter storageConsumptionParameter;
            if (param.getParameterType() != QuotaConsumptionParameter.ParameterType.STORAGE) {
                continue;
            } else {
                storageConsumptionParameter = (QuotaStorageConsumptionParameter)param;
            }
            if (!desiredStorageSizeQuotaMap.containsKey(param.getQuotaGuid())) {
                desiredStorageSizeQuotaMap.put(param.getQuotaGuid(), new HashMap<Guid, Double>());
            }
            Map<Guid, Double> quotaStorageMap = desiredStorageSizeQuotaMap.get(param.getQuotaGuid());
            if (!quotaStorageMap.containsKey(storageConsumptionParameter.getStorageDomainId())) {
                quotaStorageMap.put(storageConsumptionParameter.getStorageDomainId(), 0.0);
            }

            double requestedStorage =
                    storageConsumptionParameter.getQuotaAction() == QuotaConsumptionParameter.QuotaAction.CONSUME ?
                            storageConsumptionParameter.getRequestedStorageGB() :
                            -storageConsumptionParameter.getRequestedStorageGB();
            double currentValue = quotaStorageMap.get(storageConsumptionParameter.getStorageDomainId());

            quotaStorageMap.put(storageConsumptionParameter.getStorageDomainId(),currentValue + requestedStorage);
        }
    }

    private boolean checkQuotaStorageLimits(QuotaEnforcementTypeEnum quotaEnforcementTypeEnum,
            Quota quota,
            double limit,
            double storageUsagePercentage,
            double storageRequestPercentage,
            List<String> canDoActionMessages,
            Pair<AuditLogType, AuditLogableBase> log) {
        double storageTotalPercentage = storageUsagePercentage + storageRequestPercentage;

        boolean requestIsApproved;
        if (limit == QuotaStorage.UNLIMITED
                || storageTotalPercentage <= quota.getThresholdStoragePercentage()
                || storageRequestPercentage <= 0) {
            requestIsApproved = true;
        } else if (storageTotalPercentage <= 100) {
            log.setFirst(AuditLogType.USER_EXCEEDED_QUOTA_STORAGE_THRESHOLD);
            quotaManagerAuditLogger.addCustomValuesStorage(log.getSecond(),
                    quota.getQuotaName(),
                    storageUsagePercentage + storageRequestPercentage,
                    storageRequestPercentage);
            requestIsApproved = true;
        } else if (storageTotalPercentage <= quota.getGraceStoragePercentage() + 100) {
            log.setFirst(AuditLogType.USER_EXCEEDED_QUOTA_STORAGE_LIMIT);
            quotaManagerAuditLogger.addCustomValuesStorage(log.getSecond(),
                    quota.getQuotaName(),
                    storageUsagePercentage + storageRequestPercentage,
                    storageRequestPercentage);
            requestIsApproved = true;
        } else {
            log.setFirst(AuditLogType.USER_EXCEEDED_QUOTA_STORAGE_GRACE_LIMIT);
            quotaManagerAuditLogger.addCustomValuesStorage(log.getSecond(),
                    quota.getQuotaName(),
                    storageUsagePercentage,
                    storageRequestPercentage);
            if (QuotaEnforcementTypeEnum.HARD_ENFORCEMENT == quotaEnforcementTypeEnum) {
                canDoActionMessages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_STORAGE_LIMIT_EXCEEDED.toString());
                requestIsApproved = false;
            } else {
                requestIsApproved = true;
            }
        }
        return requestIsApproved;
    }

    private boolean checkQuotaClusterLimits(QuotaEnforcementTypeEnum quotaEnforcementTypeEnum,
            Quota quota,
            QuotaVdsGroup quotaVdsGroup,
            long memToAdd,
            int vcpuToAdd,
            List<String> canDoActionMessages,
            Pair<AuditLogType, AuditLogableBase> auditLogPair) {
        if (quotaVdsGroup.getVirtualCpu() == 0 || quotaVdsGroup.getMemSizeMB() == 0) {
            return false;
        }

        double vcpuToAddPercentage = (double) vcpuToAdd / (double) quotaVdsGroup.getVirtualCpu() * 100;
        double vcpuCurrentPercentage =
                (double) quotaVdsGroup.getVirtualCpuUsage() / (double) quotaVdsGroup.getVirtualCpu() * 100;
        double newVcpuPercent = vcpuToAddPercentage + vcpuCurrentPercentage;
        double memToAddPercentage = (double) memToAdd / (double) quotaVdsGroup.getMemSizeMB() * 100;
        double memCurrentPercentage =
                (double) quotaVdsGroup.getMemSizeMBUsage() / (double) quotaVdsGroup.getMemSizeMB() * 100;
        double newMemoryPercent = memToAddPercentage + memCurrentPercentage;
        long newMemory = memToAdd + quotaVdsGroup.getMemSizeMBUsage();
        int newVcpu = vcpuToAdd + quotaVdsGroup.getVirtualCpuUsage();

        long memLimit = quotaVdsGroup.getMemSizeMB();
        int cpuLimit = quotaVdsGroup.getVirtualCpu();
        boolean requestIsApproved;
        if (memLimit == QuotaVdsGroup.UNLIMITED_MEM && cpuLimit == QuotaVdsGroup.UNLIMITED_VCPU) { // if both cpu and                                                                                                   // mem are unlimited
            // cache
            cacheNewValues(quotaVdsGroup, newMemory, newVcpu);
            requestIsApproved = true;
        } else if ((newVcpuPercent <= quota.getThresholdVdsGroupPercentage() // if cpu and mem usages are under the limit
                && newMemoryPercent <= quota.getThresholdVdsGroupPercentage())
                || (vcpuToAdd <= 0 && memToAdd <= 0)) {
            // cache
            cacheNewValues(quotaVdsGroup, newMemory, newVcpu);
            requestIsApproved = true;
        } else if (newVcpuPercent <= 100
                && newMemoryPercent <= 100) { // passed the threshold (not the quota limit)
            auditLogPair.setFirst(AuditLogType.USER_EXCEEDED_QUOTA_VDS_GROUP_THRESHOLD);
            quotaManagerAuditLogger.addCustomValuesVdsGroup(auditLogPair.getSecond(),
                    quota.getQuotaName(),
                    vcpuCurrentPercentage + vcpuToAddPercentage,
                    vcpuToAddPercentage,
                    memCurrentPercentage + memToAddPercentage,
                    memToAddPercentage,
                    newVcpuPercent > quota.getThresholdVdsGroupPercentage(),
                    newMemoryPercent > quota.getThresholdVdsGroupPercentage());
            requestIsApproved = true;
        } else if (newVcpuPercent <= quota.getGraceVdsGroupPercentage() + 100
                && newMemoryPercent <= quota.getGraceVdsGroupPercentage() + 100) { // passed the quota limit (not the
            // grace)
            auditLogPair.setFirst(AuditLogType.USER_EXCEEDED_QUOTA_VDS_GROUP_LIMIT);
            quotaManagerAuditLogger.addCustomValuesVdsGroup(auditLogPair.getSecond(),
                    quota.getQuotaName(),
                    vcpuCurrentPercentage + vcpuToAddPercentage,
                    vcpuToAddPercentage,
                    memCurrentPercentage + memToAddPercentage,
                    memToAddPercentage,
                    newVcpuPercent > 100,
                    newMemoryPercent > 100);
            requestIsApproved = true;
        } else {
            auditLogPair.setFirst(AuditLogType.USER_EXCEEDED_QUOTA_VDS_GROUP_GRACE_LIMIT); // passed the grace
            quotaManagerAuditLogger.addCustomValuesVdsGroup(auditLogPair.getSecond(),
                    quota.getQuotaName(),
                    vcpuCurrentPercentage,
                    vcpuToAddPercentage,
                    memCurrentPercentage,
                    memToAddPercentage,
                    newVcpuPercent > quota.getGraceVdsGroupPercentage() + 100,
                    newMemoryPercent > quota.getGraceVdsGroupPercentage() + 100);
            if (QuotaEnforcementTypeEnum.HARD_ENFORCEMENT == quotaEnforcementTypeEnum) {
                canDoActionMessages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_VDS_GROUP_LIMIT_EXCEEDED.toString());
                requestIsApproved = false;
            } else {
                requestIsApproved = true;
            }
        }
        // cache
        cacheNewValues(quotaVdsGroup, newMemory, newVcpu);
        return requestIsApproved;
    }

    private void cacheNewValues(QuotaVdsGroup quotaVdsGroup, long newMemory, int newVcpu) {
        quotaVdsGroup.setVirtualCpuUsage(newVcpu);
        quotaVdsGroup.setMemSizeMBUsage(newMemory);
    }

    private boolean validateAndSetClusterQuota(QuotaConsumptionParametersWrapper parameters,
            Pair<AuditLogType, AuditLogableBase> auditLogPair) {
        boolean result = true;
        for (QuotaConsumptionParameter parameter : parameters.getParameters()) {
            QuotaVdsGroupConsumptionParameter vdsGroupConsumptionParameter;
            if (parameter.getParameterType() != QuotaConsumptionParameter.ParameterType.VDS_GROUP) {
                continue;
            } else {
                vdsGroupConsumptionParameter = (QuotaVdsGroupConsumptionParameter) parameter;
            }
            Quota quota = parameter.getQuota();
            QuotaVdsGroup quotaVdsGroup = null;

            if (quota.getGlobalQuotaVdsGroup() != null) { // global cluster quota
                quotaVdsGroup = quota.getGlobalQuotaVdsGroup();
            } else {
                for (QuotaVdsGroup vdsGroup : quota.getQuotaVdsGroups()) {
                    if (vdsGroup.getVdsGroupId().equals(vdsGroupConsumptionParameter.getVdsGroupId())) {
                        quotaVdsGroup = vdsGroup;
                        break;
                    }
                }
            }
            if (quotaVdsGroup == null) {
                parameters.getCanDoActionMessages()
                        .add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_IS_NOT_VALID.toString());
                result = false;
                break;
            }

            long requestedMemory =
                    vdsGroupConsumptionParameter.getQuotaAction() == QuotaConsumptionParameter.QuotaAction.CONSUME ?
                    vdsGroupConsumptionParameter.getRequestedMemory() : -vdsGroupConsumptionParameter.getRequestedMemory();
            int requestedCpu =
                    vdsGroupConsumptionParameter.getQuotaAction() == QuotaConsumptionParameter.QuotaAction.CONSUME ?
                    vdsGroupConsumptionParameter.getRequestedCpu() : -vdsGroupConsumptionParameter.getRequestedCpu();

            if (!checkQuotaClusterLimits(
                    parameters.getAuditLogable().getStoragePool().getQuotaEnforcementType(),
                    quota,
                    quotaVdsGroup,
                    requestedMemory,
                    requestedCpu,
                    parameters.getCanDoActionMessages(),
                    auditLogPair)) {
                result = false;
                break;
            }
        }
        return result;
    }

    public void removeQuotaFromCache(Guid storagePoolId, Guid quotaId) {
        lock.readLock().lock();
        try {
            if (!storagePoolQuotaMap.containsKey(storagePoolId)) {
                return;
            }
            synchronized (storagePoolQuotaMap.get(storagePoolId)) {
                storagePoolQuotaMap.get(storagePoolId).remove(quotaId);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void removeStoragePoolFromCache(Guid storagePoolId) {
        lock.writeLock().lock();
        try {
            storagePoolQuotaMap.remove(storagePoolId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Roll back quota by VM id. the VM is fetched from DB and the quota is rolled back
     * @param vmId - id for the vm
     */
    public void rollbackQuotaByVmId(Guid vmId) {
        VM vm = DbFacade.getInstance().getVmDao().get(vmId);
        if (vm != null) {
            rollbackQuota(vm.getstorage_pool_id(), Arrays.asList(vm.getQuotaId()));
        }
    }

    /**
     * Check if the quota exceeded the storage limit (ether for global limit or one of the specific limits).
     *
     * @param quotaId
     *            - quota id
     * @return - true if the quota exceeded the storage limitation. false if quota was not found, limit was not defined
     *         or limit not crossed.
     */
    public boolean isStorageQuotaExceeded(Guid quotaId) {
        if (quotaId == null) {
            return false;
        }

        Quota quota = getQuotaDAO().getById(quotaId);

        if (quota == null) {
            return false;
        }

        // for global quota
        if (quota.getGlobalQuotaStorage() != null) {
            if (quota.getGlobalQuotaStorage().getStorageSizeGB() != null
                    && !quota.getGlobalQuotaStorage().getStorageSizeGB().equals(QuotaStorage.UNLIMITED)
                    && quota.getGlobalQuotaStorage().getStorageSizeGB()
                    < quota.getGlobalQuotaStorage().getStorageSizeGBUsage()) {
                return true;
            }
        } else if (quota.getQuotaStorages() != null) { // for specific quota
            for (QuotaStorage quotaStorage : quota.getQuotaStorages()) {
                if (quotaStorage.getStorageSizeGB() < quotaStorage.getStorageSizeGBUsage()) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Consume from quota according to the parameters.
     *
     * @param parameters
     *            - Quota consumption parameters
     * @return - true if the request was validated and set
     */
    public boolean consume(QuotaConsumptionParametersWrapper parameters) throws InvalidQuotaParametersException {

        Pair<AuditLogType, AuditLogableBase> auditLogPair = new Pair<AuditLogType, AuditLogableBase>();
        auditLogPair.setSecond(parameters.getAuditLogable());

        storage_pool storagePool = parameters.getAuditLogable().getStoragePool();
        if (storagePool == null) {
            throw new InvalidQuotaParametersException("Null storage pool passed to QuotaManager");
        }

        lock.writeLock().lock();
        try {
            if (!storagePoolQuotaMap.containsKey(storagePool.getId())) {
                storagePoolQuotaMap.put(storagePool.getId(), new HashMap<Guid, Quota>());
            }
        } finally {
            lock.writeLock().unlock();
        }

        lock.readLock().lock();
        try {
            synchronized (storagePoolQuotaMap.get(storagePool.getId())) {
                return validateAndCompleteParameters(parameters, auditLogPair)
                        && (parameters.getStoragePool().getQuotaEnforcementType() == QuotaEnforcementTypeEnum.DISABLED
                        || internalConsumeAndReleaseHandler(parameters, auditLogPair));
            }
        } finally {
            lock.readLock().unlock();
            getQuotaManagerAuditLogger().auditLog(auditLogPair.getFirst(), auditLogPair.getSecond());
        }
    }

    /**
     * This is the start point for all quota consumption and release. This method is called after the parameters were
     * validated and competed, and the cache was updated to support all the requests in the parameters.
     *
     *
     * @param parameters
     *            - Quota consumption parameters
     * @param auditLogPair - auditLog pair
     * @return - true if the request was validated and set
     */
    private boolean internalConsumeAndReleaseHandler(QuotaConsumptionParametersWrapper parameters, Pair<AuditLogType,
            AuditLogableBase> auditLogPair) {
        boolean result = validateAndSetStorageQuotaHelper(parameters, auditLogPair);
        if (result) {
            result = validateAndSetClusterQuota(parameters, auditLogPair);
            if (result) {
                return true;
            } else {
                QuotaConsumptionParametersWrapper revertedParams = revertParametersQuantities(parameters);
                validateAndSetStorageQuotaHelper(revertedParams, auditLogPair);
            }
        }

        return result;
    }

    /**
     * Revert the quantities of the storage, cpu and mem So that a request for 5GB storage is reverted to (-5)GB request
     *
     * @param parameters
     *            the consumption properties. This object would not be mutated.
     * @return new QuotaConsumptionParameters object with reverted quantities,
     */
    private QuotaConsumptionParametersWrapper revertParametersQuantities(QuotaConsumptionParametersWrapper parameters) {
        QuotaConsumptionParametersWrapper revertedParams = null;
        try {
            revertedParams = parameters.clone();
            for (QuotaConsumptionParameter parameter : revertedParams.getParameters()) {
                parameter.setQuotaAction(QuotaConsumptionParameter.QuotaAction.CONSUME == parameter.getQuotaAction() ?
                        QuotaConsumptionParameter.QuotaAction.RELEASE : QuotaConsumptionParameter.QuotaAction.CONSUME);
            }
        } catch (CloneNotSupportedException ignored) {}

        return revertedParams;
    }

    /**
     * Validate parameters. Look for null pointers and missing data Complete the missing data in the parameters from DB
     * and cache all the needed entities.
     *
     * @param parameters
     *            - Quota consumption parameters
     */

    private boolean validateAndCompleteParameters(QuotaConsumptionParametersWrapper parameters,
            Pair<AuditLogType, AuditLogableBase> auditLogPair) throws InvalidQuotaParametersException {

        if (QuotaEnforcementTypeEnum.DISABLED == parameters.getAuditLogable().getStoragePool().getQuotaEnforcementType()) {
            return true;
        }

        boolean hardEnforcement =
                QuotaEnforcementTypeEnum.HARD_ENFORCEMENT == parameters.getAuditLogable().getStoragePool().getQuotaEnforcementType();

        // for each parameter - check and complete
        for (QuotaConsumptionParameter param : parameters.getParameters()) {
            // check that quota id is valid and fetch the quota from db (or cache). add the quota to the param
            boolean validQuotaId = checkAndFetchQuota(parameters, param, auditLogPair);
            boolean validVdsGroup = true;
            boolean  validStorageDomain = true;

            if (validQuotaId) {
                // In case this param is a QuotaVdsConsumptionParameter - check that it has a valid
                // vds group id which is handled by this quota
                if (param instanceof QuotaVdsGroupConsumptionParameter) {
                    validVdsGroup = checkVdsGroupMatchQuota(parameters, param);
                }

                // In case this param is a QuotaStorageConsumptionParameter - check that it has a valid
                // storage domain id which is handled by this quota
                if (param instanceof QuotaStorageConsumptionParameter) {
                    validStorageDomain = checkStoragePoolMatchQuota(parameters, param);
                }
            }

            if (!validQuotaId || !validVdsGroup || !validStorageDomain) {
                // if in hard enforcement - return false
                if (hardEnforcement) {
                    return false;
                } else {
                    // clear any messages written to the canDoActionMessages
                    parameters.getCanDoActionMessages().clear();
                    if (QuotaEnforcementTypeEnum.DISABLED == parameters.getAuditLogable().getStoragePool().getQuotaEnforcementType()) {
                        auditLogPair.setFirst(null);
                    }
                    return true;
                }
            }
        }
        return true;
    }

    // check that quota id is valid and fetch the quota from db (or cache). add the quota to the param
    private boolean checkAndFetchQuota(QuotaConsumptionParametersWrapper parameters, QuotaConsumptionParameter param,
            Pair<AuditLogType, AuditLogableBase> auditLogPair)
            throws InvalidQuotaParametersException {
        if(param.getQuotaGuid() == null || Guid.Empty.equals(param.getQuotaGuid())) {
            parameters.getCanDoActionMessages().add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_IS_NOT_VALID.toString());
            auditLogPair.setFirst(param.getParameterType() == QuotaConsumptionParameter.ParameterType.STORAGE ?
                    AuditLogType.MISSING_QUOTA_STORAGE_PARAMETERS_PERMISSIVE_MODE:
                    AuditLogType.MISSING_QUOTA_CLUSTER_PARAMETERS_PERMISSIVE_MODE);
            log.errorFormat("No Quota id passed from command: {0}",parameters.getAuditLogable().getClass().getName());
            return false;
        }

        Quota quota = fetchQuotaFromCache(param.getQuotaGuid(), parameters.getStoragePool().getId());
        if (quota == null) {
            parameters.getCanDoActionMessages().add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_IS_NO_LONGER_AVAILABLE_IN_SYSTEM.toString());
            auditLogPair.setFirst(param.getParameterType() == QuotaConsumptionParameter.ParameterType.STORAGE ?
                    AuditLogType.MISSING_QUOTA_STORAGE_PARAMETERS_PERMISSIVE_MODE:
                    AuditLogType.MISSING_QUOTA_CLUSTER_PARAMETERS_PERMISSIVE_MODE);
            log.errorFormat("The quota id {0} is not found in backend and DB.", param.getQuotaGuid().toString());
            return false;
        } else {
            param.setQuota(quota);
        }
        if (!quota.getStoragePoolId().equals(parameters.getStoragePoolId())) {
            throw new InvalidQuotaParametersException("The Quota storage pool id does not match the passed storage pool");
        }
        return true;
    }

    // In case this param is a QuotaVdsConsumptionParameter - check that it has a valid
    // vds group id which is handled by this quota
    private boolean checkVdsGroupMatchQuota(QuotaConsumptionParametersWrapper parameters, QuotaConsumptionParameter param) {
        Quota quota = param.getQuota();
        QuotaVdsGroupConsumptionParameter paramVds = (QuotaVdsGroupConsumptionParameter) param;

        if (paramVds.getVdsGroupId() == null) {
            parameters.getCanDoActionMessages().add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_IS_NOT_VALID.toString());
            log.errorFormat("Quota Vds parameters from command: {0} are missing vds group id",
                    parameters.getAuditLogable().getClass().getName());
            return false;
        }
        boolean vdsGroupInQuota = false;
        if(quota.getGlobalQuotaVdsGroup() != null) {
            vdsGroupInQuota = true;
        } else {
            for (QuotaVdsGroup vdsGroup : quota.getQuotaVdsGroups()) {
                if (vdsGroup.getVdsGroupId().equals(paramVds.getVdsGroupId())) {
                    vdsGroupInQuota = true;
                    break;
                }
            }
        }

        if (!vdsGroupInQuota) {
            parameters.getCanDoActionMessages().add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_IS_NOT_VALID.toString());
            log.errorFormat("Quota Vds parameters from command: {0}. Vds group does not match quota",
                    parameters.getAuditLogable().getClass().getName());
            return false;
        }
        return true;
    }

    // In case this param is a QuotaStorageConsumptionParameter - check that it has a valid
    // storage domain id which is handled by this quota
    private boolean checkStoragePoolMatchQuota(QuotaConsumptionParametersWrapper parameters, QuotaConsumptionParameter param) {
        Quota quota = param.getQuota();
        QuotaStorageConsumptionParameter paramStorage = (QuotaStorageConsumptionParameter) param;

        if (paramStorage.getStorageDomainId() == null) {
            parameters.getCanDoActionMessages().add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_IS_NOT_VALID.toString());
            log.errorFormat("Quota storage parameters from command: {0} are missing storage domain id",
                    parameters.getAuditLogable().getClass().getName());
            return false;
        }
        boolean storageDomainInQuota = false;
        if(quota.getGlobalQuotaStorage() != null) {
            storageDomainInQuota = true;
        } else {
            for (QuotaStorage quotaStorage : quota.getQuotaStorages()) {
                if (quotaStorage.getStorageId().equals(paramStorage.getStorageDomainId())) {
                    storageDomainInQuota = true;
                    break;
                }
            }
        }

        if (!storageDomainInQuota) {
            parameters.getCanDoActionMessages().add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_IS_NOT_VALID.toString());
            log.errorFormat("Quota storage parameters from command: {0}. Storage domain does not match quota",
                    parameters.getAuditLogable().getClass().getName());
            return false;
        }
        return true;
    }

    /**
     * Get Quota by Id. If in cache - get from cache. else get from DAO and add to cache.
     *
     * @param quotaId - quota id
     * @param storagePoolId - storage pool containing this quota
     * @return - found quota. null if not found.
     */
    private Quota fetchQuotaFromCache(Guid quotaId, Guid storagePoolId) throws InvalidQuotaParametersException {
        Quota quota;
        Map<Guid, Quota> quotaMap = storagePoolQuotaMap.get(storagePoolId);

        quota = quotaMap.get(quotaId);
        // if quota was not found in cache - look for it in DB
        if (quota == null) {
            quota = getQuotaDAO().getById(quotaId);
            if (quota != null) {
                // cache in quota map
                if (storagePoolId.equals(quota.getStoragePoolId())) {
                    quotaMap.put(quotaId, quota);
                } else {
                    throw new InvalidQuotaParametersException(
                            String.format("Quota %s does not match storage pool %s",quotaId.toString()
                                    , storagePoolId.toString()));
                }
            }
        }
        return quota;
    }
}
