package rocks.inspectit.ocelot.file.versioning.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Representing a version in the workspace repository (relating to a commit).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceVersion {

    /**
     * The version id (=commit id).
     */
    private String id;

    /**
     * The date the version was created.
     */
    private long date;

    /**
     * The version's author.
     */
    private String author;

    /**
     * Message describing the version and its change.
     */
    private String message;

    /**
     * Creates a {@link WorkspaceVersion} based on a given {@link RevCommit}.
     *
     * @param commit the commit to derive the version
     *
     * @return the created {@link WorkspaceVersion}
     */
    public static WorkspaceVersion of(RevCommit commit) {
        String commitId = ObjectId.toString(commit.getId());
        String message = commit.getShortMessage();
        int commitTime = commit.getCommitTime();
        String author = commit.getAuthorIdent().getName();

        return WorkspaceVersion.builder().id(commitId).message(message).author(author).date(commitTime).build();
    }

    @Override
    public String toString() {
        return "WorkspaceVersion{" + "id='" + id + '\'' + ", date=" + date + ", author='" + author + '\'' + ", message='" + message + '\'' + '}';
    }
}
