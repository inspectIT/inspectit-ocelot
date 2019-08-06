package rocks.inspectit.ocelot.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.ldap.core.support.LdapContextSource;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.LdapSettings;
import rocks.inspectit.ocelot.config.model.SecuritySettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LdapUtilsTest {

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

    @Nested
    class CreateLdapContextSource {

        @Test
        void createLdapContextSource() {
            InspectitServerSettings settings = createSettings("ldap://localhost:389", "ou=base", "manager", "password");

            LdapContextSource result = LdapUtils.createLdapContextSource(settings);

            assertThat(result.getUrls()).containsExactly("ldap://localhost:389");
            assertThat(result.getBaseLdapPathAsString()).isEqualTo("ou=base");
            assertThat(result.getUserDn()).isEqualTo("manager");
            assertThat(result.getPassword()).isEqualTo("password");
        }
    }
}