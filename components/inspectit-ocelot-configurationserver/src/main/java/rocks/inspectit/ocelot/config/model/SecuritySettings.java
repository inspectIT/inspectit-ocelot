package rocks.inspectit.ocelot.config.model;

import lombok.Data;

@Data
public class SecuritySettings {

    private boolean ldapAuthentication = false;

    private LdapSettings ldap = new LdapSettings();
}
