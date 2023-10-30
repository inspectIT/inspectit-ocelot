package rocks.inspectit.ocelot.file.accessor.git;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Accessor to access specific Git revision/commits. Using this class ensures that all operations will be executed
 * on the same commit, thus, it will return a consistent result event new commits are committed to the repository.
 */
@Slf4j
public class RevisionAccess extends AbstractFileAccessor {

    /**
     * The repository to use.
     */
    private Repository repository;

    /**
     * The commit which will be used for the operations.
     */
    private RevCommit revCommit;

    /**
     * Constructor.
     * Always resolves the commit.
     *
     * @param repository the repository to use
     * @param revCommit  the commit which will be used for the operations
     */
    public RevisionAccess(Repository repository, RevCommit revCommit) {
        this(repository, revCommit, true);
    }

    /**
     * Constructor.
     *
     * @param repository  the repository to use
     * @param revCommit   the commit which will be used for the operations
     * @param resolveTree if true, the partial commit revCommit will be resolved using a RevWalk.
     */
    public RevisionAccess(Repository repository, RevCommit revCommit, boolean resolveTree) {
        this.repository = repository;
        if (resolveTree) {
            try (RevWalk revWalk = new RevWalk(repository)) {
                //reparse in case of incomplete revCommits, e.g if the RevCommit was received as a parent from another
                this.revCommit = revWalk.parseCommit(revCommit.getId());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            this.revCommit = revCommit;
        }
    }

    /**
     * @return a unique ID for this revision
     */
    public String getRevisionId() {
        return ObjectId.toString(revCommit.getId());
    }

    /**
     * Returns the main parent of this Revision.
     * For merge-commits the main parent is the Revision into which the other changes have been merged.
     *
     * @return the primary parent of this revision or an empty optional if this is a root commit.
     */
    public Optional<RevisionAccess> getPreviousRevision() {
        if (revCommit.getParentCount() >= 1) {
            return Optional.of(new RevisionAccess(repository, revCommit.getParent(0)));
        } else {
            return Optional.empty();
        }
    }

    /**
     * @return the name of the author of this revision.
     */
    public String getAuthorName() {
        return revCommit.getAuthorIdent().getName();
    }

    /**
     * Searches the common ancestor (merge-base) of the commit represented by this accessor and the given other one.
     * We don't do a BFS anymore, because we don't want to follow merge links.
     *
     * @param other the other revision to find a common ancestor with
     *
     * @return the Revision which is a parent of both revisions.
     */
    public RevisionAccess getCommonAncestor(RevisionAccess other) {
        try {
            RevWalk walk = new RevWalk(repository);
            walk.setRevFilter(RevFilter.MERGE_BASE);
            // RevCommits need to be produced by the same RevWalk instance otherwise it can't compare them
            walk.markStart(walk.parseCommit(revCommit.toObjectId()));
            walk.markStart(walk.parseCommit(other.revCommit.toObjectId()));
            RevCommit mergeBase = walk.next();

            if (mergeBase == null) {
                throw new IllegalStateException("No common ancestor!");
            }

            return new RevisionAccess(repository, mergeBase, true);
        } catch (Exception e) {
            throw new IllegalStateException("Error while searching a common ancestor!", e);
        }
    }

    /**
     * Checks if the given file exists in this revision but not in the parent revision.
     *
     * @param path the path of the file
     *
     * @return true, if the file was added in this revision.
     */
    public boolean isConfigurationFileAdded(String path) {
        if (!configurationFileExists(path)) {
            return false;
        }
        Optional<RevisionAccess> parent = getPreviousRevision();
        return !parent.isPresent() || !parent.get().configurationFileExists(path);
    }

    /**
     * Checks if the given file exists in both this revision and the parent revision,
     * but its contents have changed.
     *
     * @param path the path of the file
     *
     * @return true, if the file exists both in this and the parent revision but with different contents.
     */
    public boolean isConfigurationFileModified(String path) {
        if (!configurationFileExists(path)) {
            return false;
        }
        Optional<RevisionAccess> parent = getPreviousRevision();
        if (!parent.isPresent() || !parent.get().configurationFileExists(path)) {
            return false;
        }
        String currentContent = readConfigurationFile(path).orElseThrow(() -> new IllegalStateException("Expected file to exist"));
        String previousContent = parent.get()
                .readConfigurationFile(path)
                .orElseThrow(() -> new IllegalStateException("Expected file to exist"));
        return !currentContent.equals(previousContent);
    }

    /**
     * Checks if the given file does not exist in this revision but existed in the parent revision.
     *
     * @param path the path of the file
     *
     * @return true, if the file was deleted in this revision.
     */
    public boolean isConfigurationFileDeleted(String path) {
        if (configurationFileExists(path)) {
            return false;
        }
        Optional<RevisionAccess> parent = getPreviousRevision();
        return parent.isPresent() && parent.get().configurationFileExists(path);
    }

    /**
     * Checks if the agent mappings exist in this revision but not in the parent revision.
     *
     * @return true, if the agent mappings file were added in this revision.
     */
    public boolean isAgentMappingsAdded() {
        if (!agentMappingsExist()) {
            return false;
        }
        Optional<RevisionAccess> parent = getPreviousRevision();
        return !parent.isPresent() || !parent.get().agentMappingsExist();
    }

    /**
     * Checks, if the agent mappings file exist and if it's content in both this revision and the parent revision
     * has changed.
     *
     * @return true, if the agent mappings file exist both in this and the parent revision but with different contents.
     */
    public boolean isAgentMappingsModified() {
        if (!agentMappingsExist()) {
            return false;
        }
        Optional<RevisionAccess> parent = getPreviousRevision();
        if (!parent.isPresent() || !parent.get().agentMappingsExist()) {
            return false;
        }
        String currentContent = readAgentMappings().orElseThrow(() -> new IllegalStateException("Expected agent mappings to exist"));
        String previousContent = parent.get()
                .readAgentMappings()
                .orElseThrow(() -> new IllegalStateException("Expected agent mappings to exist"));
        return !currentContent.equals(previousContent);
    }

    @Override
    protected String verifyPath(String relativeBasePath, String relativePath) throws IllegalArgumentException {
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        Path path = Paths.get(relativePath).normalize();

        if (StringUtils.isBlank(relativeBasePath)) {
            // replace '\' with '/'. A regex that matches a single backslash character must include four backslashes, see https://www.gnu.org/software/guile/manual/html_node/Backslash-Escapes.html
            return path.toString().replaceAll("\\\\", "/");
        }

        Path basePath = Paths.get(relativeBasePath).normalize();
        Path resolvedPath = basePath.resolve(path).normalize();

        if (!resolvedPath.startsWith(basePath)) {
            throw new IllegalArgumentException("User path escapes the base path: " + relativePath);
        }

        // replace '\' with '/'. A regex that matches a single backslash character must include four backslashes, see https://www.gnu.org/software/guile/manual/html_node/Backslash-Escapes.html
        return resolvedPath.toString().replaceAll("\\\\", "/");
    }

    @Override
    protected byte[] readFile(String path) throws IOException {
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, revCommit.getTree())) {
            if (treeWalk == null) {
                throw new FileNotFoundException("Did not find expected file '" + path + "' in git repository");
            }

            if (treeWalk.isSubtree()) {
                throw new IllegalArgumentException("Target must be a file but found directory: " + path);
            }

            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);

