package rocks.inspectit.ocelot.rest.users;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.rest.ErrorInfo;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;
import rocks.inspectit.ocelot.user.User;
import rocks.inspectit.ocelot.user.UserService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Rest controller allowing users to manage their own account.
 */
@RestController
@Slf4j
public class UserController extends AbstractBaseController {

    private static final ErrorInfo NO_USERNAME_ERROR = ErrorInfo.builder()
            .error(ErrorInfo.Type.NO_USERNAME)
            .message("Username must not be empty")
            .build();

    private static final ErrorInfo NO_PASSWORD_ERROR = ErrorInfo.builder()
            .error(ErrorInfo.Type.NO_PASSWORD)
            .message("Password must not be empty")
            .build();

    private static final ErrorInfo USERNAME_TAKEN_ERROR = ErrorInfo.builder()
            .error(ErrorInfo.Type.USERNAME_ALREADY_TAKEN)
            .message("A user with the given name already exists")
            .build();

    @Autowired
    private UserService userService;

    @Secured(UserRoleConfiguration.ADMIN_ACCESS_ROLE)
    @Operation(summary = "Select users", description = "Fetches the list of registered users." +
            " If a username query parameter is given, the list is filtered to contain only the user matching the given username." +
            " If none match, an empty list is returned.")
    @GetMapping({"users", "users/"})
    public List<User> selectUsers(@Parameter(description = "If specified only the user with the given name is returned")
                                  @RequestParam(required = false) String username) {
        if (!ObjectUtils.isEmpty(username)) {
            return userService.getUserByName(username)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else {
            return StreamSupport.stream(userService.getUsers().spliterator(), false)
                    .collect(Collectors.toList());
        }
    }

    @Secured(UserRoleConfiguration.ADMIN_ACCESS_ROLE)
    @Operation(summary = "Fetch a single user", description = "Fetches a single user based on his ID.")
    @GetMapping({"users/{id}", "users/{id}/"})
    public ResponseEntity<?> getUser(@Parameter(description = "The ID of the user")
                                     @PathVariable long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::<Object>ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @Secured(UserRoleConfiguration.ADMIN_ACCESS_ROLE)
    @Operation(summary = "Add a new user", description = "Registers a user with a given username and password.")
    @PostMapping({"users", "users/"})
    public ResponseEntity<?> addUser(
            @Parameter(description = "The user to add, must only contain username and the password")
            @RequestBody User user, UriComponentsBuilder builder) {
        if (ObjectUtils.isEmpty(user.getUsername())) {
            return ResponseEntity.badRequest().body(NO_USERNAME_ERROR);
        }
        if (ObjectUtils.isEmpty(user.getPassword())) {
            return ResponseEntity.badRequest().body(NO_PASSWORD_ERROR);
        }
        try {
            User savedUser = userService.addOrUpdateUser(
                    user.toBuilder()
                            .id(null)
                            .isLdapUser(false)
                            .build());
            return ResponseEntity.created(
                    builder.path("users/{id}").buildAndExpand(savedUser.getId()).toUri())
                    .body(savedUser);
        } catch (DataAccessException e) {
            log.error("Error adding new user", e);
            return ResponseEntity.badRequest().body(USERNAME_TAKEN_ERROR);
        }
    }


    @Secured(UserRoleConfiguration.ADMIN_ACCESS_ROLE)
    @Operation(summary = "Delete a user", description = "Deletes a user based on his id. After he is deleted, he immediately is unauthorized.")
    @DeleteMapping({"users/{id}", "users/{id}/"})
    public ResponseEntity<?> deleteUser(@Parameter(description = "The is of the user to delete")
                                        @PathVariable long id) {
        if(userService.getUserById(id).isPresent()) {
            userService.deleteUserById(id);
            return ResponseEntity.ok("");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
