package org.ovirt.engine.core.common.config;

import java.util.Map;

import org.ovirt.engine.core.common.EngineWorkingMode;

public enum ConfigValues {
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("9b9002d1-ec33-4083-8a7b-31f6b8931648")
    AdUserId,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("Postgres")
    DBEngine,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("administrator")
    AdUserName,
    @TypeConverterAttribute(Map.class)
    @DefaultValueAttribute("EXAMPLE.COM:123456")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.DomainsPasswordMap)
    AdUserPassword,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("123456")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.Password)
    LocalAdminPassword,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("EXAMPLE.COM")
    DomainName,

    /**
     * Timeout in seconds for the completion of calls to VDSM. It should
     * be quite large as some host operations can take more than 3
     * minutes to complete.
     */
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("180")
    vdsTimeout,

    /**
     * The number of times to retry host operations when there are IO errors.
     */
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("0")
    vdsRetries,

    /**
     * Timeout for establishment of connections with hosts. This should be quite
     * small, a few secones at most, as it the TCP handshake with hosts should
     * be very quick in most networks.
     */
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("2")
    vdsConnectionTimeout,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("2")
    VdsRefreshRate,
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    AsyncTaskPollingRate,

    /**
     * The rate (in seconds) to refresh the cache that holds the asynchronous tasks' statuses.
     */
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("60")
    AsyncTaskStatusCacheRefreshRateInSeconds,

    /**
     * The period of time (in minutes) to hold the asynchronous tasks' statuses in the asynchronous tasks cache.
     */
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("5")
    AsyncTaskStatusCachingTimeInMinutes,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3000")
    AsyncTaskZombieTaskLifeInMinutes,
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3600")
    UserRefreshRate,
    @TypeConverterAttribute(java.util.Date.class)
    @DefaultValueAttribute("03:35:35")
    AuditLogCleanupTime,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    AuditLogAgingThreshold,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("drac5,ilo,ipmilan,rsa,bladecenter,alom,apc,eps,wti,rsb")
    VdsFenceType,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("alom:secure=secure,port=ipport;apc:secure=secure,port=ipport,slot=port;bladecenter:secure=secure,port=ipport,slot=port;drac5:secure=secure,port=ipport;eps:slot=port;ilo:secure=ssl,port=ipport;ipmilan:;rsa:secure=secure,port=ipport;rsb:;wti:secure=secure,port=ipport,slot=port")
    VdsFenceOptionMapping,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("secure=bool,port=int,slot=int")
    VdsFenceOptionTypes,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    FenceStopStatusRetries,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("60")
    FenceStopStatusDelayBetweenRetriesInSec,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("180")
    FenceQuietTimeBetweenOperationsInSec,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("cluster,dc")
    FenceProxyDefaultPreferences,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("/data/updates/ovirt-node-image.iso")
    oVirtUploadPath,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("/usr/share/rhev-hypervisor")
    oVirtISOsRepositoryPath,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("/usr/share/vdsm-reg/vdsm-upgrade")
    oVirtUpgradeScriptName,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("5")
    VdsCertificateValidityInYears,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("USERID")
    UserId,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("PASSW0RD")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.Password)
    Password,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("100")
    SearchResultsLimit,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("2")
    VDSAttemptsToResetCount,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    TimeoutToResetVdsInSeconds,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ProductKey2003,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ProductKey2003x64,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ProductKey2008,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ProductKey2008x64,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ProductKey2008R2,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ProductKeyWindow7,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ProductKeyWindow7x64,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ProductKey,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    FreeSpaceLow,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("5")
    FreeSpaceCriticalLowInGB,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    MacPoolRanges,
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("false")
    HasCluster,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("1.0.0.0")
    VdcVersion,
    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    SSLEnabled,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("DEFAULT")
    CipherSuite,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("40")
    StoragePoolNameSizeLimit,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("50")
    StorageDomainNameSizeLimit,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    NumberOfFailedRunsOnVds,
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    TimeToReduceFailedRunOnVdsInMinutes,
    /**
     * In default rerun Vm on all Available desktops
     */
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    MaxRerunVmOnVdsCount,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    SysPrepXPPath,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    SysPrep2K3Path,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    SysPrep2K8Path,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    SysPrep2K8x64Path,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    SysPrep2K8R2Path,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    SysPrepWindows7Path,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    SysPrepWindows7x64Path,
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1000")
    MaxVmsInPool,
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("5")
    VmPoolLeaseDays,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("12:00")
    VmPoolLeaseStartTime,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("12:00")
    VmPoolLeaseEndTime,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("LDAP")
    AuthenticationMethod,
    @Reloadable
    @TypeConverterAttribute(java.util.List.class)
    @DefaultValueAttribute("1,2,4")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.CommaSeparatedStringArray)
    ValidNumOfMonitors,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("16")
    MaxNumOfVmCpus,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("16")
    MaxNumOfVmSockets,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("16")
    MaxNumOfCpuPerSocket,
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1")
    NumberVmRefreshesBeforeSave,
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("60")
    AutoRepoDomainRefreshTime,
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    InstallVds,
    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    EnableUSBAsDefault,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("300")
    SSHInactivityTimoutSeconds,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("120")
    ServerRebootTimeout,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("40")
    VmGracefulShutdownTimeout,
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("100")
    VmPriorityMaxValue,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("Shutting Down")
    VmGracefulShutdownMessage,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("smain,sinputs,scursor,splayback,srecord,sdisplay,ssmartcard,susbredir")
    SpiceSecureChannels,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("shift+f12")
    SpiceReleaseCursorKeys,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("shift+f11")
    SpiceToggleFullScreenKeys,
    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    SpiceUsbAutoShare,
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    EncryptHostCommunication,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("oVirt")
    OrganizationName,
    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    IsMultilevelAdministrationOn,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    VdsRecoveryTimeoutInMintues,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("8192")
    MaxBlockDiskSize,
    // the order is- {level}:{name}:{flags}:{vdsm};
    // {level}:{name}:{flags}:{vdsm};1:cpu_name:cpu_flags,..,:vdsm_exec,+..,-..;..
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("1:pentium3:vmx:pentium3;2:intel-qemu64-nx:vmx,sse2:qemu64,-nx,+sse2;3:intel-qemu64:vmx,sse2,nx:qemu64,+sse2;2:amd-qemu64-nx:svm,sse2:qemu64,-nx,+sse2;3:amd-qemu64:svm,sse2,nx:qemu64,+sse2")
    ServerCPUList,
    @Reloadable
    @TypeConverterAttribute(java.util.List.class)
    @DefaultValueAttribute("ovirt-guest-agent-common,ovirt-guest-agent")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.CommaSeparatedStringArray)
    AgentAppName,
    @Reloadable
    @TypeConverterAttribute(Map.class)
    @DefaultValueAttribute("{\"windows\":\"RHEV-Spice\",\"linux\":\"xorg-x11-drv-qxl\"}")
    SpiceDriverNameInGuest,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("RHEV-toolsSetup_")
    GuestToolsSetupIsoPrefix,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    VcpuConsumptionPercentage,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("/images/import/")
    ImportDefaultPath,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("None")
    VdsSelectionAlgorithm,
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    EnableVdsLoadBalancing,
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1")
    VdsLoadBalancingeIntervalInMinutes,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("0")
    LowUtilizationForEvenlyDistribute,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("6")
    LowUtilizationForPowerSave,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    HighUtilizationForEvenlyDistribute,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    HighUtilizationForPowerSave,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("80")
    UtilizationThresholdInPercent,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("2")
    CpuOverCommitDurationMinutes,
    // a default of 120% memory over commit.
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("120")
    MaxVdsMemOverCommit,
    // a default of 120% memory over commit.
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("120")
    MaxVdsMemOverCommitForServers,
    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    AutoInstallCertificateOnApprove,
    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    DebugTimerLogging,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    AutoApprovePatterns,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("99408929-82CF-4DC7-A532-9D998063FA95")
    AutoRegistrationDefaultVdsGroupID,
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    StoragePoolRefreshTimeInSeconds,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    StoragePoolNonOperationalResetTimeoutInMin,
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1")
    StorageDomainFalureTimeoutInMinutes,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ComputerADPaths,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    SPMFailOverAttempts,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("ON")
    LockPolicy,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("5")
    LockRenewalIntervalSec,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    LeaseTimeSec,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    IoOpTimeoutSec,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    LeaseRetries,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("en-us")
    VncKeyboardLayout,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    SpmCommandFailOverRetries,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1")
    SpmVCpuConsumption,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    RedirectServletReportsPage,
    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    EnableSpiceRootCertificateValidation,
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("100000")
    MaxMacsCountInPool,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    NumberOfVmsForTopSizeVms,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("256")
    VMMinMemorySizeInMB,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("20480")
    VM32BitMaxMemorySizeInMB,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("268435456")
    VM64BitMaxMemorySizeInMB,
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("0")
    BlockMigrationOnSwapUsagePercentage,
    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    EnableSwapCheck,
    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    SendSMPOnRunVm,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute(" WHERE RowNum BETWEEN %1$s AND %2$s")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.ValueDependent, dependentOn = ConfigValues.DBEngine,
    realValue = "PagingSyntax")
    DBPagingSyntax,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("Range")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.ValueDependent, dependentOn = ConfigValues.DBEngine,
    realValue = "PagingType")
    DBPagingType,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("SELECT * FROM (SELECT *, ROW_NUMBER() OVER(%1$s) as RowNum FROM (%2$s)) as T1 ) as T2 %3$s")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.ValueDependent, dependentOn = ConfigValues.DBEngine,
    realValue = "SearchTemplate")
    DBSearchTemplate,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute(" OFFSET {0} LIMIT {1}")
    PostgresPagingSyntax,      // used by behaviour DBPagingSyntax
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("Offset")
    PostgresPagingType,        // used by behaviour DBPagingType
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("SELECT * FROM ( {1}) as T1 {2}")
    PostgresSearchTemplate,    // used by behaviour DBSearchTemplate
    @Reloadable
    @TypeConverterAttribute(java.util.HashSet.class)
    @DefaultValueAttribute("4.4,4.5")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.CommaSeparatedVersionArray)
    SupportedVDSMVersions,
    @TypeConverterAttribute(java.util.HashSet.class)
    @DefaultValueAttribute("2.2,3.0")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.CommaSeparatedVersionArray)
    SupportedClusterLevels,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("ENGINE")
    OvfVirtualSystemType,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("60")
    WaitForVdsInitInSec,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("60")
    OvfUpdateIntervalInMinutes,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("100")
    OvfItemsCountPerUpdate,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("GMT Standard Time")
    DefaultWindowsTimeZone,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("Etc/GMT")
    DefaultGeneralTimeZone,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("389")
    LDAPServerPort,

    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("false")
    SANWipeAfterDelete,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("/etc/ovirt-engine")
    ConfigDir,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("/usr/share/ovirt-engine")
    DataDir,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    UserSessionTimeOutInterval,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    UserSessionTimeOutInvalidationInterval,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("/data/images/rhev")
    RhevhLocalFSPath,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("default:GSSAPI")
    LDAPSecurityAuthentication,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    UserDefinedVMProperties,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    PredefinedVMProperties,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("250")
    MaxNumberOfHostsInStoragePool,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("15")
    MaxVmNameLengthWindows,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("64")
    MaxVmNameLengthNonWindows,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("255")
    MaxVdsNameLength,

    @Reloadable
    @TypeConverterAttribute(Double.class)
    @DefaultValueAttribute("30")
    MaxStorageVdsTimeoutCheckSec,

    @Reloadable
    @TypeConverterAttribute(Double.class)
    @DefaultValueAttribute("5")
    MaxStorageVdsDelayCheckSec,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("300")
    DisableFenceAtStartupInSec,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("60")
    NicDHCPDelayGraceInMS,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    FindFenceProxyRetries,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    FindFenceProxyDelayBetweenRetriesInSec,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1024")
    LogPhysicalMemoryThresholdInMB,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("auth-conf")
    SASL_QOP,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1000")
    LdapQueryPageSize,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("100")
    MaxLDAPQueryPartsNumber,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    FenceStartStatusRetries,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("60")
    FenceStartStatusDelayBetweenRetriesInSec,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    LDAPQueryTimeout,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("0")
    SupportedStorageFormats,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("ILIKE")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.ValueDependent, dependentOn = ConfigValues.DBEngine,
    realValue = "LikeSyntax")
    DBLikeSyntax,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("ILIKE")
    PostgresLikeSyntax,    // used by behaviour DBLikeSyntax

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.ValueDependent, dependentOn = ConfigValues.DBEngine,
    realValue = "I18NPrefix")
    DBI18NPrefix,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    PostgresI18NPrefix,    // used by behaviour DBI18NPrefix

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("60000")
    UknownTaskPrePollingLapse,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    LdapServers,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("3.0.0.0")
    ProductRPMVersion,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10000")
    MaxAuditLogMessageLength,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    SysPrepDefaultUser,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.Password)
    @DefaultValueAttribute("")
    SysPrepDefaultPassword,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("ilo3=ipmilan")
    FenceAgentMapping,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("ilo3:lanplus,power_wait=4")
    FenceAgentDefaultParams,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("admin")
    AdminUser,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("internal")
    AdminDomain,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.Password)
    AdminPassword,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    SignCertTimeoutInSeconds,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("600")
    OtpExpirationInSeconds,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("20")
    ConnectToServerTimeoutInSeconds,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    IPTablesConfig,

    /**
     * Lower threshold for disk space on host to be considered low, in MB.
     */
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1000")
    VdsLocalDisksLowFreeSpace,

    /**
     * Lower threshold for disk space on host to be considered critically low (almost out of space), in MB.
     */
    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("500")
    VdsLocalDisksCriticallyLowFreeSpace,

    /**
     * The minimal size of the internal thread pool. Minimal number of threads in pool
     */
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("50")
    DefaultMinThreadPoolSize,

    /**
     * The size of the internal thread pool
     */
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("500")
    DefaultMaxThreadPoolSize,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("100")
    DefaultMaxThreadWaitQueueSize,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1")
    InitStorageSparseSizeInGB,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("ovirtmgmt")
    ManagementNetwork,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("5.8")
    OvirtInitialSupportedIsoVersion,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("rhevh")
    OvirtIsoPrefix,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("80")
    QuotaThresholdVdsGroup,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("80")
    QuotaThresholdStorage,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("20")
    QuotaGraceVdsGroup,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("20")
    QuotaGraceStorage,

    // This value indicates devices that although are given to us by VDSM
    // are still treated as managed devices
    // This should be a [device=<device> type=<type>[,]]* string
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ManagedDevicesWhiteList,

    /**
     * The origin type to be used for VM and VM template creation
     */
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("OVIRT")
    OriginType,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    HotPlugEnabled,

    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("false")
    AllowDuplicateMacAddresses,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    SetupNetworksPollingTimeout,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    JobCleanupRateInMinutes,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    SucceededJobCleanupTimeInMinutes,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("60")
    FailedJobCleanupTimeInMinutes,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("100")
    JobPageSize,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("5")
    VmPoolMonitorIntervalInMinutes,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("5")
    VmPoolMonitorBatchSize,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("0 0/5 * * * ?")
    AutoRecoverySchedule,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    VmPoolMonitorMaxAttempts,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    LiveSnapshotEnabled,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("9000")
    MaxMTU,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    PosixStorageEnabled,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    SendVmTicketUID,

    @DefaultValueAttribute("")
    @TypeConverterAttribute(String.class)
    LDAPProviderTypes,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    AdvancedNFSOptionsEnabled,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("8192")
    PayloadSize,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("255")
    ApplicationMode,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("4")
    NumberOfUSBSlots,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    NativeUSBEnabled,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    ShareableDiskEnabled,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    CpuPinningEnabled,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    DirectLUNDiskEnabled,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("false")
    FilteringLUNsEnabled,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("animation")
    WANDisableEffects,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("16")
    WANColorDepth,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    VmPoolMaxSubsequentFailures,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    CpuPinMigrationEnabled,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    SupportForceCreateVG,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    NonVmNetworkSupported,

    @TypeConverterAttribute(java.util.List.class)
    @DefaultValueAttribute("0,2")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.CommaSeparatedStringArray)
    DisconnectPoolOnReconstruct,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("120")
    NetworkConnectivityCheckTimeoutInSeconds,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    SupportBridgesReportByVDSM,

    @Reloadable
    @TypeConverterAttribute(Map.class)
    @DefaultValueAttribute("{\"storage domains\":\"true\",\"hosts\":\"true\"}")
    AutoRecoveryAllowedTypes,

    /*
     * umask is required to allow only self access
     * tar is missing from vanilla fedora-18 so we use python
     */
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute(
        "umask 0077; " +
        "MYTMP=\"$(mktemp -t ovirt-XXXXXXXXXX)\"; " +
        "trap \"chmod -R u+rwX \\\"${MYTMP}\\\" > /dev/null 2>&1; rm -fr \\\"${MYTMP}\\\" > /dev/null 2>&1\" 0; " +
        "rm -fr \"${MYTMP}\" && " +
        "mkdir \"${MYTMP}\" && " +
        "tar --warning=no-timestamp -C \"${MYTMP}\" -x && " +
        "@ENVIRONMENT@ \"${MYTMP}\"/setup DIALOG/dialect=str:machine DIALOG/customization=bool:True"
    )
    BootstrapCommand,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10000")
    BootstrapCacheRefreshInterval,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("/usr/share/ovirt-host-deploy/interface-3")
    BootstrapPackageDirectory,
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("ovirt-host-deploy.tar")
    BootstrapPackageName,
    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("ovirt-engine")
    SSHKeyAlias,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    LDAPOperationTimeout,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    LDAPConnectTimeout,

    /*
     * Whether to allow a cluster with both Virt and Gluster services enabled
     */
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    AllowClusterWithVirtGlusterEnabled,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    EnableMACAntiSpoofingFilterRules,
    // Gluster peer status command
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("gluster peer status --xml")
    GlusterPeerStatusCommand,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    MTUOverrideSupported,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    PortMirroringSupported,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1800")
    SSHInactivityHardTimoutSeconds,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("virt")
    GlusterVolumeOptionGroupVirtValue,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("36")
    GlusterVolumeOptionOwnerUserVirtValue,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("36")
    GlusterVolumeOptionOwnerGroupVirtValue,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    IPTablesConfigForVirt,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    IPTablesConfigForGluster,

    // Host time drift
    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("false")
    EnableHostTimeDrift,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("300")
    HostTimeDriftInSec,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10000")
    ThrottlerMaxWaitForVdsUpdateInMillis,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("false")
    OnlyRequiredNetworksMandatoryForVdsSelection,

    @Reloadable
    @TypeConverterAttribute(EngineWorkingMode.class)
    @DefaultValueAttribute("Active")
    EngineMode,

    /**
     * Refresh rate (in seconds) for light-weight gluster data i.e. data that can be fetched without much of an overhead
     * on the GlusterFS processes
     */
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("5")
    GlusterRefreshRateLight,

    /**
     * Refresh rate (in seconds) for heavy-weight gluster data i.e. commands to fetch such data adds a considerable
     * overhead on the GlusterFS processes.
     */
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("300")
    GlusterRefreshRateHeavy,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    BootstrapMinimalVdsmVersion,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    SupportForceExtendVG,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    MinimalETLVersion,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    SysPrepWindows8Path,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ProductKeyWindows8,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    SysPrepWindows8x64Path,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ProductKeyWindows8x64,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    SysPrepWindows2012x64Path,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    ProductKeyWindows2012x64,


    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    NetworkLinkingSupported,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    QuotaCacheIntervalInMinutes,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("60")
    MinimumPercentageToUpdateQuotaCache,

    @Reloadable
    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    HardwareInfoEnabled,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("95")
    LogMaxPhysicalMemoryUsedThresholdInPercentage,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("95")
    LogMaxCpuUsedThresholdInPercentage,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("95")
    LogMaxNetworkUsedThresholdInPercentage,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("256")
    LogMinFreeSwapThresholdInMB,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("95")
    LogMaxSwapUsedThresholdInPercentage,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    TunnelMigrationEnabled,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    MigrationSupportForNativeUsb,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("9")
    PgMajorRelease,

    @TypeConverterAttribute(java.util.List.class)
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.CommaSeparatedStringArray)
    @DefaultValueAttribute("ar,da,de,de-ch,en-gb,en-us,es,et,fi,fo,fr,fr-be,fr-ca,fr-ch,hr,hu,is,it,ja,lt,lv,mk,nl,nl-be,no,pl,pt,pt-br,ru,sl,sv,th,tr")
    VncKeyboardLayoutValidValues,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    SpiceProxyDefault,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    GlusterFsStorageEnabled,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    GlusterSupport,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    GlusterRefreshHeavyWeight,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    GlusterHooksEnabled,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("600")
    GlusterRefreshRateHooks,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("false")
    GlusterServicesEnabled,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    GlusterHostUUIDSupport,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    GlusterAysncTasksSupport,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("AttestationService/resources/PollHosts")
    PollUri,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("TrustStore.jks")
    AttestationTruststore,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("8443")
    AttestationPort,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("password")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.Password)
    AttestationTruststorePass,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    AttestationServer,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    AttestationFirstStageSize,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    SecureConnectionWithOATServers,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("20")
    AttestationSecondStageSize,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    GlusterRefreshRateTasks,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("Auto")
    KeystoneAuthUrl,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("Auto")
    ClientModeSpiceDefault,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("Native")
    ClientModeVncDefault,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    MigrationNetworkEnabled,

    @Reloadable
    @TypeConverterAttribute(Double.class)
    @DefaultValueAttribute("20")
    DelayResetForSpmInSeconds,

    @Reloadable
    @TypeConverterAttribute(Double.class)
    @DefaultValueAttribute("0.5")
    DelayResetPerVmInSeconds,

    @Reloadable
    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("/usr/bin/vdsm-tool vdsm-id")
    GetVdsmIdByVdsmToolCommand,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("Auto")
    ClientModeRdpDefault,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("Off")
    WebSocketProxy,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("120")
    WebSocketProxyTicketValiditySeconds,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    SupportCustomDeviceProperties,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    CustomDeviceProperties,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    MultipleGatewaysSupported,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    MemorySnapshotSupported,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("/osinfo.conf.d")
    OsRepositoryConfDir,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    VirtIoScsiEnabled,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("/usr/bin/vdsm-tool service-restart vdsmd")
    SshSoftFencingCommand,

    @TypeConverterAttribute(java.util.List.class)
    @DefaultValueAttribute("rhel6.2.0,pc-1.0")
    @OptionBehaviourAttribute(behaviour = OptionBehaviour.CommaSeparatedStringArray)
    ClusterEmulatedMachines,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1024")
    MaxAverageNetworkQoSValue,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("2048")
    MaxPeakNetworkQoSValue,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10240")
    MaxBurstNetworkQoSValue,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    QoSInboundAverageDefaultValue,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    QoSInboundPeakDefaultValue,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("100")
    QoSInboundBurstDefaultValue,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    SingleQxlPciEnabled,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("")
    UserMessageOfTheDay,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("3")
    IterationsWithBalloonProblem,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    QoSOutboundAverageDefaultValue,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    QoSOutboundPeakDefaultValue,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("100")
    QoSOutboundBurstDefaultValue,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    NormalizedMgmgNetworkEnabled,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    MomPoliciesOnHostSupported,

    @TypeConverterAttribute(String.class)
    @DefaultValueAttribute("http://localhost:18781/")
    ExternalSchedulerServiceURL,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("100")
    ExternalSchedulerConnectionTimeout,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("120000")
    ExternalSchedulerResponseTimeout,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("false")
    ExternalSchedulerEnabled,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1000")
    MaxSchedulerWeight,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    NetworkQosSupported,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    CloudInitSupported,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    HotPlugDiskSnapshotSupported,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    UseFqdnForRdpIfAvailable,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    AbortMigrationOnError,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    DwhHeartBeatInterval,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("20")
    GlanceImageListSize,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("500")
    GlanceImageTotalListSize,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("300")
    HostPreparingForMaintenanceIdleTime,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    SpeedOptimizationSchedulingThreshold,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("false")
    SchedulerAllowOverBooking,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    SchedulerOverBookingThreshold,

    @TypeConverterAttribute(Boolean.class)
    @DefaultValueAttribute("true")
    GlusterSupportForceCreateVolume,

    @Reloadable
    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("2")
    GlusterPeerStatusRetries,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("1")
    AutoStartVmsRunnerIntervalInSeconds,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("30")
    RetryToRunAutoStartVmIntervalInSeconds,

    @TypeConverterAttribute(Integer.class)
    @DefaultValueAttribute("10")
    MaxNumOfTriesToRunFailedAutoStartVm,

    Invalid;
}
