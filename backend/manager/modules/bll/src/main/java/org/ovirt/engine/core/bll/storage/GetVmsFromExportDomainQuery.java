package org.ovirt.engine.core.bll.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.ImportVmCommand;
import org.ovirt.engine.core.bll.QueriesCommandBase;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.storage_domain_static;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.queries.GetAllFromExportDomainQueryParameters;
import org.ovirt.engine.core.common.vdscommands.GetVmsInfoVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.ovf.OvfManager;
import org.ovirt.engine.core.utils.ovf.OvfReaderException;

public class GetVmsFromExportDomainQuery<P extends GetAllFromExportDomainQueryParameters>
        extends QueriesCommandBase<P> {
    public GetVmsFromExportDomainQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        storage_domain_static storage = DbFacade.getInstance().getStorageDomainStaticDao().get(
                getParameters().getStorageDomainId());
        if (storage.getstorage_domain_type() == StorageDomainType.ImportExport) {
            VDSReturnValue retVal = executeVerb();
            buildOvfReturnValue(retVal.getReturnValue());
        } else {
            getQueryReturnValue().setReturnValue(new java.util.ArrayList<VM>());
        }
    }

    protected VDSReturnValue executeVerb() {
        GetVmsInfoVDSCommandParameters tempVar = new GetVmsInfoVDSCommandParameters(
                getParameters().getStoragePoolId());
        tempVar.setStorageDomainId(getParameters().getStorageDomainId());
        tempVar.setVmIdList(getParameters().getIds());
        VDSReturnValue retVal = Backend.getInstance().getResourceManager()
                .RunVdsCommand(VDSCommandType.GetVmsInfo, tempVar);
        return retVal;
    }

    protected boolean isValidExportDomain() {
        storage_domains domain = DbFacade.getInstance().getStorageDomainDao().getForStoragePool(
                getParameters().getStorageDomainId(),
                getParameters().getStoragePoolId());
        if (domain != null && domain.getstorage_domain_type() == StorageDomainType.ImportExport) {
            return true;
        }
        return false;
    }

    protected void buildOvfReturnValue(Object obj) {
        boolean shouldAdd = true;
        ArrayList<String> ovfList = (ArrayList<String>) obj;
        OvfManager ovfManager = new OvfManager();
        ArrayList<VM> vms = new ArrayList<VM>();
        List<VM> existsVms = DbFacade.getInstance().getVmDao().getAll();
        java.util.HashMap<Guid, VM> existsVmDictionary = new java.util.HashMap<Guid, VM>();
        for (VM vm : existsVms) {
            existsVmDictionary.put(vm.getId(), vm);
        }

        if (isValidExportDomain()) {
            VM vm = null;
            for (String ovf : ovfList) {
                try {
                    if (!ovfManager.IsOvfTemplate(ovf)) {
                        vm = new VM();
                        ArrayList<DiskImage> diskImages = new ArrayList<DiskImage>();
                        ArrayList<VmNetworkInterface> interfaces  = new ArrayList<VmNetworkInterface>();
                        ovfManager.ImportVm(ovf, vm, diskImages, interfaces);

                        shouldAdd = getParameters().getGetAll() ? shouldAdd : !existsVmDictionary
                                .containsKey(vm.getId());

                        if (shouldAdd) {
                            // add images
                            vm.setImages(diskImages);
                            // add interfaces
                            vm.setInterfaces(interfaces);

                            // add disk map
                            Map<Guid, List<DiskImage>> images = ImportVmCommand
                                    .getImagesLeaf(diskImages);
                            for (Guid id : images.keySet()) {
                                List<DiskImage> list = images.get(id);
                                vm.getDiskMap().put(id, list.get(list.size() - 1));
                            }
                            vms.add(vm);
                        }
                    }
                } catch (OvfReaderException ex) {
                    AuditLogableBase logable = new AuditLogableBase();
                    logable.AddCustomValue("ImportedVmName", ex.getName());
                    AuditLogDirector.log(logable, AuditLogType.IMPORTEXPORT_FAILED_TO_IMPORT_VM);
                } catch (RuntimeException ex) {
                    AuditLogableBase logable = new AuditLogableBase();
                    logable.AddCustomValue("ImportedVmName", "[Unknown name]");
                    AuditLogDirector.log(logable, AuditLogType.IMPORTEXPORT_FAILED_TO_IMPORT_VM);
                }
            }
        }

        getQueryReturnValue().setReturnValue(vms);
    }
}
