package rocks.inspectit.ocelot.user.ldap;

import org.springframework.ldap.core.support.LdapContextSource;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;

public class LdapUtils {

    private LdapUtils() {
    }

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
