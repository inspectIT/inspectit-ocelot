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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class RevisionAccess extends AbstractFileAccessor {

    private Repository repository;

    private RevCommit revCommit;

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
    protected Optional<byte[]> readFile(String path) {
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, revCommit.getTree())) {
            if (treeWalk == null) {
                log.warn("Did not find expected file '{}' in git repository", path);
                return Optional.empty();
            }

            if (treeWalk.isSubtree()) {
                log.warn("Target must be a file but found directory: {}", path);
                return Optional.empty();
            }

            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);

            return Optional.of(loader.getBytes());
        } catch (Exception e) {
            log.error("Could not read file {} from git repository", path, e);
            return Optional.empty();
        }
    }

    @Override
    protected List<FileInfo> listFiles(String path) {
        try {
            return listFiles(path, true);
        } catch (IOException e) {
            log.error("Exception while listing files in path '{}'.", path, e);
            return Collections.emptyList();
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

    /**
     * Lists all files in the given path.
     *
     * @param path      The path which should be considered as root.
     * @param recursive Whether the tree should be resolved recursively.
     * @return List of{@link FileInfo} representing the content of the repository.
     * @throws IOException in case the repository cannot be read
     */
    private List<FileInfo> listFiles(String path, boolean recursive) throws IOException {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        RevTree tree = revCommit.getTree();

        boolean skipNext = false;

        TreeWalk treeWalk = null;
        try {
            if (StringUtils.isBlank(path)) {
                treeWalk = new TreeWalk(repository);
                treeWalk.addTree(tree);
                treeWalk.setRecursive(false);
            } else {
                treeWalk = TreeWalk.forPath(repository, path, tree);
                if (treeWalk == null) {
                    return Collections.emptyList();
                } else if (treeWalk.isSubtree()) {
                    treeWalk.enterSubtree();
                } else {
                    skipNext = true;
                }
            }

            return collectFiles(treeWalk, recursive, skipNext);
        } finally {
            if (treeWalk != null) {
                treeWalk.close();
            }
        }
    }

    /**
     * Collects the files within the current path of the given {@link TreeWalk}.
     *
     * @param treeWalk  The {@link TreeWalk} to traverse.
     * @param recursive Whether sub trees should be resolved.
     * @param skipNext  Flag indicating whether {@link TreeWalk#next()} should be invoked at the beginning.
     *                  For example, this may be the case if the tree is created using {@link TreeWalk#forPath(Repository, String, RevTree)}.
     * @return The files within the current tree.
     * @throws IOException in case the repository cannot be read
     */
    private List<FileInfo> collectFiles(TreeWalk treeWalk, boolean recursive, boolean skipNext) throws IOException {
        List<FileInfo> resultList = new ArrayList<>();


        while (skipNext || treeWalk.next()) {
            skipNext = false;

            String name = treeWalk.getNameString();

            FileInfo.FileInfoBuilder fileBuilder = FileInfo.builder().name(name);

            if (recursive && treeWalk.isSubtree()) {
                treeWalk.enterSubtree();
                List<FileInfo> nestedFiles = collectFiles(treeWalk, true, false);

                fileBuilder
                        .type(FileInfo.Type.DIRECTORY)
                        .children(nestedFiles);
            } else {
                fileBuilder.type(FileInfo.Type.FILE);
            }

            FileInfo fileInfo = fileBuilder.build();
            resultList.add(fileInfo);
        }

        return resultList;
    }
}
