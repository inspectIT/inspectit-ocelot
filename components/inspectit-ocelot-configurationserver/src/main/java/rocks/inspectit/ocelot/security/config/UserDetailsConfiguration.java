package rocks.inspectit.ocelot.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.user.userdetails.LocalUserDetailsService;
import rocks.inspectit.ocelot.utils.LdapUtils;

@Configuration
public class UserDetailsConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Autowired
    public UserDetailsService userDetailsService(ApplicationContext context, InspectitServerSettings settings) {
        if (settings.getSecurity().isLdapAuthentication()) {
            return createLdapUserDetailsService(settings);
        } else {
            return context.getBean(LocalUserDetailsService.class);
        }
    }

    private LdapUserDetailsService createLdapUserDetailsService(InspectitServerSettings settings) {
        LdapContextSource contextSource = LdapUtils.createLdapContextSource(settings);

        LdapSettings ldapSettings = settings.getSecurity().getLdap();

        LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch(ldapSettings.getUserSearchBase(), ldapSettings.getUserSearchFilter(), contextSource);
        DefaultLdapAuthoritiesPopulator ldapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, ldapSettings.getGroupSearchBase());
        ldapAuthoritiesPopulator.setGroupSearchFilter(ldapSettings.getGroupSearchFilter());

        return new LdapUserDetailsService(ldapUserSearch, ldapAuthoritiesPopulator);
    }
}
