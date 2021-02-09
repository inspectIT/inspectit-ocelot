package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.AssertFalse;

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
     * If enabled, all authorized and unauthorized accesses attempts to secured endpoints will be logged.
     */
    @Builder.Default
    private boolean accessLog = true;

    /**
     * If enabled, non-admin users cannot promote their own changes.
     * The writing of the configuration and the promotion needs to be done by two separate persons.
     */
    @Builder.Default
    private boolean fourEyesPromotion = false;

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
