package rocks.inspectit.ocelot.file.dirmanagers;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class GitDirectoryManager extends DirectoryManager {
    @VisibleForTesting
    @Autowired
    VersionController versionController;

    /**
     * Adds and commits all current changes to the master branch of the local repo.
     *
     * @return True when the commit was successful.
     */
    public void commitAllChanges() throws GitAPIException {
        versionController.commitAllChanges();
    }

    /**
     * Adds and commits all changes in the files subfolder.
     *
     * @return Returns true when the commit was successful.
     */
    public void commitFiles() throws GitAPIException {
        versionController.commitFile("configuration/");
    }

    /**
     * Commits the AgentMappings file.
     *
     * @return Returns true when the commit was successful.
     */
    public void commitAgentMappingFile() throws GitAPIException {
        versionController.commitFile(AGENT_MAPPINGS_FILE);
    }

    /**
     * Lists all file paths found in the latest commit. Lists only files found in the configuration subfolder
     *
     * @return A list of all file paths found in the latest commit.
     */
    public List<String> listFiles() throws IOException {
        return listFiles(true);
    }

    /**
     * Lists all file paths found in the latest commit. Lists only files found in the configuration subfolder
     *
     * @return A list of all file paths found in the latest commit.
     */
    public List<String> listFiles(boolean onlyConfigurations) throws IOException {
        if (onlyConfigurations) {
            return versionController.listFiles(FILE_PREFIX, false);
        }
        return versionController.listFiles("", false);
    }

    /**
     * Reads the content of a file in the files folder from the current repo and returns it as String.
     *
     * @param fileName The name of the file one wants to read.
     * @return The content of the file as String.
     */
    public String readFile(String fileName) throws IOException {
        return versionController.readFile(FILE_PREFIX + fileName);
    }

    /**
     * Reads the content of the agent mappings file from the current repo and returns it as String.
     *
     * @return The content of the file as String.
     */
    public String readAgentMappingFile() throws IOException {
        return versionController.readFile(AGENT_MAPPINGS_FILE);
    }

    public List listCommits() {
        return versionController.listCommits();
    }

}


