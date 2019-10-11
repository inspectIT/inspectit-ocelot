package rocks.inspectit.ocelot.file.dirmanagers;

import lombok.Data;

/**
 * This class is used to store the data of the stored data is used in the version controller to set the author for commits.
 */
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

    public GitAuthor(String name, String mail) {
        setName(name);
        setMail(mail);
    }
}
