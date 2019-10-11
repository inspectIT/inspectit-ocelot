package rocks.inspectit.ocelot.file.dirmanagers;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
