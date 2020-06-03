package rocks.inspectit.ocelot.config.model.instrumentation.rules;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EventRecordingSettings {

    /**
     * The event name. Resolves to the key in case this is left empty.
     */
    private String name;

    /**
     * The attributes to define events.
     */
    @Builder.Default
    @NotNull
    private Map<@NotBlank String, Object> attributes = Collections.emptyMap();

    /**
     * Constant data tags attributes will be resolved to.
     */
    @Builder.Default
    @NotNull
    private Map<String, String> constantTags = Collections.emptyMap();

    /**
     * Returns a new instance of EventRecordingSettings, copying the one which calls this function.
     * Sets the name prop, in case it is empty before.
     *
     * @param defaultEventName The default name which should be used in case event is null
     * @return a copy of the given Event
     */
    public EventRecordingSettings copyWithDefaultEventName(String defaultEventName) {
        String eventName = getEventNameOrDefault(defaultEventName);
        return toBuilder().name(eventName)
                .attributes(Collections.unmodifiableMap(attributes))
                .build();
    }

    private String getEventNameOrDefault(String defaultEventName) {
        return StringUtils.isEmpty(name) ? defaultEventName : name;
    }
}
