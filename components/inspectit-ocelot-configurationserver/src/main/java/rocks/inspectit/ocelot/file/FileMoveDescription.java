package rocks.inspectit.ocelot.file;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileMoveDescription {

    @ApiModelProperty(value = "The path of the source file to move or rename")
    private String source;

    @ApiModelProperty(value = "The path of the destination file")
    private String target;
}
