package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.aaa.AuthenticationProfileRepository;
import org.ovirt.engine.core.aaa.Directory;
import org.ovirt.engine.core.aaa.DirectoryGroup;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.IdParameters;
import org.ovirt.engine.core.common.businessentities.DbGroup;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public abstract class AdGroupsHandlingCommandBase<T extends IdParameters> extends CommandBase<T> {
    private DirectoryGroup mGroup;
    private String mGroupName;

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected AdGroupsHandlingCommandBase(Guid commandId) {
        super(commandId);
    }

    public AdGroupsHandlingCommandBase(T parameters) {
        super(parameters);
    }

    protected Guid getGroupId() {
        return getParameters().getId();
    }

    public String getAdGroupName() {
        if (mGroupName == null && getAdGroup() != null) {
            mGroupName = getAdGroup().getName();
        }
        return mGroupName;
    }

    protected DirectoryGroup getAdGroup() {
        if (mGroup == null && !getGroupId().equals(Guid.Empty)) {
            DbGroup dbGroup = DbFacade.getInstance().getDbGroupDao().get(getGroupId());
            if (dbGroup != null) {
                Directory directory = AuthenticationProfileRepository.getInstance().getDirectory(dbGroup.getDomain());
                mGroup = directory.findGroup(dbGroup.getExternalId());
            }
        }
        return mGroup;
    }

    @Override
    protected String getDescription() {
        return getAdGroupName();
    }

    // TODO to be removed
    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(getGroupId(), VdcObjectType.User,
                getActionType().getActionGroup()));
    }
}
