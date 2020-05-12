package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import lombok.Value;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.EventAccessor;

import java.util.*;

@Value
public class EventRecorder implements IHookAction{

    List<EventAccessor> eventAccessors;

    @Override
    public void execute(IHookAction.ExecutionContext context) {
        for(EventAccessor event : eventAccessors) {
            // Do nothing for now
        }
    }

    @Override
    public String getName() {
        return "Event Recorder";
    }

}

