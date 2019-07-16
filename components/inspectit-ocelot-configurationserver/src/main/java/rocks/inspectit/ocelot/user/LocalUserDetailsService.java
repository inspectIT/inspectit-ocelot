package rocks.inspectit.ocelot.user;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * Manages usernames and their (hashed) passwords.
 */
@Component
@Slf4j
public class LocalUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository users;

    @Autowired
    private InspectitServerSettings config;

    @Getter
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    /**
     * @return An iterator over all registered users
     */
    public Iterable<User> getUsers() {
        return users.findAll();
    }

    /**
     * Adds or updates a user.
     * If ID is null, an attempt will be made to add the given user, which fails in case the username is already taken.
     * If the ID is not null, the given User will be updated.
     * <p>
     * This method enforces the username to be lowercase.
     * <p>
     * If the given user has teh "password" field set, the password will be hashen into "passwordHash"
     * before storing the user.
     *
     * @param user the user to add or update
     * @return the updated user entity
     */
    public User addOrUpdateUser(User user) {
        String pwHash = user.getPasswordHash();
        if (!StringUtils.isEmpty(user.getPassword())) {
            pwHash = passwordEncoder.encode(user.getPassword());
        }
        User lowerCaseUser = user.toBuilder()
                .username(user.getUsername().toLowerCase())
                .passwordHash(pwHash)
                .build();
        return users.save(lowerCaseUser);
    }


    /**
     * Selects a gioven user based on his username.
     *
     * @param name the username, will be turned to lower case before querying
     * @return the user, if present
     */
    public Optional<User> getUserByName(String name) {
        return users.findByUsername(name.toLowerCase());
    }

    /**
     * Fetches a user based on his ID.
     *
     * @param id the id of the user
     * @return the user, if present
     */
    public Optional<User> getUserById(long id) {
        return users.findById(id);
    }

    /**
     * Deletes a user based on his id.
     *
     * @param id the id of the user to delete
     */
    public void deleteUserById(long id) {
        users.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = getUserByName(username);
        if (!user.isPresent()) {
            throw new UsernameNotFoundException(username);
        }
        return toUserDetails(user.get());
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities("USER") //at least one authority is required by spring, even if we don't use them yet
                .build();
    }


    @PostConstruct
    private void addDefaultUserIfRequired() {
        if (users.count() == 0) {

            String name = config.getDefaultUser().getName();
            String rawPassword = config.getDefaultUser().getPassword();

            log.warn("Generated default user as no other users were found. Please login and change the password!");
            addOrUpdateUser(
                    User.builder()
                            .username(name)
                            .password(rawPassword)
                            .build());
        }
    }
}
