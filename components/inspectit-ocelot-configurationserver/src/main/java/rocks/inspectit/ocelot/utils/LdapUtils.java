package rocks.inspectit.ocelot.utils;

import org.springframework.ldap.core.support.LdapContextSource;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;

/**
 * Utility methods when using LDAP.
 */
public class LdapUtils {

    /**
     * Hidden.
     */
    private LdapUtils() {
    }

    /**
     * Creates a {@link LdapContextSource} based on the settings of the given {@link InspectitServerSettings}.
     *
     * @param settings the inspectIT settings to use
     * @return the created {@link LdapContextSource}
     */
    public static LdapContextSource createLdapContextSource(InspectitServerSettings settings) {
        LdapSettings ldapSettings = settings.getSecurity().getLdap();

        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapSettings.getUrl());
        contextSource.setBase(ldapSettings.getBaseDn());
        contextSource.setUserDn(ldapSettings.getManagerDn());
        contextSource.setPassword(ldapSettings.getManagerPassword());
        contextSource.afterPropertiesSet();

        return contextSource;
    }
}
