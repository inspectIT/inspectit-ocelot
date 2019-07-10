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
import rocks.inspectit.ocelot.user.LocalUserDetailsService;
import rocks.inspectit.ocelot.user.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UserControllerIntTest extends IntegrationTestBase {

    @Autowired
    InspectitServerSettings settings;

    @Autowired
    LocalUserDetailsService userDetailsService;

    @Autowired
    UserController userController;

    @AfterEach
    void cleanupUsers() {
        userDetailsService.getUsers().forEach(u -> {
            if (!u.getUsername().equalsIgnoreCase(settings.getDefaultUser().getName())) {
                userDetailsService.deleteUserById(u.getId());
            }
        });
    }

    @Nested
    class SelectUsers {

        @Test
        void noUsernameFilter() {
            userDetailsService.addOrUpdateUser(User.builder()
                    .username("John")
                    .password(userDetailsService.encodePassword("doe"))
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
                        assertThat(u.getId()).isNotNull();
                    })
                    .anySatisfy((u) -> {
                        assertThat(u.getUsername()).isEqualTo("john");
                        assertThat(u.getPassword()).isNull();
                        assertThat(u.getId()).isNotNull();
                    });

        }


        @Test
        void emptyUsernameFilter() {
            userDetailsService.addOrUpdateUser(User.builder()
                    .username("John")
                    .password(userDetailsService.encodePassword("doe"))
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
                        assertThat(u.getId()).isNotNull();
                    })
                    .anySatisfy((u) -> {
                        assertThat(u.getUsername()).isEqualTo("john");
                        assertThat(u.getPassword()).isNull();
                        assertThat(u.getId()).isNotNull();
                    });

        }


        @Test
        void withUsernameFilter() {
            userDetailsService.addOrUpdateUser(User.builder()
                    .username("John")
                    .password(userDetailsService.encodePassword("doe"))
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
                        assertThat(u.getId()).isNotNull();
                    });

        }

    }


    @Nested
    class GetUser {

        @Test
        void existingUser() {
            Long id = userDetailsService.addOrUpdateUser(User.builder()
                    .username("John")
                    .password(userDetailsService.encodePassword("doe"))
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
            User userToAdd = User.builder()
                    .username(settings.getDefaultUser().getName())
                    .password("doe")
                    .build();

            ResponseEntity<String> result = authRest.postForEntity(
                    "/api/v1/users",
                    userToAdd,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }


        @Test
        void emptyUsername() {
            User userToAdd = User.builder()
                    .username("")
                    .password("doe")
                    .build();

            ResponseEntity<String> result = authRest.postForEntity(
                    "/api/v1/users",
                    userToAdd,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void nullUsername() {
            User userToAdd = User.builder()
                    .username(null)
                    .password("doe")
                    .build();

            ResponseEntity<String> result = authRest.postForEntity(
                    "/api/v1/users",
                    userToAdd,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }


        @Test
        void emptyPassword() {
            User userToAdd = User.builder()
                    .username("John")
                    .password("")
                    .build();

            ResponseEntity<String> result = authRest.postForEntity(
                    "/api/v1/users",
                    userToAdd,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void nullPassword() {
            User userToAdd = User.builder()
                    .username("John")
                    .password(null)
                    .build();

            ResponseEntity<String> result = authRest.postForEntity(
                    "/api/v1/users",
                    userToAdd,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }


        @Test
        void ensureIdIgnored() {
            Long existingId = userDetailsService.getUsers().iterator().next().getId();

            User userToAdd = User.builder()
                    .username("John")
                    .password("DoE")
                    .id(existingId)
                    .build();

            ResponseEntity<User> result = authRest.postForEntity(
                    "/api/v1/users",
                    userToAdd,
                    User.class);
            User user = result.getBody();
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getHeaders().getFirst("Location")).endsWith("users/" + user.getId());
            assertThat(user.getUsername()).isEqualTo("john");
            assertThat(user.getPassword()).isNull();
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
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }


        @Test
        void userExisting() {
            Long id = userDetailsService.addOrUpdateUser(User.builder()
                    .username("John")
                    .password(userDetailsService.encodePassword("doe"))
                    .build())
                    .getId();

            ResponseEntity<String> result = authRest.exchange(
                    "/api/v1/users/" + id,
                    HttpMethod.DELETE,
                    null,
                    String.class);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

            assertThat(userDetailsService.getUserByName("John")).isEmpty();
            assertThat(userDetailsService.getUserById(id)).isEmpty();
        }
    }
}
