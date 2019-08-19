package rocks.inspectit.ocelot.error.exceptions;

/**
 * This exception is thrown by HTTP endpoints which cannot be used in case LDAP is used for user authentication.
 */
public class NotSupportedWithLdapException extends RuntimeException {

    public NotSupportedWithLdapException() {
        super("Endpoint is not supported when LDAP authentication is used.");
    }
}
