package rocks.inspectit.ocelot.config.model.instrumentation.rules;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;

@Builder
@AllArgsConstructor
@Data
public class EventRecordingSettings {

    /**
     * The event name. Resolves to the key in case this is left empty.
     */
    private String name;

    @Builder.Default
    @NotNull
    private Map<@NotBlank String, Object> attributes = Collections.emptyMap();

    @Builder.Default
    @NotNull
    private Map<String, String> constantTags = Collections.emptyMap();
}
