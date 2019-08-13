package rocks.inspectit.ocelot.security.userdetails;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import rocks.inspectit.ocelot.config.model.DefaultUserSettings;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.SecuritySettings;
import rocks.inspectit.ocelot.security.userdetails.LocalUserDetailsService;
import rocks.inspectit.ocelot.user.User;
import rocks.inspectit.ocelot.user.UserRepository;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LocalUserDetailsServiceTest {

    @InjectMocks
    LocalUserDetailsService detailsService;

    @Mock
    UserRepository repository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Nested
    class AddOrUpdateUser {

        @Test
        void verifyUsernameConvertedToLowerCase() {
        }
    }
}
