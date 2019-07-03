package rocks.inspectit.ocelot.authentication;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages usernames and their (hashed) passwords.
 */
@Component
@Slf4j
public class LocalUserDetailsService implements UserDetailsService {

    @Autowired
    private InspectitServerSettings config;

    @Getter
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Stores the users by username, the keys of the map are converted to lowercase.
     * This way, searching for a user is case insensitive.
     */
    private Map<String, UserAccount> accounts = new HashMap<>();

    @PostConstruct
    void addDefaultUser() {
        String name = config.getDefaultUser().getName();
        String rawPassword = config.getDefaultUser().getPassword();

        log.warn("Generated default user as no other users were found. Please login and change the password!");

        accounts.put(name.toLowerCase(), UserAccount.builder()
                .username(name)
                .password(passwordEncoder.encode(rawPassword))
                .build());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = accounts.get(username.toLowerCase());
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("USER") //at least one authority is required by spring, even if we don't use them yet
                .build();
    }
}
