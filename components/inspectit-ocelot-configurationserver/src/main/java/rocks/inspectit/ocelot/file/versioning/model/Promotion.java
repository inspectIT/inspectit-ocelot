package rocks.inspectit.ocelot.file.versioning.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Container class for file promotions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    /**
     * A short message describing the commit.
     */
    @Schema(example = "Added rules for connection tracing")
    private String commitMessage;

    /**
     * Represents the id of the commit which holds the files to promote.
     */
    @Schema(example = "93af736e5b46450789a0ddd39a708f1a815b1ddc")
    private String workspaceCommitId;

    /**
     * The current live branch id which is used as basis for the promotion. This is necessary to determine whether
     * any other promotion has occurred in the meantime.
     */
    @Schema(example = "94f5ba65d4f9f9b3e1bd44e207d9ed5cdf52361b")
    private String liveCommitId;

    /**
     * The files which have to be promoted.
     */
    @Schema(example = "['/file.yml', '/dir/file.yml']")
    private List<String> files;

}
