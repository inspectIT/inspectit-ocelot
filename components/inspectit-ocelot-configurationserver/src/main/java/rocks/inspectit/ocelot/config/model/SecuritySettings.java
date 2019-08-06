package rocks.inspectit.ocelot.config.model;

import lombok.Builder;
import lombok.Data;

/**
 * Security settings of the configuration server.
 */
@Data
@Builder
public class SecuritySettings {

    /**
     * Whether LDAP should be used for user authentication.
     */
    private boolean ldapAuthentication = false;

    /**
     * The LDAP settings used in case LDAP is used for user authentication.
     */
    @Builder.Default
    private LdapSettings ldap = LdapSettings.builder().build();
}
