package rocks.inspectit.ocelot.file.accessor.git;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
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
        this.revCommit = revCommit;
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
            log.error("Could not read file {} from git repository", path, e);
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

            return collectFiles(treeWalk);
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
     * @param treeWalk The {@link TreeWalk} to traverse.
     * @return The files within the current tree.
     * @throws IOException in case the repository cannot be read
     */
    private List<FileInfo> collectFiles(TreeWalk treeWalk) throws IOException {
        Deque<List<FileInfo>> fileStack = new ArrayDeque<>();
        fileStack.push(new ArrayList<>());

        int previousDepth = treeWalk.getDepth();

        do {
            if (previousDepth > treeWalk.getDepth()) {
                // we're moving one level down in the tree
                fileStack.pop();
            }
            previousDepth = treeWalk.getDepth();

            List<FileInfo> targetList = fileStack.peek();
            if (targetList == null) {
                throw new IllegalStateException("Exception when listing revision files. The stack cannot contain null values.");
            }

            String name = treeWalk.getNameString();
            FileInfo.FileInfoBuilder fileBuilder = FileInfo.builder().name(name);

            if (treeWalk.isSubtree()) {
                // handling directories
                treeWalk.enterSubtree();

                FileInfo fileInfo = fileBuilder
                        .type(FileInfo.Type.DIRECTORY)
                        .children(new ArrayList<>())
                        .build();

                targetList.add(fileInfo);
                fileStack.push(fileInfo.getChildren());
            } else {
                // handling files
                fileBuilder.type(FileInfo.Type.FILE);
                targetList.add(fileBuilder.build());
            }
        } while (treeWalk.next());

        return fileStack.getLast();
    }
}
