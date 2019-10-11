package rocks.inspectit.ocelot.file.dirmanagers;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class VersionController {

    /**
     * The encoding used for the loaded strings.
     */
    private static final Charset ENCODING = StandardCharsets.UTF_8;

    /**
     * The subfolder of the working directory in which files managed by this class can be found.
     */
    private static final String FILES_SUBFOLDER = "files";

    /**
     * The date & time format used for commit messages.
     */
    private static final String DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    /**
     * The root path of the files managed by this class.
     */
    private Path filesRoot;

    private GitAuthor author;

    @VisibleForTesting
    @Autowired
    InspectitServerSettings config;

    /**
     * An instance of the local git provided by JGit.
     */
    Git git;

    /**
     * An instance of the local repo provided by the instance of the local git.
     */
    Repository repo;

    /**
     * Sets up the Git repo for usage.
     */
    @PostConstruct
    @VisibleForTesting
    void init() {
        try {
            //Setup the files folder in the working directory
            Path filesRoot = getNormalizedPath("files");
            Files.createDirectories(filesRoot);

            //Setup the configuration folder in the files folder
            File localPath = new File(String.valueOf(filesRoot));
            Path configurationRoot = getNormalizedPath(FILES_SUBFOLDER);
            Files.createDirectories(configurationRoot);

            //Initialise git
            git = Git.init().setDirectory(localPath).call();
            repo = git.getRepository();
            setAuthor("System", "maintainer@test.de");

            //If there is no .git folder present, commit all files found in the directory to the local repo.
            if (isGitRepository()) {
                log.info("Initially committing files in the directory...");
                commitAllChanges();
            }
            log.info("Git directory set up successfully at {} !", localPath.toString());
        } catch (GitAPIException | IOException e) {
            log.error("Error setting up git directory");
        }
    }

    /**
     * Checks if the .git folder is present in the file system.
     *
     * @return True: the .git folder was found and thus git has been setup on the file system.
     */
    private boolean isGitRepository() {
        return Files.exists(getNormalizedPath("git/.git"));
    }

    /**
     * Resolves a given string to a Path object. The resolved path is always depends on the working directory defined in
     * the application.yaml.
     *
     * @param pathToResolve The path one wants to resolve.
     * @return The resolved path.
     */
    private Path getNormalizedPath(String pathToResolve) {
        return Paths.get(config.getWorkingDirectory()).resolve(pathToResolve).toAbsolutePath().normalize();
    }

    /**
     * Adds all local files and commits them to the repo.
     *
     * @return returns true if the commit was successful.
     */
    public void commitAllChanges() throws GitAPIException {
        addAllFiles();
        commitAll();
    }

    /**
     * Commits all changes to the master branch of the local repo.
     *
     * @return Returns true of the commits was successful.
     */
    private void commitAll() throws GitAPIException {
        SimpleDateFormat formatter = new SimpleDateFormat(DATETIME_FORMAT);
        Date date = new Date();
        git.commit()
                .setAll(true)
                .setAuthor(author.getName(), author.getMail())
                .setMessage("Commit changes to all files on " + formatter.format(date))
                .call();

        log.info("Committed all changes to repository at {}", repo.getDirectory());
        git.reset();
    }

    /**
     * Commits all currently added changes to the master branch of the local repo.
     *
     * @param commitCommand A CommitCommand to which all files are added one wants to commit.
     * @return returns true if the commit was successful.
     */
    private void commit(CommitCommand commitCommand) throws GitAPIException {
        addAllFiles();
        SimpleDateFormat formatter = new SimpleDateFormat(DATETIME_FORMAT);
        Date date = new Date();
        commitCommand.setMessage("Committed changes to files on " + formatter.format(date))
                .setAuthor(author.getName(), author.getMail())
                .call();

        log.info("Committed changes to repository at {}", repo.getDirectory());
    }

    /**
     * Adds all files in the current directory to the repo.
     */
    private void addAllFiles() throws GitAPIException {
        git.add()
                .addFilepattern(".")
                .call();
    }

    /**
     * Commits all files found in the given path.
     *
     * @Return returns true if the commit was successful.
     */
    public void commitFile(String filePath) throws GitAPIException {
        CommitCommand commitCommand = git.commit().setOnly(filePath);
        commit(commitCommand);
    }

    /**
     * Lists all file paths in the last commit which do not start with the given prefix.
     *
     * @return A list of paths which have been updated in the last commit.
     */
    public List<String> listFiles(String filePrefix, boolean onlyConfigurations) throws IOException {
        if (repo.resolve(Constants.HEAD) == null) {
            return Collections.emptyList();
        }
        ArrayList<String> filesFromLastCommit = new ArrayList<>();
        TreeWalk treeWalk = getTreeWalk();
        while (treeWalk.next()) {
            if (treeWalk.isSubtree()) {
                treeWalk.enterSubtree();
            } else {
                if (isInPath(treeWalk, filePrefix)) {
                    String fileName = treeWalk.getPathString();
                    if (onlyConfigurations) {
                        if (fileName.startsWith("configuration")) {
                            if (fileName.startsWith(filePrefix)) {
                                filesFromLastCommit.add(fileName.replaceFirst(filePrefix, ""));
                            }
                        }
                    } else {
                        if (fileName.startsWith(filePrefix)) {
                            filesFromLastCommit.add(fileName.replaceFirst(filePrefix, ""));
                        }
                    }
                }
            }
        }
        return filesFromLastCommit;
    }

    /**
     * Returns the TreeWalk Object from the last commit.
     *
     * @return The TreeWalk Object from the current repo.
     */
    @VisibleForTesting
    TreeWalk getTreeWalk() throws IOException {
        RevTree tree = getTree();
        TreeWalk treeWalk = new TreeWalk(repo);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        return treeWalk;
    }

    /**
     * Returns the TreeWalk Object from the commit with the given id.
     *
     * @return The TreeWalk Object from the current repo.
     */
    @VisibleForTesting
    TreeWalk getTreeWalk(ObjectId commitId) throws IOException {
        RevCommit commit = getCommitById(commitId);
        RevTree tree = commit.getTree();
        TreeWalk treeWalk = new TreeWalk(repo);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        return treeWalk;
    }

    /**
     * Returns the RevTree of the current repo.
     *
     * @return The RevTree Object of the most recent commit to the repo.
     */
    private RevTree getTree() throws IOException {
        ObjectId lastCommitId = repo.resolve(Constants.HEAD);
        try (RevWalk revWalk = getRevWalk()) {
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            RevTree tree = commit.getTree();
            return tree;
        }
    }

    /**
     * Creates a new instance of a RevWalk object with the current repository.
     *
     * @return A RevWalk Object with the current repository.
     */
    @VisibleForTesting
    RevWalk getRevWalk() {
        return new RevWalk(repo);
    }

    /**
     * Checks if a given TreeWalk object's path starts with a given prefix.
     *
     * @param treeWalk   The TreeWalk object one wants to check the path of.
     * @param filePrefix The path in which the TreeWalk Object should be found.
     * @return Returns true if the given TreeWalk object can be found in a given path.
     */
    private boolean isInPath(TreeWalk treeWalk, String filePrefix) {
        return treeWalk.getPathString().startsWith(filePrefix);
    }

    /**
     * Reads the content of a file from the latest commit.
     *
     * @param filePath the path to the file.
     * @return The file's content as it is found in the latest commit.
     */
    public String readFile(String filePath) throws IOException {
        ObjectId lastCommitId = repo.resolve(Constants.HEAD);
        return readFileFromCommit(filePath, lastCommitId);
    }

    /**
     * Returns the content of a given path as it was in the given commit.
     *
     * @param path   The path to the file one wants the content of.
     * @param commit The commit id one wants to get the files content from.
     * @return The file's content as it is found in the given commit.
     */
    public String getFileFromVersion(String path, Object commit) throws IOException {
        return readFileFromCommit(path, commit);
    }

    /**
     * Searches for a commit with the given ID. Then searches within this commit for a file with a given path at returns
     * the files content as it is present in the given commit.
     * Returns null if the file is not found in the given commit.
     *
     * @param filePath The path to the file.
     * @param commitId The ID of the commit the file's content needs to be retrieved from.
     * @return The file's content as it is found in the given commit.
     */
    private String readFileFromCommit(String filePath, Object commitId) throws IOException {
        ObjectId resolvedId = resolveCommitId(commitId);
        if (resolvedId == null) {
            log.error("Could not find commit with id {} from git repo", commitId);
            return null;
        }

        TreeWalk treeWalk = getTreeWalk(resolvedId);
        treeWalk.setFilter(PathFilter.create(filePath));
        if (!treeWalk.next()) {
            log.error("Could not read file {} from git repo", filePath);
            return null;
        }

        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = repo.open(objectId);
        if (loader != null) {
            return getStringFromLoader(loader);
        }
        return null;
    }

    String getStringFromLoader(ObjectLoader loader) {
        return new String(loader.getBytes(), ENCODING);
    }

    /**
     * If a String is passed as an argument to this method it returns an ObjectId instance based on this string.
     * If an ObjectId instance is passed, this instance is returned.
     * If neither a String nor an ObjectId is passed null is returned.
     *
     * @param id The Id which should be turned into an ObjectId.
     * @return An ObjectId instance based on the given parameter.
     */
    @VisibleForTesting
    ObjectId resolveCommitId(Object id) {
        ObjectId objectId = null;
        if (id instanceof String) {
            objectId = ObjectId.fromString((String) id);
        } else if (id instanceof ObjectId) {
            objectId = (ObjectId) id;
        } else {
            return null;
        }
        return objectId;
    }

    /**
     * Returns the ids of all commits present on the local repo.
     *
     * @return The ids of the commits present on the local repo.
     */
    public List<ObjectId> getAllCommits() throws IOException, GitAPIException {
        String treeName = "refs/heads/master";
        List<ObjectId> commitIds = new ArrayList<>();
        ObjectId resolvedRepo = repo.resolve(treeName);
        if (resolvedRepo == null) {
            return commitIds;
        }
        for (RevCommit commit : git.log().add(resolvedRepo).call()) {
            commitIds.add(commit.getId());
        }
        return commitIds;
    }

    /**
     * Returns all the ids of all commits which introduced a new version of a given file.
     *
     * @param filePath the file.
     * @return A List of ObjectIds of all commits where the given file was edited.
     */
    public List<ObjectId> getCommitsOfFile(String filePath) throws IOException, GitAPIException {
        return getAllCommits().stream()
                .filter(commitId -> commitContainsPath(filePath, commitId))
                .collect(Collectors.toList());

    }

    /**
     * Returns true if a given file was edited in a given commit.
     * Uses the resolveCommitId method. Therefore either a String or an ObjectId can be used as commit Id.
     *
     * @param filePath the path of the file one wants to check.
     * @param commitId the id of the commit one wants to check.
     * @return Returns true if the given file was edited in the given commit.
     */
    @VisibleForTesting
    boolean commitContainsPath(String filePath, Object commitId) {
        ObjectId resolvedCommitId = resolveCommitId(commitId);
        LogCommand logCommand = null;
        try {
            logCommand = git.log()
                    .add(git.getRepository().resolve(Constants.HEAD))
                    .addPath(filePath);
        } catch (IOException e) {
            log.error("Error while perfoming Git operation git.log(): " + e.getMessage());
        }
        try {
            for (RevCommit revCommit : logCommand.call()) {
                if (revCommit.getId().equals(resolvedCommitId)) {
                    return true;
                }
            }
        } catch (GitAPIException e) {
            log.error("Error while perfoming Git operation logCommand.call(): " + e.getMessage());
        }
        return false;
    }

    /**
     * This method sets the author of the commits added to the local repo.
     *
     * @param name the authors username.
     * @param mail the authors email adress.
     */
    public void setAuthor(String name, String mail) {
        author = new GitAuthor(name, mail);
    }

    /**
     * Returns the time the commit with the given id was committed on.
     *
     * @param commitId The id of the commit one wants the time of.
     * @return The time when the commit was committed in milliseconds.
     */
    public int getTimeOfCommit(Object commitId) throws IOException {
        RevCommit commit = getCommitById(commitId);
        return commit.getCommitTime() * 100;

    }

    /**
     * Returns the author's name of a commit.
     *
     * @param commitId the id of the commit of which one wants to get the author's name from.
     * @return The author's name.
     */
    public String getAuthorOfCommit(Object commitId) throws IOException {
        RevCommit commit = getCommitById(commitId);
        return commit.getAuthorIdent().getName();
    }

    /**
     * Returns the full message of a commit.
     *
     * @param commitId the id of the commit of which one wants to get the full message from.
     * @return the full message.
     */
    public String getFullMessageOfCommit(Object commitId) throws IOException {
        RevCommit commit = getCommitById(commitId);
        return commit.getFullMessage();
    }

    /**
     * Returns a commit which can be found under a specific id as RevCommit object.
     *
     * @param id The id of the commit one wants to get.
     * @return the commit as RevCommit object.
     */
    public RevCommit getCommitById(Object id) throws IOException {
        RevCommit commit;
        ObjectId lastCommitId = resolveCommitId(resolveCommitId(id));
        try (RevWalk revWalk = getRevWalk()) {
            commit = revWalk.parseCommit(lastCommitId);
        }
        return commit;
    }

    /**
     * Returns all file paths present in a commit.
     *
     * @param id The id of the commit one wants to get the paths of.
     * @return A List of paths found in the commit.
     */
    public List<String> getPathsOfCommit(Object id) throws IOException {
        RevWalk revWalk = getRevWalk();
        ObjectId head = repo.resolve(Constants.HEAD);
        RevCommit commit = revWalk.parseCommit(head);
        RevCommit[] parentList = getParentsOfRevCommit(commit);
        if (parentList.length == 0) {
            return getAllFiles(id);
        }
        RevCommit parent = revWalk.parseCommit(getParentOfRevCommit(commit, 0).getId());
        DiffFormatter df = getDiffFormatter();
        df.setRepository(repo);
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setDetectRenames(true);
        return df.scan(getRevtreeOfRevCommit(parent), getRevtreeOfRevCommit(commit))
                .stream()
                .map(diff -> diff.getNewPath())
                .collect(Collectors.toList());
    }

    /**
     * Creates and returns a new instance of the gitDiffFormatter class. Used 'DisabledOutputStream.INSTANCE' as
     * constructor argument for the instance.
     *
     * @return A new instance of the DiffFormatter class.
     */
    @VisibleForTesting
    DiffFormatter getDiffFormatter() {
        return new DiffFormatter(DisabledOutputStream.INSTANCE);
    }

    /**
     * Invokes the getParents method of a given commit and returns it's result.
     *
     * @param commit The commit getParents() should be called on.
     * @return The parent RevWalk of the commit as array.
     */
    @VisibleForTesting
    RevCommit[] getParentsOfRevCommit(RevCommit commit) {
        return commit.getParents();
    }

    /**
     * Invokes the getParent method of a given commit with a given integer and returns it's result.
     *
     * @param commit      The commit getParent should be called on.
     * @param parentIndex The index which should be passed to the getParent method.
     * @return The parent RevWalk that can be found in the given index.
     */
    @VisibleForTesting
    RevCommit getParentOfRevCommit(RevCommit commit, int parentIndex) {
        return commit.getParent(parentIndex);
    }

    /**
     * Invokes the getTree method of a given commit and returns the value returned by the method.
     *
     * @param commit The commit getTree should be called on.
     * @return The RevTree which was returned by the getTree method.
     */
    @VisibleForTesting
    RevTree getRevtreeOfRevCommit(RevCommit commit) {
        return commit.getTree();
    }

    /**
     * Returns a List of files which have been edited in the given commit.
     *
     * @param id the id of the commit the files should be retrieved from.
     * @return The List of files changed in the given commit.
     */
    private List<String> getAllFiles(Object id) throws IOException {
        List<String> filePaths = new ArrayList<>();
        try (TreeWalk treeWalk = getTreeWalk()) {
            treeWalk.reset(getCommitById(id).getTree());
            while (treeWalk.next()) {
                filePaths.add(treeWalk.getPathString());
            }
        }
        return filePaths;
    }
}
