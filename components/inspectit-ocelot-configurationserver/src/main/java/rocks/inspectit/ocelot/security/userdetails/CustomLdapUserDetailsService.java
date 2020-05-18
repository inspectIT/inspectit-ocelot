package rocks.inspectit.ocelot.security.userdetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

/**
 * The user details service used for authentication against the configured LDAP system.
 */
@Component
@Order(1)
@ConditionalOnProperty(value = "inspectit-config-server.security.ldap-authentication", havingValue = "true")
public class CustomLdapUserDetailsService extends LdapUserDetailsService {

    @Autowired
    private CustomUserAuthoritiesMapper customUserAuthoritiesMapper;

    @Autowired
    public CustomLdapUserDetailsService(InspectitServerSettings serverSettings, LdapContextSource contextSource) {
        super(getLdapUserSearch(serverSettings, contextSource), getLdapAuthoritiesPopulator(serverSettings, contextSource));
    }

    private static LdapUserSearch getLdapUserSearch(InspectitServerSettings serverSettings, LdapContextSource contextSource) {
        return new FilterBasedLdapUserSearch(serverSettings.getSecurity().getLdap().getUserSearchBase(), serverSettings.getSecurity().getLdap().getUserSearchFilter(), contextSource);
    }

    private static DefaultLdapAuthoritiesPopulator getLdapAuthoritiesPopulator(InspectitServerSettings serverSettings, LdapContextSource contextSource) {
        DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, serverSettings.getSecurity().getLdap().getGroupSearchBase());
        defaultLdapAuthoritiesPopulator.setGroupSearchFilter(serverSettings.getSecurity().getLdap().getGroupSearchFilter());
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
