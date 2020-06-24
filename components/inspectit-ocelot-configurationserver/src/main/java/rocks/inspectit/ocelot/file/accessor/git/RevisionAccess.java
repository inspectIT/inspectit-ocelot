package rocks.inspectit.ocelot.file.accessor.git;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        Path normalizedPath = Paths.get(relativePath).normalize();

        if (StringUtils.isBlank(relativeBasePath)) {
            return normalizedPath.toString();
        }

        Path normalizedBasePath = Paths.get(relativeBasePath).normalize();

        if (!normalizedPath.startsWith(normalizedBasePath)) {
            throw new IllegalArgumentException("User path escapes the base path: " + relativePath);
        }

        return normalizedPath.toString();
    }

    @Override
    protected Optional<byte[]> readFile(String path) {
        return Optional.empty();
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
        return false;
    }

    @Override
    protected boolean isDirectory(String path) {
        return false;
    }

//    private List<String> readElementsAt(String path) throws IOException {
//        // and using commit's tree find the path
//        RevTree tree = revCommit.getTree();
//        //System.out.println("Having tree: " + tree + " for commit " + commit);
//
//        List<String> items = new ArrayList<>();
//
//        // shortcut for root-path
//        if (path.isEmpty()) {
//            try (TreeWalk treeWalk = new TreeWalk(repository)) {
//                treeWalk.addTree(tree);
//                treeWalk.setRecursive(true);
//                treeWalk.setPostOrderTraversal(false);
//
//                while (treeWalk.next()) {
//                    items.add(treeWalk.getPathString());
//                }
//            }
//        } else {
//            // now try to find a specific file
//            try (TreeWalk treeWalk = buildTreeWalk(tree, path)) {
//                if ((treeWalk.getFileMode(0).getBits() & FileMode.TYPE_TREE) == 0) {
//                    throw new IllegalStateException("Tried to read the elements of a non-tree for path '" + path + "', had filemode " + treeWalk.getFileMode(0).getBits());
//                }
//
//                try (TreeWalk dirWalk = new TreeWalk(repository)) {
//                    dirWalk.addTree(treeWalk.getObjectId(0));
//                    dirWalk.setRecursive(true);
//                    while (dirWalk.next()) {
//                        items.add(dirWalk.getPathString());
//                    }
//                }
//            }
//        }
//
//        return items;
//    }
//
//    private TreeWalk buildTreeWalk(RevTree tree, final String path) throws IOException {
//        TreeWalk treeWalk = TreeWalk.forPath(repository, path, tree);
//
//        if (treeWalk == null) {
//            throw new FileNotFoundException("Did not find expected file '" + path + "' in tree '" + tree.getName() + "'");
//        }
//
//        return treeWalk;
//    }

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

        // using commit's tree find the path
        RevTree tree = revCommit.getTree();

        boolean skipNext = false;

        TreeWalk treeWalk;
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
