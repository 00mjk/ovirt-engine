package org.ovirt.engine.core.bll.network.dc;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.ObjectUtils;
import org.ovirt.engine.core.bll.RenamedEntityInfoProvider;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.network.cluster.NetworkClusterHelper;
import org.ovirt.engine.core.bll.validator.NetworkValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddNetworkStoragePoolParameters;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.validation.group.UpdateEntity;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.NetworkUtils;

public class UpdateNetworkCommand<T extends AddNetworkStoragePoolParameters> extends NetworkCommon<T> implements RenamedEntityInfoProvider{
    private Network oldNetwork;

    public UpdateNetworkCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void executeCommand() {
        getNetworkDAO().update(getNetwork());

        for (NetworkCluster clusterAttachment : getNetworkClusterDAO().getAllForNetwork(getNetwork().getId())) {
            NetworkClusterHelper.setStatus(clusterAttachment.getClusterId(), getNetwork());
        }

        if (networkChangedToNonVmNetwork()) {
            removeVnicProfiles();
        }

        setSucceeded(true);
    }

    private boolean networkChangedToNonVmNetwork() {
        return getOldNetwork().isVmNetwork() && !getNetwork().isVmNetwork();
    }

    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__UPDATE);
    }

    @Override
    protected boolean canDoAction() {
        if (onlyPermittedFieldsChanged()) {
            return true;
        }

        NetworkValidator validatorNew = new NetworkValidator(getNetwork());
        UpdateNetworkValidator validatorOld = new UpdateNetworkValidator(getOldNetwork());
        return validate(validatorNew.dataCenterExists())
                && validate(validatorNew.vmNetworkSetCorrectly())
                && validate(validatorNew.stpForVmNetworkOnly())
                && validate(validatorNew.mtuValid())
                && validate(validatorNew.networkPrefixValid())
                && validate(validatorNew.vlanIdNotUsed())
                && validate(validatorOld.networkIsSet())
                && validate(validatorOld.notRenamingManagementNetwork(getNetwork()))
                && validate(validatorNew.networkNameNotUsed())
                && validate(validatorOld.networkNotUsedByVms())
                && validate(validatorOld.networkNotUsedByTemplates())
                && (oldAndNewNetworkIsNotExternal()
                || validate(validatorOld.externalNetworkDetailsUnchanged(getNetwork())));
    }

    /**
     * @return <code>true</code> iff only the description or comment field were changed, otherwise <code>false</code>.
     */
    private boolean onlyPermittedFieldsChanged() {
        Network oldNetwork = getOldNetwork();
        Network newNetwork = getNetwork();

        if (oldNetwork == null || newNetwork == null) {
            return false;
        }

        return Objects.equals(oldNetwork.getName(), newNetwork.getName()) &&
                Objects.equals(oldNetwork.getDataCenterId(), newNetwork.getDataCenterId()) &&
                Objects.equals(oldNetwork.getId(), newNetwork.getId()) &&
                Objects.equals(oldNetwork.getMtu(), newNetwork.getMtu()) &&
                Objects.equals(oldNetwork.getName(), newNetwork.getName()) &&
                Objects.equals(oldNetwork.getProvidedBy(), newNetwork.getProvidedBy()) &&
                Objects.equals(oldNetwork.getStp(), newNetwork.getStp()) &&
                Objects.equals(oldNetwork.getVlanId(), newNetwork.getVlanId()) &&
                Objects.equals(oldNetwork.isVmNetwork(), newNetwork.isVmNetwork());
    }

    private boolean oldAndNewNetworkIsNotExternal() {
        return !getOldNetwork().isExternal() && !getNetwork().isExternal();
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.NETWORK_UPDATE_NETWORK : AuditLogType.NETWORK_UPDATE_NETWORK_FAILED;
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(UpdateEntity.class);
        return super.getValidationGroups();
    }

    private Network getOldNetwork() {
        if (oldNetwork == null) {
            oldNetwork = getNetworkDAO().get(getNetwork().getId());
        }
        return oldNetwork;
    }

    protected static class UpdateNetworkValidator extends NetworkValidator {

        public UpdateNetworkValidator(Network network) {
            super(network);
        }

        public ValidationResult notRenamingManagementNetwork(Network newNetwork) {
            String managementNetwork = NetworkUtils.getEngineNetwork();
            return network.getName().equals(managementNetwork) &&
                    !newNetwork.getName().equals(managementNetwork)
                    ? new ValidationResult(VdcBllMessages.NETWORK_CAN_NOT_REMOVE_DEFAULT_NETWORK)
                    : ValidationResult.VALID;
        }

        /**
         * Check that the external network details that can't be updated were not updated.<br>
         * The check is undefined if both the validator's network and the new network are internal networks.
         *
         * @param newNetwork
         *            The new network definition to check.
         * @return A valid result iff the details that shouldn't be changed remained unchanged, An error otherwise.
         */
        public ValidationResult externalNetworkDetailsUnchanged(Network newNetwork) {
            return ObjectUtils.equals(network.getVlanId(), newNetwork.getVlanId())
                    && network.getMtu() == newNetwork.getMtu()
                    && network.getStp() == newNetwork.getStp()
                    && network.isVmNetwork() == newNetwork.isVmNetwork()
                    && ObjectUtils.equals(network.getProvidedBy(), newNetwork.getProvidedBy())
                    ? ValidationResult.VALID
                    : new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_EXTERNAL_NETWORK_DETAILS_CANNOT_BE_EDITED);
        }

    }

    @Override
    public String getEntityType() {
        return VdcObjectType.Network.getVdcObjectTranslation();
    }

    @Override
    public String getEntityOldName() {
        return getOldNetwork().getName();
    }

    @Override
    public String getEntityNewName() {
        return getParameters().getNetwork().getName();
    }

    @Override
    public void setEntityId(AuditLogableBase logable) {

    }
}
