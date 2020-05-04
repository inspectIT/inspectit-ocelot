package rocks.inspectit.ocelot.security.config;

/**
 * This class contains all access roles used within the application. The permission sets are used for user objects.
 * Each user object given a specific permission set is granted all access defined in the respective set.
 * The Role fields are used for the @Secured Annotations in the REST-Controller classes.
 */
public class UserRoleConfiguration {

    /**
     * Permission name for read access.
     */
    public static final String READ_ACCESS = "OCELOT_READ";

    /**
     * Permission name for write access.
     */
    private static final String WRITE_ACCESS = "OCELOT_WRITE";

    /**
     * Permission name for commit access.
     */
    private static final String COMMIT_ACCESS = "OCELOT_COMMIT";

    /**
     * Permission name for admin access.
     */
    private static final String ADMIN_ACCESS = "OCELOT_ADMIN";

    /**
     * Permission set for the reader-role.
     */
    public static final String[] READ_ROLE_PERMISSION_SET = {READ_ACCESS};

    /**
     * Permission set for the writer-role.
     */
    public static final String[] WRITE_ROLE_PERMISSION_SET = {READ_ACCESS, WRITE_ACCESS};

    /**
     * Permission set for the committer-role.
     */
    public static final String[] COMMIT_ROLE_PERMISSION_SET = {READ_ACCESS, WRITE_ACCESS, COMMIT_ACCESS};

    /**
     * Permission set for the admin-role.
     */
    public static final String[] ADMIN_ROLE_PERMISSION_SET = {READ_ACCESS, WRITE_ACCESS, COMMIT_ACCESS, ADMIN_ACCESS};

    /**
     * Role prefix used for the Strings used in the security annotations.
     */
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Permission required for read access.
     */
    public static final String READ_ACCESS_ROLE = ROLE_PREFIX + READ_ACCESS;

    /**
     * Permission required for write access.
     */
    public static final String WRITE_ACCESS_ROLE = ROLE_PREFIX + WRITE_ACCESS;

    /**
     * Permission required for commit access.
     */
    public static final String COMMIT_ACCESS_ROLE = ROLE_PREFIX + COMMIT_ACCESS;

    /**
     * Permission required for admin access.
     */
    public static final String ADMIN_ACCESS_ROLE = ROLE_PREFIX + ADMIN_ACCESS;
}
