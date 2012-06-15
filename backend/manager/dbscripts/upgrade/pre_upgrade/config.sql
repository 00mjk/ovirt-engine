/************************************************************************************
                DATABASE APPLICATION CONFIGURATION FILE

This file is used to update the vdc_options configuration table.
The following sections are available:
    Add Section
    Update section (w/o overriding current value)
    Delete section
    Split config section
    Simple upgrades not available using a fn_db* function call
    Complex upgrades using temporary functions

In each section (except simple/function sections), entries are ordered by key,
please keep this when modifing this file.

PLEASE NOTE THAT THIS SCRIPT MUST REMAIN RE-ENTRANT!

************************************************************************************/


------------------------------------------------------------------------------------
--                  Add configuration values section
------------------------------------------------------------------------------------

select fn_db_add_config_value('CpuPinningEnabled','false','2.2');
select fn_db_add_config_value('CpuPinningEnabled','false','3.0');
select fn_db_add_config_value('CpuPinningEnabled','true','3.1');
select fn_db_add_config_value('AdminDomain','internal','general');
select fn_db_add_config_value('AdminPassword','','general');
select fn_db_add_config_value('AdminUser','admin','general');
select fn_db_add_config_value('AdUserId','','general');
select fn_db_add_config_value('AdUserName','','general');
select fn_db_add_config_value('AdUserPassword','','general');
select fn_db_add_config_value('AdvancedNFSOptionsEnabled','false','2.2');
select fn_db_add_config_value('AdvancedNFSOptionsEnabled','false','3.0');
select fn_db_add_config_value('AdvancedNFSOptionsEnabled','true','3.1');
select fn_db_add_config_value('AgentAppName','RHEV-Agent','general');
select fn_db_add_config_value('AllowDuplicateMacAddresses','false','general');
select fn_db_add_config_value('ApplicationMode','255','general');
select fn_db_add_config_value('AsyncPollingCyclesBeforeCallbackCleanup','120','general');
select fn_db_add_config_value('AsyncPollingCyclesBeforeRefreshSuspend','30','general');
select fn_db_add_config_value('AsyncTaskPollingRate','10','general');
select fn_db_add_config_value('AsyncTaskStatusCacheRefreshRateInSeconds','30','general');
select fn_db_add_config_value('AsyncTaskStatusCachingTimeInMinutes','1','general');
select fn_db_add_config_value('AsyncTaskZombieTaskLifeInMinutes','300','general');
select fn_db_add_config_value('AuditLogAgingThreashold','30','general');
select fn_db_add_config_value('AuditLogCleanupTime','03:35:35','general');
--Handling Authentication Method
select fn_db_add_config_value('AuthenticationMethod','LDAP','general');
select fn_db_add_config_value('AutoMode','PerServer','general');
select fn_db_add_config_value('AutoRecoverySchedule','0 0/5 * * * ?','general');
select fn_db_add_config_value('AutoRepoDomainRefreshTime','60','general');
select fn_db_add_config_value('AutoSuspendTimeInMinutes','30','general');
select fn_db_add_config_value('BlockMigrationOnSwapUsagePercentage','0','general');
select fn_db_add_config_value('BootstrapInstallerFileName','backend/manager/conf/vds_installer.py','general');
--Handling CA Base Directory
select fn_db_add_config_value('CABaseDirectory','ca','general');
--Handling CA certificate path
select fn_db_add_config_value('CACertificatePath','ca/certs.pem','general');
--Handling PEM File Name
select fn_db_add_config_value('CAEngineKey','engine.pem','general');
select fn_db_add_config_value('CBCCertificateScriptName','/usr/share/vdsm-reg/vdsm-gen-cert','general');
select fn_db_add_config_value('CbcCheckOnVdsChange','false','general');
select fn_db_add_config_value('CBCCloseCertificateScriptName','/usr/share/vdsm-reg/vdsm-complete','general');
--Handling Certificate alias
select fn_db_add_config_value('CertAlias','engine','general');
--Handling Certificate File Name
select fn_db_add_config_value('CertificateFileName','vdc.pfx','general');
select fn_db_add_config_value('CertificateFingerPrint','73 18 22 44 5d 98 b0 5d c0 f7 36 7d f8 1d 85 da e1 3c f1 c6','general');
select fn_db_add_config_value('CertificatePassword','jlOTIS0q5avsg1GaRjf/6/tnEM1pXcCRvNoeJ5MCgHF1kCzcqqhZvzR8Pn/5iBxaKVC7Y4OdA0joXVMLGasVGLnUkxExzNCMT+6QwyFM1L9/0g+1OgGTuMbvYTfEi0jLOFv0xwWDl5MWunPUjZriGEhkiu5d6QJ5ZeEhD4rRooA=','general');
select fn_db_add_config_value('CipherSuite','DEFAULT','general');
--Handling Configuration directory for ENGINE
select fn_db_add_config_value('ConfigDir','/etc/engine','general');
select fn_db_add_config_value('ConnectToServerTimeoutInSeconds','20','general');
select fn_db_add_config_value('CpuOverCommitDurationMinutes','2','general');
select fn_db_add_config_value('CustomPublicConfig_AppsWebSite','','general');
--Handling Data directory for ENGINE
select fn_db_add_config_value('DataDir','/usr/share/engine','general');
select fn_db_add_config_value('DBEngine','SQLServer','general');
select fn_db_add_config_value('DebugSearchLogging','false','general');
select fn_db_add_config_value('DebugTimerLogging','true','general');
select fn_db_add_config_value('DefaultMaxThreadPoolSize','500','general');
select fn_db_add_config_value('DefaultMinThreadPoolSize','50','general');
select fn_db_add_config_value('DefaultTimeZone','(GMT) GMT Standard Time','general');
--Handling Default Workgroup
select fn_db_add_config_value('DefaultWorkgroup','WORKGROUP','general');
select fn_db_add_config_value('DesktopAudioDeviceType','default,ac97','2.2');
select fn_db_add_config_value('DesktopAudioDeviceType','WindowsXP,ac97,RHEL4,ac97,RHEL3,ac97,Windows2003x64,ac97,RHEL4x64,ac97,RHEL3x64,ac97,OtherLinux,ac97,Other,ac97,default,ich6','3.0');
select fn_db_add_config_value('DesktopAudioDeviceType','WindowsXP,ac97,RHEL4,ac97,RHEL3,ac97,Windows2003x64,ac97,RHEL4x64,ac97,RHEL3x64,ac97,OtherLinux,ac97,Other,ac97,default,ich6','3.1');
select fn_db_add_config_value('DisableFenceAtStartupInSec','300','general');
select fn_db_add_config_value('DiskConfigurationList','System,Sparse,COW,true;Data,Preallocated,RAW,false;Shared,Preallocated,RAW,false;Swap,Preallocated,RAW,false;Temp,Sparse,COW,false','general');
select fn_db_add_config_value('DirectLUNDiskEnabled','false','2.2');
select fn_db_add_config_value('DirectLUNDiskEnabled','false','3.0');
select fn_db_add_config_value('DirectLUNDiskEnabled','true','3.1');
select fn_db_add_config_value('DocsURL','','general');
--Handling NetBIOS Domain Name
select fn_db_add_config_value('DomainName','example.com','general');
select fn_db_add_config_value('EmulatedMachine','rhel5.5.0','2.2');
select fn_db_add_config_value('EmulatedMachine','rhel6.0.0','3.0');
select fn_db_add_config_value('EmulatedMachine','pc-0.14','3.1');
--Handling Enable Spice Root Certification Validation
select fn_db_add_config_value('EnableSpiceRootCertificateValidation','true','general');
select fn_db_add_config_value('EnableSwapCheck','true','general');
--Handling Enable USB devices attachment to the VM by default
select fn_db_add_config_value('EnableUSBAsDefault','true','general');
--Handling Enables Host Load Balancing system.
select fn_db_add_config_value('EnableVdsLoadBalancing','true','general');
select fn_db_add_config_value('ENGINEEARLib','%JBOSS_HOME%/server/engine-slimmed/deploy/engine.ear','general');
--Handling Mail User Domain
select fn_db_add_config_value('ENMailDomain','','general');
--Handling Use HTML in message body
select fn_db_add_config_value('ENMailIsBodyHtml','true','general');
--Handling Use Default Credentials
select fn_db_add_config_value('ENMailUseDefaultCredentials','false','general');
select fn_db_add_config_value('FailedJobCleanupTimeInMinutes','60','general');
select fn_db_add_config_value('FenceAgentDefaultParams','ilo3:lanplus,timeout=4','general');
select fn_db_add_config_value('FenceAgentMapping','ilo3=ipmilan','general');
select fn_db_add_config_value('FenceQuietTimeBetweenOperationsInSec','180','general');
select fn_db_add_config_value('FenceStartStatusDelayBetweenRetriesInSec','60','general');
select fn_db_add_config_value('FenceStartStatusRetries','3','general');
select fn_db_add_config_value('FenceStopStatusDelayBetweenRetriesInSec','60','general');
select fn_db_add_config_value('FenceStopStatusRetries','3','general');
select fn_db_add_config_value('FilteringLUNsEnabled','true','2.2');
select fn_db_add_config_value('FilteringLUNsEnabled','true','3.0');
select fn_db_add_config_value('FilteringLUNsEnabled','false','3.1');
select fn_db_add_config_value('FindFenceProxyDelayBetweenRetriesInSec','30','general');
select fn_db_add_config_value('FindFenceProxyRetries','3','general');
select fn_db_add_config_value('FreeSpaceCriticalLowInGB','5','general');
select fn_db_add_config_value('FreeSpaceLow','10','general');
select fn_db_add_config_value('GuestToolsSetupIsoPrefix','RHEV-toolsSetup_','general');
select fn_db_add_config_value('HighUtilizationForEvenlyDistribute','75','general');
select fn_db_add_config_value('HighUtilizationForPowerSave','75','general');
select fn_db_add_config_value('HotPlugEnabled','false','2.2');
select fn_db_add_config_value('HotPlugEnabled','false','3.0');
select fn_db_add_config_value('HotPlugEnabled','true','3.1');
select fn_db_add_config_value('HotPlugSupportedOsList','Windows7,Windows7x64,Windows2008,Windows2008x64,Windows2008R2x64,RHEL5,RHEL5x64,RHEL6,RHEL6x64','general');
select fn_db_add_config_value('InitStorageSparseSizeInGB','1','general');
--Handling Install virtualization software on Add Host
select fn_db_add_config_value('InstallVds','true','general');
select fn_db_add_config_value('IoOpTimeoutSec','10','general');
select fn_db_add_config_value('IPTablesConfig',
'# oVirt default firewall configuration. Automatically generated by vdsm bootstrap script.
*filter
:INPUT ACCEPT [0:0]
:FORWARD ACCEPT [0:0]
:OUTPUT ACCEPT [0:0]
-A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
-A INPUT -p icmp -j ACCEPT
-A INPUT -i lo -j ACCEPT
# vdsm
-A INPUT -p tcp --dport 54321 -j ACCEPT
# libvirt tls
-A INPUT -p tcp --dport 16514 -j ACCEPT
# SSH
-A INPUT -p tcp --dport 22 -j ACCEPT
# guest consoles
-A INPUT -p tcp -m multiport --dports 5634:6166 -j ACCEPT
# migration
-A INPUT -p tcp -m multiport --dports 49152:49216 -j ACCEPT
# snmp
-A INPUT -p udp --dport 161 -j ACCEPT
# Reject any other input traffic
-A INPUT -j REJECT --reject-with icmp-host-prohibited
-A FORWARD -m physdev ! --physdev-is-bridged -j REJECT --reject-with icmp-host-prohibited
COMMIT
','general');
select fn_db_add_config_value('IsMultilevelAdministrationOn','true','general');
select fn_db_add_config_value('IsNeedSupportForOldVgAPI','true','2.2');
select fn_db_add_config_value('IsNeedSupportForOldVgAPI','false','3.0');
select fn_db_add_config_value('JobCleanupRateInMinutes','10','general');
select fn_db_add_config_value('JobPageSize','100','general');
select fn_db_add_config_value('keystorePass','NoSoup4U','general');
--Handling Keystore URL
select fn_db_add_config_value('keystoreUrl','.keystore','general');
select fn_db_add_config_value('LdapQueryPageSize','1000','general');
select fn_db_add_config_value('LDAPQueryTimeout','30','general');
--Handling LDAP Security Authentication Method
select fn_db_add_config_value('LDAPSecurityAuthentication','GSSAPI','general');
select fn_db_add_config_value('LDAPServerPort','389','general');
select fn_db_add_config_value('LdapServers','','general');
select fn_db_add_config_value('LDAPProviderTypes','','general');
select fn_db_add_config_value('LeaseRetries','3','general');
select fn_db_add_config_value('LeaseTimeSec','60','general');
select fn_db_add_config_value('LicenseCertificateFingerPrint','5f 38 41 89 b1 33 49 0c 24 13 6b b3 e5 ba 9e c7 fd 83 80 3b','general');
select fn_db_add_config_value('LimitNumberOfNetworkInterfaces','true','2.2');
select fn_db_add_config_value('LimitNumberOfNetworkInterfaces','false','3.0');
select fn_db_add_config_value('LimitNumberOfNetworkInterfaces','false','3.1');
select fn_db_add_config_value('LiveSnapshotEnabled','false','2.2');
select fn_db_add_config_value('LiveSnapshotEnabled','false','3.0');
select fn_db_add_config_value('LiveSnapshotEnabled','true','3.1');
select fn_db_add_config_value('LocalAdminPassword','123456','general');
select fn_db_add_config_value('LocalStorageEnabled','false','2.2');
select fn_db_add_config_value('LocalStorageEnabled','true','3.0');
select fn_db_add_config_value('LocalStorageEnabled','true','3.1');
--Handling Enable lock policy for Storage Pool Manager on activation
select fn_db_add_config_value('LockPolicy','ON','general');
select fn_db_add_config_value('LockRenewalIntervalSec','5','general');
select fn_db_add_config_value('LogDBCommands','false','general');
select fn_db_add_config_value('LogPhysicalMemoryThresholdInMB','1024','general');
--Handling Log XML-RPC Data
select fn_db_add_config_value('LogXmlRpcData','false','general');
select fn_db_add_config_value('LowUtilizationForEvenlyDistribute','0','general');
select fn_db_add_config_value('LowUtilizationForPowerSave','20','general');
select fn_db_add_config_value('MacPoolRanges','00:1A:4A:16:01:51-00:1A:4A:16:01:e6','general');
select fn_db_add_config_value('ManagedDevicesWhiteList','','general');
select fn_db_add_config_value('ManagementNetwork','ovirtmgmt','general');
select fn_db_add_config_value('MaxAuditLogMessageLength','10000','general');
select fn_db_add_config_value('MaxDiskSize','16384','general');
select fn_db_add_config_value('MaxLDAPQueryPartsNumber','100','general');
select fn_db_add_config_value('MaxMacsCountInPool','100000','general');
select fn_db_add_config_value('MaxNumberOfHostsInStoragePool','250','general');
select fn_db_add_config_value('MaxNumOfCpuPerSocket','16','2.2');
select fn_db_add_config_value('MaxNumOfCpuPerSocket','16','3.0');
select fn_db_add_config_value('MaxNumOfCpuPerSocket','16','3.1');
select fn_db_add_config_value('MaxNumOfVmCpus','16','2.2');
select fn_db_add_config_value('MaxNumOfVmCpus','64','3.0');
select fn_db_add_config_value('MaxNumOfVmCpus','64','3.1');
select fn_db_add_config_value('MaxNumOfVmSockets','16','2.2');
select fn_db_add_config_value('MaxNumOfVmSockets','16','3.0');
select fn_db_add_config_value('MaxNumOfVmSockets','16','3.1');
select fn_db_add_config_value('MaxRerunVmOnVdsCount','3','general');
select fn_db_add_config_value('MaxStorageVdsDelayCheckSec','5','general');
select fn_db_add_config_value('MaxStorageVdsTimeoutCheckSec','30','general');
select fn_db_add_config_value('MaxVdsMemOverCommit','200','general');
select fn_db_add_config_value('MaxVdsMemOverCommitForServers','150','general');
select fn_db_add_config_value('MaxVdsNameLength','255','general');
select fn_db_add_config_value('MaxVmNameLengthNonWindows','64','general');
select fn_db_add_config_value('MaxVmNameLengthWindows','15','general');
select fn_db_add_config_value('MaxVmsInPool','1000','general');
select fn_db_add_config_value('MinimalETLVersion','3.0.0','general');
select fn_db_add_config_value('NativeUSBEnabled','false','2.2');
select fn_db_add_config_value('NativeUSBEnabled','false','3.0');
select fn_db_add_config_value('NativeUSBEnabled','true','3.1');
select fn_db_add_config_value('NicDHCPDelayGraceInMS','60','general');
select fn_db_add_config_value('NumberOfFailedRunsOnVds','3','general');
select fn_db_add_config_value('NumberOfUSBSlots','4','general');
select fn_db_add_config_value('NumberOfVmsForTopSizeVms','10','general');
select fn_db_add_config_value('NumberVmRefreshesBeforeSave','5','general');
--Handling Organization Name
select fn_db_add_config_value('OrganizationName','Redhat','general');
select fn_db_add_config_value('OriginType','OVIRT','general');
select fn_db_add_config_value('OvfVirtualSystemType','ENGINE','general');
select fn_db_add_config_value('OvirtInitialSupportedIsoVersion','5.8','general');
select fn_db_add_config_value('OvirtIsoPrefix','rhevh','general');
--Handling The RHEV-H installation files path
select fn_db_add_config_value('oVirtISOsRepositoryPath','ovirt-isos','general');
select fn_db_add_config_value('oVirtUpgradeScriptName','/usr/share/vdsm-reg/vdsm-upgrade','general');
select fn_db_add_config_value('oVirtUploadPath','/data/updates/ovirt-node-image.iso','general');
select fn_db_add_config_value('PayloadSize','8192','general');
select fn_db_add_config_value('PosixStorageEnabled','false','2.2');
select fn_db_add_config_value('PosixStorageEnabled','false','3.0');
select fn_db_add_config_value('PosixStorageEnabled','true','3.1');
select fn_db_add_config_value('PostgresI18NPrefix','','general');
select fn_db_add_config_value('PostgresLikeSyntax','ILIKE','general');
select fn_db_add_config_value('PostgresPagingSyntax',E' OFFSET (%1$s -1) LIMIT %2$s','general');
select fn_db_add_config_value('PostgresPagingType','Offset','general');
select fn_db_add_config_value('PostgresSearchTemplate',E'SELECT * FROM (%2$s) %1$s) as T1 %3$s','general');
--Handling Allow Running Guests Without Tools
select fn_db_add_config_value('PowerClientAllowRunningGuestsWithoutTools','false','general');
select fn_db_add_config_value('PowerClientAllowUsingAsIRS','false','general');
--Handling Auto-AdjustMemory Base On Available Memory
select fn_db_add_config_value('PowerClientAutoAdjustMemoryBaseOnAvailableMemory','false','general');
--Handling Client Auto Adjust Memory
select fn_db_add_config_value('PowerClientAutoAdjustMemory','false','general');
select fn_db_add_config_value('PowerClientAutoAdjustMemoryGeneralReserve','768','general');
--Handling Auto-Adjust Memory Log
select fn_db_add_config_value('PowerClientAutoAdjustMemoryLog','false','general');
select fn_db_add_config_value('PowerClientAutoAdjustMemoryMaxMemory','2048','general');
select fn_db_add_config_value('PowerClientAutoAdjustMemoryModulus','64','general');
select fn_db_add_config_value('PowerClientAutoAdjustMemorySpicePerMonitorReserve','0','general');
select fn_db_add_config_value('PowerClientAutoAdjustMemorySpicePerSessionReserve','0','general');
--Handling Auto Approve Patterns
select fn_db_add_config_value('PowerClientAutoApprovePatterns','','general');
select fn_db_add_config_value('PowerClientAutoInstallCertificateOnApprove','true','general');
select fn_db_add_config_value('PowerClientAutoMigrateFromPowerClientToVdsWhenConnectingFromRegularClient','false','general');
--Handling AutoMigrate To PowerClient On Connect
select fn_db_add_config_value('PowerClientAutoMigrateToPowerClientOnConnect','false','general');
select fn_db_add_config_value('PowerClientAutoRegistrationDefaultVdsGroupID','99408929-82CF-4DC7-A532-9D998063FA95','general');
select fn_db_add_config_value('PowerClientDedicatedVmLaunchOnVdsWhilePowerClientStarts','false','general');
--Handling Enable Power Client GUI
select fn_db_add_config_value('PowerClientGUI','false','general');
select fn_db_add_config_value('PowerClientLogDetection','false','general');
select fn_db_add_config_value('PowerClientMaxNumberOfConcurrentVMs','1','general');
select fn_db_add_config_value('PowerClientRunVmShouldVerifyPendingVMsAsWell','false','general');
--Handling Spice Dynamic Compression Management
select fn_db_add_config_value('PowerClientSpiceDynamicCompressionManagement','false','general');
select fn_db_add_config_value('PredefinedVMProperties','','2.2');
select fn_db_add_config_value('PredefinedVMProperties','sap_agent=^(true|false)$;sndbuf=^[0-9]+$;vhost=^(([a-zA-Z0-9_]*):(true|false))(,(([a-zA-Z0-9_]*):(true|false)))*$;viodiskcache=^(none|writeback|writethrough)$','3.0');
select fn_db_add_config_value('PredefinedVMProperties','sap_agent=^(true|false)$;sndbuf=^[0-9]+$;vhost=^(([a-zA-Z0-9_]*):(true|false))(,(([a-zA-Z0-9_]*):(true|false)))*$;viodiskcache=^(none|writeback|writethrough)$','3.1');
select fn_db_add_config_value('ProductKey2003','','general');
select fn_db_add_config_value('ProductKey2003x64','','general');
select fn_db_add_config_value('ProductKey2008','','general');
select fn_db_add_config_value('ProductKey2008R2','','general');
select fn_db_add_config_value('ProductKey2008x64','','general');
--Handling Product Key (for Windows XP)
select fn_db_add_config_value('ProductKey','','general');
select fn_db_add_config_value('ProductKeyWindow7','','general');
select fn_db_add_config_value('ProductKeyWindow7x64','','general');
select fn_db_add_config_value('ProductRPMVersion','3.0.0.0','general');
select fn_db_add_config_value('PublicURLPort','8080','general');
select fn_db_add_config_value('QuotaGraceStorage','20','general');
select fn_db_add_config_value('QuotaGraceVdsGroup','20','general');
select fn_db_add_config_value('QuotaThresholdStorage','80','general');
select fn_db_add_config_value('QuotaThresholdVdsGroup','80','general');
--Handling Connect to RDP console with Fully Qualified User-Name (user@domain)
select fn_db_add_config_value('RDPLoginWithFQN','true','general');
select fn_db_add_config_value('RedirectServletReportsPageError','Reports not installed, please contact your administrator','general');
select fn_db_add_config_value('RedirectServletReportsPage','','general');
select fn_db_add_config_value('RenewGuestIpOnVdsSubnetChange','false','general');
select fn_db_add_config_value('RenewGuestIpOnVdsSubnetChangeOnParseError','false','general');
select fn_db_add_config_value('RhevhLocalFSPath','/data/images/','general');
select fn_db_add_config_value('SANWipeAfterDelete','false','general');
--Handling SASL QOP
select fn_db_add_config_value('SASL_QOP','auth-conf','general');
select fn_db_add_config_value('ScriptsPath','/usr/share/engine','general');
select fn_db_add_config_value('SearchesRefreshRateInSeconds','1','general');
select fn_db_add_config_value('SearchResultsLimit','100','general');
select fn_db_add_config_value('SelectCommandTimeout','120','general');
select fn_db_add_config_value('SendSMPOnRunVm','true','general');
select fn_db_add_config_value('SendVmTicketUID','false','2.2');
select fn_db_add_config_value('SendVmTicketUID','false','3.0');
select fn_db_add_config_value('SendVmTicketUID','true','3.1');
select fn_db_add_config_value('ServerCPUList','2:Intel Xeon w/o XD/NX:vmx,sse2:qemu64,-nx,+sse2; 3:Intel Xeon:vmx,sse2,nx:qemu64,+sse2; 4:Intel Conroe Family:vmx,sse2,nx,cx16,ssse3:qemu64,+sse2,+cx16,+ssse3; 5:Intel Penryn Family:vmx,sse2,nx,cx16,ssse3,sse4_1:qemu64,+sse2,+cx16,+ssse3,+sse4.1; 6:Intel Nehalem Family:vmx,sse2,nx,cx16,ssse3,sse4_1,sse4_2,popcnt:qemu64,+sse2,+cx16,+ssse3,+sse4.1,+sse4.2,+popcnt; 2:AMD Opteron G1 w/o NX:svm,sse2:qemu64,-nx,+sse2; 3:AMD Opteron G1:svm,sse2,nx:qemu64,+sse2; 4:AMD Opteron G2:svm,sse2,nx,cx16:qemu64,+sse2,+cx16; 5:AMD Opteron G3:svm,sse2,nx,cx16,sse4a,misalignsse,popcnt,abm:qemu64,+sse2,+cx16,+sse4a,+misalignsse,+popcnt,+abm;','2.2');
select fn_db_add_config_value('ServerCPUList','3:Intel Conroe Family:vmx,nx,model_Conroe:Conroe; 4:Intel Penryn Family:vmx,nx,model_Penryn:Penryn; 5:Intel Nehalem Family:vmx,nx,model_Nehalem:Nehalem; 6:Intel Westmere Family:aes,vmx,nx,model_Westmere:Westmere; 2:AMD Opteron G1:svm,nx,model_Opteron_G1:Opteron_G1; 3:AMD Opteron G2:svm,nx,model_Opteron_G2:Opteron_G2; 4:AMD Opteron G3:svm,nx,model_Opteron_G3:Opteron_G3;','3.0');
select fn_db_add_config_value('ServerCPUList','3:Intel Conroe Family:vmx,nx,model_Conroe:Conroe; 4:Intel Penryn Family:vmx,nx,model_Penryn:Penryn; 5:Intel Nehalem Family:vmx,nx,model_Nehalem:Nehalem; 6:Intel Westmere Family:aes,vmx,nx,model_Westmere:Westmere; 2:AMD Opteron G1:svm,nx,model_Opteron_G1:Opteron_G1; 3:AMD Opteron G2:svm,nx,model_Opteron_G2:Opteron_G2; 4:AMD Opteron G3:svm,nx,model_Opteron_G3:Opteron_G3;','3.1');
select fn_db_add_config_value('ServerRebootTimeout','300','general');
select fn_db_add_config_value('SetupNetworksPollingTimeout','3','general');
-- Add shareable disk property in vdc_options to support only 3.1 version.
select fn_db_add_config_value('ShareableDiskEnabled','false','2.2');
select fn_db_add_config_value('ShareableDiskEnabled','false','3.0');
select fn_db_add_config_value('ShareableDiskEnabled','true','3.1');
select fn_db_add_config_value('SignCertTimeoutInSeconds','30','general');
select fn_db_add_config_value('SignLockFile','/var/lock/engine/.openssl.exclusivelock','general');
--Handling Script name for signing
select fn_db_add_config_value('SignScriptName','SignReq.sh','general');
select fn_db_add_config_value('SpiceDriverNameInGuest','RHEV-Spice','general');
select fn_db_add_config_value('SpiceReleaseCursorKeys','shift+f12','general');
select fn_db_add_config_value('SpiceToggleFullScreenKeys','shift+f11','general');
--Handling Enable USB devices sharing by default in SPICE
select fn_db_add_config_value('SpiceUsbAutoShare','true','general');
select fn_db_add_config_value('WANDisableEffects','animation','general');
select fn_db_add_config_value('WANColorDepth','16','general');
select fn_db_add_config_value('SpmCommandFailOverRetries','3','general');
select fn_db_add_config_value('SPMFailOverAttempts','3','general');
select fn_db_add_config_value('SpmVCpuConsumption','1','general');
select fn_db_add_config_value('SQLServerI18NPrefix','N','general');
select fn_db_add_config_value('SQLServerLikeSyntax','LIKE','general');
select fn_db_add_config_value('SQLServerPagingSyntax',E' WHERE RowNum BETWEEN %1$s AND %2$s','general');
select fn_db_add_config_value('SQLServerPagingType','Range','general');
select fn_db_add_config_value('SQLServerSearchTemplate',E'SELECT * FROM (SELECT *, ROW_NUMBER() OVER(%1$s) as RowNum FROM (%2$s)) as T1) as T2 %3$s','general');
select fn_db_add_config_value('SSHInactivityTimoutSeconds','600','general');
--Handling SPICE SSL Enabled
select fn_db_add_config_value('SSLEnabled','true','general');
select fn_db_add_config_value('StorageDomainFalureTimeoutInMinutes','5','general');
select fn_db_add_config_value('StorageDomainNameSizeLimit','50','general');
select fn_db_add_config_value('StoragePoolNameSizeLimit','40','general');
select fn_db_add_config_value('StoragePoolNonOperationalResetTimeoutInMin','3','general');
select fn_db_add_config_value('StoragePoolRefreshTimeInSeconds','10','general');
select fn_db_add_config_value('SucceededJobCleanupTimeInMinutes','10','general');
select fn_db_add_config_value('SupportCustomProperties','false','2.2');
select fn_db_add_config_value('SupportCustomProperties','true','3.0');
select fn_db_add_config_value('SupportCustomProperties','true','3.1');
select fn_db_add_config_value('SupportedClusterLevels','2.2,3.0','general');
select fn_db_add_config_value('SupportedStorageFormats','0','2.2');
select fn_db_add_config_value('SupportedStorageFormats','0,2','3.0');
select fn_db_add_config_value('SupportedStorageFormats','0,2','3.1');
select fn_db_add_config_value('SupportedVDSMVersions','4.5,4.9','general');
select fn_db_add_config_value('SupportGetDevicesVisibility','false','2.2');
select fn_db_add_config_value('SupportGetDevicesVisibility','true','3.0');
select fn_db_add_config_value('SupportGetDevicesVisibility','true','3.1');
select fn_db_add_config_value('SupportStorageFormat','false','2.2');
select fn_db_add_config_value('SupportStorageFormat','true','3.0');
select fn_db_add_config_value('SupportStorageFormat','true','3.1');
select fn_db_add_config_value('SysPrep2K3Path','backend/manager/conf/sysprep/sysprep.2k3','general');
select fn_db_add_config_value('SysPrep2K8Path','backend/manager/conf/sysprep/sysprep.2k8','general');
select fn_db_add_config_value('SysPrep2K8R2Path','backend/manager/conf/sysprep/sysprep.2k8','general');
select fn_db_add_config_value('SysPrep2K8x64Path','backend/manager/conf/sysprep/sysprep.2k8x86','general');
select fn_db_add_config_value('SysPrepDefaultPassword','','general');
select fn_db_add_config_value('SysPrepDefaultUser','','general');
select fn_db_add_config_value('SysPrepWindows7Path','backend/manager/conf/sysprep/sysprep.w7','general');
select fn_db_add_config_value('SysPrepWindows7x64Path','backend/manager/conf/sysprep/sysprep.w7x64','general');
--Handling Path to an XP machine Sys-Prep file.
select fn_db_add_config_value('SysPrepXPPath','backend/manager/conf/sysprep/sysprep.xp','general');
select fn_db_add_config_value('TimeoutToResetVdsInSeconds','60','general');
select fn_db_add_config_value('TimeToReduceFailedRunOnVdsInMinutes','30','general');
select fn_db_add_config_value('TruststorePass','NoSoup4U','general');
--Handling Truststore URL
select fn_db_add_config_value('TruststoreUrl','.keystore','general');
select fn_db_add_config_value('UknownTaskPrePollingLapse','60000','general');
select fn_db_add_config_value('UserDefinedVMProperties','','2.2');
select fn_db_add_config_value('UserDefinedVMProperties','','3.0');
select fn_db_add_config_value('UserDefinedVMProperties','','3.1');
select fn_db_add_config_value('UserRefreshRate','3600','general');
select fn_db_add_config_value('UserSessionTimeOutInterval','30','general');
select fn_db_add_config_value('UseRtl8139_pv','true','2.2');
select fn_db_add_config_value('UseRtl8139_pv','false','3.0');
select fn_db_add_config_value('UseRtl8139_pv','false','3.1');
--Handling Use Secure Connection with Hosts
select fn_db_add_config_value('UseSecureConnectionWithServers','true','general');
select fn_db_add_config_value('UseVdsBrokerInProc','true','general');
select fn_db_add_config_value('UtilizationThresholdInPercent','80','general');
select fn_db_add_config_value('ValidNumOfMonitors','1,2,4','general');
select fn_db_add_config_value('VcpuConsumptionPercentage','10','general');
--Handling Host Installation Bootstrap Script URL
select fn_db_add_config_value('VdcBootStrapUrl','http://example.com/engine/vds_scripts','general');
select fn_db_add_config_value('VdcVersion','3.0.0.0','general');
select fn_db_add_config_value('VDSAttemptsToResetCount','2','general');
select fn_db_add_config_value('VdsCertificateValidityInYears','5','general');
select fn_db_add_config_value('VdsFenceOptionMapping','alom:secure=secure,port=ipport;apc:secure=secure,port=ipport,slot=port;bladecenter:secure=secure,port=ipport,slot=port;drac5:secure=secure,port=ipport;eps:slot=port;ilo:secure=ssl,port=ipport;ipmilan:;rsa:secure=secure,port=ipport;rsb:;wti:secure=secure,port=ipport,slot=port;cisco_ucs:secure=ssl,slot=port','general');
select fn_db_add_config_value('VdsFenceOptions','','general');
select fn_db_add_config_value('VdsFenceOptionTypes','secure=bool,port=int,slot=int','general');
select fn_db_add_config_value('VdsFenceType','alom,apc,bladecenter,drac5,eps,ilo,ipmilan,rsa,rsb,wti,cisco_ucs','2.2');
select fn_db_add_config_value('VdsFenceType','alom,apc,bladecenter,drac5,eps,ilo,ipmilan,rsa,rsb,wti,cisco_ucs','3.0');
select fn_db_add_config_value('VdsFenceType','alom,apc,bladecenter,drac5,eps,ilo,ipmilan,rsa,rsb,wti,cisco_ucs','3.1');
select fn_db_add_config_value('VdsLoadBalancingeIntervalInMinutes','1','general');
select fn_db_add_config_value('VdsLocalDisksCriticallyLowFreeSpace','100','general');
select fn_db_add_config_value('VdsLocalDisksLowFreeSpace','500','general');
select fn_db_add_config_value('VdsRecoveryTimeoutInMintues','3','general');
select fn_db_add_config_value('VdsRefreshRate','2','general');
--Handling Host Selection Algorithm default for cluster
select fn_db_add_config_value('VdsSelectionAlgorithm','None','general');
select fn_db_add_config_value('vdsTimeout','180','general');
--Handling Virtual Machine Domain Name
select fn_db_add_config_value('VirtualMachineDomainName','VirtualMachineDomainName','general');
select fn_db_add_config_value('VM32BitMaxMemorySizeInMB','20480','general');
select fn_db_add_config_value('VM64BitMaxMemorySizeInMB','262144','2.2');
select fn_db_add_config_value('VM64BitMaxMemorySizeInMB','524288','3.0');
select fn_db_add_config_value('VM64BitMaxMemorySizeInMB','524288','3.1');
select fn_db_add_config_value('VmGracefulShutdownMessage','The oVirt Engine is shutting down this Virtual Machine','general');
select fn_db_add_config_value('VmGracefulShutdownTimeout','30','general');
select fn_db_add_config_value('VMMinMemorySizeInMB','256','general');
select fn_db_add_config_value('VmPoolMonitorBatchSize','5','general');
select fn_db_add_config_value('VmPoolMonitorIntervalInMinutes','5','general');
select fn_db_add_config_value('VmPoolMonitorMaxAttempts','3','general');
select fn_db_add_config_value('VmPriorityMaxValue','100','general');
--Handling Keyboard Layout configuration for VNC
select fn_db_add_config_value('VncKeyboardLayout','en-us','general');
select fn_db_add_config_value('WaitForVdsInitInSec','60','general');

------------------------------------------------------------------------------------
--                  Update with override section
------------------------------------------------------------------------------------

select fn_db_update_config_value('DBEngine','Postgres','general');
select fn_db_update_config_value('DebugSearchLogging','false','general');
select fn_db_update_config_value('DefaultTimeZone','(GMT) GMT Standard Time','general');
select fn_db_update_config_value('ENGINEEARLib','%JBOSS_HOME%/standalone/deployments/engine.ear','general');
select fn_db_update_config_value('FenceAgentDefaultParams','ilo3:lanplus,power_wait=4','general');
select fn_db_update_config_value('IsMultilevelAdministrationOn','true','general');
select fn_db_update_config_value('MinimalETLVersion','3.0.0','general');
select fn_db_update_config_value('PostgresPagingSyntax','OFFSET (%1$s -1) LIMIT %2$s','general');
select fn_db_update_config_value('PostgresSearchTemplate','SELECT * FROM (%2$s) %1$s) as T1 %3$s','general');
select fn_db_update_config_value('RhevhLocalFSPath','/data/images/rhev','general');
select fn_db_update_config_value('ServerCPUList','2:Intel Xeon w/o XD/NX:vmx,sse2:qemu64,-nx,+sse2; 3:Intel Xeon:vmx,sse2,nx:qemu64,+sse2; 4:Intel Conroe Family:vmx,sse2,nx,cx16,ssse3:qemu64,+sse2,+cx16,+ssse3; 5:Intel Penryn Family:vmx,sse2,nx,cx16,ssse3,sse4_1:qemu64,+sse2,+cx16,+ssse3,+sse4.1; 6:Intel Nehalem Family:vmx,sse2,nx,cx16,ssse3,sse4_1,sse4_2,popcnt:qemu64,+sse2,+cx16,+ssse3,+sse4.1,+sse4.2,+popcnt; 2:AMD Opteron G1 w/o NX:svm,sse2:qemu64,-nx,+sse2; 3:AMD Opteron G1:svm,sse2,nx:qemu64,+sse2; 4:AMD Opteron G2:svm,sse2,nx,cx16:qemu64,+sse2,+cx16; 5:AMD Opteron G3:svm,sse2,nx,cx16,sse4a,misalignsse,popcnt,abm:qemu64,+sse2,+cx16,+sse4a,+misalignsse,+popcnt,+abm;','2.2');
select fn_db_update_config_value('ServerCPUList','3:Intel Conroe Family:vmx,nx,model_Conroe:Conroe; 4:Intel Penryn Family:vmx,nx,model_Penryn:Penryn; 5:Intel Nehalem Family:vmx,nx,model_Nehalem:Nehalem; 6:Intel Westmere Family:aes,vmx,nx,model_Westmere:Westmere; 2:AMD Opteron G1:svm,nx,model_Opteron_G1:Opteron_G1; 3:AMD Opteron G2:svm,nx,model_Opteron_G2:Opteron_G2; 4:AMD Opteron G3:svm,nx,model_Opteron_G3:Opteron_G3;','3.0');
select fn_db_update_config_value('ServerCPUList','3:Intel Conroe Family:vmx,nx,model_Conroe:Conroe; 4:Intel Penryn Family:vmx,nx,model_Penryn:Penryn; 5:Intel Nehalem Family:vmx,nx,model_Nehalem:Nehalem; 6:Intel Westmere Family:aes,vmx,nx,model_Westmere:Westmere; 7:Intel SandyBridge Family:vmx,nx,model_SandyBridge:SandyBridge; 2:AMD Opteron G1:svm,nx,model_Opteron_G1:Opteron_G1; 3:AMD Opteron G2:svm,nx,model_Opteron_G2:Opteron_G2; 4:AMD Opteron G3:svm,nx,model_Opteron_G3:Opteron_G3;','3.1');
select fn_db_update_config_value('ServerCPUList','3:Intel Conroe Family:vmx,nx,model_Conroe:Conroe; 4:Intel Penryn Family:vmx,nx,model_Penryn:Penryn; 5:Intel Nehalem Family:vmx,nx,model_Nehalem:Nehalem; 6:Intel Westmere Family:aes,vmx,nx,model_Westmere:Westmere; 7:Intel SandyBridge Family:vmx,nx,model_SandyBridge:SandyBridge; 2:AMD Opteron G1:svm,nx,model_Opteron_G1:Opteron_G1; 3:AMD Opteron G2:svm,nx,model_Opteron_G2:Opteron_G2; 4:AMD Opteron G3:svm,nx,model_Opteron_G3:Opteron_G3; 5:AMD Opteron G4:svm,nx,model_Opteron_G4:Opteron_G4;','3.1');
select fn_db_update_config_value('SignLockFile','/var/lock/ovirt-engine/.openssl.exclusivelock','general');
select fn_db_update_config_value('SpiceDriverNameInGuest','{"windows": "RHEV-Spice", "linux" : "xorg-x11-drv-qxl" }','general');
select fn_db_update_config_value('SupportedClusterLevels','2.2,3.0,3.1','general');
select fn_db_update_config_value('VdcVersion','3.1.0.0','general');
select fn_db_update_config_value('VdsFenceOptionMapping','alom:secure=secure,port=ipport;apc:secure=secure,port=ipport,slot=port;bladecenter:secure=secure,port=ipport,slot=port;drac5:secure=secure,port=ipport;eps:slot=port;ilo:secure=ssl,port=ipport;ipmilan:;rsa:secure=secure,port=ipport;rsb:;wti:secure=secure,port=ipport,slot=port;cisco_ucs:secure=ssl,slot=port','general');
select fn_db_update_config_value('VdsFenceOptionMapping','alom:secure=secure,port=ipport;apc:secure=secure,port=ipport,slot=port;bladecenter:secure=secure,port=ipport,slot=port;drac5:secure=secure,port=ipport;eps:slot=port;ilo:secure=ssl,port=ipport;ipmilan:;rsa:secure=secure,port=ipport;rsb:;wti:secure=secure,port=ipport,slot=port;cisco_ucs:secure=ssl,slot=port;ilo3:','general');
select fn_db_update_config_value('VdsFenceOptionMapping','alom:secure=secure,port=ipport;apc:secure=secure,port=ipport,slot=port;bladecenter:secure=secure,port=ipport,slot=port;drac5:secure=secure,slot=port;eps:slot=port;ilo:secure=ssl,port=ipport;ipmilan:;rsa:secure=secure,port=ipport;rsb:;wti:secure=secure,port=ipport,slot=port;cisco_ucs:secure=ssl,slot=port','general');
select fn_db_update_config_value('VdsFenceOptionMapping','alom:secure=secure,port=ipport;apc:secure=secure,port=ipport,slot=port;bladecenter:secure=secure,port=ipport,slot=port;drac5:secure=secure,slot=port;eps:slot=port;ilo:secure=ssl,port=ipport;ipmilan:;rsa:secure=secure,port=ipport;rsb:;wti:secure=secure,port=ipport,slot=port;cisco_ucs:secure=ssl,slot=port;ilo3:','general');
select fn_db_update_config_value('VdsFenceType','alom,apc,bladecenter,drac5,eps,ilo,ilo3,ipmilan,rsa,rsb,wti,cisco_ucs','3.0');
select fn_db_update_config_value('VdsFenceType','alom,apc,bladecenter,drac5,eps,ilo,ipmilan,rsa,rsb,wti,cisco_ucs','2.2');
select fn_db_update_config_value('VmGracefulShutdownMessage','The oVirt Engine is shutting down this Virtual Machine','general');
select fn_db_update_config_value('SupportedVDSMVersions','4.5,4.9,4.10','general');

------------------------------------------------------------------------------------
--   Update only if default not changed section
------------------------------------------------------------------------------------

select fn_db_update_default_config_value('AdUserId','example.com:00000000-0000-0000-0000-000000000000','','general',false);
select fn_db_update_default_config_value('AdUserName','example.com:SampleUser','','general',false);
select fn_db_update_default_config_value('AdUserPassword','example.com:SamplePassword','','general',false);
-- Increase AsyncTaskZombieTaskLifeInMinutes to 50 hours if it's the default 5 hours.
select fn_db_update_default_config_value('AsyncTaskZombieTaskLifeInMinutes','300','3000','general',false);
select fn_db_update_default_config_value('DomainName','example.com','','general',false);
select fn_db_update_default_config_value('EmulatedMachine','rhel6.0.0', 'pc-0.14','3.0',false);
select fn_db_update_default_config_value('LDAPSecurityAuthentication','GSSAPI','default:GSSAPI','general',false);
select fn_db_update_default_config_value('LDAPSecurityAuthentication','SIMPLE','default:SIMPLE','general',false);
select fn_db_update_default_config_value('oVirtISOsRepositoryPath','ovirt-isos', '/usr/share/rhev-hypervisor','general',false);
select fn_db_update_default_config_value('VdsLocalDisksCriticallyLowFreeSpace','100','500','general',false);
select fn_db_update_default_config_value('VdsLocalDisksLowFreeSpace','500', '1000','general',false);
------------------------------------------------------------------------------------
--              Cleanup deprecated configuration values section
------------------------------------------------------------------------------------

select fn_db_delete_config_value('ENMailEnableSsl','general');
select fn_db_delete_config_value('ENMailHost','general');
select fn_db_delete_config_value('ENMailPassword','general');
select fn_db_delete_config_value('ENMailPort','general');
select fn_db_delete_config_value('ENMailUser','general');
select fn_db_delete_config_value('FreeSpaceCriticalLow','general');
select fn_db_delete_config_value('ImagesSyncronizationTimeout','general');
select fn_db_delete_config_value('LdapServers','3.0');
select fn_db_delete_config_value('NetConsolePort','general');
select fn_db_delete_config_value('PredefinedVMProperties','general');
select fn_db_delete_config_value('RpmsRepositoryUrl','general');
select fn_db_delete_config_value('SysPrep3.0Path','general');
select fn_db_delete_config_value('UseENGINERepositoryRPMs','general');
select fn_db_delete_config_value('VdsErrorsFileName','general');
select fn_db_delete_config_value('VM64BitMaxMemorySizeInMB','general');

------------------------------------------------------------------------------------
--                  Split config section
-- The purpose of this section is to treat config option that was once
-- general, and should now be version-specific.
-- To ease this the fn_db_split_config_value can be used, input is the
-- option_name, the old value and the new value. Result is creating one row for each old
-- cluster level with the original value if exists, or the input old value
-- and one row for the newest one, with the input value.
------------------------------------------------------------------------------------
select fn_db_split_config_value('SpiceSecureChannels','smain,sinputs','smain,sinputs,scursor,splayback,srecord,sdisplay');

------------------------------------------------------------------------------------
--                  Simple direct updates section
------------------------------------------------------------------------------------

-- update keys from internal version 2.3 to official 3.0`
update vdc_options set version = '3.0' where version = '2.3';

-- update bootstrap path

update vdc_options set option_value = replace (option_value , '/4/6', '')
where option_name = 'VdcBootStrapUrl';

------------------------------------------------------------------------------------
--                 complex updates using a temporary function section
--                 each temporary function name should start with __temp
------------------------------------------------------------------------------------

-- remove default security authentication

CREATE OR REPLACE FUNCTION __temp_upgrade_remove_default_security_auth(a_input VARCHAR(40))
  RETURNS void AS
$BODY$
   DECLARE
   v_entry VARCHAR(4000);
   v_pos integer;
BEGIN
    v_entry := option_value FROM vdc_options WHERE option_name='LDAPSecurityAuthentication';
    v_pos := strpos(lower(v_entry), ',' || lower(a_input) || ',');

    IF (v_pos = 0) THEN
                UPDATE vdc_options
                SET option_value = regexp_replace(option_value, ',?' || a_input || ',?' ,'','i')
                WHERE option_name = 'LDAPSecurityAuthentication';
    ELSE
                UPDATE vdc_options
                SET option_value = regexp_replace(option_value, ',' || a_input || ',' ,',','i')
                WHERE option_name = 'LDAPSecurityAuthentication';
    END IF;

END; $BODY$
LANGUAGE plpgsql;

SELECT __temp_upgrade_remove_default_security_auth('default:GSSAPI');
SELECT __temp_upgrade_remove_default_security_auth('default:SIMPLE');

DROP FUNCTION __temp_upgrade_remove_default_security_auth(VARCHAR);


--- upgrade domains to have a provider type

create or replace function __temp_update_ldap_provier_types()
RETURNS void
AS $procedure$
    DECLARE
    v_domains text;
    v_provider_types text;
    v_temp text;
    v_values record;
    boo smallint;

BEGIN

    v_temp := '';
    v_domains := (SELECT option_value FROM vdc_options where option_name = 'DomainName');
    v_provider_types := (SELECT option_value FROM vdc_options where option_name = 'LDAPProviderTypes');
    boo := (SELECT count(*) from regexp_matches(v_provider_types ,'[:]'));

    IF (boo = 0) THEN

        FOR v_values in select regexp_split_to_table(v_domains, ',') as val
        LOOP
            IF (length(v_values.val) > 0) THEN
                v_temp := v_temp || v_values.val || ':general,';
            END IF;
        END LOOP;

        v_temp = rtrim(v_temp,',');

        UPDATE vdc_options SET option_value = v_temp where option_name = 'LDAPProviderTypes';

    END IF;

END; $procedure$
LANGUAGE plpgsql;

SELECT  __temp_update_ldap_provier_types();
DROP FUNCTION __temp_update_ldap_provier_types();

