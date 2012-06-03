package org.ovirt.engine.core.common.queries;

import org.ovirt.engine.core.compat.Guid;

public class GetNewVdsFenceStatusParameters extends VdcQueryParametersBase {
    private static final long serialVersionUID = -3663389765505476776L;

    public GetNewVdsFenceStatusParameters() {
        _storagePoolId = Guid.Empty;
    }

    public GetNewVdsFenceStatusParameters(Guid vds_id, Guid storagePolId, String managementIp,
            ValueObjectMap fencinOptions, String pmType, String user, String password) {
        _vds_id = vds_id;
        _storagePoolId = storagePolId;
        _managementIp = managementIp;
        _fencingOptions = fencinOptions;
        _pmType = pmType;
        _user = user;
        _password = password;
    }

    private Guid _vds_id;

    public Guid getVdsId() {
        return _vds_id;
    }

    public void setVdsId(Guid value) {
        _vds_id = value;
    }

    private Guid _storagePoolId = new Guid();

    public Guid getStoragePoolId() {
        return _storagePoolId;
    }

    public void setStoragePoolId(Guid value) {
        _storagePoolId = value;
    }

    private String _managementIp;

    public String getManagementIp() {
        return _managementIp;
    }

    public void setManagementIp(String value) {
        _managementIp = value;
    }

    private ValueObjectMap _fencingOptions;

    public ValueObjectMap getFencingOptions() {
        return _fencingOptions;
    }

    public void setFencingOptions(ValueObjectMap value) {
        _fencingOptions = value;
    }

    private String _pmType;

    public String getPmType() {
        return _pmType;
    }

    public void setPmType(String value) {
        _pmType = value;
    }

    private String _user;

    public String getUser() {
        return _user;
    }

    public void setUser(String value) {
        _user = value;
    }

    private String _password;

    public String getPassword() {
        return _password;
    }

    public void setPassword(String value) {
        _password = value;
    }

    private boolean _isNewHost;

    public boolean getIsNewHost() {
        return _isNewHost;
    }
}
