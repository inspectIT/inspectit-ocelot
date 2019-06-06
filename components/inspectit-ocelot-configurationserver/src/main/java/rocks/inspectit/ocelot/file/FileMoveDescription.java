package rocks.inspectit.ocelot.file;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FileMoveDescription {

    private String source;
    private String target;
}
