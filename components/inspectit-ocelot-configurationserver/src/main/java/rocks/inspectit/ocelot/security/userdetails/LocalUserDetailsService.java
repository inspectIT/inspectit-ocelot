package rocks.inspectit.ocelot.security.userdetails;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;
import rocks.inspectit.ocelot.user.User;
import rocks.inspectit.ocelot.user.UserService;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * The user details service used for authentication against the embedded user database.
 */
@Component
@Slf4j
@Order(2)
public class LocalUserDetailsService implements UserDetailsService {

    @Autowired
    @VisibleForTesting
    InspectitServerSettings settings;

    @Autowired
    private UserService userService;

    /**
     * The access role which will be assigned to the users.
     */
    private String[] accessRoles;

    /**
     * Sets the access role field {@link #accessRoles} which will be assigned to authenticated users.
     */
    @PostConstruct
    private void postConstruct() {
        if (settings.getSecurity().isLdapAuthentication()) {
            accessRoles = new String[]{settings.getSecurity().getLdap().getAdminGroup()};
        } else {
            accessRoles = UserRoleConfiguration.ADMIN_ROLE_PERMISSION_SET;
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOptional = userService.getUserByName(username);
        if (!userOptional.isPresent()) {
            throw new UsernameNotFoundException("User with username '" + username + "' has not been found.");
        }

        User user = userOptional.get();

        if (user.isLdapUser()) {
            log.debug("User is LDAP user, thus, not returned by this user details service.");
            throw new UsernameNotFoundException("User with username '" + username + "' has not been found because it is a LDAP user.");
        }

        return toUserDetails(user);
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles(accessRoles)
                .build();
    }
}
