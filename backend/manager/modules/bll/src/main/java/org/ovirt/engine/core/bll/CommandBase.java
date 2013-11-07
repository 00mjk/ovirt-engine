package org.ovirt.engine.core.bll;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ejb.TransactionRolledbackLocalException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.context.CompensationContext;
import org.ovirt.engine.core.bll.context.DefaultCompensationContext;
import org.ovirt.engine.core.bll.context.NoOpCompensationContext;
import org.ovirt.engine.core.bll.interfaces.BackendCommandObjectsHandler;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.bll.job.ExecutionContext;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.quota.InvalidQuotaParametersException;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParametersWrapper;
import org.ovirt.engine.core.bll.quota.QuotaManager;
import org.ovirt.engine.core.bll.quota.QuotaStorageDependent;
import org.ovirt.engine.core.bll.quota.QuotaVdsDependent;
import org.ovirt.engine.core.bll.session.SessionDataContainer;
import org.ovirt.engine.core.bll.tasks.AsyncTaskUtils;
import org.ovirt.engine.core.bll.tasks.SPMAsyncTaskHandler;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionParametersBase.CommandExecutionReason;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskCreationInfo;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskParameters;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskType;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.ActionVersionMap;
import org.ovirt.engine.core.common.businessentities.AsyncTaskResultEnum;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatus;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatusEnum;
import org.ovirt.engine.core.common.businessentities.AsyncTasks;
import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.BusinessEntitySnapshot;
import org.ovirt.engine.core.common.businessentities.BusinessEntitySnapshot.EntityStatusSnapshot;
import org.ovirt.engine.core.common.businessentities.BusinessEntitySnapshot.SnapshotType;
import org.ovirt.engine.core.common.businessentities.IVdsAsyncCommand;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.tags;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.errors.VdcFault;
import org.ovirt.engine.core.common.job.ExternalSystemType;
import org.ovirt.engine.core.common.job.Step;
import org.ovirt.engine.core.common.job.StepEnum;
import org.ovirt.engine.core.common.users.VdcUser;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.ValidationUtils;
import org.ovirt.engine.core.common.vdscommands.SPMTaskGuidBaseVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TimeSpan;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dal.job.ExecutionMessageDirector;
import org.ovirt.engine.core.dao.BusinessEntitySnapshotDAO;
import org.ovirt.engine.core.dao.GenericDao;
import org.ovirt.engine.core.dao.StatusAwareDao;
import org.ovirt.engine.core.dao.VdsSpmIdMapDAO;
import org.ovirt.engine.core.dao.VmAndTemplatesGenerationsDAO;
import org.ovirt.engine.core.utils.Deserializer;
import org.ovirt.engine.core.utils.ReflectionUtils;
import org.ovirt.engine.core.utils.SerializationFactory;
import org.ovirt.engine.core.utils.ThreadLocalParamsContainer;
import org.ovirt.engine.core.utils.lock.EngineLock;
import org.ovirt.engine.core.utils.lock.LockManager;
import org.ovirt.engine.core.utils.lock.LockManagerFactory;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;
import org.ovirt.engine.core.utils.transaction.RollbackHandler;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;
import org.springframework.dao.DataAccessException;


