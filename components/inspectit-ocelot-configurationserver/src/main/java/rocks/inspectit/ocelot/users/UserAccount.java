package rocks.inspectit.ocelot.users;

import lombok.Builder;
import lombok.Data;

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
