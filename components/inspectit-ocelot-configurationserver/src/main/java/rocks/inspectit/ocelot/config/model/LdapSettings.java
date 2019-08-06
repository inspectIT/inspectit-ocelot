package rocks.inspectit.ocelot.config.model;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration container for the LDAP settings.
 */
@Data
@Builder
public class LdapSettings {

    /**
     * The group name which is required by a user to get admin access rights.
     */
    private String adminGroup;

    /**
     * The LDAP server url.
     */
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
    private String userSearchBase;

    /**
     * The filter expression which is used for user search requests.
     */
    private String userSearchFilter;

    /**
     * The base DN which is used for group search requests.
     */
    private String groupSearchBase;

    /**
     * The filter expression which is used for group search requests.
     */
    private String groupSearchFilter;

}
