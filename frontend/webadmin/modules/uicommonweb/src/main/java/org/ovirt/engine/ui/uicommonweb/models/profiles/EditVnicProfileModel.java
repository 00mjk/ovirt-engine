package org.ovirt.engine.ui.uicommonweb.models.profiles;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.network.VnicProfile;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.key_value.KeyValueModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class EditVnicProfileModel extends VnicProfileModel {

    public EditVnicProfileModel(EntityModel sourceModel,
            Version dcCompatibilityVersion,
            VnicProfile profile,
            Guid dcId,
            boolean customPropertiesVisible) {
        super(sourceModel, dcCompatibilityVersion, customPropertiesVisible, dcId, profile.getNetworkQosId());
        setTitle(ConstantsManager.getInstance().getConstants().vnicProfileTitle());
        setHashName("edit_vnic_profile"); //$NON-NLS-1$

        setProfile(profile);

        getName().setEntity(profile.getName());
        getDescription().setEntity(profile.getDescription());
        getPortMirroring().setEntity(getProfile().isPortMirroring());
        getPublicUse().setIsAvailable(false);
    }

    public EditVnicProfileModel(EntityModel sourceModel, Version dcCompatibilityVersion, VnicProfile profile, Guid dcId) {
        this(sourceModel, dcCompatibilityVersion, profile, dcId, true);
    }

    public EditVnicProfileModel(VnicProfile profile) {
        this(null, null, profile, null, false);
    }

    @Override
    protected void initCustomProperties() {
        getCustomPropertySheet().deserialize(KeyValueModel
                .convertProperties(getProfile().getCustomProperties()));
    }

    @Override
    protected VdcActionType getVdcActionType() {
        return VdcActionType.UpdateVnicProfile;
    }
}
