package rocks.inspectit.ocelot.rest.alert.kapacitor.model;

import lombok.Builder;
import lombok.Value;

/**
 * Response Object for the state of Kapacitor.
 */
@Value
@Builder
public class KapacitorState {

    /**
     * True, if the connection to kapacitor has been configured.
     */
    boolean enabled;

    /**
     * True, if a ping to Kapacitor succeeded.
     */
    boolean kapacitorOnline;
}
