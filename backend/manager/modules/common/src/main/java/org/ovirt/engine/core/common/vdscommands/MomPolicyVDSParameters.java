package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.common.businessentities.VDS;

/**
 * Parameters used to fine tune MoM policy
 */
public class MomPolicyVDSParameters extends VdsIdVDSCommandParametersBase {
    private boolean enableBalloon;

    public MomPolicyVDSParameters(VDS vds, boolean enableBalloon) {
        super(vds.getId());
        this.enableBalloon = enableBalloon;
    }

    public MomPolicyVDSParameters() {
    }

    public boolean isEnableBalloon() {
        return enableBalloon;
    }

    public void setEnableBalloon(boolean enableBalloon) {
        this.enableBalloon = enableBalloon;
    }
}
