package rocks.inspectit.ocelot.file;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Wrapper for a file description, can include meta information about the file in the future.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileData {

    String content;

    @JsonCreator
    public FileData(String content) {
        this.content = content;
    }
}
