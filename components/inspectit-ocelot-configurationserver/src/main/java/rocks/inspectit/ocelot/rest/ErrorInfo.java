package rocks.inspectit.ocelot.rest;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorInfo {
    /**
     * The error type
     */
    private String error;

    /**
     * The human-readable message of what went wrong.
     */
    private String message;
}
