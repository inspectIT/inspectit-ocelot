package rocks.inspectit.ocelot.file.manager.directory;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * This class is used to store the data of the stored data is used in the version controller to set the author for commits.
 */
@AllArgsConstructor
@Data
public class GitAuthor {
    /**
     * The name of the author of an commit
     */
    private String name;

    /**
     * The mail of an author of an commit
     */
    private String mail;
}
