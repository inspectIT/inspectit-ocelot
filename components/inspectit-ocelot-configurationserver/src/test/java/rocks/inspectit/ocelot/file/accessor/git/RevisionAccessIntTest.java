package rocks.inspectit.ocelot.file.accessor.git;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileTestBase;
import rocks.inspectit.ocelot.file.versioning.VersioningManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RevisionAccessIntTest extends FileTestBase {

    /**
     * For convenience: if this field is not null, it will used as working directory during tests.
     * Note: the specified directory is CLEANED before each run, thus, if you have files there, they will be gone ;)
     */
    public static final String TEST_DIRECTORY = "C:\\test";

    private VersioningManager versioningManager;

    @BeforeEach
    public void beforeEach() throws IOException, GitAPIException {
        if (TEST_DIRECTORY == null) {
            tempDirectory = Files.createTempDirectory("ocelot");
        } else {
            tempDirectory = Paths.get(TEST_DIRECTORY);
            FileUtils.cleanDirectory(tempDirectory.toFile());
        }

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        versioningManager = new VersioningManager(tempDirectory, () -> authentication);
        versioningManager.initialize();

        System.out.println("Test data in: " + tempDirectory.toString());
    }

    @AfterEach
    public void afterEach() throws IOException {
        if (TEST_DIRECTORY == null) {
            FileUtils.deleteDirectory(tempDirectory.toFile());
        }
    }

    @Nested
    class X {

        @Test
        public void xxx() throws GitAPIException {
            versioningManager.setAmendTimeout(-1);

            createTestFiles("files/file_a.yml=a1", "files/z/file_z.yml=z", "untracked.yml");
            versioningManager.commit("first");

            createTestFiles("files/file_b.yml=b");
            versioningManager.commit("second");

            createTestFiles("files/file_b.yml=b2", "files/file_c.yml=c");
            versioningManager.commit("third");

            RevisionAccess revision = versioningManager.getLiveRevision();

            List<FileInfo> result = revision.listConfigurationFiles("");

            assertThat(result).isNotEmpty();
        }

    }
}