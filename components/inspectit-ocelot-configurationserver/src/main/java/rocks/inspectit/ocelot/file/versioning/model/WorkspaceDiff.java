package rocks.inspectit.ocelot.file.versioning.model;

import lombok.Data;

import java.util.List;

@Data
public class WorkspaceDiff {

    private List<SimpleDiffEntry> diffEntries;

    private String liveCommitId;

    private String workspaceCommitId;
}
