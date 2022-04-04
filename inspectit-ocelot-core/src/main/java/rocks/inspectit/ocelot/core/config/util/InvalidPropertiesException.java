package rocks.inspectit.ocelot.core.config.util;

/**
 * Exception class used to indicate that a configuration property is invalid.
 */
public class InvalidPropertiesException extends Exception {

    public InvalidPropertiesException(String errorMessage) {
        super(errorMessage);
    }
}
