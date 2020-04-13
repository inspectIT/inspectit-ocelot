package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.NotNull;

/**
 * Security settings of the configuration server.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SecuritySettings {

    /**
     * Whether LDAP should be used for user authentication.
     */
    @Builder.Default
    private boolean ldapAuthentication = false;

    /**
     * The LDAP settings used in case LDAP is used for user authentication.
     */
    @Valid
    private LdapSettings ldap;

    /**
     * The settings for the authentication proxy.
     */
    @Valid
    @NotNull
    private AuthProxySettings authProxy;

    /**
     * If enabled, all authorized and unauthorized accesses attempts to secured endpoints will be logged.
     */
    @Builder.Default
    private boolean accessLog = true;

    /**
     * Verify that LDAP settings exist if LDAP is enabled.
     */
    @AssertFalse(message = "LDAP setting must not be null when LDAP authentication is enabled.")
    public boolean isLdapSettingsMissing() {
        if (ldapAuthentication) {
            return ldap == null;
        } else {
            return false;
        }
    }
}
