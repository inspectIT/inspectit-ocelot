package rocks.inspectit.ocelot.security.userdetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.stereotype.Component;

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
    public CustomLdapUserDetailsService(LdapUserSearch ldapUserSearch, DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator) {
        super(ldapUserSearch, defaultLdapAuthoritiesPopulator);
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
