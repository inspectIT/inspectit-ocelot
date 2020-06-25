package rocks.inspectit.ocelot.file.versioning.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a diff between two commits.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceDiff {

    /**
     * All elements which have been changed.
     */
    @JsonProperty("entries")
    private List<SimpleDiffEntry> diffEntries;

    /**
     * The base commit - representing the live branch.
     */
    private String liveCommitId;

    /**
     * The target commit, holding the new filed - representing the working directory.
     */
    private String workspaceCommitId;
}
