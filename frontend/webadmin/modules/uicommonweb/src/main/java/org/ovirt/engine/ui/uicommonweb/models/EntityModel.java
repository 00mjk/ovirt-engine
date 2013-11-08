package org.ovirt.engine.ui.uicommonweb.models;

import org.ovirt.engine.ui.uicommonweb.Convertible;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.ValidationResult;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.EventDefinition;
import org.ovirt.engine.ui.uicompat.IProvidePropertyChangedEvent;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.ProvidePropertyChangedEvent;

@SuppressWarnings("unused")
public class EntityModel<T> extends Model
{

    public static EventDefinition EntityChangedEventDefinition;
    private Event privateEntityChangedEvent;

    public Event getEntityChangedEvent()
    {
        return privateEntityChangedEvent;
    }

    private void setEntityChangedEvent(Event value)
    {
        privateEntityChangedEvent = value;
    }

    private T entity;

    public T getEntity()
    {
        return entity;
    }

    public void setEntity(T value)
    {
        if (entity != value)
        {
            entityChanging(value, entity);
            entity = value;
            onEntityChanged();
            // EntityChanged(this, EventArgs.Empty);
            getEntityChangedEvent().raise(this, EventArgs.Empty);
            onPropertyChanged(new PropertyChangedEventArgs("Entity")); //$NON-NLS-1$
        }
    }

    @Override
    public EntityModel setIsChangable(boolean value) {
        super.setIsChangable(value);
        return this;
    }

    @Override
    public EntityModel setTitle(String value) {
        super.setTitle(value);
        return this;
    }

    public void setEntity(T value, boolean fireEvents) {
        if (fireEvents) {
            setEntity(value);
        }
        else {
            entity = value;
        }
    }

    static
    {
        EntityChangedEventDefinition = new EventDefinition("EntityChanged", EntityModel.class); //$NON-NLS-1$
    }

    public EntityModel()
    {
        setEntityChangedEvent(new Event(EntityChangedEventDefinition));
    }

    public EntityModel(T entity) {
        this();

        setEntity(entity);
    }

    public EntityModel(String title, T entity) {
        this(entity);

        setTitle(title);
    }

    protected void entityChanging(T newValue, T oldValue)
    {
        IProvidePropertyChangedEvent notifier =
                (IProvidePropertyChangedEvent) ((oldValue instanceof IProvidePropertyChangedEvent) ? oldValue : null);
        if (notifier != null)
        {
            notifier.getPropertyChangedEvent().removeListener(this);
        }

        notifier =
                (IProvidePropertyChangedEvent) ((newValue instanceof IProvidePropertyChangedEvent) ? newValue : null);
        if (notifier != null)
        {
            notifier.getPropertyChangedEvent().addListener(this);
        }
    }

    protected void onEntityChanged()
    {
    }

    /**
     * Invoked whenever some property of the entity was changed.
     */
    protected void entityPropertyChanged(Object sender, PropertyChangedEventArgs e)
    {
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (ev.matchesDefinition(EntityChangedEventDefinition))
        {
            onEntityChanged();
        }
        else if (ev.matchesDefinition(ProvidePropertyChangedEvent.Definition))
        {
            entityPropertyChanged(sender, (PropertyChangedEventArgs) args);
        }
    }

    public void validateEntity(IValidation[] validations)
    {
        setIsValid(true);

        if (!getIsAvailable() || !getIsChangable())
        {
            return;
        }

        for (IValidation validation : validations)
        {
            ValidationResult result = validation.validate(getEntity());
            if (!result.getSuccess())
            {
                for (String reason : result.getReasons())
                {
                    getInvalidityReasons().add(reason);
                }
                setIsValid(false);

                break;
            }
        }
    }

    public Convertible asConvertible() {
        return new Convertible(this);
    }

}
