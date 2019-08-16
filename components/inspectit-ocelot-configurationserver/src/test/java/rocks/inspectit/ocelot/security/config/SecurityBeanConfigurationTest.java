package rocks.inspectit.ocelot.security.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.config.model.SecuritySettings;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityBeanConfigurationTest {

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
    class CreateLdapContextSource {

        private InspectitServerSettings createSettings(String url, String baseDn, String managerDn, String managerPassword) {
            LdapSettings ldapSettings = LdapSettings.builder()
                    .url(url)
                    .baseDn(baseDn)
                    .managerDn(managerDn)
                    .managerPassword(managerPassword)
                    .build();
            SecuritySettings securitySettings = SecuritySettings.builder()
                    .ldap(ldapSettings)
                    .build();
            InspectitServerSettings settings = InspectitServerSettings.builder()
                    .security(securitySettings)
                    .build();
            return settings;
        }

        @Test
        void createLdapContextSource() {
            InspectitServerSettings settings = createSettings("ldap://localhost:389", "ou=base", "manager", "password");

            LdapContextSource result = configuration.ldapContextSource(settings);

            assertThat(result.getUrls()).containsExactly("ldap://localhost:389");
            assertThat(result.getBaseLdapPathAsString()).isEqualTo("ou=base");
            assertThat(result.getUserDn()).isEqualTo("manager");
            assertThat(result.getPassword()).isEqualTo("password");
        }
    }
}