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
    UserRepository users;

    @Autowired
    private InspectitServerSettings config;

    @Getter
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    /**
     * Hashes the given raw password to store it in the DB.
     *
     * @param rawPassword the raw password
     * @return the hashed password
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }


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
     *
     * @param user the user to add or update
     * @return the updated user entity
     */
    public User addOrUpdateUser(User user) {
        User lowerCaseUser = user.toBuilder()
                .username(user.getUsername().toLowerCase())
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
                .password(user.getPassword())
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
                            .password(encodePassword(rawPassword))
                            .build());
        }
    }
}
