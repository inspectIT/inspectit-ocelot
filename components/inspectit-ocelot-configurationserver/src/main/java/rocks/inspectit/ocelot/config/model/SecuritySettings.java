package rocks.inspectit.ocelot.config.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecuritySettings {

    private boolean ldapAuthentication = false;

    @Builder.Default
    private LdapSettings ldap = LdapSettings.builder().build();
}
