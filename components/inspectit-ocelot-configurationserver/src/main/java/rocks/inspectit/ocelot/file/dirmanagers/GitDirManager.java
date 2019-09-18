package rocks.inspectit.ocelot.file.dirmanagers;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
public class GitDirManager {

    /**
     * The name of the agent mappings Yaml file used to read and persist mappings.
     */
    private static final String AGENT_MAPPINGS_FILE = "agent_mappings.yaml";

    private static final String FILES_SUBFOLDER = "git/files";

    private static final String FILE_PREFIX = "files/";

    /**
     * The path under which the file system accessible by this component lies.
     * This is the absolute, normalized path represented by {@link InspectitServerSettings#getWorkingDirectory()} with {@link #FILES_SUBFOLDER} appended.
     */
    private Path filesRoot;

    @Autowired
    InspectitServerSettings config;

    @VisibleForTesting
    @Autowired
    GitProvider gitProvider;

    @VisibleForTesting
    @Autowired
    WorkingDirManager workingDirManager;

    @VisibleForTesting
    void init() {
        try {
            filesRoot = Paths.get(config.getWorkingDirectory()).resolve("git/files").toAbsolutePath().normalize();
            Files.createDirectories(filesRoot);
            filesRoot = Paths.get(config.getWorkingDirectory()).resolve(FILES_SUBFOLDER).toAbsolutePath().normalize();
            Files.createDirectories(filesRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds and commits all current changes to the master branch of the local repo
     *
     * @return
     * @throws GitAPIException
     */
    public boolean commitAllChanges() throws GitAPIException {
        return gitProvider.commitAllChanges();
    }

    /**
     * Adds and commits all changes in the files subfolder
     *
     * @return
     * @throws GitAPIException
     * @throws IOException
     */
    public boolean commitFiles() throws GitAPIException, IOException {
        gitProvider.commitFile("files/");
        return true;
    }

    /**
     * Commits the AgentMappings file
     *
     * @return
     * @throws GitAPIException
     * @throws IOException
     */
    public boolean commitAgentMappingFile() throws GitAPIException, IOException {
        gitProvider.commitFile(AGENT_MAPPINGS_FILE);
        return true;
    }

    /**
     * Lists all files saved in the files folder from the last commit
     *
     * @return
     * @throws IOException
     */
    public List<String> listFiles() throws IOException {
        return gitProvider.listFiles(FILE_PREFIX, false);
    }

    /**
     * Lists all files saved in the files folder from the last commit
     *
     * @return
     * @throws IOException
     */
    public List<String> listSPFiles() throws IOException {
        return gitProvider.listFiles("", true);
    }


    /**
     * Reads the content of a file in the files folder from the current repo and returns it as String
     *
     * @param fileName The name of the file one wants to read
     * @return The content of the file as String
     * @throws IOException
     */
    public String readFile(String fileName) throws IOException {
        return gitProvider.readFile(FILE_PREFIX + fileName);
    }

    /**
     * Reads the content of the agent mappings file from the current repo and returns it as String
     *
     * @return The content of the file as String
     * @throws IOException
     */
    public String readAgentMappingFile() throws IOException {
        return gitProvider.readFile(AGENT_MAPPINGS_FILE);
    }

}


