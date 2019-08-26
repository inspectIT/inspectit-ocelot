package rocks.inspectit.ocelot.user;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * Repository for loading and storing user credentials in the embedded database.
 */
public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
