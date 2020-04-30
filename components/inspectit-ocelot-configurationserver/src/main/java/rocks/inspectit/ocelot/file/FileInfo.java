package rocks.inspectit.ocelot.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Data
@Builder
public class FileInfo {

    public enum Type {
        @JsonProperty("directory")
        DIRECTORY,
        @JsonProperty("file")
        FILE
    }

    private Type type;

    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FileInfo> children;

    /**
     * If this is a file, its absolute path is returned given the containing directory.
     * Otherwise the absolute paths of all (transitively) contianed files (not directories) is returned.
     *
     * @param containingDir the path directory containing this file, can be empty if this file is at the root
     * @return a stream of absolute paths
     */
    public Stream<String> getAbsoluteFilePaths(String containingDir) {
        String absolutePath = containingDir.isEmpty() ? name : containingDir + "/" + name;
        if (type == FileInfo.Type.DIRECTORY) {
            return children.stream()
                    .map(f -> f.getAbsoluteFilePaths(absolutePath))
                    .reduce(Stream.empty(), Stream::concat);
        } else {
            return Stream.of(absolutePath);
        }
    }

    /**
     * Adds a child and initializes the children list in case it does not exist.
     *
     * @param fileInfo the child to add
     */
    synchronized void addChild(FileInfo fileInfo) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(fileInfo);
    }
}
