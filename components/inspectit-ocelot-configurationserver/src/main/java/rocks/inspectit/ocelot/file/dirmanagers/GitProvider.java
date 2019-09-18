package rocks.inspectit.ocelot.file.dirmanagers;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Scope("singleton")
@Slf4j
public class GitProvider {

    private static final Charset ENCODING = StandardCharsets.UTF_8;
    private static final String FILES_SUBFOLDER = "files";
    private Path filesRoot;

    @VisibleForTesting
    @Autowired
    InspectitServerSettings config;

    @VisibleForTesting
    Git git;

    @VisibleForTesting
    Repository repo;


    /**
     * Sets up the Git repo for usage
     */
    @PostConstruct
    @VisibleForTesting
    void init() {
        try {
            boolean isFirstInit = isFirstGitInit();
            Path filesRoot = Paths.get(config.getWorkingDirectory()).resolve("files").toAbsolutePath().normalize();
            Files.createDirectories(filesRoot);
            File localPath = new File(String.valueOf(filesRoot));
            filesRoot = Paths.get(config.getWorkingDirectory()).resolve(FILES_SUBFOLDER).toAbsolutePath().normalize();
            Files.createDirectories(filesRoot);
            git = Git.init().setDirectory(localPath).call();
            repo = git.getRepository();
            if (isFirstInit) {
                log.info("Initially committing files in the directory...");
                commitAllChanges();
                log.info("Done.");
            }
            log.info("Git directory set up successfully at {} !", localPath.toString());
        } catch (GitAPIException | IOException e) {
            log.error("Error setting up git directory");
        }
    }

    private boolean isFirstGitInit() {
        return !Files.exists(Paths.get(config.getWorkingDirectory()).resolve("git/.git").toAbsolutePath().normalize());
    }

    public boolean commitAllChanges() throws GitAPIException {
        addAllFiles();
        return commitAll();
    }

    /**
     * Commits all  changes to the master branch of the local repo
     *
     * @return
     * @throws GitAPIException
     */
    boolean commitAll() throws GitAPIException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        git.commit()
                .setAll(true)
                .setMessage("Commit changes to all files on " + formatter.format(date))
                .call();

        log.info("Committed all changes to repository at {}", repo.getDirectory());
        git.reset();
        return true;
    }

    /**
     * Commits all currently added changes to the master branch of the local repo
     *
     * @param commitCommand A CommitCommand to which all files are added one wants to commit
     * @return
     * @throws GitAPIException
     */
    boolean commit(CommitCommand commitCommand) throws GitAPIException {
        addAllFiles();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        commitCommand.setMessage("Committed changes to files on " + formatter.format(date))
                .call();

        log.info("Committed changes to repository at {}", repo.getDirectory());
        return true;
    }

    /**
     * Adds all files in the current directory to the repo
     *
     * @throws GitAPIException
     */
    private void addAllFiles() throws GitAPIException {
        git.add().addFilepattern(".").call();
    }


    /**
     * Commits all files found in the given path.
     *
     * @throws GitAPIException
     */
    void commitFile(String filePath) throws GitAPIException {
        commit(git.commit().setOnly(filePath));
    }


    /**
     * Lists all file paths which do not start with the given prefix from the last commit.
     *
     * @return
     * @throws IOException
     */
    public List<String> listFiles(String filePrefix, boolean specialFiles) throws IOException {
        if (repo.resolve(Constants.HEAD) == null) {
            return Arrays.asList();
        }
        ArrayList<String> filesFromLastCommit = new ArrayList<>();
        TreeWalk treeWalk = getTreeWalk();
        while (treeWalk.next()) {
            if (treeWalk.isSubtree()) {
                treeWalk.enterSubtree();
            } else {
                if (isInPath(treeWalk, filePrefix)) {
                    String fileName = treeWalk.getPathString();
                    if (specialFiles) {
                        if (!fileName.startsWith("configuration")) {
                            filesFromLastCommit.add(fileName.replace(filePrefix, ""));
                        }
                    } else {
                        if (fileName.startsWith("configuration")) {
                            filesFromLastCommit.add(fileName.replace(filePrefix, ""));
                        }
                    }
                }
            }
        }
        return filesFromLastCommit;
    }

    private TreeWalk getTreeWalk() throws IOException {
        RevTree tree = getTree(repo);
        TreeWalk treeWalk = new TreeWalk(repo);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(false);
        return treeWalk;
    }

    private static RevTree getTree(Repository repository) throws IOException {
        ObjectId lastCommitId = repository.resolve(Constants.HEAD);
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            RevTree tree = commit.getTree();
            return tree;
        }
    }

    private boolean isInPath(TreeWalk treeWalk, String filePrefix) {
        return treeWalk.getPathString().startsWith(filePrefix);
    }

    /**
     * Reads the content of a file from the current repo
     *
     * @return The content of the file as String
     * @throws IOException
     */
    public String readFile(String filePath) throws IOException {
        ObjectLoader loader;
        try (Repository repository = repo) {
            ObjectId lastCommitId = repository.resolve(Constants.HEAD);
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(lastCommitId);
                RevTree tree = commit.getTree();
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(PathFilter.create(filePath));
                    if (!treeWalk.next()) {
                        log.error("Could not read file {} from git repo", filePath);
                        return null;
                    }
                    ObjectId objectId = treeWalk.getObjectId(0);
                    loader = repository.open(objectId);
                }
                revWalk.dispose();
                if (loader != null) {
                    return new String(loader.getBytes(), ENCODING);
                }
            }
        }
        return null;
    }
}
