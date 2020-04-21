package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.FileInfo;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Component
public class WorkingDirectoryAccessor extends AbstractWorkingDirectoryAccessor {

    private InspectitServerSettings config;

    private Path workingDirectory;

    @Autowired
    public WorkingDirectoryAccessor(InspectitServerSettings config, ApplicationEventPublisher eventPublisher) {
        super(eventPublisher);

        this.config = config;

        workingDirectory = Paths.get(config.getWorkingDirectory()).toAbsolutePath().normalize();
    }

    private Path resolve(String path) {
        if (StringUtils.isEmpty(path)) {
            return workingDirectory;
        } else {
            return workingDirectory.resolve(path).normalize();
        }
    }

    @Override
    protected synchronized Optional<byte[]> readFile(String path) {
        Path targetPath = resolve(path);

        if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
            return Optional.empty();
        }

        try {
            return Optional.of(Files.readAllBytes(targetPath));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    protected synchronized List<FileInfo> listFiles(String path) {
        log.info("list: " + path);
        Path targetPath = resolve(path);

        if (!Files.exists(targetPath)) {
            return Collections.emptyList();
        }

        try (Stream<Path> files = Files.list(targetPath)) {
            List<FileInfo> result = new ArrayList<>();

            files.forEach(childPath -> {
                boolean isDirectory = Files.isDirectory(childPath);
                String fileName = childPath.getFileName().toString();

                FileInfo.FileInfoBuilder builder = FileInfo.builder()
                        .name(fileName)
                        .type(isDirectory ? FileInfo.Type.DIRECTORY : FileInfo.Type.FILE);

                if (isDirectory) {
                    String nestedPath = path + File.separator + fileName;
                    builder.children(listFiles(nestedPath));
                }

                result.add(builder.build());
            });

            return result;
        } catch (IOException e) {
            log.error("Exception while listing files in path '" + path + "'.", e);
            return Collections.emptyList();
        }
    }

    @Override
    protected synchronized void writeFile(String path, String content) throws IOException {
        Path targetFile = resolve(path);
        if (Files.exists(targetFile) && !Files.isRegularFile(targetFile)) {
            throw new AccessDeniedException("'" + targetFile + "' is a directory!");
        }
        FileUtils.forceMkdir(targetFile.getParent().toFile());
        Files.write(targetFile, content.getBytes(FILE_ENCODING));
    }

    @Override
    protected synchronized void moveFile(String sourcePath, String targetPath) throws IOException {
        Path source = resolve(sourcePath);
        Path target = resolve(targetPath);

        FileUtils.forceMkdir(target.getParent().toFile());

        if (Files.isDirectory(source)) {
            FileUtils.moveDirectory(source.toFile(), target.toFile());
        } else {
            FileUtils.moveFile(source.toFile(), target.toFile());
        }
    }

    @Override
    protected synchronized void deleteFile(String path) throws IOException {
        Path targetFile = resolve(path);
        if (Files.isRegularFile(targetFile)) {
            Files.delete(targetFile);
        } else {
            throw new AccessDeniedException("'" + targetFile + "' could not be deleted.");
        }
    }
}
