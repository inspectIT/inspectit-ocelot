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
            Path filesRoot = resolvePath("files");
            Files.createDirectories(filesRoot);

            //Setup the configuration folder in the files folder
            File localPath = new File(String.valueOf(filesRoot));
            Path configurationRoot = resolvePath(FILES_SUBFOLDER);
            Files.createDirectories(configurationRoot);

            //Initialise git
            git = Git.init().setDirectory(localPath).call();
            repo = git.getRepository();

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
        return !Files.exists(resolvePath("git/.git"));
    }

    /**
     * Resolves a given string to a Path object. The resolved path is always depends on the working directory defined in
     * the application.yaml.
     *
     * @param pathToResolve The path one wants to resolve.
     * @return The resolved path.
     */
    private Path resolvePath(String pathToResolve) {
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
    //TODO Return values entfernen
    private void commitAll() throws GitAPIException {
        SimpleDateFormat formatter = new SimpleDateFormat(DATETIME_FORMAT);
        Date date = new Date();
        git.commit()
                .setAll(true)
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
        commit(git.commit().setOnly(filePath));
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
     * Returns the TreeWalk Object from the current repo.
     *
     * @return The TreeWalk Object from the current repo.
     */
    @VisibleForTesting
    TreeWalk getTreeWalk() throws IOException {
        RevTree tree = getTree();
        TreeWalk treeWalk = new TreeWalk(repo);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(false);
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
     * Reads the content of a file from the current repo.
     *
     * @return The content of the file as String.
     */
    public String readFile(String filePath) throws IOException {
        ObjectLoader loader;
        ObjectId lastCommitId = repo.resolve(Constants.HEAD);
        RevWalk revWalk = getRevWalk();
        try {
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            RevTree tree = commit.getTree();
            TreeWalk treeWalk = new TreeWalk(repo);
            try {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(filePath));
                if (!treeWalk.next()) {
                    log.error("Could not read file {} from git repo", filePath);
                    return null;
                }
                ObjectId objectId = treeWalk.getObjectId(0);
                loader = repo.open(objectId);
            } finally {
                treeWalk.close();
            }
            if (loader != null) {
                return new String(loader.getBytes(), ENCODING);
            }
        } finally {
            revWalk.dispose();
        }
        return null;
    }


    private ObjectId resolveCommitId(Object id) {
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
     * Returns the ids of all commits present on the local repo
     *
     * @return The ids of the commits present on the local repo
     * @throws IOException
     * @throws GitAPIException
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
     * Returns all the ids of all commits which introduced a new version of a given file
     *
     * @param filePath the file
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    public List<ObjectId> getCommitsOfFile(String filePath) throws IOException, GitAPIException {
        return getAllCommits().stream()
                .filter(commitId -> commitContainsPath(filePath, commitId))
                .collect(Collectors.toList());

    }

    boolean commitContainsPath(String filePath, Object commitId) {
        ObjectId resolvedCommmitId = resolveCommitId(commitId);
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
                if (revCommit.getId().equals(resolvedCommmitId)) {
                    return true;
                }
            }
        } catch (GitAPIException e) {
            log.error("Error while perfoming Git operation logCommand.call(): " + e.getMessage());
        }
        return false;
    }

    /**
     * This method sets the author of the commits added to the local repo
     *
     * @param name the authors username
     * @param mail the authors email adress
     * @return
     */

    public void setAuthor(String name, String mail) {
        author = new GitAuthor(name, mail);
    }

    /**
     * Returns the time the commit with the given id was committed on.
     *
     * @param commitId The id of the commit one wants the time of
     * @return The time when the commit was committed in milliseconds
     * @throws IOException
     */
    public int getTimeOfCommit(Object commitId) throws IOException {
        RevCommit commit = getCommitById(commitId);
        return commit.getCommitTime() * 100;

    }

    /**
     * Returns AuthorIdent of the author of a commit
     *
     * @param commitId
     * @return
     * @throws IOException
     */
    public String getAuthorOfCommit(Object commitId) throws IOException {
        RevCommit commit = getCommitById(commitId);
        return commit.getAuthorIdent().getName();
    }

    /**
     * Returns a commit which can be found under a specific id as RevCommit object
     *
     * @param id The id of the commit one wants to get
     * @return the commit as RevCommit object
     * @throws IOException
     */
    public RevCommit getCommitById(Object id) throws IOException {
        ObjectId commitId = resolveCommitId(id);
        RevCommit commit;
        try (Repository repository = repo) {
            ObjectId lastCommitId = resolveCommitId(commitId);
            try (RevWalk revWalk = new RevWalk(repository)) {
                commit = revWalk.parseCommit(lastCommitId);
                revWalk.dispose();
            }

        }
        return commit;
    }

    /**
     * Returns all file paths present in a commit
     *
     * @param id The id of the commit one wants to get the paths of
     * @return A List of paths found in the commit
     * @throws IOException
     */
    public List<String> getPathsOfCommit(Object id) throws IOException {
        RevWalk rw = new RevWalk(repo);
        ObjectId head = repo.resolve(Constants.HEAD);
        RevCommit commit = rw.parseCommit(head);
        RevCommit[] parentList = commit.getParents();
        if (parentList.length == 0) {
            return getAllFiles(id);
        }
        RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(repo);
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setDetectRenames(true);
        return df.scan(parent.getTree(), commit.getTree())
                .stream()
                .map(diff -> diff.getNewPath())
                .collect(Collectors.toList());
    }

    private List<String> getAllFiles(Object id) throws IOException {
        List<String> filePaths = new ArrayList<>();
        try (TreeWalk treeWalk = new TreeWalk(repo)) {
            treeWalk.reset(getCommitById(id).getTree());
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                filePaths.add(treeWalk.getPathString());
            }
        }
        return filePaths;
    }
}
