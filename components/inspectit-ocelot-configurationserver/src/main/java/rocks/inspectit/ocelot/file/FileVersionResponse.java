package rocks.inspectit.ocelot.file;

import lombok.Data;

/**
 * This class holds the data of a commit.
 */
@Data
public class FileVersionResponse {
    /**
     * The title of the given commit
     */
    private String commitTitle;

    /**
     * The object id of the given commit
     */
    private String commitId;

    /**
     * The content of the commit
     */
    private Object commitContent;

    /**
     * The time when the commit was done in milliseconds
     */
    private int timeinMilis;

    /**
     * The author of the commit
     */
    private String author;

    public FileVersionResponse(String commitTitle, String commitId, Object commitContent, int timeinMilis, String author) {
        setCommitTitle(commitTitle);
        setCommitId(commitId);
        setCommitContent(commitContent);
        setTimeinMilis(timeinMilis);
        setAuthor(author);
    }

}
