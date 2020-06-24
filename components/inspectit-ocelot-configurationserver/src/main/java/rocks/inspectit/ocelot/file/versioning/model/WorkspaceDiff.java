package rocks.inspectit.ocelot.file.versioning.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceDiff {

    private List<SimpleDiffEntry> diffEntries;

    private String liveCommitId;

    private String workspaceCommitId;
}
