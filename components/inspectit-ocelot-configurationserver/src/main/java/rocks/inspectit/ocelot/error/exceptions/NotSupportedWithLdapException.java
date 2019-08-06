package rocks.inspectit.ocelot.error.exceptions;

public class NotSupportedWithLdapException extends RuntimeException {

    public NotSupportedWithLdapException() {
        super("Endpoint is not supported when LDAP authentication is enabled.");
    }
}
