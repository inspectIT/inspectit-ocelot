package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import rocks.inspectit.ocelot.file.FileTestBase;
import rocks.inspectit.ocelot.file.versioning.VersioningManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoCommitWorkingDirectoryProxyIntTest extends FileTestBase {

    /**
     * For convenience: if this field is not null, it will used as working directory during tests.
     * Note: the specified directory is CLEANED before each run, thus, if you have files there, they will be gone ;)
     */
    public static final String TEST_DIRECTORY = null;

    private AutoCommitWorkingDirectoryProxy accessor;

    private ApplicationEventPublisher eventPublisher;

    private VersioningManager versioningManager;

    @BeforeEach
    public void beforeEach() throws IOException, GitAPIException {
        if (TEST_DIRECTORY == null) {
            tempDirectory = Files.createTempDirectory("ocelot");
        } else {
            tempDirectory = Paths.get(TEST_DIRECTORY);
            FileUtils.cleanDirectory(tempDirectory.toFile());
        }

        eventPublisher = mock(ApplicationEventPublisher.class);
        Lock readLock = mock(Lock.class);
        Lock writeLock = mock(Lock.class);

        WorkingDirectoryAccessor workingDirectoryAccessor = new WorkingDirectoryAccessor(readLock, writeLock, tempDirectory);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user");
        versioningManager = new VersioningManager(tempDirectory, () -> authentication, eventPublisher, "@test.com");
        versioningManager.initialize();

        accessor = new AutoCommitWorkingDirectoryProxy(writeLock, workingDirectoryAccessor, versioningManager);
    }

    @AfterEach
    public void afterEach() throws IOException {
        if (TEST_DIRECTORY == null) {
            FileUtils.deleteDirectory(tempDirectory.toFile());
        }
    }

    @Nested
    class WriteFile {

        @Test
        public void writeUntracked() throws IOException, GitAPIException {
            boolean first = versioningManager.isClean();

            accessor.writeFile("test.txt", "hello");

            boolean second = versioningManager.isClean();

            int commitCount = versioningManager.getCommitCount();

            assertThat(commitCount).isOne();
            assertThat(first).isTrue();
            assertThat(second).isTrue();
        }

        @Test
        public void modifyFile() throws IOException, GitAPIException {
            boolean first = versioningManager.isClean();

            accessor.writeFile("files/file.yml", "hello");

            boolean second = versioningManager.isClean();
            int commitCountFirst = versioningManager.getCommitCount();

            accessor.writeFile("files/file.yml", "new content");

            boolean third = versioningManager.isClean();
            int commitCountSecond = versioningManager.getCommitCount();

            assertThat(commitCountFirst).isEqualTo(2);
            assertThat(commitCountSecond).isEqualTo(2);
            assertThat(first).isTrue();
            assertThat(second).isTrue();
            assertThat(third).isTrue();
        }
    }
}