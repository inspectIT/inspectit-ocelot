package rocks.inspectit.ocelot.rest.users;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.rest.ErrorInfo;
import rocks.inspectit.ocelot.user.LocalUserDetailsService;
import rocks.inspectit.ocelot.user.User;

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
            .error("NoUsername")
            .message("Username must not be empty")
            .build();

    private static final ErrorInfo NO_PASSWORD_ERROR = ErrorInfo.builder()
            .error("NoPassword")
            .message("Password must not be empty")
            .build();

    private static final ErrorInfo USERNAME_TAKEN_ERROR = ErrorInfo.builder()
            .error("UsernameAlreadyTaken")
            .message("A user with the given name already exists")
            .build();

    @Autowired
    private LocalUserDetailsService userDetailsService;

    @ApiOperation(value = "Select users", notes = "Fetches the list of registered users." +
            " If a username query parameter is given, the list is filtered to contain only the user matching the given username." +
            " If none match, an empty list is returned.")
    @GetMapping("users")
    public List<User> selectUsers(@ApiParam("If specified only the user with the given name is returned")
                                  @RequestParam(required = false) String username) {
        if (!StringUtils.isEmpty(username)) {
            return userDetailsService.getUserByName(username)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else {
            return StreamSupport.stream(userDetailsService.getUsers().spliterator(), false)
                    .collect(Collectors.toList());
        }
    }

    @ApiOperation(value = "Fetch a single user", notes = "Fetches a single user based on his ID.")
    @GetMapping("users/{id}")
    public ResponseEntity<?> getUser(@ApiParam("The ID of the user")
                                     @PathVariable long id) {
        return userDetailsService.getUserById(id)
                .map(ResponseEntity::<Object>ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @ApiOperation(value = "Add a new user", notes = "Registers a user with a given username and password.")
    @PostMapping("users")
    public ResponseEntity<?> addUser(
            @ApiParam("The user to add, must only contain username and the password")
            @RequestBody User user, UriComponentsBuilder builder) {
        if (StringUtils.isEmpty(user.getUsername())) {
            return ResponseEntity.badRequest().body(NO_USERNAME_ERROR);
        }
        if (StringUtils.isEmpty(user.getPassword())) {
            return ResponseEntity.badRequest().body(NO_PASSWORD_ERROR);
        }
        try {
            User savedUser = userDetailsService.addOrUpdateUser(
                    user.toBuilder()
                            .id(null)
                            .build());
            return ResponseEntity.created(
                    builder.path("users/{id}").buildAndExpand(savedUser.getId()).toUri())
                    .body(savedUser
                            .toBuilder()
                            .password(null)
                            .build());
        } catch (JpaSystemException e) {
            log.error("Error adding new user", e);
            return ResponseEntity.badRequest().body(USERNAME_TAKEN_ERROR);
        }
    }


    @ApiOperation(value = "Delete a user", notes = "Deletes a user based on his id. After he is deleted, he immediately is unauthorized.")
    @DeleteMapping("users/{id}")
    public ResponseEntity<?> deleteUser(@ApiParam("The is of the user to delete")
                                        @PathVariable long id) {
        try {
            userDetailsService.deleteUserById(id);
            return ResponseEntity.ok("");
        } catch (EmptyResultDataAccessException e) {
            log.error("Error deleting user", e);
            return ResponseEntity.notFound().build();
        }
    }
}
