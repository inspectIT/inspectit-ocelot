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

    @Autowired
    LocalUserDetailsService userDetailsService;

    @ApiOperation(value = "Select users", notes = "Fetches the list of registered users." +
            " If a username query parameter is given, the list is filtered to contain only the user matching the given username." +
            " If none match, an empty list is returned.")
    @GetMapping("users")
    public List<User> selectUsers(@ApiParam("If specified only the user with the given name is returned")
                                  @RequestParam(required = false) String username) {
        if (!StringUtils.isEmpty(username)) {
            return userDetailsService.getUserByName(username)
                    .map(u -> Collections.singletonList(erasePassword(u)))
                    .orElse(Collections.emptyList());
        } else {
            return StreamSupport.stream(userDetailsService.getUsers().spliterator(), false)
                    .map(this::erasePassword)
                    .collect(Collectors.toList());
        }
    }

    @ApiOperation(value = "Fetch a single user", notes = "Fetches a single user based on his ID.")
    @GetMapping("users/{id}")
    public ResponseEntity<?> getUser(@ApiParam("The ID of the user")
                                     @PathVariable long id) {
        return userDetailsService.getUserById(id)
                .map(this::erasePassword)
                .map(ResponseEntity::<Object>ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @ApiOperation(value = "Add a new user", notes = "Registers a user with a given username and password.")
    @PostMapping("users")
    public ResponseEntity<?> addUser(
            @ApiParam("The user to add, must only contain username and the password")
            @RequestBody User user, UriComponentsBuilder builder) {
        if (StringUtils.isEmpty(user.getUsername())) {
            return ResponseEntity.badRequest().body("username must not be empty");
        }
        if (StringUtils.isEmpty(user.getPassword())) {
            return ResponseEntity.badRequest().body("password must not be empty");
        }
        try {
            User savedUser = userDetailsService.addOrUpdateUser(
                    user.toBuilder()
                            .id(null)
                            .password(userDetailsService.encodePassword(user.getPassword()))
                            .build());
            return ResponseEntity.created(
                    builder.path("users/{id}").buildAndExpand(savedUser.getId()).toUri())
                    .body(erasePassword(savedUser));
        } catch (JpaSystemException e) {
            log.error("Error adding new user", e);
            return ResponseEntity.badRequest().body("Username already taken");
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
            return ResponseEntity.badRequest().body("User does not exist");
        }
    }

    private User erasePassword(User user) {
        return user.toBuilder().password(null).build();
    }
}
