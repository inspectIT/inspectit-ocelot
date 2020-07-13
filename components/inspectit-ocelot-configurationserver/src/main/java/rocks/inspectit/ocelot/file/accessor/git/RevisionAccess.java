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
import org.eclipse.jgit.treewalk.TreeWalk;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
     *
     * @param repository the repository to use
     * @param revCommit  the commit which will be used for the operations
     */
    public RevisionAccess(Repository repository, RevCommit revCommit) {
        this.repository = repository;
        try (RevWalk revWalk = new RevWalk(repository)) {
            this.revCommit = revWalk.parseCommit(revCommit.getId()); //reparse in case of incomplete revCommits
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return a unique ID for this revision
     */
    public String getRevisionID() {
        return ObjectId.toString(revCommit.getId());
    }

    /**
     * @return the main parent of this revision or an empty optional if this is a root commit.
     */
    public Optional<RevisionAccess> getPreviousRevision() {
        if (revCommit.getParentCount() >= 1) {
            return Optional.of(new RevisionAccess(repository, revCommit.getParent(0)));
        } else {
            return Optional.empty();
        }
    }

    public String getAuthorName() {
        return revCommit.getAuthorIdent().getName();
    }

    /**
     * Walks the history backwards to the a revision (A) which is a parent of this revision (B) and another given revision (C).
     * This method will return a revision which minimizes MAX(distance(A,B), distance(A,C)).
     * <p>
     * Such a common ancestor should exist for all commits, because all branches originate from the root commit of the repo.
     *
     * @param other the other revision to find a common ancestor with
     *
     * @return the Revision which is a parent of both revisions.
     */
    public RevisionAccess getCommonAncestor(RevisionAccess other) {
        //unfortunately RevFilter.MERGE_BASE does not return the best ancenstor,
        // therefore we perform a BFS ourselves.
        Set<String> ownVisited = new HashSet<>();
        Set<String> otherVisited = new HashSet<>();
        Deque<Node> openList = new ArrayDeque<>();
        openList.addLast(new Node(true, this));
        openList.addLast(new Node(false, other));
        while (!openList.isEmpty()) {
            Node current = openList.removeFirst();
            String hash = current.revAccess.getRevisionID();
            if (current.isFromOwn) {
                if (otherVisited.contains(hash)) {
                    return current.revAccess;
                }
                ownVisited.add(hash);
            } else {
                if (ownVisited.contains(hash)) {
                    return current.revAccess;
                }
                otherVisited.add(hash);
            }
            for (int i = 0; i < current.revAccess.revCommit.getParentCount(); i++) {
                RevCommit parent = current.revAccess.revCommit.getParent(i);
                openList.addLast(new Node(current.isFromOwn, new RevisionAccess(repository, parent)));
            }
        }
        throw new IllegalStateException("No common ancestor!");
    }

    /**
     * Checks if the given file exists in this revision but not in the parent revision.
     *
     * @param path the path of the file
     *
     * @return true, if the file was added in this revision.
     */
    public boolean wasConfigurationFileAdded(String path) {
        Optional<RevisionAccess> parent = getPreviousRevision();
        boolean existsInCurrentRevision = configurationFileExists(path);
        if (!existsInCurrentRevision) {
            return false;
        }
        if (!parent.isPresent()) {
            return true;
        }
        return !parent.get().configurationFileExists(path);
    }

    /**
     * Checks if the given file exists in both this revision and the parent revision,
     * but it's contents have changed.
     *
     * @param path the path of the file
     *
     * @return true, if the file exists both in this and the parent revision but with different contents.
     */
    public boolean wasConfigurationFileModified(String path) {
        Optional<RevisionAccess> parent = getPreviousRevision();
        boolean existsInCurrentRevision = configurationFileExists(path);
        if (!existsInCurrentRevision || !parent.isPresent() || !parent.get().configurationFileExists(path)) {
            return false;
        }
        String currentContent = readConfigurationFile(path).orElse("");
        String previousContent = parent.get().readConfigurationFile(path).orElse("");
        return !currentContent.equals(previousContent);
    }

    /**
     * Checks if the given file does not exist in this revision but existed in the parent revision.
     *
     * @param path the path of the file
     *
     * @return true, if the file was deleted in this revision.
     */
    public boolean wasConfigurationFileDeleted(String path) {
        Optional<RevisionAccess> parent = getPreviousRevision();
        boolean existsInCurrentRevision = configurationFileExists(path);
        boolean existsInParenRevision = parent.isPresent() && parent.get().configurationFileExists(path);
        return !existsInCurrentRevision && existsInParenRevision;
    }

    @Override
    protected String verifyPath(String relativeBasePath, String relativePath) throws IllegalArgumentException {
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        Path path = Paths.get(relativePath).normalize();

        if (StringUtils.isBlank(relativeBasePath)) {
            return path.toString().replaceAll("\\\\", "/");
        }

        Path basePath = Paths.get(relativeBasePath).normalize();
        Path resolvedPath = basePath.resolve(path).normalize();

        if (!resolvedPath.startsWith(basePath)) {
            throw new IllegalArgumentException("User path escapes the base path: " + relativePath);
        }

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

                fileBuilder
                        .type(FileInfo.Type.DIRECTORY)
                        .children(nestedFiles);
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

        boolean isFromOwn;

        RevisionAccess revAccess;
    }
}
