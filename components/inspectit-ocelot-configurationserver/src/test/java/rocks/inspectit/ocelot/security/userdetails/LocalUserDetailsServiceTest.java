package rocks.inspectit.ocelot.security.userdetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import rocks.inspectit.ocelot.user.User;
import rocks.inspectit.ocelot.user.UserService;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LocalUserDetailsServiceTest {

    @InjectMocks
    LocalUserDetailsService detailsService;

    @Mock
    UserService userService;

    @Nested
    public class LoadUserByUsername {

        @BeforeEach
        public void before() throws Exception {
            Field field = LocalUserDetailsService.class.getDeclaredField("accessRole");
            field.setAccessible(true);
            field.set(detailsService, "role");
        }

        @Test
        public void successfullyFindUser() {
            User user = User.builder().username("username").passwordHash("hash").isLdapUser(false).build();
            when(userService.getUserByName("username")).thenReturn(Optional.of(user));

            UserDetails result = detailsService.loadUserByUsername("username");

            assertThat(result.getUsername()).isEqualTo("username");
            assertThat(result.getPassword()).isEqualTo("hash");
            assertThat(result.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_role");
            verify(userService).getUserByName(anyString());
            verifyNoMoreInteractions(userService);
        }

        @Test
        public void userDoesNotExist() {
            when(userService.getUserByName(anyString())).thenReturn(Optional.empty());

            assertThatExceptionOfType(UsernameNotFoundException.class)
                    .isThrownBy(() -> detailsService.loadUserByUsername("username"))
                    .withMessage("User with username 'username' has not been found.");

            verify(userService).getUserByName(anyString());
            verifyNoMoreInteractions(userService);
        }

        @Test
        public void userIsLdapUser() {
            User user = User.builder().username("username").passwordHash("hash").isLdapUser(true).build();
            when(userService.getUserByName("username")).thenReturn(Optional.of(user));

            assertThatExceptionOfType(UsernameNotFoundException.class)
                    .isThrownBy(() -> detailsService.loadUserByUsername("username"))
                    .withMessage("User with username 'username' has not been found because it is a LDAP user.");

            verify(userService).getUserByName(anyString());
            verifyNoMoreInteractions(userService);
        }
    }
}
