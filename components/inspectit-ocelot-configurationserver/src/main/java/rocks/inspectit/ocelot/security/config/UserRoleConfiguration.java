package rocks.inspectit.ocelot.security.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class contains all access roles used within the application. The permission sets are used for user objects.
 * Each user object given a specific permission set is granted all access defined in the respective set.
 * The Role fields are used for the @Secured Annotations in the REST-Controller classes.
 */
public class UserRoleConfiguration {

    /**
     * Permission name for read access without the role-prefix. This prefix may not be present here since this variable
     * is used for the security configuration.
     */
    public static final String READ_ACCESS = "OCELOT_READ";

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
    public static final String WRITE_ACCESS_ROLE = ROLE_PREFIX + "OCELOT_WRITE";

    /**
     * Permission required for commit access.
     */
    public static final String PROMOTE_ACCESS_ROLE = ROLE_PREFIX + "OCELOT_PROMOTE";

    /**
     * Permission required for admin access.
     */
    public static final String ADMIN_ACCESS_ROLE = ROLE_PREFIX + "OCELOT_ADMIN";

    /**
     * Permission set for the reader-role.
     */
    public static final List<? extends GrantedAuthority> READ_ROLE_PERMISSION_SET = Collections.singletonList(
            new SimpleGrantedAuthority(READ_ACCESS_ROLE)
    );

    /**
     * Permission set for the writer-role.
     */
    public static final List<? extends GrantedAuthority> WRITE_ROLE_PERMISSION_SET = Arrays.asList(
            new SimpleGrantedAuthority(READ_ACCESS_ROLE),
            new SimpleGrantedAuthority(WRITE_ACCESS_ROLE)
    );

    /**
     * Permission set for the committer-role.
     */
    public static final List<? extends GrantedAuthority> PROMOTE_ROLE_PERMISSION_SET = Arrays.asList(
            new SimpleGrantedAuthority(READ_ACCESS_ROLE),
            new SimpleGrantedAuthority(WRITE_ACCESS_ROLE),
            new SimpleGrantedAuthority(PROMOTE_ACCESS_ROLE)
    );

    /**
     * Permission set for the admin-role.
     */
    public static final List<? extends GrantedAuthority> ADMIN_ROLE_PERMISSION_SET = Arrays.asList(
            new SimpleGrantedAuthority(READ_ACCESS_ROLE),
            new SimpleGrantedAuthority(WRITE_ACCESS_ROLE),
            new SimpleGrantedAuthority(PROMOTE_ACCESS_ROLE),
            new SimpleGrantedAuthority(ADMIN_ACCESS_ROLE)
    );
}
