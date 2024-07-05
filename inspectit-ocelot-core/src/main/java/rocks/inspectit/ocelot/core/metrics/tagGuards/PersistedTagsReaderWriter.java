package rocks.inspectit.ocelot.core.metrics.tagGuards;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


@Getter
@Slf4j
public class PersistedTagsReaderWriter {

    private final ObjectMapper mapper;

    @NotNull
    private final Path path;

    private PersistedTagsReaderWriter(final ObjectMapper mapper, @NotNull final Path path) {
        this.mapper = mapper;
        this.path = path;
    }

    public static PersistedTagsReaderWriter of(final String filenameInput) {
        final Path path = Paths.get(filenameInput);
        return new PersistedTagsReaderWriter(new ObjectMapper(), path);
    }


    public Map<String, Map<String, Set<String>>> read() {
        if (!Files.exists(path)) {
            log.info("Could not find tag-guard database file. File will be created during next write");
            return new HashMap<>();
        }
        return readTagsFromFile();
    }

    @NotNull
    private Map<String, Map<String, Set<String>>> readTagsFromFile() {
        try (final BufferedReader content = Files.newBufferedReader(path)) {
            final Map<String, Map<String, Set<String>>> result = mapper.readValue(content, new TypeReference<Map<String, Map<String, Set<String>>>>() {
            });
            return Objects.nonNull(result) ? result : new HashMap<>();
        } catch (final Exception e) {
            log.error("Error loading tag-guard database from persistence file", e);
            return new HashMap<>();
        }
    }

    public void write(Map<String, Map<String, Set<String>>> tagValues) {
        if(!isWritable(path)){
            return;
        }

        try (final Writer filesWriter = Files.newBufferedWriter(path)) {
            createFileDirectory(path.getParent());
            mapper.writeValue(filesWriter, tagValues);
        } catch (final IOException e) {
            log.error("Error writing tag-guard database to file", e);
        }
    }

    private void createFileDirectory(final Path parent) throws IOException {
        if (!Files.isDirectory(parent)) {
            Files.createDirectories(parent);
        }
    }

    private boolean isWritable(final Path path) {
        return Files.exists(path) ? Files.isWritable(path) : Files.isWritable(path.getParent());
    }
}