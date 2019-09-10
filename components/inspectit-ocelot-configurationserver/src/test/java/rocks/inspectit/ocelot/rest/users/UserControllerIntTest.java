package rocks.inspectit.ocelot.rest.users;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.user.User;
import rocks.inspectit.ocelot.user.UserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UserControllerIntTest extends IntegrationTestBase {

    @Autowired
    InspectitServerSettings settings;

    @Autowired
    UserService userService;

    @Autowired
    UserController userController;

    @AfterEach
    void cleanupUsers() {
        userService.getUsers().forEach(u -> {
            if (!u.getUsername().equalsIgnoreCase(settings.getDefaultUser().getName())) {
                userService.deleteUserById(u.getId());
            }
        });
    }

    @Nested
    class SelectUsers {

        @Test
        void noUsernameFilter() {
            userService.addOrUpdateUser(User.builder()
                    .username("John")
                    .password("doe")
                    .build());

            ResponseEntity<List<User>> result = authRest.exchange(
                    "/api/v1/users",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<User>>() {
                    });
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody())
                    .hasSize(2)
                    .anySatisfy((u) -> {
                        assertThat(u.getUsername()).isEqualTo(settings.getDefaultUser().getName());
                        assertThat(u.getPassword()).isNull();
                        assertThat(u.getPasswordHash()).isNull();
                        assertThat(u.getId()).isNotNull();
                    })
                    .anySatisfy((u) -> {
                        assertThat(u.getUsername()).isEqualTo("john");
                        assertThat(u.getPassword()).isNull();
                        assertThat(u.getPasswordHash()).isNull();
                        assertThat(u.getId()).isNotNull();
                    });

        }


        @Test
        void emptyUsernameFilter() {
            userService.addOrUpdateUser(User.builder()
                    .username("John")
                    .password("doe")
                    .build());

            ResponseEntity<List<User>> result = authRest.exchange(
                    "/api/v1/users?username=",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<User>>() {
                    });
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody())
                    .hasSize(2)
                    .anySatisfy((u) -> {
                        assertThat(u.getUsername()).isEqualTo(settings.getDefaultUser().getName());
                        assertThat(u.getPassword()).isNull();
                        assertThat(u.getPasswordHash()).isNull();
                        assertThat(u.getId()).isNotNull();
                    })
                    .anySatisfy((u) -> {
                        assertThat(u.getUsername()).isEqualTo("john");
                        assertThat(u.getPassword()).isNull();
                        assertThat(u.getPasswordHash()).isNull();
                        assertThat(u.getId()).isNotNull();
                    });

        }


        @Test
        void withUsernameFilter() {
            userService.addOrUpdateUser(User.builder()
                    .username("John")
                    .password("doe")
                    .build());

            ResponseEntity<List<User>> result = authRest.exchange(
                    "/api/v1/users?username=JOhN",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<User>>() {
                    });
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody())
                    .hasSize(1)
                    .anySatisfy((u) -> {
                        assertThat(u.getUsername()).isEqualTo("john");
                        assertThat(u.getPassword()).isNull();
                        assertThat(u.getPasswordHash()).isNull();
                        assertThat(u.getId()).isNotNull();
                    });

        }

    }


    @Nested
    class GetUser {

        @Test
        void existingUser() {
            Long id = userService.addOrUpdateUser(User.builder()
                    .username("John")
                    .password("doe")
                    .build())
                    .getId();

            ResponseEntity<User> result = authRest.exchange(
                    "/api/v1/users/" + id,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<User>() {
                    });
            User user = result.getBody();
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(user.getUsername()).isEqualTo("john");
            assertThat(user.getPassword()).isNull();
            assertThat(user.getPasswordHash()).isNull();
            assertThat(user.getId()).isNotNull();

        }


        @Test
        void nonExistingUser() {
            ResponseEntity<User> result = authRest.exchange(
                    "/api/v1/users/123456789",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<User>() {
                    });
            User user = result.getBody();
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        }

    }


    @Nested
    class AddUser {

        @Test
        void usernameAlreadyExists() {
            InputUser userToAdd = new InputUser(settings.getDefaultUser().getName(), "doe", null);

            ResponseEntity<String> result = authRest.postForEntity(
                    "/api/v1/users",
                    userToAdd,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }


        @Test
        void emptyUsername() {
            InputUser userToAdd = new InputUser("", "doe", null);

            ResponseEntity<String> result = authRest.postForEntity(
                    "/api/v1/users",
                    userToAdd,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void nullUsername() {
            InputUser userToAdd = new InputUser(null, "doe", null);

            ResponseEntity<String> result = authRest.postForEntity(
                    "/api/v1/users",
                    userToAdd,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }


        @Test
        void emptyPassword() {
            InputUser userToAdd = new InputUser("John", "", null);

            ResponseEntity<String> result = authRest.postForEntity(
                    "/api/v1/users",
                    userToAdd,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void nullPassword() {
            InputUser userToAdd = new InputUser("John", null, null);

            ResponseEntity<String> result = authRest.postForEntity(
                    "/api/v1/users",
                    userToAdd,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }


        @Test
        void ensureIdIgnored() {
            Long existingId = userService.getUsers().iterator().next().getId();

            InputUser userToAdd = new InputUser("John", "DoE", existingId);

            ResponseEntity<User> result = authRest.postForEntity(
                    "/api/v1/users",
                    userToAdd,
                    User.class);
            User user = result.getBody();
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getHeaders().getFirst("Location")).endsWith("users/" + user.getId());
            assertThat(user.getUsername()).isEqualTo("john");
            assertThat(user.getPassword()).isNull();
            assertThat(user.getPasswordHash()).isNull();
            assertThat(user.getId()).isNotEqualTo(existingId);

            //do a get to check if the user is able to authenticate


            ResponseEntity<List<User>> authResult = rest
                    .withBasicAuth("joHn", "DoE")
                    .exchange(
                            "/api/v1/users",
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<List<User>>() {
                            });
            assertThat(authResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }


    @Nested
    class DeleteUser {

        @Test
        void userNotExisting() {
            ResponseEntity<String> result = authRest.exchange(
                    "/api/v1/users/" + 123456789,
                    HttpMethod.DELETE,
                    null,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }


        @Test
        void userExisting() {
            Long id = userService.addOrUpdateUser(User.builder()
                    .username("John")
                    .password("doe")
                    .build())
                    .getId();

            ResponseEntity<String> result = authRest.exchange(
                    "/api/v1/users/" + id,
                    HttpMethod.DELETE,
                    null,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(userService.getUserByName("John")).isEmpty();
            assertThat(userService.getUserById(id)).isEmpty();
        }
    }

    private static class InputUser {
        private final String username;
        private final String password;
        private final Long id;

        public InputUser(String username, String password, Long id) {
            this.username = username;
            this.password = password;
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public Long getId() {
            return id;
        }
    }
}
