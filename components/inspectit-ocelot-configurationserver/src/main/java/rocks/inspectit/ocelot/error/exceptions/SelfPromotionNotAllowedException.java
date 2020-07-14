package rocks.inspectit.ocelot.error.exceptions;

/**
 * Thrown when four-eyes promotion is enabled and a non-admin user attempts to promote one of his own changes.
 */
public class SelfPromotionNotAllowedException extends RuntimeException {

    public SelfPromotionNotAllowedException(String message) {
        super(message);
    }

}
