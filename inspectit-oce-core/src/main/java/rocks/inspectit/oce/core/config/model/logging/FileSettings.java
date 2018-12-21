package rocks.inspectit.oce.core.config.model.logging;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

@Data
@NoArgsConstructor
public class FileSettings {

    private boolean enabled;
    private String pattern;
    private Path path;
    private boolean includeServiceName;

}
