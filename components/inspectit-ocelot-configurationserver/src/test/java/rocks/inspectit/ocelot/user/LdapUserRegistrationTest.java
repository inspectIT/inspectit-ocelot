package rocks.inspectit.ocelot.user;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LdapUserRegistrationTest {

    @InjectMocks
    LdapUserRegistration userRegistration;

    @Mock
    UserService userService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    AuthenticationSuccessEvent authEvent;

    @Nested
    class OnAuthentication {

        @Test
        void verifyLdapUserRegistered() {
            LdapUserDetails ldapUser = Mockito.mock(LdapUserDetails.class);
            doReturn("John").when(ldapUser).getUsername();
            when(authEvent.getAuthentication().getPrincipal()).thenReturn(ldapUser);

            userRegistration.onAuthentication(authEvent);

            ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
            verify(userService).addOrUpdateUser(argument.capture());
            assertThat(argument.getValue().getUsername()).isEqualTo("John");
            assertThat(argument.getValue().isLdapUser()).isEqualTo(true);
        }

        @Test
        void verifyExistingLdapUserHandled() {
            LdapUserDetails ldapUser = Mockito.mock(LdapUserDetails.class);
            doReturn("John").when(ldapUser).getUsername();
            when(authEvent.getAuthentication().getPrincipal()).thenReturn(ldapUser);
            when(userService.userExists("John")).thenReturn(true);

            userRegistration.onAuthentication(authEvent);

            verify(userService).userExists("John");
            verifyNoMoreInteractions(userService);
        }

        @Test
        void verifyExistingLdapUserHandledForConstraintViolation() {
            LdapUserDetails ldapUser = Mockito.mock(LdapUserDetails.class);
            doReturn("John").when(ldapUser).getUsername();
            when(authEvent.getAuthentication().getPrincipal()).thenReturn(ldapUser);

            doReturn(false).when(userService).userExists(any());
            doThrow(new DataAccessException("blub") {
            }).when(userService).addOrUpdateUser(any());

            userRegistration.onAuthentication(authEvent);

            ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
            verify(userService).addOrUpdateUser(argument.capture());
            assertThat(argument.getValue().getUsername()).isEqualTo("John");
            assertThat(argument.getValue().isLdapUser()).isEqualTo(true);
        }

        @Test
        void verifyNonLdapUserIgnored() {
            UserDetails nonLdapUser = Mockito.mock(UserDetails.class);
            when(authEvent.getAuthentication().getPrincipal()).thenReturn(nonLdapUser);

            userRegistration.onAuthentication(authEvent);

            verify(userService, never()).addOrUpdateUser(any());
        }
    }
}
