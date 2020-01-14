package rocks.inspectit.ocelot.file.manager.directory;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class DirectoryManager {
    /**
     * The encoding used for the loaded strings.
     */
    static final Charset ENCODING = StandardCharsets.UTF_8;

    @Autowired
    @VisibleForTesting
    InspectitServerSettings config;
}
