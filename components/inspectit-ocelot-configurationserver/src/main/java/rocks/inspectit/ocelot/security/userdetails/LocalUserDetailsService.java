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
import rocks.inspectit.ocelot.user.User;
import rocks.inspectit.ocelot.user.UserService;

import java.util.Optional;

/**
 * The user details service used for authentication against the embedded user database.
 */
@Component
@Slf4j
@Order(2)
public class LocalUserDetailsService implements UserDetailsService {

    public static final String DEFAULT_ACCESS_USER_ROLE = "OCELOT_ADMIN";

    @Autowired
    @VisibleForTesting
    InspectitServerSettings settings;

    @Autowired
    private UserService userService;

    /**
     * Returns the access role which will be assigned to authenticated users.
     *
     * @return the access role name
     */
    private String getAccessRole() {
        if (settings.getSecurity().isLdapAuthentication()) {
            return settings.getSecurity().getLdap().getAdminGroup();
        } else {
            return DEFAULT_ACCESS_USER_ROLE;
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOptional = userService.getUserByName(username);
        if (!userOptional.isPresent()) {
            throw new UsernameNotFoundException(username);
        }

        User user = userOptional.get();

        if (user.isLdapUser()) {
            log.info("User is LDAP user, thus, not returned by this user details service.");
            throw new UsernameNotFoundException(username);
        }

        return toUserDetails(user);
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles(getAccessRole())
                .build();
    }
}
