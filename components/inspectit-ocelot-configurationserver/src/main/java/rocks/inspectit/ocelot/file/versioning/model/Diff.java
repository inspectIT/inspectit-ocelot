package rocks.inspectit.ocelot.file.versioning.model;

import lombok.Data;

import java.util.List;

@Data
public class Diff {

    private List<SimpleDiffEntry> diffEntries;

    private String liveCommitId;

    private String workspaceCommitId;
}
