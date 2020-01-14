package rocks.inspectit.ocelot.file.manager.directory;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileVersionResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * This class manages the versioning of the working directory.
 */
@Slf4j
@Component
public class GitDirectoryManager extends DirectoryManager {

    @Autowired
    @VisibleForTesting
    VersioningManager versioningManager;

    /**
     * Adds and commits all current changes to the master branch of the local repo.
     */
    public void commit() throws GitAPIException {
        versioningManager.commit();
    }

    /**
     * If the given path points to a directory the whole directory is committed. If it points to a single file the
     * single file is committed.
     *
     * @param path the path to the file or directory one wants to commit
     */
    public void commitFile(String path) throws GitAPIException {
        versioningManager.commitFile(path);
    }

    /**
     * Lists all file paths found in the latest commit.
     *
     * @return A list of all file paths found in the latest commit.
     */
    public List<FileInfo> listFiles(String path, boolean recursive) throws IOException {
        return versioningManager.listFiles(path, recursive);
    }

    /**
     * Reads the content of a file in the files folder from the current repo and returns it as String.
     *
     * @param fileName The name of the file one wants to read.
     * @return The content of the file as String.
     */
    public String readFile(String fileName) throws IOException {
        return versioningManager.readFile(fileName);
    }

    /**
     * Returns all commits of the repo as a list of FileVersionResponse Objects.
     *
     * @return A List of FileVersionResponse instances containing the data of all commits.
     */
    public List<FileVersionResponse> getCommits() throws IOException, GitAPIException {
        TreeSet<FileVersionResponse> responseList = new TreeSet<>(new CommitComparator());
        for (ObjectId commitId : versioningManager.getAllCommits()) {
            responseList.add(buildResponseFromCommit(commitId));
        }
        return new ArrayList<>(responseList);
    }

    /**
     * Returns the content of a file found under the given path as it was in the given commit.
     *
     * @param path   The path to the file one wants the content of.
     * @param commit The commit one wants the version of the file from.
     * @return The content of the file from the given commit.
     */
    public String getFileContent(String path, String commit) throws IOException {
        return versioningManager.readFile(path, commit);
    }

    /**
     * Returns all commits in which changes to a given file have been committed.
     *
     * @param filePath the path to the file the commits should be returned of.
     * @return A List of FileVersionResponse Objects which contain the data of each commit.
     */
    public List<FileVersionResponse> getCommitsByFile(String filePath) throws IOException, GitAPIException {
        TreeSet<FileVersionResponse> responseList = new TreeSet<>(new CommitComparator());
        for (ObjectId commitId : versioningManager.getCommitsOfFile(filePath)) {
            responseList.add(buildResponseFromCommit(commitId));
        }
        return new ArrayList<>(responseList);
    }

    /**
     * Sets the author for the next commits.
     *
     * @param name the authors username.
     * @param mail the authors mail adress.
     */
    public void setAuthor(String name, String mail) {
        versioningManager.setAuthor(name, mail);
    }

    /**
     * Returns the name of the given ObjectId.
     *
     * @param id The ObjectId instance the name should be returned from.
     * @return The name of the given ObjectId instance.
     */
    @VisibleForTesting
    String getNameOfObjectId(ObjectId id) {
        return id.getName();
    }

    /**
     * Creates a FileVersionResponse based on the data in a commit with the given id.
     *
     * @param commitId The id of the commit the data should be retrieved from.
     * @return A FileVersionResponse containing Commit Message, commitId, all Paths that have changed in the commit,
     * the time of the commit and the author.
     */
    private FileVersionResponse buildResponseFromCommit(ObjectId commitId) throws IOException {
        return new FileVersionResponse(versioningManager.getFullMessageOfCommit(commitId),
                getNameOfObjectId(commitId),
                versioningManager.getPathsOfCommit(commitId),
                versioningManager.getTimeOfCommit(commitId),
                versioningManager.getAuthorOfCommit(commitId));
    }

    /**
     * This Comparator is used to return the commits in a chronological order.
     */
    public class CommitComparator implements Comparator {
        @Override
        public int compare(Object o, Object t1) {
            FileVersionResponse resp1 = (FileVersionResponse) o;
            FileVersionResponse resp2 = (FileVersionResponse) t1;
            if (resp1.getTimeinMilis() > resp2.getTimeinMilis()) {
                return -1;
            }
            if (resp1.getTimeinMilis() < resp2.getTimeinMilis()) {
                return 1;
            }
            return 1;
        }
    }
}




