package rocks.inspectit.ocelot.user;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for adding Users which are authenticated via LDAP
 * to the database.
 */
@Component
@Slf4j
public class LdapUserRegistration {

    @Autowired
    private UserService userService;

    @EventListener
    @VisibleForTesting
    void onAuthentication(AuthenticationSuccessEvent authEvent) {
        Authentication auth = authEvent.getAuthentication();
        if (auth.getPrincipal() instanceof LdapUserDetails) {
            addUserToDatabaseIfNotPresent((LdapUserDetails) auth.getPrincipal());
        }
    }

    private void addUserToDatabaseIfNotPresent(UserDetails user) {
        try {
            String name = user.getUsername();
            if (!userService.userExists(name)) {
                userService.addOrUpdateUser(User.builder()
                        .username(name)
                        .passwordHash("<EMPTY>")
                        .isLdapUser(true)
                        .build());
                log.info("User `{}` was authenticated using LDAP and added to the database.", name);
            }
        } catch (DataAccessException dae) {
            log.debug("User already exists", dae);
        }
    }
}
