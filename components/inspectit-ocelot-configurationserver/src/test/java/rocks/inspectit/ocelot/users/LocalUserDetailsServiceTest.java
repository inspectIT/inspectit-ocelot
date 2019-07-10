package rocks.inspectit.ocelot.users;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.user.LocalUserDetailsService;
import rocks.inspectit.ocelot.user.User;
import rocks.inspectit.ocelot.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LocalUserDetailsServiceTest {

    @Mock
    UserRepository repository;

    @InjectMocks
    LocalUserDetailsService detailsService;

    @Nested
    class AddOrUpdateUser {

        @Test
        void verifyUsernameConvertedToLowerCase() {
            User result = new User();
            User input = User.builder()
                    .id(42L)
                    .password("test")
                    .username("CamelCase")
                    .build();
            doReturn(result).when(repository).save(any());

            User actualResult = detailsService.addOrUpdateUser(input);

            ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
            verify(repository).save(argument.capture());
            assertThat(argument.getValue()).isEqualTo(
                    input.toBuilder().username("camelcase").build()
            );
            assertThat(actualResult).isSameAs(result);
        }
    }


    @Nested
    class GetUserByName {

        @Test
        void verifyUsernameConvertedToLowerCase() {
            detailsService.getUserByName("CamelCase");

            ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
            verify(repository).findByUsername(argument.capture());
            assertThat(argument.getValue()).isEqualTo("camelcase");
        }

    }
}
