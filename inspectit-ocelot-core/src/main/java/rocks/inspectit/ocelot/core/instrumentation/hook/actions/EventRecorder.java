package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import lombok.Value;
import rocks.inspectit.ocelot.core.exporter.EventExporterService;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.EventAccessor;
import rocks.inspectit.ocelot.sdk.events.Event;

import java.util.*;

@Value
public class EventRecorder implements IHookAction{

    List<EventAccessor> eventAccessors;

    EventExporterService eventExporterService;

    @Override
    public String getName() {
        return "Event Recorder";
    }

    @Override
    public void execute(IHookAction.ExecutionContext context) {
        for(EventAccessor event : eventAccessors) {
            Event res = new Event();
            res.setName(resolveEventName(event, context));
            res.setTimestamp(System.currentTimeMillis());

            HashMap<String, Object> resolvedAtts = new HashMap<>();
            resolveMap(resolvedAtts, event.getAttributes(), event, context);
            res.setAttributes(resolvedAtts);

            eventExporterService.export(res);
        }
    }

    private String resolveEventName(EventAccessor eventAccessor, ExecutionContext context) {
        String dataKey = eventAccessor.getName();

        try {
            VariableAccessor valueAccessor = eventAccessor.getVariableAccessors().get(dataKey);
            Object value = valueAccessor.get(context);
            if(value != null) {
                return (String) value;
            }
            return dataKey;
        } catch (Throwable t) {
            if (eventAccessor.getConstantTags().containsKey(dataKey)) {
                return eventAccessor.getConstantTags().get(dataKey);
            }
            return dataKey;
        }
    }

    private void resolveMap(Map<String, Object> copy, Map<String, Object> origin, EventAccessor eventAccessor, ExecutionContext context) {
        for (Map.Entry<String, Object> entry : origin.entrySet()) {
            Object content = entry.getValue();
            if (content instanceof Map) {
                HashMap<String, Object> copyContent = new HashMap<>();
                copy.put(entry.getKey(), copyContent);

                Map<String, Object> contentAsMap = (Map) content;
                resolveMap(copyContent, contentAsMap, eventAccessor, context);
            } else {
                Object value = resolveDataKey((String) content, eventAccessor, context);
                if (value != null) {
                    copy.put(entry.getKey(), value);
                }
            }
        }
    }

    private Object resolveDataKey(String key, EventAccessor eventAccessor, ExecutionContext context) {
        if(eventAccessor.getVariableAccessors().containsKey(key)) {
            try {
                VariableAccessor valueAccessor = eventAccessor.getVariableAccessors().get(key);
                return valueAccessor.get(context);
            } catch(Throwable t){
                //ignore
            }
        } else if(eventAccessor.getConstantTags().containsKey(key)) {
            return eventAccessor.getConstantTags().get(key);
        }
        return null;
    }

}

