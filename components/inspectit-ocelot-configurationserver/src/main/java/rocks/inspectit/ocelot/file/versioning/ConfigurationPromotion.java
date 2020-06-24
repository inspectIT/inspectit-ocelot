package rocks.inspectit.ocelot.file.versioning;

import lombok.Data;

import java.util.List;

@Data
public class ConfigurationPromotion {

    private String workspaceCommitId;

    private String liveCommitId;

    private List<String> files;

}
