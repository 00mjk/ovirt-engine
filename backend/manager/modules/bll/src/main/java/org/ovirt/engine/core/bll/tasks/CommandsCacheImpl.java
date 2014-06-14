package org.ovirt.engine.core.bll.tasks;

import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskType;
import org.ovirt.engine.core.common.businessentities.CommandEntity;
import org.ovirt.engine.core.compat.CommandStatus;
import org.ovirt.engine.core.compat.DateTime;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

import java.util.List;

public class CommandsCacheImpl implements CommandsCache {

    private static final String COMMAND_MAP_NAME = "commandMap";
    CacheWrapper<Guid, CommandEntity> commandMap;
    private volatile boolean cacheInitialized;
    private Object LOCK = new Object();

    public CommandsCacheImpl() {
        commandMap = CacheProviderFactory.<Guid, CommandEntity> getCacheWrapper(COMMAND_MAP_NAME);
    }

    private void initializeCache() {
        if (!cacheInitialized) {
            synchronized(LOCK) {
                if (!cacheInitialized) {
                    List<CommandEntity> cmdEntities = DbFacade.getInstance().getCommandEntityDao().getAll();
                    for (CommandEntity cmdEntity : cmdEntities) {
                        commandMap.put(cmdEntity.getId(), cmdEntity);
                    }
                    cacheInitialized = true;
                }
            }
        }
    }

    @Override
    public CommandEntity get(Guid commandId) {
        initializeCache();
        return commandMap.get(commandId);
    }

    @Override
    public void remove(Guid commandId) {
        commandMap.remove(commandId);
        DbFacade.getInstance().getCommandEntityDao().remove(commandId);
    }

    @Override
    public void put(Guid commandId, Guid rootCommandId, VdcActionType actionType, VdcActionParametersBase params, CommandStatus status, boolean enableCallBack) {
        CommandEntity cmdEntity = buildCommandEntity(commandId, rootCommandId, actionType, params, status, enableCallBack);
        commandMap.put(commandId, cmdEntity);
        DbFacade.getInstance().getCommandEntityDao().saveOrUpdate(cmdEntity);
    }

    private CommandEntity buildCommandEntity(Guid commandId, Guid rootCommandId, VdcActionType actionType, VdcActionParametersBase params, CommandStatus status, boolean enableCallBack) {
        CommandEntity entity = new CommandEntity();
        entity.setId(commandId);
        entity.setRootCommandId(rootCommandId);
        entity.setCommandType(actionType);
        entity.setActionParameters(params);
        entity.setCommandStatus(status);
        return entity;
    }

    public void removeAllCommandsBeforeDate(DateTime cutoff) {
        DbFacade.getInstance().getCommandEntityDao().removeAllBeforeDate(cutoff);
        cacheInitialized = false;
        initializeCache();
    }

    public void updateCommandStatus(Guid commandId, AsyncTaskType taskType, CommandStatus status) {
        CommandEntity cmdEntity = get(commandId);
        if (cmdEntity != null) {
            cmdEntity.setCommandStatus(status);
            if (taskType.equals(AsyncTaskType.notSupported)) {
                DbFacade.getInstance().getCommandEntityDao().saveOrUpdate(cmdEntity);
            }
        }
    }
}
