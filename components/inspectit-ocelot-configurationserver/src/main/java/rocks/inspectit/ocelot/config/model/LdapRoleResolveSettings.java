package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration Container for the authentication role resolving of the LDAP-Settings.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LdapRoleResolveSettings {

    /**
     * Roles defined in this list are granted read access.
     */
    @Builder.Default
    private List<String> read = new ArrayList<>();

    /**
     * Roles defined in this list are granted read and write access.
     */
    @Builder.Default
    private List<String> write = new ArrayList<>();

    /**
     * Roles defined in this list are granted read, write and commit access.
     */
    @Builder.Default
    private List<String> commit = new ArrayList<>();

    /**
     * Roles defined in this list are granted read, write, commit and admin access.
     */
    @Builder.Default
    private List<String> admin = new ArrayList<>();
}
