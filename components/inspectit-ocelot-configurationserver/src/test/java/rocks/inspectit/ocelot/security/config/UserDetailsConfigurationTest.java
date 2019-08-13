package rocks.inspectit.ocelot.security.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.config.model.SecuritySettings;
import rocks.inspectit.ocelot.security.userdetails.LocalUserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsConfigurationTest {

    @InjectMocks
    private SecurityBeanConfiguration configuration;

    @Nested
    public class PasswordEncoderBean {

        @Test
        public void initBean() {
            PasswordEncoder result = configuration.passwordEncoder();

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(BCryptPasswordEncoder.class);
        }
    }

    @Nested
    public class UserDetailsServiceBean {

        @Mock
        ApplicationContext context;

        @Test
        public void initBeanWithLocalAuthentication() {
            LdapSettings ldapSettings = LdapSettings.builder().build();
            SecuritySettings securitySettings = SecuritySettings.builder().ldap(ldapSettings).ldapAuthentication(false).build();
            InspectitServerSettings settings = InspectitServerSettings.builder().security(securitySettings).build();

            LocalUserDetailsService serviceMock = mock(LocalUserDetailsService.class);
            when(context.getBean(LocalUserDetailsService.class)).thenReturn(serviceMock);

            UserDetailsService result = configuration.userDetailsService(context, settings);

            assertThat(result).isSameAs(serviceMock);
        }

        @Test
        public void initBeanWithLdapAuthentication() {
            LdapSettings ldapSettings = LdapSettings.builder()
                    .userSearchFilter("user-filter")
                    .userSearchBase("user-base")
                    .groupSearchFilter("group-filter")
                    .groupSearchBase("group-base")
                    .build();
            SecuritySettings securitySettings = SecuritySettings.builder().ldap(ldapSettings).ldapAuthentication(true).build();
            InspectitServerSettings settings = InspectitServerSettings.builder().security(securitySettings).build();

            UserDetailsService result = configuration.userDetailsService(context, settings);

            assertThat(result).isInstanceOf(LdapUserDetailsService.class);
        }
    }
}