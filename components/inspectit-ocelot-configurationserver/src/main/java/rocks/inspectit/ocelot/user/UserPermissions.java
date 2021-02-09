package rocks.inspectit.ocelot.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user permissions.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPermissions {

    /**
     * True, if the user has write access.
     */
    private boolean write;

    /**
     * True, if the user has promote access.
     */
    private boolean promote;

    /**
     * True, if the user has admin access.
     */
    private boolean admin;
}
