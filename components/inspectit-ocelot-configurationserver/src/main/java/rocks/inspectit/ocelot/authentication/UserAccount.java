package rocks.inspectit.ocelot.authentication;

import lombok.Builder;
import lombok.Data;

/**
 * Data model for a user account
 */
@Data
@Builder
public class UserAccount {

    /**
     * Name of the user.
     */
    private String username;

    /**
     * The hashed password.
     */
    private String password;
}
