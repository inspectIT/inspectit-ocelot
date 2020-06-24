package rocks.inspectit.ocelot.file.versioning.model;

import lombok.Data;

import java.util.List;

@Data
public class ConfigurationPromotion {

    private String workspaceCommitId;

    private String liveCommitId;

    private List<String> files;

}
