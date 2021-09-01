package rocks.inspectit.ocelot.file;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * FileVisitor for walking a file tree and creating a {@link FileInfo} representation of it.
 */
@Slf4j
public class FileInfoVisitor implements FileVisitor<Path> {

    /**
     * Type used by Gson for deserializing.
     */
    private static final Type TYPE_MAP = new TypeToken<Map<String, String>>() {
    }.getType();

    /**
     * The directory stack. The latest directory is the current one.
     */
    private final Stack<FileInfo> directoryStack = new Stack<>();

    /**
     * The {@link FileInfo} which represents the starting directory of the walk.
     */
    private FileInfo rootDirectory;

    /**
     * Gson instance for JSON deserialization.
     */
    private final Gson gson = new Gson();

    @Override
    public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
        if (Files.isHidden(directory)) {
            return FileVisitResult.SKIP_SUBTREE;
        }

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
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (Files.isSymbolicLink(file)) {
            if (Files.exists(file) && !Files.isDirectory(file)) { // .exists() follows sym link
                FileInfo fileInfo = FileInfo.builder()
                        .name(file.getFileName().toString())
                        .type(resolveFileType(file.toRealPath().toFile()))
                        .build();

                directoryStack.peek().addChild(fileInfo);
            }

            return FileVisitResult.CONTINUE;
        }

        // adding each file to the current directory
        FileInfo fileInfo = FileInfo.builder()
                .name(file.getFileName().toString())
                .type(resolveFileType(file.toFile()))
                .build();

        directoryStack.peek().addChild(fileInfo);

        return FileVisitResult.CONTINUE;
    }

    /**
     * Resolves the type of a file by reading the JSON in the first line of the file and resolving the type to a value
     * of {@link FileInfo.Type}. If no JSON is present or an error occurs during the resolving the type, FILE is used
     * as fallback.
     *
     * @param file The File instance which should be checked.
     *
     * @return {@link FileInfo.Type}.UI_FILE if the given file is a ui-file. Otherwise {@link FileInfo.Type}.FILE is returned.
     */
    private FileInfo.Type resolveFileType(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String firstLine = reader.readLine();

            if (firstLine == null) {
                return FileInfo.Type.FILE;
            } else {
                firstLine = firstLine.trim();
            }

            FileInfo.Type fileType = FileInfo.Type.FILE;
            if (StringUtils.length(firstLine) > 1) {
                // Remove comment-character from line. In case the line has a different format, we assume it was
                // done by the use, so we don't care whether it can parsed or not.
                String rawJson = firstLine.substring(1);

                try {
                    Map<String, String> jsonMap = gson.fromJson(rawJson, TYPE_MAP);
                    if (jsonMap == null || !jsonMap.containsKey("type")) {
                        return fileType;
                    }

                    // Build a String from the type-value which corresponds to the Enum-Naming scheme.
                    String typeString = "UI_" + jsonMap.get("type").toUpperCase().replace("-", "_");
                    if (EnumUtils.isValidEnum(FileInfo.Type.class, typeString)) {
                        fileType = FileInfo.Type.valueOf(typeString);
                    }
                } catch (JsonSyntaxException ignored) {
                }
            }

            return fileType;
        }
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
