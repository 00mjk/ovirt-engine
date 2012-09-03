package org.ovirt.engine.core.bll;

import java.util.List;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.quota.Quotable;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.UpdateVmTemplateParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.validation.group.UpdateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;


public class UpdateVmTemplateCommand<T extends UpdateVmTemplateParameters> extends VmTemplateCommand<T>
        implements Quotable {
    private VmTemplate mOldTemplate;

    public UpdateVmTemplateCommand(T parameters) {
        super(parameters);
        setVmTemplate(parameters.getVmTemplateData());
        setVmTemplateId(getVmTemplate().getId());
        setVdsGroupId(getVmTemplate().getvds_group_id());
        if (getVdsGroup() != null) {
            setStoragePoolId(getVdsGroup().getstorage_pool_id() != null ? getVdsGroup().getstorage_pool_id()
                        .getValue() : Guid.Empty);
        }
    }

    @Override
    protected boolean canDoAction() {
        boolean returnValue = false;
        mOldTemplate = DbFacade.getInstance().getVmTemplateDAO().get(getVmTemplate().getId());
        VmTemplateHandler.UpdateDisksFromDb(mOldTemplate);
        if (mOldTemplate != null) {
            if (VmTemplateHandler.BlankVmTemplateId.equals(mOldTemplate.getId())) {
                addCanDoActionMessage(VdcBllMessages.VMT_CANNOT_EDIT_BLANK_TEMPLATE.toString());
            } else if (!StringHelper.EqOp(mOldTemplate.getname(), getVmTemplate().getname())
                    && isVmTemlateWithSameNameExist(getVmTemplateName())) {
                addCanDoActionMessage(VdcBllMessages.VMT_CANNOT_CREATE_DUPLICATE_NAME);
            } else {
                if (getVdsGroup() == null) {
                    addCanDoActionMessage(VdcBllMessages.VMT_CLUSTER_IS_NOT_VALID);
                } else if (VmHandler.isMemorySizeLegal(mOldTemplate.getos(),
                        mOldTemplate.getmem_size_mb(),
                        getReturnValue()
                                .getCanDoActionMessages(),
                        getVdsGroup().getcompatibility_version().toString())) {
                    if (IsVmPriorityValueLegal(getParameters().getVmTemplateData().getpriority(), getReturnValue()
                            .getCanDoActionMessages())
                            && IsDomainLegal(getParameters().getVmTemplateData().getdomain(), getReturnValue()
                                    .getCanDoActionMessages())) {
                        returnValue = VmTemplateHandler.mUpdateVmTemplate.IsUpdateValid(mOldTemplate, getVmTemplate());
                        if (!returnValue) {
                            addCanDoActionMessage(VdcBllMessages.VMT_CANNOT_UPDATE_ILLEGAL_FIELD);
                        }
                    }
                }
            }
        }

        // Check that the USB policy is legal
        if (returnValue) {
            returnValue = VmHandler.isUsbPolicyLegal(getParameters().getVmTemplateData().getusb_policy(), getParameters().getVmTemplateData().getos(), getVdsGroup(), getReturnValue().getCanDoActionMessages());
        }

        if (returnValue) {
            returnValue = AddVmCommand.CheckCpuSockets(getParameters().getVmTemplateData().getnum_of_sockets(),
                    getParameters().getVmTemplateData().getcpu_per_socket(), getVdsGroup().getcompatibility_version()
                            .toString(), getReturnValue().getCanDoActionMessages());
        }

        return returnValue;
    }

    @Override
    protected void executeCommand() {
        if (getVmTemplate() != null) {
            UpdateVmTemplate();
            if (getVmTemplate().getstorage_pool_id() != null
                    && !VmTemplateHandler.BlankVmTemplateId.equals(getVmTemplate().getId())) {
                UpdateTemplateInSpm(
                        getVmTemplate().getstorage_pool_id().getValue(),
                        new java.util.ArrayList<VmTemplate>(java.util.Arrays
                                .asList(new VmTemplate[] { getVmTemplate() })));
            }
            setSucceeded(true);
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_UPDATE_VM_TEMPLATE : AuditLogType.USER_FAILED_UPDATE_VM_TEMPLATE;
    }

    private void UpdateVmTemplate() {
        DbFacade.getInstance().getVmTemplateDAO().update(getVmTemplate());
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(UpdateEntity.class);
        return super.getValidationGroups();
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__UPDATE);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM_TEMPLATE);
    }

    @Override
    public boolean validateAndSetQuota() {
        return getQuotaManager().validateQuotaForStoragePool(getStoragePool(),
                getVdsGroupId(),
                getQuotaId(),
                getReturnValue().getCanDoActionMessages());
    }

    @Override
    public void rollbackQuota() {
    }

    @Override
    public Guid getQuotaId() {
        return getParameters().getVmTemplateData().getQuotaId();
    }

    @Override
    public void addQuotaPermissionSubject(List<PermissionSubject> quotaPermissionList) {
        if (getStoragePool() != null &&
                getQuotaId() != null &&
                !getStoragePool().getQuotaEnforcementType().equals(QuotaEnforcementTypeEnum.DISABLED)) {
            VmTemplate template = getVmTemplate();
            if (template != null && !getQuotaId().equals(template.getQuotaId())) {
                quotaPermissionList.add(new PermissionSubject(getQuotaId(),
                        VdcObjectType.Quota,
                        ActionGroup.CONSUME_QUOTA));
            }
        }
    }

}
