package rocks.inspectit.ocelot.security.userdetails;

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
        @Test
        public void successfullyFindUser() {
            User user = User.builder().username("username").passwordHash("hash").isLdapUser(false).build();
            when(userService.getUserByName("username")).thenReturn(Optional.of(user));

            UserDetails result = detailsService.loadUserByUsername("username");

            assertThat(result.getUsername()).isEqualTo("username");
            assertThat(result.getPassword()).isEqualTo("hash");
            assertThat(result.getAuthorities())
                    .extracting(object -> object.toString().substring("ROLE_".length()))
                    .containsExactlyInAnyOrder("OCELOT_WRITE", "OCELOT_READ", "OCELOT_PROMOTE", "OCELOT_ADMIN");
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
