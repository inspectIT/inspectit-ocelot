package rocks.inspectit.ocelot.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import rocks.inspectit.ocelot.config.conditional.ConditionalOnLdap;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;

/**
 * Spring beans used for user management.
 */
@Configuration
public class SecurityBeanConfiguration {

    /**
     * Returns the password encoder used for the local user storage.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Initializes the context source used for authenticating users via LDAP.
     */
    @Bean
    @ConditionalOnLdap
    public LdapContextSource ldapContextSource(InspectitServerSettings settings) {
        LdapSettings ldapSettings = settings.getSecurity().getLdap();

        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapSettings.getUrl());
        contextSource.setBase(ldapSettings.getBaseDn());

        if (ldapSettings.getManagerDn() != null) {
            contextSource.setUserDn(ldapSettings.getManagerDn());
        }
        if (ldapSettings.getManagerPassword() != null) {
            contextSource.setPassword(ldapSettings.getManagerPassword());
        }

        contextSource.afterPropertiesSet();

        return contextSource;
    }

    /**
     * Initializes the user search used for authenticating users via LDAP.
     */
    @Bean
    @ConditionalOnLdap
    public LdapUserSearch ldapUserSearch(InspectitServerSettings settings, LdapContextSource contextSource) {
        LdapSettings ldapSettings = settings.getSecurity().getLdap();
        return new FilterBasedLdapUserSearch(ldapSettings.getUserSearchBase(), ldapSettings.getUserSearchFilter(), contextSource);
    }

    /**
     * Initializes the authorities populator used for authenticating users via LDAP.
     */
    @Bean
    @ConditionalOnLdap
    public DefaultLdapAuthoritiesPopulator ldapAuthoritiesPopulator(InspectitServerSettings settings, LdapContextSource contextSource) {
        LdapSettings ldapSettings = settings.getSecurity().getLdap();
        DefaultLdapAuthoritiesPopulator ldapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, ldapSettings.getGroupSearchBase());
        ldapAuthoritiesPopulator.setGroupSearchFilter(ldapSettings.getGroupSearchFilter());
        return ldapAuthoritiesPopulator;
    }
}