package rocks.inspectit.ocelot.file.dirmanagers;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.FileChangedEvent;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class DirectoryManager {

    /**
     * The name of the agent mappings Yaml file used to read and persist mappings.
     */
    static final String AGENT_MAPPINGS_FILE = "agent_mappings.yaml";

    /**
     * The subfolder within the working directory in which the agent mappings file is present.
     */
    @VisibleForTesting
    static final String AGENT_MAPPING_SUBFOLDER = "files";

    /**
     * The agent mapping subfolder resolved as a path object.
     */
    @VisibleForTesting
    Path workingDirRoot;

    /**
     * The subfolder within the working directory in which the configuration files are stored.
     */
    @VisibleForTesting
    static final String FILES_SUBFOLDER = "files/configuration";

    /**
     * The prefix of the path of files found in the configuration folder.
     */
    static final String FILE_PREFIX = "configuration/";

    /**
     * The encoding used for the loaded strings.
     */
    static final Charset ENCODING = StandardCharsets.UTF_8;

    @Autowired
    @VisibleForTesting
    InspectitServerSettings config;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * The path under which the file system accessible by this component lies.
     * This is the absolute, normalized path represented by {@link InspectitServerSettings#getWorkingDirectory()} with {@link #FILES_SUBFOLDER} appended.
     */
    @VisibleForTesting
    Path configurationRoot;

    @PostConstruct
    @VisibleForTesting
    void init() throws IOException {
        configurationRoot = Paths.get(config.getWorkingDirectory()).resolve(FILES_SUBFOLDER).toAbsolutePath().normalize();
        Files.createDirectories(configurationRoot);
        workingDirRoot = Paths.get(config.getWorkingDirectory()).resolve(AGENT_MAPPING_SUBFOLDER).toAbsolutePath().normalize();
        Files.createDirectories(workingDirRoot);
    }

    /**
     * Publishes a FileChangedEvent. This method should be used in all methods which are used to edit files.
     */
    void fireFileChangeEvent() {
        eventPublisher.publishEvent(new FileChangedEvent(this));
    }
}
