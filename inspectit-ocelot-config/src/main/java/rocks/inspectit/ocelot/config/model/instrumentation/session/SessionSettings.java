package rocks.inspectit.ocelot.config.model.instrumentation.session;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@NoArgsConstructor
public class SessionSettings {

    /**
     * HTTP-header, which will be used to determine the session
     */
    private String sessionIdHeader;

    /**
     * How many sessions can be stored at the same time
     */
    private int sessionLimit;

    /**
     * How long the data should be stored
     */
    private Duration timeToLive;
}
