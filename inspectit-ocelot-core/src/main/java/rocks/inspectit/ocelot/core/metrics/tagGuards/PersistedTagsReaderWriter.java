package rocks.inspectit.ocelot.core.metrics.tagGuards;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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

        try {
            byte[] content = Files.readAllBytes(path);
            return mapper.readValue(content, new TypeReference<Map<String, Map<String, Set<String>>>>() {
            });
        } catch (final Exception e) {
            log.error("Error loading tag-guard database from persistence file", e);
            return new HashMap<>();
        }
    }

    public void write(Map<String, Map<String, Set<String>>> tagValues) {
        try {
            final Path parent = path.getParent();
            if (Objects.isNull(parent) || !Files.isWritable(parent) ) {
                log.error("Cannot find write the file because of an invalid path.");
                return;
            }

            createFileDirectory(parent);
            final String tagValuesString = mapper.writeValueAsString(tagValues);
            Files.writeString(path, tagValuesString);
        } catch (final IOException e) {
            log.error("Error writing tag-guard database to file", e);
        }
    }

    private void createFileDirectory(final Path parent) throws IOException {
        if (!Files.isDirectory(parent)) {
            Files.createDirectories(parent);
        }
    }
}