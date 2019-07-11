package rocks.inspectit.ocelot.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

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

    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FileInfo> children;
}
