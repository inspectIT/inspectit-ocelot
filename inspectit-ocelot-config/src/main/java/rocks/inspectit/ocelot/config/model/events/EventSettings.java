package rocks.inspectit.ocelot.config.model.events;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@NoArgsConstructor
public class EventSettings {

    /**
     * Master switch for enabling event recording.
     */
    private boolean enabled;

    /**
     * Frequency to export events towards registered plugins.
     */
    private Duration frequency;
}
