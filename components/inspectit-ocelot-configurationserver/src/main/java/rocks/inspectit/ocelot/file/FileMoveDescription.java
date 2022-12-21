package rocks.inspectit.ocelot.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMoveDescription {

    @Schema(description = "The path of the source file to move or rename")
    private String source;

    @Schema(description = "The path of the destination file")
    private String target;
}
