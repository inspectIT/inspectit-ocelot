package rocks.inspectit.ocelot.file.manager.directory;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractRepositoryTest {

    public static final String BASE_DIRECTORY = "files";

    public static Path tempDirectory;

    public InspectitServerSettings serverSettings;


    @BeforeEach
    public void setupTempRepository() throws Exception {
        tempDirectory = Files.createTempDirectory("inspectit-versioning");
        serverSettings = new InspectitServerSettings();
        serverSettings.setWorkingDirectory(tempDirectory.toString());
    }

    @AfterEach
    private void cleanDirectory() throws Exception {
        FileUtils.deleteDirectory(tempDirectory.toFile());
    }

    public File createTestFile(String filePath) throws IOException {
        File file = tempDirectory.resolve(BASE_DIRECTORY).resolve(filePath).toFile();

        if (file.exists()) {
            file.delete();
        }

        file.getParentFile().mkdirs();
        file.createNewFile();
        return file;
    }

    public void createTestFiles(String... filePaths) throws IOException {
        for (String filePath : filePaths) {
            createTestFile(filePath);
        }
    }

    public void createTestFile(String filePath, String content) throws IOException {
        File testFile = createTestFile(filePath);
        Files.write(testFile.toPath(), content.getBytes());
    }
}
