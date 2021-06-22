package rocks.inspectit.ocelot.file;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * FileVisitor for walking a file tree and creating a {@link FileInfo} representation of it.
 */
@Slf4j
public class FileInfoVisitor implements FileVisitor<Path> {

    /**
     * The directory stack. The latest directory is the current one.
     */
    private Stack<FileInfo> directoryStack = new Stack<>();

    private final String UI_FILE_IDENTIFIER = "# {\"type\": \"Method-Configuration\"";

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
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws FileNotFoundException {
        // adding each file to the current directory
        FileInfo fileInfo = FileInfo.builder()
                .name(file.getFileName().toString())
                .type(resolveFileType(file.toFile()))
                .build();

        directoryStack.peek().addChild(fileInfo);

        return FileVisitResult.CONTINUE;
    }

    /**
     * Checks if a given File is a ui-file by checking the first line of the file starts with the String provided in
     * UI_FILE_IDENTIFIER.
     * @param file The File instance which should be checked.
     * @return {@link FileInfo.Type}.UI_FILE if the given file is a ui-file. Otherwise {@link FileInfo.Type}.FILE is returned.
     */
    private FileInfo.Type resolveFileType(File file) throws FileNotFoundException {
        Scanner fileScanner = new Scanner(file);
        String firstLine = fileScanner.nextLine();
        fileScanner.close();
        return firstLine.startsWith(UI_FILE_IDENTIFIER) ? FileInfo.Type.UI_FILE: FileInfo.Type.FILE;
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
