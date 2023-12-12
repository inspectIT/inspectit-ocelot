package rocks.inspectit.ocelot.config.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration container for the LDAP settings.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LdapSettings {

    /**
     * The group name which is required by a user to get admin access rights.
     *
     * @deprecated This property is deprecated in favor of role based access. A List of LDAP Groups that
     * should be granted admin access can be defined in inspectit-config-server.security.ldap.roles.admin.
     */
    @Deprecated
    private String adminGroup;

    /**
     * The LDAP server url.
     */
    @NotEmpty
    private String url;

    /**
     * The base DN which is used by all actions.
     */
    private String baseDn;

    /**
     * The DN of the manager user which is used to fetch and verify users.
     */
    private String managerDn;

    /**
     * The manager's password.
     */
    private String managerPassword;

    /**
     * The base DN which is used for user search requests.
     */
    @NotNull
    private String userSearchBase;

    /**
     * The filter expression which is used for user search requests.
     */
    @NotNull
    private String userSearchFilter;

    /**
     * The base DN which is used for group search requests.
     */
    @NotNull
    private String groupSearchBase;

    /**
     * The filter expression which is used for group search requests.
     */
    @NotNull
    private String groupSearchFilter;

    /**
     * Contains the LDAP Group mapped to access roles.
     */
    @Builder.Default
    private LdapRoleResolveSettings roles = new LdapRoleResolveSettings();

}
