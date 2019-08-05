package rocks.inspectit.ocelot.user;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.user.ldap.LdapUtils;

import javax.annotation.PostConstruct;

@Component
public class UserDetailsServiceManager {

    @Autowired
    private InspectitServerSettings settings;

    @Autowired
    @Getter
    private UserDetailsService userDetailsService;

    @Autowired
    private ApplicationContext context;

    @PostConstruct
    private void initUserDetailsService() {
        if (settings.getSecurity().isLdapAuthentication()) {
            userDetailsService = createLdapUserDetailsService();
        } else {
            userDetailsService = context.getBean(LocalUserDetailsService.class);
        }
    }

    public PasswordEncoder getPasswordEncoder() {
        if (userDetailsService instanceof LocalUserDetailsService) {
            return ((LocalUserDetailsService)userDetailsService).getPasswordEncoder();
        } else {
            return null;
        }
    }

    private LdapUserDetailsService createLdapUserDetailsService() {
        LdapContextSource contextSource = LdapUtils.createLdapContextSource(settings);

        LdapSettings ldapSettings = settings.getSecurity().getLdap();

        LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch(ldapSettings.getUserSearchBase(), ldapSettings.getUserSearchFilter(), contextSource);
        DefaultLdapAuthoritiesPopulator ldapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, ldapSettings.getGroupSearchBase());
        ldapAuthoritiesPopulator.setGroupSearchFilter(ldapSettings.getGroupSearchFilter());

        return new LdapUserDetailsService(ldapUserSearch, ldapAuthoritiesPopulator);
    }
}
