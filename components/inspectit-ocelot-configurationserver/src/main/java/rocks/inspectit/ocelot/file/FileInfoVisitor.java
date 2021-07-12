package rocks.inspectit.ocelot.file;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
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

    /**
     * The {@link FileInfo} which represents the starting directory of the walk.
     */
    private FileInfo rootDirectory;

    private final Gson gson = new Gson();

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
     * Resolves the type of a file by reading the JSON in the first line of the file and resolving the type to a value
     * of {@link FileInfo.Type}. If no JSON is present or an error occurs during the resolving the type, FILE is used
     * as fallback.
     *
     * @param file The File instance which should be checked.
     * @return {@link FileInfo.Type}.UI_FILE if the given file is a ui-file. Otherwise {@link FileInfo.Type}.FILE is returned.
     */
    private FileInfo.Type resolveFileType(File file) throws FileNotFoundException {
        Scanner fileScanner = new Scanner(file);
        FileInfo.Type fileType = FileInfo.Type.FILE;

        if(fileScanner.hasNext()) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            //Remove comment-character from line.
            String firstLine = fileScanner.nextLine().trim().substring(1);

            try {
                Map<String, String> jsonMap = gson.fromJson(firstLine, type);
                //Build a String from the type-value which corresponds to the Enum-Naming scheme.
                String typeString = "UI_" + jsonMap.get("type").toUpperCase().replace("-", "_");
                if(EnumUtils.isValidEnum(FileInfo.Type.class, typeString)){
                    fileType = FileInfo.Type.valueOf(typeString);
                }
            } catch (JsonSyntaxException ignored){}

        }

        fileScanner.close();
        return fileType;
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
