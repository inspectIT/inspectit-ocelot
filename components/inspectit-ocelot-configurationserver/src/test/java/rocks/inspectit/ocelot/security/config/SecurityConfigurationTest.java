package rocks.inspectit.ocelot.security.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.SecuritySettings;
import rocks.inspectit.ocelot.user.userdetails.UserDetailsServiceManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SecurityConfigurationTest {

    @InjectMocks
    SecurityConfiguration configuration;

    @Mock
    UserDetailsServiceManager userDetailsServiceManager;

    @Nested
    class Configure_AuthenticationManagerBuilder {

        @Mock
        AuthenticationManagerBuilder auth;

        @Test
        public void useLocalUserService() throws Exception {
            SecuritySettings securitySettings = SecuritySettings.builder().ldapAuthentication(false).build();
            InspectitServerSettings settings = InspectitServerSettings.builder().security(securitySettings).build();
            configuration.serverSettings = settings;

            configuration.configure(auth);

            verify(auth).userDetailsService(any());
        }

    }

}