package rocks.inspectit.ocelot.security.userdetails;

import com.google.common.annotations.VisibleForTesting;
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

    private static final String[] READ_ROLE_PERMISSION_SET = {READ_ACCESS};

    private static final String[] WRITE_ROLE_PERMISSION_SET = {READ_ACCESS, WRITE_ACCESS};

    private static final String[] COMMIT_ROLE_PERMISSION_SET = {READ_ACCESS, WRITE_ACCESS, COMMIT_ACCESS};

    private static final String[] ADMIN_ROLE_PERMISSION_SET = {READ_ACCESS, WRITE_ACCESS, COMMIT_ACCESS, ADMIN_ACCESS};

    public static final String READ_ACCESS_ROLE = ROLE_PREFIX + READ_ACCESS;

    public static final String WRITE_ACCESS_ROLE = ROLE_PREFIX + WRITE_ACCESS;

    public static final String COMMIT_ACCESS_ROLE = ROLE_PREFIX + COMMIT_ACCESS;

    public static final String ADMIN_ACCESS_ROLE = ROLE_PREFIX + ADMIN_ACCESS;

    public static final List<String> OCELOT_ACCESS_USER_ROLES = Arrays.asList(READ_ACCESS, WRITE_ACCESS, COMMIT_ACCESS, ADMIN_ACCESS);

    @Autowired
    @VisibleForTesting
    InspectitServerSettings settings;

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
        String[] roles = resolveAccessRoleSet(user);
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(roles)
                .build();
    }

    /**
     * Maps the in the ldap section of the server config defined ldap roles to a role-set internally used for access
     * control. Always returns the role-set with highest access level if user contains multiple matching authorities.
     *
     * @param user The LDAP-User object the roles should be resolved of.
     * @return The highest level of access role the user's authorities could be resolved to.
     */
    @VisibleForTesting
    String[] resolveAccessRoleSet(UserDetails user) {
        RoleSettings role_settings = settings.getSecurity().getLdap().getRoles();
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        if (containsAuthority(authorities, role_settings.getAdmin())) {
            return ADMIN_ROLE_PERMISSION_SET;
        }
        if (containsAuthority(authorities, role_settings.getCommit())) {
            return COMMIT_ROLE_PERMISSION_SET;
        }
        if (containsAuthority(authorities, role_settings.getWrite())) {
            return WRITE_ROLE_PERMISSION_SET;
        }
        return READ_ROLE_PERMISSION_SET;
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
