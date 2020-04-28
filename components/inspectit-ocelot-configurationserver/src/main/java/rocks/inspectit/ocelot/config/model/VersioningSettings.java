package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VersioningSettings {

    /**
     * The mail address to use in Git commits.
     */
    private String gitMail;

    /**
     * The username to use in Git commits.
     */
    private String gitUsername;
}
