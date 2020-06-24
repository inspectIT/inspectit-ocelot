package rocks.inspectit.ocelot.file.versioning;

/**
 * Exception if a configuration promotion has failed.
 */
public class PromotionFailedException extends RuntimeException {

    PromotionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}