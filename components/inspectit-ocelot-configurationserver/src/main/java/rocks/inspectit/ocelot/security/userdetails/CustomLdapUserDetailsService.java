package rocks.inspectit.ocelot.security.userdetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.utils.LdapUtils;

@Component
@Order(1)
public class CustomLdapUserDetailsService implements UserDetailsService {

    /**
     * The user details service to use for fetching LDAP data.
     */
    private LdapUserDetailsService ldapUserDetailsService;

    @Autowired
    private InspectitServerSettings settings;

    /**
     * Loads {@link UserDetails} by a username. See {@link UserDetailsService#loadUserByUsername(String)}.
     * <p>
     * If LDAP authentication is disabled, this method will always throw a {@link UsernameNotFoundException}.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!settings.getSecurity().isLdapAuthentication()) {
            throw new UsernameNotFoundException(username);
        }
        return getLdapUserDetailsService().loadUserByUsername(username);
    }

    /**
     * Returns the value of {@link #ldapUserDetailsService}. If the field is `null` a new {@link LdapUserDetailsService}
     * will be initialized.
     *
     * @return a instance of type {@link LdapUserDetailsService}
     */
    private LdapUserDetailsService getLdapUserDetailsService() {
        if (ldapUserDetailsService == null) {
            initLdapUserDetailsService();
        }

        return ldapUserDetailsService;
    }

    /**
     * Creates a new {@link LdapUserDetailsService} and stores it in {@link #ldapUserDetailsService}.
     */
    private void initLdapUserDetailsService() {
        LdapContextSource contextSource = LdapUtils.createLdapContextSource(settings);

        LdapSettings ldapSettings = settings.getSecurity().getLdap();

        LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch(ldapSettings.getUserSearchBase(), ldapSettings.getUserSearchFilter(), contextSource);
        DefaultLdapAuthoritiesPopulator ldapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, ldapSettings.getGroupSearchBase());
        ldapAuthoritiesPopulator.setGroupSearchFilter(ldapSettings.getGroupSearchFilter());

        ldapUserDetailsService = new LdapUserDetailsService(ldapUserSearch, ldapAuthoritiesPopulator);
    }
}
