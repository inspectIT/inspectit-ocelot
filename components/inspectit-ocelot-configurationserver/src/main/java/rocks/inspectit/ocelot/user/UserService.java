package rocks.inspectit.ocelot.user;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * Service class for managing local users stored in the embedded user database.
 */
@Component
@Slf4j
public class UserService {

    @Autowired
    @VisibleForTesting
    InspectitServerSettings settings;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * @return An iterator over all registered users
     */
    public Iterable<User> getUsers() {
        return userRepository.findAll();
    }

    /**
     * Selects a given user based on his username.
     *
     * @param name the username, will be turned to lower case before querying
     * @return the user, if present
     */
    public Optional<User> getUserByName(String name) {
        return userRepository.findByUsername(name.toLowerCase());
    }

    /**
     * Checks if a user with a given name exists.
     *
     * @param name the username, will be turned to lower case before querying
     * @return true, if a user with the given name exists.
     */
    public boolean userExists(String name) {
        return userRepository.existsByUsername(name.toLowerCase());
    }

    /**
     * Fetches a user based on his ID.
     *
     * @param id the id of the user
     * @return the user, if present
     */
    public Optional<User> getUserById(long id) {
        return userRepository.findById(id);
    }

    /**
     * Deletes a user based on his id.
     *
     * @param id the id of the user to delete
     */
    public void deleteUserById(long id) {
        userRepository.deleteById(id);
    }

    /**
     * Adds or updates a user.
     * If ID is null, an attempt will be made to add the given user, which fails in case the username is already taken.
     * If the ID is not null, the given User will be updated.
     * <p>
     * This method enforces the username to be lowercase.
     * <p>
     * If the given user has the "password" field set, the password will be hashed into "passwordHash"
     * before storing the user.
     *
     * @param user the user to add or update
     * @return the updated user entity
     */
    public User addOrUpdateUser(User user) {
        User.UserBuilder userBuilder = user
                .toBuilder()
                .username(user.getUsername().toLowerCase());

        if (!StringUtils.isEmpty(user.getPassword())) {
            String passwordHash = passwordEncoder.encode(user.getPassword());
            userBuilder.passwordHash(passwordHash);
        }

        return userRepository.save(userBuilder.build());
    }

    @PostConstruct
    private void addDefaultUserIfRequired() {
        if (!settings.getSecurity().isLdapAuthentication() && userRepository.count() == 0) {
            String name = settings.getDefaultUser().getName();
            String rawPassword = settings.getDefaultUser().getPassword();

            log.warn("Generated default user as no other users were found. Please login and change the password!");
            User user = User.builder()
                    .username(name)
                    .password(rawPassword)
                    .build();

            addOrUpdateUser(user);
        }
    }
}
