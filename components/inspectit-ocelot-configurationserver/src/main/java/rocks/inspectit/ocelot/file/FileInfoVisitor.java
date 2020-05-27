package rocks.inspectit.ocelot.file;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * FileVisitor for walking a file tree and creating a {@link FileInfo} representation of it.
 */
@Slf4j
public class FileInfoVisitor implements FileVisitor<Path> {

    /**
     * The directory stack. The latest directory is the current one.
     */
    private Stack<FileInfo> directoryStack = new Stack<>();

    /**
     * The {@link FileInfo} which represents the starting directory of the walk.
     */
    private FileInfo rootDirectory;

    @Override
    public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) {
        FileInfo currentDirectory = FileInfo.builder()
                .name(directory.getFileName().toString())
                .type(FileInfo.Type.DIRECTORY)
                .children(new ArrayList<>())
                .build();

        // add directory as child, otherwise set it as root
        if (directoryStack.isEmpty()) {
            rootDirectory = currentDirectory;
        } else {
            directoryStack.peek().addChild(currentDirectory);
        }

        directoryStack.add(currentDirectory);

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        // adding each file to the current directory
        FileInfo fileInfo = FileInfo.builder()
                .name(file.getFileName().toString())
                .type(FileInfo.Type.FILE)
                .build();

        directoryStack.peek().addChild(fileInfo);

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        log.error("Could not visit file '{}'.", file, exc);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        // leaving directory, so set stack to the parent directory
        directoryStack.pop();
        return FileVisitResult.CONTINUE;
    }

    /**
     * @return Returns the list of visited files and directories.
     */
    public List<FileInfo> getFileInfos() {
        if (rootDirectory == null) {
            return Collections.emptyList();
        } else {
            List<FileInfo> children = rootDirectory.getChildren();
            if (children == null) {
                return Collections.emptyList();
            } else {
                return children;
            }
        }
    }
}
