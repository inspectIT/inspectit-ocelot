package rocks.inspectit.ocelot.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FileInfo {

    enum Type {
        @JsonProperty("directory")
        DIRECTORY,
        @JsonProperty("file")
        FILE
    }

    private Type type;
    private String path;
}
