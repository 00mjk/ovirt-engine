package org.ovirt.engine.core.bll.scheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.ovirt.engine.core.bll.scheduling.policyunits.CPUPolicyUnit;
import org.ovirt.engine.core.bll.scheduling.policyunits.CpuLevelFilterPolicyUnit;
import org.ovirt.engine.core.bll.scheduling.policyunits.EvenDistributionBalancePolicyUnit;
import org.ovirt.engine.core.bll.scheduling.policyunits.EvenDistributionWeightPolicyUnit;
import org.ovirt.engine.core.bll.scheduling.policyunits.HostedEngineHAClusterFilterPolicyUnit;
import org.ovirt.engine.core.bll.scheduling.policyunits.HostedEngineHAClusterWeightPolicyUnit;
import org.ovirt.engine.core.bll.scheduling.policyunits.MemoryPolicyUnit;
import org.ovirt.engine.core.bll.scheduling.policyunits.NetworkPolicyUnit;
import org.ovirt.engine.core.bll.scheduling.policyunits.NoneBalancePolicyUnit;
import org.ovirt.engine.core.bll.scheduling.policyunits.NoneWeightPolicyUnit;
import org.ovirt.engine.core.bll.scheduling.policyunits.PinToHostPolicyUnit;
import org.ovirt.engine.core.bll.scheduling.policyunits.PowerSavingBalancePolicyUnit;
import org.ovirt.engine.core.bll.scheduling.policyunits.PowerSavingWeightPolicyUnit;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.common.scheduling.PolicyUnitType;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class PolicyUnitImpl extends PolicyUnit {
    public static final int MaxSchedulerWeight = Config.<Integer> getValue(ConfigValues.MaxSchedulerWeight);;

    public static PolicyUnitImpl getPolicyUnitImpl(PolicyUnit policyUnit) {
        switch (policyUnit.getName()) {
        case "PinToHost":
            return new PinToHostPolicyUnit(policyUnit);
        case "CPU":
            return new CPUPolicyUnit(policyUnit);
        case "Memory":
            return new MemoryPolicyUnit(policyUnit);
        case "Network":
            return new NetworkPolicyUnit(policyUnit);
        case "HA":
            if (policyUnit.getPolicyUnitType() == PolicyUnitType.Weight) {
                return new HostedEngineHAClusterWeightPolicyUnit(policyUnit);
            } else if (policyUnit.getPolicyUnitType() == PolicyUnitType.Filter) {
                return new HostedEngineHAClusterFilterPolicyUnit(policyUnit);
            }
        case "CPU-Level":
            return new CpuLevelFilterPolicyUnit(policyUnit);
        case "None":
            if (policyUnit.getPolicyUnitType() == PolicyUnitType.Weight) {
                return new NoneWeightPolicyUnit(policyUnit);
            }
            else if (policyUnit.getPolicyUnitType() == PolicyUnitType.LoadBalancing) {
                return new NoneBalancePolicyUnit(policyUnit);
            }
            break;
        case "OptimalForPowerSaving":
            if (policyUnit.getPolicyUnitType() == PolicyUnitType.Weight) {
                return new PowerSavingWeightPolicyUnit(policyUnit);
            }
            else if (policyUnit.getPolicyUnitType() == PolicyUnitType.LoadBalancing) {
                return new PowerSavingBalancePolicyUnit(policyUnit);
            }
            break;
        case "OptimalForEvenDistribution":
            if (policyUnit.getPolicyUnitType() == PolicyUnitType.Weight) {
                return new EvenDistributionWeightPolicyUnit(policyUnit);
            }
            else if (policyUnit.getPolicyUnitType() == PolicyUnitType.LoadBalancing) {
                return new EvenDistributionBalancePolicyUnit(policyUnit);
            }
            break;
        default:
            break;
        }
        throw new NotImplementedException("policyUnit: " + policyUnit.getName());
    }

    protected static final Log log = LogFactory.getLog(PolicyUnitImpl.class);
    private final PolicyUnit policyUnit;
    protected VdsFreeMemoryChecker memoryChecker;

    public PolicyUnitImpl(PolicyUnit policyUnit) {
        this.policyUnit = policyUnit;
    }

    public List<VDS> filter(List<VDS> hosts, VM vm, Map<String, String> parameters, List<String> messages) {
        log.error("policy unit:" + getName() + "filter is not implemented");
        return hosts;
    }

    public List<Pair<Guid, Integer>> score(List<VDS> hosts, VM vm, Map<String, String> parameters) {
        log.error("policy unit:" + getPolicyUnit().getName() + "function is not implemented");
        List<Pair<Guid, Integer>> pairs = new ArrayList<Pair<Guid, Integer>>();
        for (VDS vds : hosts) {
            pairs.add(new Pair<Guid, Integer>(vds.getId(), 1));
        }
        return pairs;
    }

    public Pair<List<Guid>, Guid> balance(VDSGroup cluster,
            List<VDS> hosts,
            Map<String, String> parameters,
            ArrayList<String> messages) {
        log.error("policy unit:" + getName() + "balance is not implemented");
        return null;
    }

    @Override
    public final Guid getId() {
        return policyUnit.getId();
    }

    @Override
    public final void setId(Guid id) {
        policyUnit.setId(id);
    }

    @Override
    public final String getName() {
        return policyUnit.getName();
    }

    @Override
    public final void setName(String name) {
        policyUnit.setName(name);
    }

    @Override
    public String getDescription() {
        return policyUnit.getDescription();
    }

    @Override
    public void setDescription(String description) {
        policyUnit.setDescription(description);
    }

    @Override
    public final boolean isInternal() {
        return policyUnit.isInternal();
    }

    @Override
    public final void setInternal(boolean internal) {
        policyUnit.setInternal(internal);
    }

    @Override
    public PolicyUnitType getPolicyUnitType() {
        return policyUnit.getPolicyUnitType();
    }

    @Override
    public void setPolicyUnitType(PolicyUnitType policyUnitType) {
        policyUnit.setPolicyUnitType(policyUnitType);
    }

    @Override
    public final Map<String, String> getParameterRegExMap() {
        return policyUnit.getParameterRegExMap();
    }

    @Override
    public final void setParameterRegExMap(Map<String, String> parameterRegExMap) {
        policyUnit.setParameterRegExMap(parameterRegExMap);
    }

    @Override
    public final boolean isEnabled() {
        return policyUnit.isEnabled();
    }

    @Override
    public final void setEnabled(boolean enabled) {
        // TODO Auto-generated method stub
        policyUnit.setEnabled(enabled);
    }

    public final PolicyUnit getPolicyUnit() {
        return policyUnit;
    }

    public void setMemoryChecker(VdsFreeMemoryChecker memoryChecker) {
        this.memoryChecker = memoryChecker;

    }
}