@SuppressWarnings("serial")
public abstract class CommandBase<T extends VdcActionParametersBase> extends AuditLogableBase implements
        RollbackHandler, TransactionMethod<Object> {

    /* Multiplier used to convert GB to bytes or vice versa. */
    protected static final long BYTES_IN_GB = 1024 * 1024 * 1024;
    private static final String DEFAULT_TASK_KEY = "DEFAULT_TASK_KEY";
    private static final String BACKEND_COMMAND_OBJECTS_HANDLER_JNDI_NAME =
            "java:global/engine/bll/Backend!org.ovirt.engine.core.bll.interfaces.BackendCommandObjectsHandler";
    private T _parameters;
    private VdcReturnValueBase _returnValue;
    private CommandActionState _actionState = CommandActionState.EXECUTE;
    private VdcActionType actionType;
    private final List<Class<?>> validationGroups = new ArrayList<Class<?>>();
    private final Guid commandId;
    private boolean quotaChanged = false;
    private String _description = "";
    private TransactionScopeOption scope;
    private TransactionScopeOption endActionScope;
    private List<QuotaConsumptionParameter> consumptionParameters;
    /** Indicates whether the acquired locks should be released after the execute method or not */
    private boolean releaseLocksAtEndOfExecute = true;
    /** Object which is representing a lock that some commands will acquire */
    private EngineLock commandLock;

    protected Log log = LogFactory.getLog(getClass());

    /** The context defines how to monitor the command and handle its compensation */
    private final CommandContext context = new CommandContext();

    /** A map contains the properties for describing the job */
    protected Map<String, String> jobProperties;

    /** Handlers for performing the logical parts of the command */
    private List<SPMAsyncTaskHandler> taskHandlers;

    private Map<Guid, CommandBase<?>> childCommandsMap = new HashMap<>();
    private Map<Guid, Pair<VdcActionType, VdcActionParametersBase>> childCommandInfoMap = new HashMap<>();

    public void addChildCommandInfo(Guid id, VdcActionType vdcActionType, VdcActionParametersBase parameters) {
        childCommandInfoMap.put(id, new Pair<VdcActionType, VdcActionParametersBase>(vdcActionType, parameters));
    }

    protected List<VdcReturnValueBase> executeChildCommands() {
        List<VdcReturnValueBase> results = new ArrayList<>(childCommandsMap.size());
        for (Entry<Guid, CommandBase<?>> entry : childCommandsMap.entrySet()) {
            results.add(executeChildCommand(entry.getKey()));
        }
        return results;
    }

    protected VdcReturnValueBase executeChildCommand(Guid idInCommandsMap) {
        CommandBase<?> command = childCommandsMap.get(idInCommandsMap);
        return getBackendCommandObjectsHandler().runAction(command, getExecutionContext());
    }


    protected CommandActionState getActionState() {
        return _actionState;
    }

    protected CommandBase() {
        commandId = Guid.newGuid();
    }

    protected CommandBase(T parameters) {
        _parameters = parameters;
        // get the user from the session if the user is logged in
        VdcUser user = SessionDataContainer.getInstance().addUserToThreadContext(parameters.getSessionId(), true);
        if (user != null) {
            setCurrentUser(user);
        } else
        // if the user is not logged in, get the user from the command parameters
        // this is used for async task completion to get the user who initiated
        // the task after the user has logged out.
        if (parameters.getParametersCurrentUser() != null) {
            setCurrentUser(parameters.getParametersCurrentUser());
        }
        setCorrelationId(parameters.getCorrelationId());

        Guid commandIdFromParameters = parameters.getCommandId();
        if (commandIdFromParameters == null) {
            commandIdFromParameters = Guid.newGuid();
            getParameters().setCommandId(commandIdFromParameters);
        }

        commandId = commandIdFromParameters;
        taskHandlers = initTaskHandlers();
    }

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected CommandBase(Guid commandId) {
        this.commandId = commandId;
    }

    /**
     * This method should be used only at {@link CommandBase} code for creating
     * and execution {@link CommandBase} objects directly.
     * This is the reason for the method being private and the JNDI name not introduced to
     * the {@link BeanType} enum.
     * @return proxy object to create the {@link CommandBase} objects and run them
     */
    private BackendCommandObjectsHandler getBackendCommandObjectsHandler() {
        try {
            InitialContext ctx = new InitialContext();
            return (BackendCommandObjectsHandler) ctx.lookup(BACKEND_COMMAND_OBJECTS_HANDLER_JNDI_NAME);
        } catch (NamingException e) {
            log.error("Getting backend command objects handler failed" + e.getMessage());
            log.debug("", e);
            return null;
        }
    }

    protected List<SPMAsyncTaskHandler> initTaskHandlers() {
        return null;
    }

    /**
     * Checks if possible to perform rollback using command, and if so performs it
     *
     * @param commandType
     *            command type for the rollback
     * @param params
     *            parameters for the rollback
     * @param context
     *            command context for the rollback
     * @return result of the command execution
     */
    protected VdcReturnValueBase attemptRollback(VdcActionType commandType,
            VdcActionParametersBase params,
            CommandContext rollbackContext) {
        if (canPerformRollbackUsingCommand(commandType, params)) {
            params.setExecutionReason(CommandExecutionReason.ROLLBACK_FLOW);
            params.setTransactionScopeOption(TransactionScopeOption.RequiresNew);
            return getBackend().runInternalAction(commandType, params, rollbackContext);
        }
        return new VdcReturnValueBase();
    }

    protected BackendInternal getBackend() {
        return Backend.getInstance();
    }

    /**
     * Checks if possible to perform rollback using command, and if so performs it
     *
     * @param commandType
     *            command type for the rollback
     * @param params
     *            parameters for the rollback
     * @return result of the command execution
     */
    protected VdcReturnValueBase checkAndPerformRollbackUsingCommand(VdcActionType commandType,
            VdcActionParametersBase params) {
        return attemptRollback(commandType, params, null);
    }

    /**
     * Checks if it is possible to rollback the command using a command (and not VDSM)
     *
     * @param commandType
     *            the rollback command to be executed
     * @param params
     *            parameters for the rollback command
     * @return true if it is possible to run rollback using command
     */
    protected boolean canPerformRollbackUsingCommand
            (VdcActionType commandType,
                    VdcActionParametersBase params) {
        return true;
    }

    /**
     * Create an appropriate compensation context. The default is one that does nothing for command that don't run in a
     * transaction, and a real one for commands that run in a transaction.
     *
     * @param transactionScopeOption
     *            The transaction scope.
     * @param forceCompensation
     * @return The compensation context to use.
     */
    private CompensationContext createCompensationContext(TransactionScopeOption transactionScopeOption,
            boolean forceCompensation) {
        if (transactionScopeOption == TransactionScopeOption.Suppress && !forceCompensation) {
            return NoOpCompensationContext.getInstance();
        }

        DefaultCompensationContext defaultContext = new DefaultCompensationContext();
        defaultContext.setCommandId(commandId);
        defaultContext.setCommandType(getClass().getName());
        defaultContext.setBusinessEntitySnapshotDAO(getBusinessEntitySnapshotDAO());
        defaultContext.setSnapshotSerializer(
                SerializationFactory.getSerializer());
        return defaultContext;
    }

    protected BusinessEntitySnapshotDAO getBusinessEntitySnapshotDAO() {
        return DbFacade.getInstance().getBusinessEntitySnapshotDao();
    }


    protected VmAndTemplatesGenerationsDAO getVmAndTemplatesGenerationsDAO() {
        return DbFacade.getInstance().getVmAndTemplatesGenerationsDao();
    }


    protected VdsSpmIdMapDAO getVdsSpmIdMapDAO() {
        return DbFacade.getInstance().getVdsSpmIdMapDao();
    }

    /**
     * @return the compensationContext
     */
    protected CompensationContext getCompensationContext() {
        return context.getCompensationContext();
    }

    /**
     * @param compensationContext the compensationContext to set
     */
    public void setCompensationContext(CompensationContext compensationContext) {
        context.setCompensationContext(compensationContext);
    }

    public VdcReturnValueBase canDoActionOnly() {
        setActionMessageParameters();
        getReturnValue().setCanDoAction(internalCanDoAction());
        String tempVar = getDescription();
        getReturnValue().setDescription((tempVar != null) ? tempVar : getReturnValue().getDescription());
        return _returnValue;
    }

    public VdcReturnValueBase executeAction() {
        determineExecutionReason();
        _actionState = CommandActionState.EXECUTE;
        String tempVar = getDescription();
        getReturnValue().setDescription((tempVar != null) ? tempVar : getReturnValue().getDescription());
        setActionMessageParameters();
        Step validatingStep=null;
        boolean actionAllowed = false;
        boolean isExternal = this.getParameters().getJobId() != null || this.getParameters().getStepId() != null;
        if (!isExternal) {
            validatingStep = ExecutionHandler.addStep(getExecutionContext(), StepEnum.VALIDATING, null);
        }

        try {
            actionAllowed = getReturnValue().getCanDoAction() || internalCanDoAction();
            if (!isExternal) {
                ExecutionHandler.endStep(getExecutionContext(), validatingStep, actionAllowed);
            }

            if (actionAllowed) {
                execute();
            } else {
                getReturnValue().setCanDoAction(false);
            }
        } finally {
            freeLockExecute();
            if (!getReturnValue().getSucceeded()) {
                clearAsyncTasksWithOutVdsmId();
            }
        }
        return getReturnValue();
    }

    private void clearAsyncTasksWithOutVdsmId() {
        for (Guid asyncTaskId : getReturnValue().getTaskPlaceHolderIdList()) {
            AsyncTasks task = getAsyncTaskDao().get(asyncTaskId);
            if (task != null && Guid.isNullOrEmpty(task.getVdsmTaskId())) {
                AsyncTaskManager.removeTaskFromDbByTaskId(task.getTaskId());
            }
        }
    }

    private void determineExecutionReason() {
        if (getParameters().getExecutionReason() == null) {
            getParameters().setExecutionReason(CommandExecutionReason.REGULAR_FLOW);
        }
    }

    /**
     * Run the default compensation logic (inside a new transaction):<br>
     * <ol>
     * <li>Get all the entity snapshots that this command has created.</li>
     * <li>For each snapshot:</li>
     * <ol>
     * <li>Deserialize the entity.</li>
     * <li>Using the entity DAO:</li>
     * <ul>
     * <li>If the entity was added by the command, remove it.</li>
     * <li>Otherwise, If the entity is not in DB anymore, restore it.</li>
     * <li>Otherwise, update it.</li>
     * </ul>
     * </ol>
     * <li>Remove all the snapshots for this command, since we handled them.</li> </ol>
     */
    protected void compensate() {
        if (hasTaskHandlers()) {
            getParameters().setExecutionReason(CommandExecutionReason.ROLLBACK_FLOW);
            getCurrentTaskHandler().compensate();
            revertPreviousHandlers();
        } else {
            internalCompensate();
        }
    }

    @SuppressWarnings({ "unchecked", "synthetic-access" })
    protected final void internalCompensate() {
        try {
            if (isQuotaDependant()) {
                rollbackQuota();
            }
        } catch (NullPointerException e) {
            log.debug("RollbackQuota: failed (may be because quota is disabled)", e);
        }
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Object>() {
            @Override
            public Object runInTransaction() {
                Deserializer deserializer =
                        SerializationFactory.getDeserializer();
                List<BusinessEntitySnapshot> entitySnapshots =
                        getBusinessEntitySnapshotDAO().getAllForCommandId(commandId);
                log.debugFormat("Command [id={0}]: {1} compensation data.", commandId,
                        entitySnapshots.isEmpty() ? "No" : "Going over");
                for (BusinessEntitySnapshot snapshot : entitySnapshots) {
                    Class<Serializable> snapshotClass =
                            (Class<Serializable>) ReflectionUtils.getClassFor(snapshot.getSnapshotClass());
                    Serializable snapshotData = deserializer.deserialize(snapshot.getEntitySnapshot(), snapshotClass);
                    log.infoFormat("Command [id={0}]: Compensating {1} of {2}; snapshot: {3}.",
                            commandId,
                            snapshot.getSnapshotType(),
                            snapshot.getEntityType(),
                            (snapshot.getSnapshotType() == SnapshotType.CHANGED_ENTITY ? "id=" + snapshot.getEntityId()
                                    : snapshotData.toString()));
                    Class<BusinessEntity<Serializable>> entityClass =
                            (Class<BusinessEntity<Serializable>>) ReflectionUtils.getClassFor(snapshot.getEntityType());
                    GenericDao<BusinessEntity<Serializable>, Serializable> daoForEntity =
                            DbFacade.getInstance().getDaoForEntity(entityClass);

                    switch (snapshot.getSnapshotType()) {
                    case CHANGED_STATUS_ONLY:
                        EntityStatusSnapshot entityStatusSnapshot = (EntityStatusSnapshot) snapshotData;
                        ((StatusAwareDao<Serializable, Enum<?>>) daoForEntity).updateStatus(
                                entityStatusSnapshot.getId(), entityStatusSnapshot.getStatus());
                        break;
                    case CHANGED_ENTITY:
                        BusinessEntity<Serializable> entitySnapshot = (BusinessEntity<Serializable>) snapshotData;
                        if (daoForEntity.get(entitySnapshot.getId()) == null) {
                            daoForEntity.save(entitySnapshot);
                        } else {
                            daoForEntity.update(entitySnapshot);
                        }
                        break;
                    case NEW_ENTITY_ID:
                        daoForEntity.remove(snapshotData);
                        break;
                    }
                }

                cleanUpCompensationData();
                return null;
            }

        });
    }

    /**
     * Delete the compensation data, so that we don't accidentaly try to compensate it at a later time.
     */
    private void cleanUpCompensationData() {
        if (!(getCompensationContext() instanceof NoOpCompensationContext)) {
            getBusinessEntitySnapshotDAO().removeAllForCommandId(commandId);
        }
    }

    public VdcReturnValueBase endAction() {
        if (!hasTaskHandlers() || getExecutionIndex() == getTaskHandlers().size() - 1) {
            ExecutionHandler.startFinalizingStep(getExecutionContext());
        }

        try {
            initiateLockEndAction();
            setActionState();
            handleTransactivity();
            TransactionSupport.executeInScope(endActionScope, this);
        } catch (TransactionRolledbackLocalException e) {
            log.infoFormat("EndAction: Transaction was aborted in {0}", this.getClass().getName());
        } finally {
            freeLockEndAction();
            if (getCommandShouldBeLogged()) {
                logCommand();
            }
        }

        return getReturnValue();
    }

    /**
     * The following method should initiate a lock , in order to release it at endAction()
     */
    private void initiateLockEndAction() {
        if (commandLock == null) {
            LockIdNameAttribute annotation = getClass().getAnnotation(LockIdNameAttribute.class);
            if (annotation != null && !annotation.isReleaseAtEndOfExecute()) {
                commandLock = buildLock();
            }

        }
    }

    private void handleTransactivity() {
        scope =
                (getParameters() != null) ? getParameters().getTransactionScopeOption()
                        : TransactionScopeOption.Required;
        endActionScope = scope;
        boolean forceCompensation = getForceCompensation();
        // @NonTransactiveAttribute annotation overrides the scope passed by the
        // command parameters
        if (!getTransactive()) {
            scope = TransactionScopeOption.Suppress;

            // Set the end action scope to suppress only for non-compensating commands, or the end action for commands
            // will run without transaction but compensation is not supported for end action.
            endActionScope = forceCompensation ? endActionScope : scope;
        }

        if (getCompensationContext() == null) {
            context.setCompensationContext(createCompensationContext(scope, forceCompensation));
        }
    }

    private void setActionState() {
        // This mechanism should change,
        // And for ROLLBACK_FLOW we should
        // introduce a new actionState.
        // Currently it was decided that ROLLBACK_FLOW will cause endWithFailure
        if (isEndSuccessfully()) {
            _actionState = CommandActionState.END_SUCCESS;
        } else {
            _actionState = CommandActionState.END_FAILURE;
        }
    }

    protected boolean isEndSuccessfully() {
        return getParameters().getTaskGroupSuccess()
                && getParameters().getExecutionReason() == CommandExecutionReason.REGULAR_FLOW;
    }

    public void endActionInTransactionScope() {
        boolean exceptionOccurred = false;
        try {
            if (isEndSuccessfully()) {
                internalEndSuccessfully();
            } else {
                internalEndWithFailure();
            }
        } catch (RuntimeException e) {
            exceptionOccurred = true;
            throw e;
        } finally {
            freeLockEndAction();
            if (TransactionSupport.current() == null) {

                // In the unusual case that we have no current transaction, try to cleanup after yourself and if the
                // cleanup fails (probably since the transaction is aborted) then try to compensate.
                try {
                    cleanUpCompensationData();
                } catch (RuntimeException e) {
                    logExceptionAndCompensate(e);
                }
            } else {
                try {
                    if (!exceptionOccurred && TransactionSupport.current().getStatus() == Status.STATUS_ACTIVE) {
                        cleanUpCompensationData();
                    } else {
                        compensate();
                    }
                } catch (SystemException e) {
                    logExceptionAndCompensate(e);
                }
            }
        }
    }

    /**
     * Log the exception & call compensate.
     *
     * @param e
     *            The exception to log.
     */
    protected void logExceptionAndCompensate(Exception e) {
        log.errorFormat("Exception while wrapping-up compensation in endAction: {0}.",
                ExceptionUtils.getMessage(e), e);
        compensate();
    }

    private void internalEndSuccessfully() {
        log.infoFormat("Ending command successfully: {0}", getClass().getName());
        if (hasTaskHandlers()) {
            getCurrentTaskHandler().endSuccessfully();
            getParameters().incrementExecutionIndex();
            if (getExecutionIndex() < getTaskHandlers().size()) {
                _actionState = CommandActionState.EXECUTE;
                execute();
            }
        } else {
            endSuccessfully();
        }
    }

    protected void endSuccessfully() {
        setSucceeded(true);
    }

    void logRenamedEntity() {
        if (this instanceof RenamedEntityInfoProvider) {
            RenamedEntityInfoProvider renameable = (RenamedEntityInfoProvider) this;
            String oldEntityName = renameable.getEntityOldName();
            String newEntityName = renameable.getEntityNewName();
            if (!StringUtils.equals(oldEntityName, newEntityName)) {
                // log entity rename details
                AuditLogableBase logable = new AuditLogableBase();
                String entityType = renameable.getEntityType();
                logable.addCustomValue("EntityType", entityType);
                logable.addCustomValue("OldEntityName", oldEntityName);
                logable.addCustomValue("NewEntityName", newEntityName);
                renameable.setEntityId(logable);
                auditLog(logable, AuditLogType.ENTITY_RENAMED);
            }
        }
    }

    void auditLog(AuditLogableBase logable, AuditLogType logType) {
        AuditLogDirector.log(logable, logType);
    }

    private void internalEndWithFailure() {
        log.errorFormat("Ending command with failure: {0}", getClass().getName());
        if (hasTaskHandlers()) {
            if (hasStepsToRevert()) {
                getCurrentTaskHandler().endWithFailure();
                revertPreviousHandlers();
            }
            startPollingAsyncTasks();
        } else {
            endWithFailure();
        }
        rollbackQuota();
    }

    private void rollbackQuota() {
        // Quota accounting is done only in the most external Command.
        if (isQuotaChanged()) {
            List<QuotaConsumptionParameter> consumptionParameters = getQuotaConsumptionParameters();
            if (consumptionParameters != null) {
                for (QuotaConsumptionParameter parameter : consumptionParameters) {
                    getQuotaManager().removeQuotaFromCache(getStoragePool().getId(), parameter.getQuotaGuid());
                }
            }
        }
    }

    protected List<QuotaConsumptionParameter> getQuotaConsumptionParameters() {

        // This a double marking mechanism which was created to ensure Quota dependencies would not be inherited
        // by descendants commands. Each Command is both marked by the QuotaDependency and implements the required
        // Interfaces (NONE does not implement any of the two interfaces).
        // The enum markings prevent Quota dependencies unintentional inheritance.
        if (consumptionParameters == null) {
            switch (getActionType().getQuotaDependency()) {
                case NONE:
                    return null;
                case STORAGE:
                    consumptionParameters = getThisQuotaStorageDependent().getQuotaStorageConsumptionParameters();
                    break;
                case VDS_GROUP:
                    consumptionParameters = getThisQuotaVdsDependent().getQuotaVdsConsumptionParameters();
                    break;
                default:
                    consumptionParameters = getThisQuotaStorageDependent().getQuotaStorageConsumptionParameters();
                    consumptionParameters.addAll(getThisQuotaVdsDependent().getQuotaVdsConsumptionParameters());
                    break;
            }
        }
        return consumptionParameters;
    }

    private QuotaStorageDependent getThisQuotaStorageDependent() {
        return (QuotaStorageDependent) this;
    }

    private QuotaVdsDependent getThisQuotaVdsDependent() {
        return (QuotaVdsDependent) this;
    }

    private void revertPreviousHandlers() {
        getParameters().decrementExecutionIndex();
        if (hasStepsToRevert()) {
            logRollbackedTask();
            getParameters().setExecutionReason(CommandExecutionReason.ROLLBACK_FLOW);
            getCurrentTaskHandler().compensate();

            if (!hasRevertTask()) {
                // If there is no task to take us onwards, just run the previous handler's revert
                revertPreviousHandlers();
            }
        }
        else {
            setSucceeded(true);
        }
    }

    protected void logRollbackedTask() {
        String type = (getCurrentTaskHandler().getRevertTaskType() != null ? getCurrentTaskHandler().getRevertTaskType().name() : AsyncTaskType.unknown.name());
        log.errorFormat("Reverting task {0}, handler: {1}", type, getCurrentTaskHandler().getClass().getName());
    }

    private boolean hasRevertTask() {
        return getCurrentTaskHandler().getRevertTaskType() != null;
    }

    protected void endWithFailure() {
        setSucceeded(true);
        rollbackQuota();
    }

    private boolean internalCanDoAction() {
        boolean returnValue = false;
        try {
            Transaction transaction = TransactionSupport.suspend();
            try {
                returnValue =
                        isUserAuthorizedToRunAction() && isBackwardsCompatible() && validateInputs() && acquireLock()
                                && canDoAction()
                                && internalValidateAndSetQuota();
                if (!returnValue && getReturnValue().getCanDoActionMessages().size() > 0) {
                    log.warnFormat("CanDoAction of action {0} failed. Reasons:{1}", getActionType(),
                            StringUtils.join(getReturnValue().getCanDoActionMessages(), ','));
                }
            } finally {
                TransactionSupport.resume(transaction);
            }
        } catch (DataAccessException dataAccessEx) {
            log.error("Data access error during CanDoActionFailure.", dataAccessEx);
            addCanDoActionMessage(VdcBllMessages.CAN_DO_ACTION_DATABASE_CONNECTION_FAILURE);
        } catch (RuntimeException ex) {
            log.error("Error during CanDoActionFailure.", ex);
            addCanDoActionMessage(VdcBllMessages.CAN_DO_ACTION_GENERAL_FAILURE);
        } finally {
            if (!returnValue) {
                freeLock();
            }
        }
        return returnValue;
    }

    private boolean internalValidateAndSetQuota() {
        // Quota accounting is done only in the most external Command.
        if (isInternalExecution() || !isQuotaDependant()) {
            return true;
        }

        QuotaConsumptionParametersWrapper quotaConsumptionParametersWrapper = new QuotaConsumptionParametersWrapper(this,
                getReturnValue().getCanDoActionMessages());
        quotaConsumptionParametersWrapper.setParameters(getQuotaConsumptionParameters());

        List<QuotaConsumptionParameter> quotaParams = quotaConsumptionParametersWrapper.getParameters();
        if (quotaParams == null) {
            throw new InvalidQuotaParametersException("Command: " + this.getClass().getName()
                    + ". No Quota parameters available.");
        }

        // Some commands are not quotable, given the values of their parameters.
        // e.g AddDisk is storage-quotable but when the disk type is external LUN there is no storage pool to it.
        // scenarios like this must set its QuotaConsumptionParameter to an empty list.
        if (quotaParams.isEmpty()) {
            return true;
        }

        if (getStoragePool() == null) {
            throw new InvalidQuotaParametersException("Command: " + this.getClass().getName()
                    + ". Storage pool is not available for quota calculation. ");
        }

        boolean result = getQuotaManager().consume(quotaConsumptionParametersWrapper);
        setQuotaChanged(result);
        return result;
    }

    private boolean isQuotaDependant() {
        return getActionType().getQuotaDependency() != VdcActionType.QuotaDependency.NONE;
    }

    /**
     * @return true if all parameters class and its inner members passed
     *         validation
     */
    protected boolean validateInputs() {
        return validate(getParameters());
    }

    protected <T> boolean validate(T value) {
        List<String> messages = ValidationUtils.validateInputs(getValidationGroups(), value);
        if (!messages.isEmpty()) {
            getReturnValue().getCanDoActionMessages().addAll(messages);
            return false;
        }
        return true;
    }

    /**
     * Set the parameters for bll messages (such as type and action).
     * The parameters should be initialized through the command that is called,
     * instead set them at the canDoAction()
     */
    protected void setActionMessageParameters() {
        // No-op method for inheritors to implement
    }

    protected List<Class<?>> getValidationGroups() {
        return validationGroups;
    }

    protected List<Class<?>> addValidationGroup(Class<?>... validationGroup) {
        validationGroups.addAll(Arrays.asList(validationGroup));
        return validationGroups;
    }

    protected boolean isBackwardsCompatible() {
        boolean result = true;
        ActionVersionMap actionVersionMap = DbFacade.getInstance()
                .getActionGroupDao().getActionVersionMapByActionType(getActionType());
        // if actionVersionMap not null check cluster level
        // cluster level ok check storage_pool level
        if (actionVersionMap != null
                && ((getVdsGroup() != null && getVdsGroup().getcompatibility_version().compareTo(
                        new Version(actionVersionMap.getcluster_minimal_version())) < 0) ||
                (!"*".equals(actionVersionMap.getstorage_pool_minimal_version()) && getStoragePool() != null && getStoragePool()
                        .getcompatibility_version().compareTo(
                                new Version(actionVersionMap.getstorage_pool_minimal_version())) < 0))) {
            result = false;
            addCanDoActionMessage(VdcBllMessages.ACTION_NOT_SUPPORTED_FOR_CLUSTER_POOL_LEVEL);
        }
        return result;
    }

    /**
     * Checks if the current user is authorized to run the given action on the given object.
     *
     * @param action
     *            the action to check
     * @param object
     *            the object to check
     * @param type
     *            the type of the object to check
     * @return <code>true</code> if the current user is authorized to run the action, <code>false</code> otherwise
     */
    protected boolean checkUserAuthorization(Guid userId,
            final ActionGroup actionGroup,
            final Guid object,
            final VdcObjectType type) {
        // Grant if there is matching permission in the database:
        final Guid permId =
                getDbFacade().getPermissionDao().getEntityPermissions(userId, actionGroup, object, type);
        if (permId != null) {
            if (log.isDebugEnabled()) {
                log.debugFormat("Found permission {0} for user when running {1}, on {2} with id {3}",
                        permId,
                        getActionType(),
                        type.getVdcObjectTranslation(),
                        object);
            }
            return true;
        }

        // Deny otherwise:
        if (log.isDebugEnabled()) {
            log.debugFormat("No permission found for user when running action {0}, on object {1} for action group {2} with id {3}.",
                    getActionType(),
                    type.getVdcObjectTranslation(),
                    actionGroup,
                    object);
        }
        return false;
    }

    /**
     * Checks if the input user and groups is authorized to run the given action on the given object.
     *
     * @param userId
     *            the user to check
     * @param groupIds
     *            the groups to check
     * @param actionGroup
     *            the action group to check
     * @param object
     *            the object to check
     * @param type
     *            the type of the object to check
     * @param ignoreEveryone
     *            if true, the "everyone" will not be considered
     * @return <code>true</code> if the current user is authorized to run the action, <code>false</code> otherwise
     */
    protected boolean checkUserAndGroupsAuthorization(Guid userId,
            String groupIds,
            final ActionGroup actionGroup,
            final Guid object,
            final VdcObjectType type,
            final boolean ignoreEveryone) {
        // Grant if there is matching permission in the database:
        final Guid permId =
                getPermissionDAO().getEntityPermissionsForUserAndGroups(userId, groupIds, actionGroup, object, type, ignoreEveryone);
        if (permId != null) {
            if (log.isDebugEnabled()) {
                log.debugFormat("Found permission {0} for user when running {1}, on {2} with id {3}",
                        permId,
                        getActionType(),
                        type.getVdcObjectTranslation(),
                        object);
            }
            return true;
        }

        // Deny otherwise:
        if (log.isDebugEnabled()) {
            log.debugFormat("No permission found for user when running action {0}, on object {1} for action group {2} with id {3}.",
                    getActionType(),
                    type.getVdcObjectTranslation(),
                    actionGroup,
                    object);
        }
        return false;
    }

    /**
     * Check if current user is authorized to run current action. Skip check if
     * MLA is off or command is internal.
     *
     * @return <code>true</code> if the user is authorized to run the given action,
     *   <code>false</code> otherwise
     */
    protected boolean isUserAuthorizedToRunAction() {
        // Skip check if this is an internal action:
        if (isInternalExecution()) {
            if (log.isDebugEnabled()) {
                log.debugFormat("Permission check skipped for internal action {0}.", getActionType());
            }
            return true;
        }

        // Skip check if multilevel administration is disabled:
        if (!MultiLevelAdministrationHandler.isMultilevelAdministrationOn()) {
            if (log.isDebugEnabled()) {
                log.debugFormat("Permission check for action {0} skipped because multilevel administration is disabled.",
                        getActionType());
            }
            return true;
        }

        // Deny the permissions if there is no logged in user:
        if (getCurrentUser() == null) {
            addCanDoActionMessage(VdcBllMessages.USER_IS_NOT_LOGGED_IN);
            return false;
        }

        // Get identifiers and types of the objects whose permissions have to be
        // checked:
        final List<PermissionSubject> permSubjects = getPermissionCheckSubjects();

        if (permSubjects == null || permSubjects.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debugFormat("The set of objects to check is null or empty for action {0}.", getActionType());
            }
            addCanDoActionMessage(VdcBllMessages.USER_NOT_AUTHORIZED_TO_PERFORM_ACTION);
            return false;
        }

        if (isQuotaDependant()) {
            addQuotaPermissionSubject(permSubjects);
        }

        // If we are here then we should grant the permission:
        return checkPermissions(permSubjects);
    }

    protected boolean checkPermissions(final List<PermissionSubject> permSubjects) {
        for (PermissionSubject permSubject : permSubjects) {
            if (!checkSinglePermission(permSubject, getReturnValue().getCanDoActionMessages())) {
                return false;
            }
        }
        return true;
    }

    final protected boolean checkSinglePermission(PermissionSubject permSubject, Collection<String> messages) {
        final Guid objectId = permSubject.getObjectId();
        final VdcObjectType objectType = permSubject.getObjectType();
        final ActionGroup objectActionGroup = permSubject.getActionGroup();

        // if objectId is null we can't check permission
        if (objectId == null) {
            if (log.isDebugEnabled()) {
                log.debugFormat("The object to check is null for action {0}.", getActionType());
            }
            messages.add(VdcBllMessages.USER_NOT_AUTHORIZED_TO_PERFORM_ACTION.name());
            return false;
        }
        // Check that an action group is defined for this action;
        if (objectActionGroup == null) {
            if (log.isDebugEnabled()) {
                log.debugFormat("No action group is defined for action {0}.", getActionType());
            }
            return false;
        }

        // Check the authorization:
        if (!checkUserAuthorization(getCurrentUser().getUserId(), objectActionGroup, objectId, objectType)) {
            messages.add(permSubject.getMessage().name());
            return false;
        }
        return true;
    }

    public void addQuotaPermissionSubject(List<PermissionSubject> quotaPermissionList) {
        // if quota enforcement is not in HARD_ENFORCEMENT the quota may be null.
        if (!isInternalExecution() && getStoragePool() != null
                && getStoragePool().getQuotaEnforcementType() != QuotaEnforcementTypeEnum.DISABLED
                && getStoragePool().getQuotaEnforcementType() != QuotaEnforcementTypeEnum.SOFT_ENFORCEMENT) {

            List<QuotaConsumptionParameter> consumptionParameters = getQuotaConsumptionParameters();

            if (consumptionParameters != null) {
                for (QuotaConsumptionParameter parameter : getQuotaConsumptionParameters()) {
                    if (parameter.getQuotaGuid() != null && !Guid.Empty.equals(parameter.getQuotaGuid())
                            && !QuotaConsumptionParameter.QuotaAction.RELEASE.equals(parameter.getQuotaAction())) {
                        quotaPermissionList.add(new PermissionSubject(parameter.getQuotaGuid(),
                                VdcObjectType.Quota,
                                ActionGroup.CONSUME_QUOTA,
                                VdcBllMessages.USER_NOT_AUTHORIZED_TO_CONSUME_QUOTA));
                    }
                }
            }
        }
    }

    protected List<tags> getTagsAttachedToObject() {
        // tags_permissions_map
        return new ArrayList<tags>();
    }

    protected boolean canDoAction() {
        return true;
    }

    /**
     * Factory to determine the type of the ReturnValue field
     *
     * @return
     */
    protected VdcReturnValueBase createReturnValue() {
        return new VdcReturnValueBase();
    }

    protected boolean getSucceeded() {
        return getReturnValue().getSucceeded();
    }

    protected void setSucceeded(boolean value) {
        getReturnValue().setSucceeded(value);
    }

    public boolean getCommandShouldBeLogged() {
        return getParameters().getShouldBeLogged();
    }

    public void setCommandShouldBeLogged(boolean value) {
        getParameters().setShouldBeLogged(value);
    }

    protected void setActionReturnValue(Object value) {
        getReturnValue().setActionReturnValue(value);
    }

    protected Object getActionReturnValue() {
        return getReturnValue().getActionReturnValue();
    }

    public TimeSpan getTransactionTimeout() {
        return new TimeSpan(1, 1, 0);
    }

    /**
     * Calculates the proper parameters for the task
     * @param parentCommandType parent command type for which the task is created
     * @param parameters parameter of the creating command
     * @return
     */
    protected VdcActionParametersBase getParametersForTask(VdcActionType parentCommandType,
            VdcActionParametersBase parameters) {
        // If there is no parent command, the command that its type
        // will be stored in the DB for thr task is the one creating the command
        VdcActionParametersBase parentParameters = parameters.getParentParameters();
        if (parentCommandType == VdcActionType.Unknown || parentParameters == null) {
            return parameters;
        }

        // The parent parameters are the ones that are kept for the task.
        // In order to make sure that in case of rollback-by-command, the ROLLBACK
        // flow will be called, the execution reason of the child command is set
        // to the one of the parent command (if its REGULAR_FLOW, the execution
        // reason of the parent command remains REGULAR_FLOW).
        parentParameters.setExecutionReason(parameters.getExecutionReason());
        parentParameters.setCommandType(parentCommandType);
        return parentParameters;
    }

    private boolean executeWithoutTransaction() {
        boolean functionReturnValue = false;
        boolean exceptionOccurred = true;
        try {
            logRunningCommand();
            if (hasTaskHandlers()) {
                getCurrentTaskHandler().execute();
            } else {
                executeCommand();
            }
            functionReturnValue = getSucceeded();
            exceptionOccurred = false;
        } catch (VdcBLLException e) {
            log.error(String.format("Command %1$s throw Vdc Bll exception. With error message %2$s",
                    getClass().getName(),
                    e.getMessage()));
            if (log.isDebugEnabled()) {
                log.debug(String.format("Command %1$s throw Vdc Bll exception", getClass().getName()), e);
            }
            processExceptionToClient(new VdcFault(e, e.getVdsError().getCode()));
        } catch (RuntimeException e) {
            processExceptionToClient(new VdcFault(e, VdcBllErrors.ENGINE));
            log.error(String.format("Command %1$s throw exception", getClass().getName()), e);
        } finally {
            // If we failed to execute due to exception or some other reason, we compensate for the failure.
            if (exceptionOccurred || !getSucceeded()) {
                compensate();
            } else {
                cleanUpCompensationData();
            }
        }
        return functionReturnValue;
    }

    protected TransactionScopeOption getTransactionScopeOption() {
        return getParameters().getTransactionScopeOption();
    }

    /**
     * Log the running command , and log the affected entity id and type (if
     * there are any).
     */
    private void logRunningCommand() {
        // Set start of log for running command.
        StringBuilder logInfo = new StringBuilder("Running command: ")
                .append(getClass().getSimpleName());

        if (hasTaskHandlers()) {
            logInfo.append(" Task handler: ").append(getCurrentTaskHandler().getClass().getSimpleName());
        }

        logInfo.append(" internal: ").append(isInternalExecution()).append(".");

        // Get permissions of object ,to get object id.
        List<PermissionSubject> permissionSubjectList = getPermissionCheckSubjects();

        // Log if there is entry in the permission map.
        if (permissionSubjectList != null && !permissionSubjectList.isEmpty()) {
            // Build entities string for entities affected by this operation.
            StringBuilder logEntityIdsInfo = new StringBuilder();

            // Iterate all over the entities , which should be affected.
            for (PermissionSubject permSubject : permissionSubjectList) {
                if (permSubject.getObjectId() != null) {
                    // Add comma when there are more then one entity
                    // affected.
                    if (logEntityIdsInfo.length() != 0) {
                        logEntityIdsInfo.append(", ");
                    }
                    logEntityIdsInfo.append(" ID: ").append(permSubject.getObjectId())
                            .append(" Type: ").append(permSubject.getObjectType());
                }
            }

            // If found any entities, add the log to the logInfo.
            if (logEntityIdsInfo.length() != 0) {
                // Print all the entities affected.
                logInfo.append(" Entities affected : ").append(
                        logEntityIdsInfo);
            }
        }

        // Log the final appended message to the log.
        log.info(logInfo);
    }

    private void executeActionInTransactionScope() {
        if (TransactionSupport.current() != null) {
            TransactionSupport.registerRollbackHandler(CommandBase.this);
        }

        // If we didn't managed to acquire lock for command or the object wasn't managed to execute properly, then
        // rollback the transaction.
        if (!executeWithoutTransaction()) {
            if (TransactionSupport.current() == null) {
                cancelTasks();
            }

            // we don't want to commit transaction here
            TransactionSupport.setRollbackOnly();
        }
    }

    protected final void execute() {
        getReturnValue().setCanDoAction(true);
        getReturnValue().setIsSyncronious(true);

        if (!hasTaskHandlers() || getExecutionIndex() == 0) {
            ExecutionHandler.addStep(getExecutionContext(), StepEnum.EXECUTING, null);
        }

        try {
            handleTransactivity();
            TransactionSupport.executeInScope(scope, this);
        } catch (TransactionRolledbackLocalException e) {
            log.infoFormat("Transaction was aborted in {0}", this.getClass().getName());
            // Transaction was aborted - we must sure we compensation for all previous applicative stages of the command
            compensate();
        } finally {
            try {
                if (getCommandShouldBeLogged()) {
                    logRenamedEntity();
                    logCommand();
                }
                if (getSucceeded()) {
                    // only after creating all tasks, we can start polling them (we
                    // don't want
                    // to start polling before all tasks were created, otherwise we
                    // might change
                    // the VM/VmTemplate status to 'Down'/'OK' too soon.
                    startPollingAsyncTasks();
                }
            } finally {
                if (!hasTasks() && !ExecutionHandler.checkIfJobHasTasks(getExecutionContext())) {
                    ExecutionHandler.endJob(getExecutionContext(), getSucceeded());
                }
            }
        }
    }

    private boolean hasTasks() {
        return !getReturnValue().getVdsmTaskIdList().isEmpty();
    }

    private boolean getForceCompensation() {
        NonTransactiveCommandAttribute annotation = getClass().getAnnotation(NonTransactiveCommandAttribute.class);
        return annotation != null && annotation.forceCompensation();
    }

    /**
     * This method is called before executeAction to insert the async task
     * placeholders for the child commands.
     */
    protected void insertAsyncTaskPlaceHolders() {
        TransactionSupport.executeInScope(TransactionScopeOption.Required,
                new TransactionMethod<Void>() {
                    @Override
                    public Void runInTransaction() {
                        buildChildCommandInfos();
                        for (Map.Entry<Guid, Pair<VdcActionType, VdcActionParametersBase>> entry : childCommandInfoMap.entrySet()) {
                            CommandBase<?> command =
                                    getBackendCommandObjectsHandler().createAction(entry.getValue().getFirst(),
                                            entry.getValue().getSecond());
                            command.insertAsyncTaskPlaceHolders();
                            childCommandsMap.put(entry.getKey(), command);
                        }
                        return null;
                    }
                });
    }

    protected abstract void executeCommand();

    /**
     * provides the information on child commands
     */
    protected void buildChildCommandInfos() {
    }

    /**
     * calls execute action the child command.
     * @param command
     * @param parameters
     * @return
     */
    protected VdcReturnValueBase runCommand(CommandBase<?> command) {
        VdcReturnValueBase returnValue = command.executeAction();
        returnValue.setCorrelationId(command.getParameters().getCorrelationId());
        returnValue.setJobId(command.getJobId());
        return returnValue;
    }

    private void logCommand() {
        Class<?> type = getClass();
        InternalCommandAttribute annotation = type.getAnnotation(InternalCommandAttribute.class);
        if (annotation == null) {
            log();
        }
    }

    private boolean getTransactive() {
        NonTransactiveCommandAttribute annotation = getClass().getAnnotation(NonTransactiveCommandAttribute.class);
        return annotation == null;
    }

    public T getParameters() {
        return _parameters;
    }

    public VdcReturnValueBase getReturnValue() {
        if (_returnValue == null) {
            _returnValue = createReturnValue();
        }
        return _returnValue;
    }

    protected VdcActionType getActionType() {
        try {
            if (actionType == null) {
                String name = getClass().getName();
                name = name.substring(0, name.length() - 7);
                name = name.substring(name.lastIndexOf('.') + 1);
                actionType = VdcActionType.valueOf(name);
            }
            return actionType;
        } catch (Exception e) {
            return VdcActionType.Unknown;
        }
    }

    protected String getDescription() {
        return _description;
    }

    protected void setDescription(String value) {
        _description = value;
    }

    private void processExceptionToClient(VdcFault fault) {
        fault.setSessionID(getParameters().getSessionId());
        _returnValue.getExecuteFailedMessages().add(fault.getError().name());
        _returnValue.setFault(fault);
    }

    Map<String, Guid> taskKeyToTaskIdMap = new HashMap<>();

    public Guid persistAsyncTaskPlaceHolder(VdcActionType parentCommand) {
        return persistAsyncTaskPlaceHolder(parentCommand, DEFAULT_TASK_KEY);
    }



    public Guid persistAsyncTaskPlaceHolder(VdcActionType parentCommand, final String taskKey) {
        if (taskKeyToTaskIdMap.containsKey(taskKey)) {
            return taskKeyToTaskIdMap.get(taskKey);
        }

        Guid taskId = Guid.Empty;
        try {
            AsyncTaskCreationInfo creationInfo = new AsyncTaskCreationInfo();
            if (!hasTaskHandlers()) {
                creationInfo.setTaskType(getTaskType());
            } else {
                creationInfo.setTaskType(getCurrentTaskHandler().getTaskType());
            }
            final AsyncTasks task = createAsyncTask(creationInfo, parentCommand);
            taskId = task.getTaskId();
            TransactionScopeOption scopeOption =
                    getTransactive() ? TransactionScopeOption.RequiresNew : TransactionScopeOption.Required;
            TransactionSupport.executeInScope(scopeOption, new TransactionMethod<Void>() {

                @Override
                public Void runInTransaction() {
                    saveTaskAndPutInMap(taskKey, task);
                    return null;
                }
            });
            addToReturnValueTaskPlaceHolderIdList(taskId);
        } catch (RuntimeException ex) {
            log.errorFormat("Error during persistAsyncTaskPlaceHolder for command: {0}. Exception {1}", getClass().getName(), ex);
        }
        return taskId;
    }

    private void saveTaskAndPutInMap(String taskKey, AsyncTasks task) {
        getAsyncTaskDao().save(task);
        taskKeyToTaskIdMap.put(taskKey, task.getTaskId());
    }

    private void addToReturnValueTaskPlaceHolderIdList(Guid taskId) {
        if (!getReturnValue().getTaskPlaceHolderIdList().contains(taskId)) {
            getReturnValue().getTaskPlaceHolderIdList().add(taskId);
        }
    }

    public void deleteAsyncTaskPlaceHolder() {
        deleteAsyncTaskPlaceHolder(DEFAULT_TASK_KEY);
    }

    public void deleteAsyncTaskPlaceHolder(String taskKey) {
        Guid taskId = taskKeyToTaskIdMap.remove(taskKey);
        if (!Guid.isNullOrEmpty(taskId)) {
            AsyncTaskManager.removeTaskFromDbByTaskId(taskId);
        }
    }

    public Guid getAsyncTaskId() {
        return getAsyncTaskId(DEFAULT_TASK_KEY);
    }

    public Guid getAsyncTaskId(String taskKey) {
        if (!taskKeyToTaskIdMap.containsKey(taskKey)) {
            return Guid.Empty;
        }
        return taskKeyToTaskIdMap.get(taskKey);
    }
    /**
     * Use this method in order to create task in the AsyncTaskManager in a safe way. If you use this method within a
     * certain command, make sure that the command implemented the ConcreteCreateTask method.
     *
     * @param asyncTaskCreationInfo
     *            info to send to AsyncTaskManager when creating the task.
     * @param parentCommand
     *            VdcActionType of the command that its EndAction we want to invoke when tasks are finished.
     * @param entityType
     *            type of entities that are associated with the task
     * @param entityIds
     *            Ids of entities to be associated with task
     * @return Guid of the created task.
     */
    protected Guid createTask(Guid taskId,
            AsyncTaskCreationInfo asyncTaskCreationInfo,
            VdcActionType parentCommand,
            Map<Guid, VdcObjectType> entitiesMap) {
        return createTask(taskId, asyncTaskCreationInfo, parentCommand, null, entitiesMap);
    }

    /**
     * Same as {@link #createTask(AsyncTaskCreationInfo, VdcActionType, VdcObjectType, Guid...)}
     * but without suspending the current transaction.
     *
     * Note: it is better to use {@link #createTask(AsyncTaskCreationInfo, VdcActionType, VdcObjectType, Guid...)}
     * since it suspend the current transaction, thus the changes are being updated in the
     * DB right away. call this method only you have a good reason for it and
     * the current transaction is short.
     *
     * @see {@link #createTask(AsyncTaskCreationInfo, VdcActionType, VdcObjectType, Guid...)}
     */
    protected Guid createTaskInCurrentTransaction(Guid taskId,
            AsyncTaskCreationInfo asyncTaskCreationInfo,
            VdcActionType parentCommand,
            VdcObjectType entityType,
            Guid... entityIds) {
        return createTaskImpl(taskId, asyncTaskCreationInfo, parentCommand, null, entityType, entityIds);
    }

    /**
     * Use this method in order to create task in the AsyncTaskManager in a safe way. If you use this method within a
     * certain command, make sure that the command implemented the ConcreteCreateTask method.
     *
     * @param asyncTaskCreationInfo
     *            info to send to AsyncTaskManager when creating the task.
     * @param parentCommand
     *            VdcActionType of the command that its EndAction we want to invoke when tasks are finished.
     * @param entityType
     *            type of entities that are associated with the task
     * @param entityIds
     *            Ids of entities to be associated with task
     * @return Guid of the created task.
     */
    protected Guid createTask(Guid taskId, AsyncTaskCreationInfo asyncTaskCreationInfo,
            VdcActionType parentCommand) {
        return createTask(taskId,
                asyncTaskCreationInfo,
                parentCommand,
                null,
                // The reason Collections.emptyMap is not used here as
                // the map should be mutable
                new HashMap<Guid, VdcObjectType>());
    }

    protected Guid createTask(Guid taskId, AsyncTaskCreationInfo asyncTaskCreationInfo,
            VdcActionType parentCommand,
            VdcObjectType vdcObjectType, Guid... entityIds) {
        return createTask(taskId,
                asyncTaskCreationInfo,
                parentCommand,
                createEntitiesMapForSingleEntityType(vdcObjectType, entityIds));
    }

    protected Guid createTask(Guid taskId, AsyncTaskCreationInfo asyncTaskCreationInfo,
            VdcActionType parentCommand,
            String description, VdcObjectType entityType, Guid... entityIds) {
        return createTask(taskId,
                asyncTaskCreationInfo,
                parentCommand,
                description,
                createEntitiesMapForSingleEntityType(entityType, entityIds));
    }

    /**
     * Use this method in order to create task in the AsyncTaskManager in a safe way. If you use this method within a
     * certain command, make sure that the command implemented the ConcreteCreateTask method.
     *
     * @param asyncTaskCreationInfo
     *            info to send to AsyncTaskManager when creating the task.
     * @param parentCommand
     *            VdcActionType of the command that its EndAction we want to invoke when tasks are finished.
     * @param description
     *            A message which describes the task
     * @param entitiesMap - map of entities
     */
    protected Guid createTask(Guid taskId, AsyncTaskCreationInfo asyncTaskCreationInfo,
            VdcActionType parentCommand,
            String description, Map<Guid, VdcObjectType> entitiesMap) {

        Transaction transaction = TransactionSupport.suspend();

        try {
            return createTaskImpl(taskId, asyncTaskCreationInfo, parentCommand, description, entitiesMap);
        } catch (RuntimeException ex) {
            log.errorFormat("Error during CreateTask for command: {0}. Exception {1}", getClass().getName(), ex);
        } finally {
            TransactionSupport.resume(transaction);
        }

        return Guid.Empty;
    }

    private Guid createTaskImpl(Guid taskId, AsyncTaskCreationInfo asyncTaskCreationInfo, VdcActionType parentCommand,
            String description, VdcObjectType entityType, Guid... entityIds) {
        return createTaskImpl(taskId,
                asyncTaskCreationInfo,
                parentCommand,
                description,
                createEntitiesMapForSingleEntityType(entityType, entityIds));
    }

    private Guid createTaskImpl(Guid taskId,
            AsyncTaskCreationInfo asyncTaskCreationInfo, VdcActionType parentCommand,
            String description,
            Map<Guid, VdcObjectType> entitiesMap) {
        Step taskStep =
                ExecutionHandler.addTaskStep(getExecutionContext(),
                        StepEnum.getStepNameByTaskType(asyncTaskCreationInfo.getTaskType()),
                        description);
        if (taskStep != null) {
            asyncTaskCreationInfo.setStepId(taskStep.getId());
        }
        SPMAsyncTask task = concreteCreateTask(taskId, asyncTaskCreationInfo, parentCommand);
        task.setEntitiesMap(entitiesMap);
        AsyncTaskUtils.addOrUpdateTaskInDB(task);
        getAsyncTaskManager().lockAndAddTaskToManager(task);
        Guid vdsmTaskId = task.getVdsmTaskId();
        ExecutionHandler.updateStepExternalId(taskStep, vdsmTaskId, ExternalSystemType.VDSM);
        return vdsmTaskId;

    }

    private Map<Guid, VdcObjectType> createEntitiesMapForSingleEntityType(VdcObjectType entityType, Guid... entityIds) {
        Map<Guid, VdcObjectType> entitiesMap = new HashMap<Guid, VdcObjectType>();
        for (Guid entityId : entityIds) {
            entitiesMap.put(entityId, entityType);
        }
        return entitiesMap;
    }

    /**
     * Create the {@link SPMAsyncTask} object to be run
     * @param taskId the id of the async task place holder in the database
     * @param asyncTaskCreationInfo Info on how to create the task
     * @param parentCommand The type of command issuing the task
     * @return An {@link SPMAsyncTask} object representing the task to be run
     */
    public SPMAsyncTask concreteCreateTask(
            Guid taskId,
            AsyncTaskCreationInfo asyncTaskCreationInfo,
            VdcActionType parentCommand) {
        AsyncTaskParameters p =
                new AsyncTaskParameters(asyncTaskCreationInfo,
                getAsyncTask(taskId, asyncTaskCreationInfo, parentCommand));
        p.setEntityInfo(getParameters().getEntityInfo());
        return CreateTask(internalGetTaskType(), p);
    }

    public SPMAsyncTask CreateTask(AsyncTaskType taskType, AsyncTaskParameters taskParameters) {
        return AsyncTaskFactory.construct(taskType, taskParameters, false);
    }

    private AsyncTasks getAsyncTask(
            Guid taskId,
            AsyncTaskCreationInfo asyncTaskCreationInfo,
            VdcActionType parentCommand) {
        AsyncTasks asyncTask = null;
        if (!taskId.equals(Guid.Empty)) {
            asyncTask = DbFacade.getInstance().getAsyncTaskDao().get(taskId);
        }
        if (asyncTask != null) {
            VdcActionParametersBase parentParameters = getParentParameters(parentCommand);
            asyncTask.setaction_type(parentCommand);
            asyncTask.setVdsmTaskId(asyncTaskCreationInfo.getVdsmTaskId());
            asyncTask.setActionParameters(parentParameters);
            asyncTask.setTaskParameters(getParameters());
            asyncTask.setStepId(asyncTaskCreationInfo.getStepId());
            asyncTask.setCommandId(getCommandId());
            asyncTask.setRootCommandId(parentParameters.getCommandId());
            asyncTask.setStoragePoolId(asyncTaskCreationInfo.getStoragePoolID());
            asyncTask.setTaskType(asyncTaskCreationInfo.getTaskType());
        } else {
            asyncTask = createAsyncTask(asyncTaskCreationInfo, parentCommand);
        }
        return asyncTask;
    }

    private VdcActionParametersBase getParentParameters(VdcActionType parentCommand) {
        VdcActionParametersBase parentParameters = getParametersForTask(parentCommand, getParameters());
        if (parentParameters.getParametersCurrentUser() == null && getCurrentUser() != null) {
            parentParameters.setParametersCurrentUser(getCurrentUser());
        }
        return parentParameters;
    }

    private AsyncTasks createAsyncTask(
            AsyncTaskCreationInfo asyncTaskCreationInfo,
            VdcActionType parentCommand) {
        VdcActionParametersBase parentParameters = getParentParameters(parentCommand);
        return new AsyncTasks(parentCommand,
                AsyncTaskResultEnum.success,
                AsyncTaskStatusEnum.running,
                asyncTaskCreationInfo.getVdsmTaskId(),
                parentParameters,
                getParameters(),
                asyncTaskCreationInfo.getStepId(),
                getCommandId(),
                parentParameters.getCommandId(),
                asyncTaskCreationInfo.getStoragePoolID(),
                asyncTaskCreationInfo.getTaskType());
    }

    /** @return The type of task that should be created for this command. Commands that do not create async tasks should throw a {@link UnsupportedOperationException} */
    private AsyncTaskType internalGetTaskType() {
        if (hasTaskHandlers()) {
            if (getParameters().getExecutionReason() == CommandExecutionReason.REGULAR_FLOW) {
                return getCurrentTaskHandler().getTaskType();
            }
            return getCurrentTaskHandler().getRevertTaskType();
        }
        return getTaskType();
    }

    /** @return The type of task that should be created for this command. Commands that do not create async tasks should throw a {@link UnsupportedOperationException} */
    protected AsyncTaskType getTaskType() {
        throw new UnsupportedOperationException();
    }

    protected void startPollingAsyncTasks(Collection<Guid> taskIds) {
        for (Guid taskID : taskIds) {
            getAsyncTaskManager().StartPollingTask(taskID);
        }
    }

    protected void startPollingAsyncTasks() {
        startPollingAsyncTasks(getReturnValue().getVdsmTaskIdList());
    }

    protected ArrayList<Guid> getTaskIdList() {
        return getParameters().getParentCommand() != VdcActionType.Unknown ? getReturnValue().getInternalVdsmTaskIdList()
                : getReturnValue().getVdsmTaskIdList();
    }

    @Override
    public void rollback() {
        log.errorFormat("Transaction rolled-back for command: {0}.", CommandBase.this.getClass().getName());
        try {
            if (isQuotaDependant()) {
                rollbackQuota();
            }
        } catch (NullPointerException e) {
            log.error("RollbackQuota: failed (may be because quota is disabled)", e);
        }
        cancelTasks();
    }

    private void cancelTasks() {
        if (hasTasks()) {
            ThreadPoolUtil.execute(new Runnable() {
                @Override
                public void run() {
                    log.infoFormat("Rollback for command: {0}.", CommandBase.this.getClass().getName());
                    try {
                        getAsyncTaskManager().CancelTasks(getReturnValue().getVdsmTaskIdList());
                    } catch (Exception e) {
                        log.errorFormat("Failed to cancel tasks for command: {0}.",
                                CommandBase.this.getClass().getName());
                    }
                }
            });
        }
    }

    protected void revertTasks() {
        if (getParameters().getVdsmTaskIds() != null) {
            // list to send to the PollTasks method
            ArrayList<Guid> taskIdAsList = new ArrayList<Guid>();

            for (Guid taskId : getParameters().getVdsmTaskIds()) {
                taskIdAsList.add(taskId);
                ArrayList<AsyncTaskStatus> tasksStatuses = getAsyncTaskManager().PollTasks(
                        taskIdAsList);
                // call revert task only if ended successfully
                if (tasksStatuses.get(0).getTaskEndedSuccessfully()) {
                    getBackend().getResourceManager().RunVdsCommand(
                            VDSCommandType.SPMRevertTask,
                            new SPMTaskGuidBaseVDSCommandParameters(
                                    getStoragePool().getId(), taskId));
                }
                taskIdAsList.clear();
            }
        }
    }

    protected EngineLock getLock() {
        return commandLock;
    }

    protected void setLock(EngineLock lock) {
        commandLock = lock;
    }

    protected boolean acquireLock() {
        LockIdNameAttribute annotation = getClass().getAnnotation(LockIdNameAttribute.class);
        boolean returnValue = true;
        if (annotation != null) {
            releaseLocksAtEndOfExecute = annotation.isReleaseAtEndOfExecute();
            if (!annotation.isWait()) {
                returnValue = acquireLockInternal();
            } else {
                acquireLockAndWait();
            }
        }
        return returnValue;
    }

    /**
     * The following method should be called after restart of engine during initialization of asynchronous task
     * @return
     */
    public final boolean acquireLockAsyncTask() {
        LockIdNameAttribute annotation = getClass().getAnnotation(LockIdNameAttribute.class);
        boolean returnValue = true;
        if (annotation != null) {
            releaseLocksAtEndOfExecute = annotation.isReleaseAtEndOfExecute();
            if (!releaseLocksAtEndOfExecute) {
                returnValue = acquireLockInternal();
            }
        }
        return returnValue;
    }

    protected boolean acquireLockInternal() {
        // if commandLock is null then we acquire new lock, otherwise probably we got lock from caller command.
        if (commandLock == null) {
            EngineLock lock = buildLock();
            if (lock != null) {
                Pair<Boolean, Set<String>> lockAcquireResult = getLockManager().acquireLock(lock);
                if (lockAcquireResult.getFirst()) {
                    log.infoFormat("Lock Acquired to object {0}", lock);
                    commandLock = lock;
                } else {
                    log.infoFormat("Failed to Acquire Lock to object {0}", lock);
                    getReturnValue().getCanDoActionMessages()
                    .addAll(extractVariableDeclarations(lockAcquireResult.getSecond()));
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This method gets {@link Iterable} of strings that might contain
     * variable declarations inside them, and return a new List in which
     * every variable declaration is extracted to a separate string in
     * order to conform the convention of the can-do-action messages.
     * for example:
     * "ACTION_TYPE_FAILED_TEMPLATE_IS_USED_FOR_CREATE_VM$VmName MyVm"
     * will be splited to 2 strings:
     * "ACTION_TYPE_FAILED_TEMPLATE_IS_USED_FOR_CREATE_VM" and "$VmName MyVm"
     */
    protected List<String> extractVariableDeclarations(Iterable<String> appendedCanDoMsgs) {
        final List<String> result = new ArrayList<String>();
        Iterator<String> iter = appendedCanDoMsgs.iterator();
        while(iter.hasNext()) {
            result.addAll(Arrays.asList(iter.next().split("(?=\\$)")));
        }
        return result;
    }

    private EngineLock buildLock() {
        EngineLock lock = null;
        Map<String, Pair<String, String>> exclusiveLocks = getExclusiveLocks();
        Map<String, Pair<String, String>> sharedLocks = getSharedLocks();
        if (exclusiveLocks != null || sharedLocks != null) {
            lock = new EngineLock(exclusiveLocks, sharedLocks);
        }
        return lock;
    }

    private void acquireLockAndWait() {
        // if commandLock is null then we acquire new lock, otherwise probably we got lock from caller command.
        if (commandLock == null) {
            Map<String, Pair<String, String>> exclusiveLocks = getExclusiveLocks();
            if (exclusiveLocks != null) {
                EngineLock lock = new EngineLock(exclusiveLocks, null);
                getLockManager().acquireLockWait(lock);
                commandLock = lock;
            }
        }
    }

    private void freeLockExecute() {
        if (releaseLocksAtEndOfExecute || !getSucceeded() ||
                (!hasTasks() && !(this instanceof IVdsAsyncCommand))) {
            freeLock();
        }
    }

    /**
     * If the command has more than one task handler, we can reach the end action
     * phase and in that phase execute the next task handler. In that case, we
     * don't want to release the locks, so we ask whether we're not in execute state.
     */
    private void freeLockEndAction() {
        if (getActionState() != CommandActionState.EXECUTE) {
            freeLock();
        }
    }

    protected void freeLock() {
        if (commandLock != null) {
            getLockManager().releaseLock(commandLock);
            log.infoFormat("Lock freed to object {0}", commandLock);
            commandLock = null;
        }
    }

    protected LockManager getLockManager() {
        return LockManagerFactory.getLockManager();
    }

    /**
     * The following method should return a map which is represent exclusive lock
     * @return
     */
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        return null;
    }

    /**
     * The following method should return a map which is represent shared lock
     * @return
     */
    protected Map<String, Pair<String, String>> getSharedLocks() {
        return null;
    }

    @Override
    public Object runInTransaction() {
        if (_actionState == CommandActionState.EXECUTE) {
            executeActionInTransactionScope();
        } else {
            endActionInTransactionScope();
        }
        return null;
    }

    /**
     * Use for call chaining of validation commands, so that their result will be validated and kept in the messages if
     * the validation had failed.<br>
     * <br>
     * <b>Example:</b>
     *
     * <pre>
     * boolean isValid = validate(SomeValidator.validateSomething(param1, param2, ...));
     * </pre>
     *
     * @param validationResult
     *            The validation result from the inline call to validate.
     * @return <code>true</code> if the validation was successful, and <code>false</code> if it wasn't.
     */
    protected boolean validate(ValidationResult validationResult) {
        if (!validationResult.isValid()) {
            addCanDoActionMessage(validationResult.getMessage());
            if (validationResult.getVariableReplacements() != null) {
                for (String variableReplacement : validationResult.getVariableReplacements()) {
                    addCanDoActionMessage(variableReplacement);
                }
            }
        }

        return validationResult.isValid();
    }

    /**
     * Add a message to the {@link CommandBase#canDoAction()}'s return value.
     * This return value will be sent to the client for the detailed information
     * of why the action can't be performed.
     *
     * @param message
     *            The message to add.
     */
    protected void addCanDoActionMessage(VdcBllMessages message) {
        getReturnValue().getCanDoActionMessages().add(message.name());
    }

    /**
     * Add validation message with variable replacements and return false.
     *
     * @param message   the message to add
     * @param variableReplacements variable replacements
     * @return  false always
     * @see {@link #addCanDoActionMessage(String)}
     */
    protected final boolean failCanDoAction(VdcBllMessages message, String ... variableReplacements) {
        addCanDoActionMessage(message);
        for (String variableReplacement : variableReplacements) {
            addCanDoActionMessage(variableReplacement);
        }
        return false;
    }

    /**
     * Add a message to the {@link CommandBase#canDoAction()}'s return value.
     * This return value will be sent to the client for the detailed information of why the action can't be performed.
     *
     * @param message The message to add.
     */
    protected void addCanDoActionMessage(String message) {
        getReturnValue().getCanDoActionMessages().add(message);
    }

    /**
     * Run the given command in the VDS and return the VDS's response.
     *
     * @param commandType
     *            The command to run.
     * @param parameters
     *            The corresponding parameters for the command.
     * @return The return from the VDS, containing success/failure, async task ids (in case of success), or error data
     *         (in case of failure).
     * @throws VdcBLLException
     *             In case of an unhandled exception (Usually more severe than failure of the command, because we don't
     *             know why).
     */
    protected VDSReturnValue runVdsCommand(VDSCommandType commandType, VDSParametersBase parameters)
            throws VdcBLLException {
        return getBackend().getResourceManager().RunVdsCommand(commandType, parameters);
    }

    /**
     * Permissions are attached to object so every command must declare its
     * object target type and its GUID
     *
     * @return Map of GUIDs to Object types
     */
    public abstract List<PermissionSubject> getPermissionCheckSubjects();

    /**
     * Returns the properties which used to populate the job message. The default properties resolving will use
     * {@link #getPermissionCheckSubjects()} to get the entities associated with the command. The property key is the
     * type of the entity by {@code VdcObjectType.name()} and the value is the name of the entity or the entity
     * {@code Guid} in case non-resolvable entity name.
     *
     * @return A map which contains the data to be used to populate the {@code Job} description.
     */
    public Map<String, String> getJobMessageProperties() {
        jobProperties = new HashMap<>();
        List<PermissionSubject> subjects = getPermissionCheckSubjects();
        if (!subjects.isEmpty()) {
            VdcObjectType entityType;
            Guid entityId;
            String value;
            for (PermissionSubject permSubject : subjects) {
                entityType = permSubject.getObjectType();
                entityId = permSubject.getObjectId();
                if (entityType != null && entityId != null) {
                    value = DbFacade.getInstance().getEntityNameByIdAndType(entityId, entityType);
                    if (value == null) {
                        value = entityId.toString();
                    }
                    jobProperties.put(entityType.name().toLowerCase(), value);
                }
            }
        }
        return jobProperties;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
        context.setExecutionContext(executionContext);
    }

    public ExecutionContext getExecutionContext() {
        return context.getExecutionContext();
    }

    public Guid getCommandId() {
        return commandId;
    }

    public void setContext(CommandContext context) {
        if (context == null) {
            return;
        }

        CompensationContext compensationContext = context.getCompensationContext();
        if (compensationContext != null) {
            setCompensationContext(compensationContext);
        }

        ExecutionContext executionContext = context.getExecutionContext();
        if (executionContext != null) {
            setExecutionContext(executionContext);
            if (executionContext.getJob() != null) {
                setJobId(executionContext.getJob().getId());
            } else if (executionContext.getStep() != null) {
                setJobId(executionContext.getStep().getJobId());
            }
        }

        if (commandLock == null) {
            commandLock = context.getLock();
        }
    }

    /**
     * Adds a sub step on the current execution context by providing parent and new step information and step description
     * @param parentStep parent step to add the new sub step on
     * @param newStep step to add
     * @param description  description of step to be added
     * @return
     */
    protected Step addSubStep(StepEnum parentStep, StepEnum newStep, String description) {
        return ExecutionHandler.addSubStep(getExecutionContext(),
                (getExecutionContext().getJob() != null) ? getExecutionContext().getJob().getStep(parentStep)
                        : getExecutionContext().getStep(),
                newStep,
                description);
    }

    /**
     * Adds a sub step on the current execution context by providing parent and new step information and map that will be resolved to create a text message that describes the new step
     * @param parentStep parent step to add the new sub step on
     * @param newStep step to add
     * @param map of values that will be used to compose the description of the step
     * @return
     */
    protected Step addSubStep(StepEnum parentStep, StepEnum newStep, Map<String, String> valuesMap) {
        return addSubStep(parentStep, newStep, ExecutionMessageDirector.resolveStepMessage(newStep, valuesMap));
    }

    protected QuotaManager getQuotaManager() {
        return QuotaManager.getInstance();
    }

    protected AsyncTaskManager getAsyncTaskManager() {
        return AsyncTaskManager.getInstance();
    }

    protected List<SPMAsyncTaskHandler> getTaskHandlers() {
        return taskHandlers;
    }

    protected boolean hasTaskHandlers() {
        return getTaskHandlers() != null;
    }

    protected SPMAsyncTaskHandler getCurrentTaskHandler() {
        return getTaskHandlers().get(getExecutionIndex());
    }

    private int getExecutionIndex() {
        return getParameters().getExecutionIndex();
    }

    private boolean hasStepsToRevert() {
        return getExecutionIndex() >= 0;
    }

    public boolean isQuotaChanged() {
        return quotaChanged;
    }

    public void setQuotaChanged(boolean quotaChanged) {
        this.quotaChanged = quotaChanged;
    }

    @Override
    public void setCorrelationId(String correlationId) {
        // correlation ID thread local variable is set for non multi-action
        if (!_parameters.getMultipleAction()) {
            ThreadLocalParamsContainer.setCorrelationId(correlationId);
        }
        super.setCorrelationId(correlationId);
    }
}
