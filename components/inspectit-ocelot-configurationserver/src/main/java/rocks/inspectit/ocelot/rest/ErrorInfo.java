package rocks.inspectit.ocelot.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value class for representing error messages which are sent as response to REST requests in the body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorInfo {

    public enum Type {
        NO_USERNAME,
        NO_PASSWORD,
        USERNAME_ALREADY_TAKEN
    }

    /**
     * The error type
     */
    private Type error;

    /**
     * The human-readable message of what went wrong.
     */
    private String message;
}
