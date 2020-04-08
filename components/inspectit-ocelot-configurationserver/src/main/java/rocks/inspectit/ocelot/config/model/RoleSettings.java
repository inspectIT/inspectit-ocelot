package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Configuration Container for the authentication role resolving of the LDAP-Settings.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleSettings {

    /**
     * Roles defined in this list are granted read access.
     */
    private List<String> read;

    /**
     * Roles defined in this list are granted read and write access.
     */
    private List<String> write;

    /**
     * Roles defined in this list are granted read, write and commit access.
     */
    private List<String> commit;

    /**
     * Roles defined in this list are granted read, write, commit and admin access.
     */
    private List<String> admin;
}
