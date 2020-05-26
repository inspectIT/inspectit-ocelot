package rocks.inspectit.ocelot.security.userdetails;

import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;

/**
 * The user details service used for authentication against the configured LDAP system.
 */
public class CustomLdapUserDetailsService extends LdapUserDetailsService {

    private CustomUserAuthoritiesMapper customUserAuthoritiesMapper;

    public CustomLdapUserDetailsService(
            InspectitServerSettings serverSettings,
            LdapContextSource contextSource
    ) {
        super(
                buildFilterBasedLdapUserSearch(serverSettings, contextSource),
                buildDefaultLdapAuthoritiesPopulator(serverSettings, contextSource)
        );
        customUserAuthoritiesMapper = new CustomUserAuthoritiesMapper(serverSettings);
    }

    /**
     * Creates and returns an instance of FilterBasedLdapUserSearch based on the given parameters.
     *
     * @param serverSettings The InspectitServerSettings the server was started with
     * @param contextSource  The LdapContextSource of the current Application context.
     * @return An instance of FilterBasedLdapUserSearch
     */
    private static FilterBasedLdapUserSearch buildFilterBasedLdapUserSearch(InspectitServerSettings serverSettings, LdapContextSource contextSource
    ) {
        LdapSettings ldapSettings = serverSettings.getSecurity().getLdap();
        return new FilterBasedLdapUserSearch(ldapSettings.getGroupSearchBase(), ldapSettings.getUserSearchFilter(), contextSource);
    }

    /**
     * Creates and returns an instance of DefaultLdapAuthoritiesPopulator based on the given parameters.
     *
     * @param serverSettings The InspectitServerSettings the server was started with
     * @param contextSource  The LdapContextSource of the current Application context.
     * @return An instance of DefaultLdapAuthoritiesPopulator
     */
    private static DefaultLdapAuthoritiesPopulator buildDefaultLdapAuthoritiesPopulator(
            InspectitServerSettings serverSettings,
            LdapContextSource contextSource
    ) {
        LdapSettings ldapSettings = serverSettings.getSecurity().getLdap();
        DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, ldapSettings.getGroupSearchBase());
        defaultLdapAuthoritiesPopulator.setGroupSearchFilter(ldapSettings.getGroupSearchFilter());
        return defaultLdapAuthoritiesPopulator;
    }

    /**
     * Loads {@link UserDetails} by a username. See {@link UserDetailsService#loadUserByUsername(String)}.
     * <p>
     * If LDAP authentication is disabled, this method will always throw a {@link UsernameNotFoundException}.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = super.loadUserByUsername(username);
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(customUserAuthoritiesMapper.mapAuthorities(user.getAuthorities()))
                .build();
    }
}
