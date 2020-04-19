package rocks.inspectit.ocelot.security.userdetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.conditional.ConditionalOnLdap;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.RoleSettings;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The user details service used for authentication against the configured LDAP system.
 */
@Component
@Order(1)
@ConditionalOnLdap
public class CustomLdapUserDetailsService extends LdapUserDetailsService {

    private static final String ROLE_PREFIX = "ROLE_";

    private static final String READ_ACCESS = "OCELOT_READ";

    private static final String WRITE_ACCESS = "OCELOT_WRITE";

    private static final String COMMIT_ACCESS = "OCELOT_COMMIT";

    private static final String ADMIN_ACCESS = "OCELOT_ADMIN";

    private static final String NO_ACCESS_ROLE = "OCELOT_NONE";

    public static final String READ_ACCESS_ROLE = ROLE_PREFIX + READ_ACCESS;

    public static final String WRITE_ACCESS_ROLE = ROLE_PREFIX + WRITE_ACCESS;

    public static final String COMMIT_ACCESS_ROLE = ROLE_PREFIX + COMMIT_ACCESS;

    public static final String ADMIN_ACCESS_ROLE = ROLE_PREFIX + ADMIN_ACCESS;

    public static final List<String> OCELOT_ACCESS_USER_ROLES = Arrays.asList(READ_ACCESS, WRITE_ACCESS, COMMIT_ACCESS, ADMIN_ACCESS);

    @Autowired
    private InspectitServerSettings settings;

    @Autowired
    public CustomLdapUserDetailsService(LdapUserSearch ldapUserSearch, DefaultLdapAuthoritiesPopulator ldapAuthoritiesPopulator) {
        super(ldapUserSearch, ldapAuthoritiesPopulator);
    }

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
        UserDetails user = super.loadUserByUsername(username);
        String resolvedRole = resolveAccessRole(user);
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(resolvedRole)
                .build();
    }

    /**
     * Maps the in the ldap section of the server config defined ldap roles to a role internally used for access
     * control. Always returns the role with highest access level if user contains multiple matching authorities.
     *
     * @param user The LDAP-User object the roles should be resolved of.
     * @return The highest level of access role the user's authorities could be resolved to.
     */
    private String resolveAccessRole(UserDetails user) {
        RoleSettings role_settings = settings.getSecurity().getLdap().getRoles();
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        String resolvedRole = NO_ACCESS_ROLE;
        if (containsAuthority(authorities, role_settings.getRead())) {
            resolvedRole = READ_ACCESS;
        }
        if (containsAuthority(authorities, role_settings.getWrite())) {
            resolvedRole = WRITE_ACCESS;
        }
        if (containsAuthority(authorities, role_settings.getCommit())) {
            resolvedRole = COMMIT_ACCESS;
        }
        if (containsAuthority(authorities, role_settings.getAdmin())) {
            resolvedRole = ADMIN_ACCESS;
        }
        return resolvedRole;
    }

    /**
     * Checks if at least one entry of a Collection of authorities is contained in a List of Strings.
     *
     * @param authorities A Collection containing GrantedAuthority objects.
     * @param roleList    The List of Strings the authorities are checked with.
     * @return Returns true if at least one element of authorities is contained in roleList or vice versa.
     */
    private boolean containsAuthority(Collection<? extends GrantedAuthority> authorities, List<String> roleList) {
        return !authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .distinct()
                .filter(roleList::contains)
                .collect(Collectors.toSet()).isEmpty();
    }
}
