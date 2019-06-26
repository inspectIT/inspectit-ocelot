package rocks.inspectit.ocelot.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
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