            return loader.getBytes();
        }
    }

    @Override
    protected boolean exists(String path) {
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, revCommit.getTree())) {
            return treeWalk != null;
        } catch (Exception e) {
            log.error("Assuming file {} does not exist due to exception", path, e);
            return false;
        }
    }

    @Override
    protected boolean isDirectory(String path) {
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, revCommit.getTree())) {
            return treeWalk != null && treeWalk.isSubtree();
        } catch (Exception e) {
            log.error("Could not read file {} from git repository", path, e);
            return false;
        }
    }

    @Override
    protected List<FileInfo> listFiles(String path) {
        if (StringUtils.startsWith(path, "/")) {
            path = path.substring(1);
        }

        RevTree tree = revCommit.getTree();

        TreeWalk treeWalk = null;
        try {
            if (StringUtils.isBlank(path)) {
                treeWalk = new TreeWalk(repository);
                treeWalk.addTree(tree);
                treeWalk.setRecursive(false);
                treeWalk.next();
            } else {
                treeWalk = TreeWalk.forPath(repository, path, tree);
                if (treeWalk == null) {
                    return Collections.emptyList();
                } else if (treeWalk.isSubtree()) {
                    treeWalk.enterSubtree();
                    treeWalk.next();
                }
            }

            ArrayList<FileInfo> files = new ArrayList<>();
            collectFiles(treeWalk, files);
            return files;
        } catch (IOException e) {
            log.error("Exception while listing files in path '{}'.", path, e);
            return Collections.emptyList();
        } finally {
            if (treeWalk != null) {
                treeWalk.close();
            }
        }
    }

    /**
     * Collects the files within the current path of the given {@link TreeWalk}.
     *
     * @param treeWalk   The {@link TreeWalk} to traverse.
     * @param resultList the list which will be filled with the found files
     *
     * @return The files within the current tree.
     *
     * @throws IOException in case the repository cannot be read
     */
    private boolean collectFiles(TreeWalk treeWalk, List<FileInfo> resultList) throws IOException {
        int initialDepth = treeWalk.getDepth();
        boolean hasNext;

        do {
            String name = treeWalk.getNameString();
            FileInfo.FileInfoBuilder fileBuilder = FileInfo.builder().name(name);

            if (treeWalk.isSubtree()) {
                treeWalk.enterSubtree();
                treeWalk.next();

                List<FileInfo> nestedFiles = new ArrayList<>();
                hasNext = collectFiles(treeWalk, nestedFiles);

                fileBuilder.type(FileInfo.Type.DIRECTORY).children(nestedFiles);
            } else {
                fileBuilder.type(FileInfo.Type.FILE);
                hasNext = treeWalk.next();
            }

            resultList.add(fileBuilder.build());

            if (hasNext && initialDepth != treeWalk.getDepth()) {
                return true;
            }
        } while (hasNext);

        return false;
    }

    /**
     * Container used for finding the commonAncestor
     */
    @Value
    private static class Node {

        boolean isReachableFromOwn;

        RevisionAccess revAccess;
    }
}
