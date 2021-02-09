package rocks.inspectit.ocelot.security.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.authentication.configurers.userdetails.DaoAuthenticationConfigurer;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.config.model.SecuritySettings;
import rocks.inspectit.ocelot.security.userdetails.LocalUserDetailsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigurationTest {

    @InjectMocks
    SecurityConfiguration configuration;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    LocalUserDetailsService localUserDetailsService;

    @Mock
    LdapAuthenticationProviderConfigurer ldapConfigurer;

    @Nested
    class Configure_AuthenticationManagerBuilder {

        @Mock
        AuthenticationManagerBuilder auth;

        @Mock
        DaoAuthenticationConfigurer daoAuthenticationConfigurer;

        @Test
        public void useLocalUserService() throws Exception {
            SecuritySettings securitySettings = SecuritySettings.builder().ldapAuthentication(false).build();
            InspectitServerSettings settings = InspectitServerSettings.builder().security(securitySettings).build();
            configuration.serverSettings = settings;

            when(auth.userDetailsService(any())).thenReturn(daoAuthenticationConfigurer);

            configuration.configure(auth);

            verify(auth).userDetailsService(localUserDetailsService);
            verify(daoAuthenticationConfigurer).passwordEncoder(passwordEncoder);
            verifyNoMoreInteractions(auth);
        }

        @Test
        public void useLdapUserService() throws Exception {
            LdapSettings ldapSettings = LdapSettings.builder()
                    .userSearchFilter("user-filter")
                    .userSearchBase("user-base")
                    .groupSearchFilter("group-filter")
                    .groupSearchBase("group-base")
                    .build();
            SecuritySettings securitySettings = SecuritySettings.builder()
                    .ldapAuthentication(true)
                    .ldap(ldapSettings)
                    .build();
            InspectitServerSettings settings = InspectitServerSettings.builder().security(securitySettings).build();
            configuration.serverSettings = settings;

            when(auth.ldapAuthentication()).thenReturn(ldapConfigurer);
            when(ldapConfigurer.userSearchFilter(anyString())).thenReturn(ldapConfigurer);
            when(ldapConfigurer.userSearchBase(anyString())).thenReturn(ldapConfigurer);
            when(ldapConfigurer.groupSearchFilter(anyString())).thenReturn(ldapConfigurer);
            when(ldapConfigurer.groupSearchBase(anyString())).thenReturn(ldapConfigurer);
            when(ldapConfigurer.userDetailsContextMapper(any())).thenReturn(ldapConfigurer);
            when(auth.userDetailsService(any())).thenReturn(daoAuthenticationConfigurer);

            configuration.configure(auth);

            verify(auth).ldapAuthentication();
            verify(ldapConfigurer).userSearchFilter("user-filter");
            verify(ldapConfigurer).userSearchBase("user-base");
            verify(ldapConfigurer).groupSearchFilter("group-filter");
            verify(ldapConfigurer).groupSearchBase("group-base");
            verify(ldapConfigurer).contextSource(any());
            verify(ldapConfigurer).userDetailsContextMapper(any());
            verify(auth).userDetailsService(localUserDetailsService);
            verifyNoMoreInteractions(auth);
        }
    }

}