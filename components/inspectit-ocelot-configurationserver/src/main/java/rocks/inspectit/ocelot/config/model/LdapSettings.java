package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

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
     */
    @NotEmpty
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

    private RoleSettings roles;

}
