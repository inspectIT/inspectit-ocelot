package rocks.inspectit.ocelot.security.userdetails;

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
import rocks.inspectit.ocelot.user.User;
import rocks.inspectit.ocelot.user.UserService;

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
            //this check is not really needed, it however prevents the printing of unnecessary constraint violation errors
            if (!userService.userExists(name)) {
                userService.addOrUpdateUser(User.builder()
                        .username(name)
                        .passwordHash("<EMPTY>")
                        .isLdapUser(true)
                        .build());
                log.info("User `{}` was authenticated using LDAP and added to the database.", name);
            }
        } catch (DataAccessException dae) { //thrown if the user already exists, therefore ignored
            log.debug("User already exists", dae);
        }
    }
}
