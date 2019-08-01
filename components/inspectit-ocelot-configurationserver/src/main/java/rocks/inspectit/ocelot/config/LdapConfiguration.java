package rocks.inspectit.ocelot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;

@Configuration
public class LdapConfiguration {

    @Bean
    @Autowired
    public LdapContextSource ldapContextSource(InspectitServerSettings settings) {
        LdapSettings ldapSettings = settings.getSecurity().getLdap();

        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapSettings.getUrl());
        contextSource.setBase(ldapSettings.getBaseDn());
        contextSource.setUserDn(ldapSettings.getManagerDn());
        contextSource.setPassword(ldapSettings.getManagerPassword());
        contextSource.afterPropertiesSet();

        return contextSource;
    }

    @Bean
    @Autowired
    public LdapUserDetailsService ldapUserDetailsService(InspectitServerSettings settings, LdapContextSource contextSource) throws Exception {
        LdapSettings ldapSettings = settings.getSecurity().getLdap();

        LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch(ldapSettings.getUserSearchBase(), ldapSettings.getUserSearchFilter(), contextSource);
        DefaultLdapAuthoritiesPopulator ldapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, ldapSettings.getGroupSearchBase());
        ldapAuthoritiesPopulator.setGroupSearchFilter(ldapSettings.getGroupSearchFilter());

        return new LdapUserDetailsService(ldapUserSearch, ldapAuthoritiesPopulator);
    }

}
