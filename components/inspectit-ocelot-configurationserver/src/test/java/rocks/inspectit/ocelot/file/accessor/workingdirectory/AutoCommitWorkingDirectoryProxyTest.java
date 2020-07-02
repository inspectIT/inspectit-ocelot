package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.file.versioning.VersioningManager;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.Mockito.*;

class AutoCommitWorkingDirectoryProxyTest {

    private AutoCommitWorkingDirectoryProxy accessor;

    private VersioningManager versioningManager;

    private WorkingDirectoryAccessor wdAccessor;

    @BeforeEach
    public void beforeEach() {
        wdAccessor = mock(WorkingDirectoryAccessor.class);
        versioningManager = mock(VersioningManager.class);

        Lock writeLock = mock(Lock.class);

        accessor = new AutoCommitWorkingDirectoryProxy(writeLock, wdAccessor, versioningManager);
    }

    @Nested
    class CreateDirectory {

        @Test
        public void createDirectory() throws IOException, GitAPIException {
            accessor.createDirectory("test");

            verify(wdAccessor).createDirectory("test");
            verify(versioningManager).commitAsExternalChange();
            verify(versioningManager).commitAllChanges("Commit configuration file and agent mapping changes");
            verifyNoMoreInteractions(wdAccessor, versioningManager);
        }

        @Test
        public void ioException() throws IOException, GitAPIException {
            doThrow(IOException.class).when(wdAccessor).createDirectory(anyString());

            assertThatIOException()
                    .isThrownBy(() -> accessor.createDirectory("test"));

            verify(wdAccessor).createDirectory("test");
            verify(versioningManager).commitAsExternalChange();
            verifyNoMoreInteractions(wdAccessor, versioningManager);
        }

        @Test
        public void gitException() throws GitAPIException, IOException {
            doThrow(CanceledException.class).when(versioningManager).commitAllChanges("Commit configuration file and agent mapping changes");

            accessor.createDirectory("test");

            verify(wdAccessor).createDirectory("test");
            verify(versioningManager).commitAsExternalChange();
            verify(versioningManager).commitAllChanges("Commit configuration file and agent mapping changes");
            verifyNoMoreInteractions(wdAccessor, versioningManager);
        }
    }

    @Nested
    class WriteFile {

        @Test
        public void createDirectory() throws IOException, GitAPIException {
            accessor.writeFile("path", "content");

            verify(wdAccessor).writeFile("path", "content");
            verify(versioningManager).commitAsExternalChange();
            verify(versioningManager).commitAllChanges("Commit configuration file and agent mapping changes");
            verifyNoMoreInteractions(wdAccessor, versioningManager);
        }

        @Test
        public void ioException() throws IOException, GitAPIException {
            doThrow(IOException.class).when(wdAccessor).writeFile(anyString(), anyString());

            assertThatIOException()
                    .isThrownBy(() -> accessor.writeFile("path", "content"));

            verify(wdAccessor).writeFile("path", "content");
            verify(versioningManager).commitAsExternalChange();
            verifyNoMoreInteractions(wdAccessor, versioningManager);
        }

        @Test
        public void gitException() throws GitAPIException, IOException {
            doThrow(CanceledException.class).when(versioningManager).commitAllChanges("Commit configuration file and agent mapping changes");

            accessor.writeFile("path", "content");

            verify(wdAccessor).writeFile("path", "content");
            verify(versioningManager).commitAsExternalChange();
            verify(versioningManager).commitAllChanges("Commit configuration file and agent mapping changes");
            verifyNoMoreInteractions(wdAccessor, versioningManager);
        }
    }

    @Nested
    class Move {

        @Test
        public void createDirectory() throws IOException, GitAPIException {
            accessor.move("src", "trgt");

            verify(wdAccessor).move("src", "trgt");
            verify(versioningManager).commitAsExternalChange();
            verify(versioningManager).commitAllChanges("Commit configuration file and agent mapping changes");
            verifyNoMoreInteractions(wdAccessor, versioningManager);
        }

        @Test
        public void ioException() throws IOException, GitAPIException {
            doThrow(IOException.class).when(wdAccessor).move(anyString(), anyString());

            assertThatIOException()
                    .isThrownBy(() -> accessor.move("src", "trgt"));

            verify(wdAccessor).move("src", "trgt");
            verify(versioningManager).commitAsExternalChange();
            verifyNoMoreInteractions(wdAccessor, versioningManager);
        }

        @Test
        public void gitException() throws GitAPIException, IOException {
            doThrow(CanceledException.class).when(versioningManager).commitAllChanges("Commit configuration file and agent mapping changes");

            accessor.move("src", "trgt");

            verify(wdAccessor).move("src", "trgt");
            verify(versioningManager).commitAsExternalChange();
            verify(versioningManager).commitAllChanges("Commit configuration file and agent mapping changes");
            verifyNoMoreInteractions(wdAccessor, versioningManager);
        }
    }

    @Nested
    class Delete {

        @Test
        public void createDirectory() throws IOException, GitAPIException {
            accessor.delete("test");

            verify(wdAccessor).delete("test");
            verify(versioningManager).commitAsExternalChange();
            verify(versioningManager).commitAllChanges("Commit configuration file and agent mapping changes");
            verifyNoMoreInteractions(wdAccessor, versioningManager);
        }

        @Test
        public void ioException() throws IOException, GitAPIException {
            doThrow(IOException.class).when(wdAccessor).delete(anyString());

            assertThatIOException()
                    .isThrownBy(() -> accessor.delete("test"));

            verify(wdAccessor).delete("test");
            verify(versioningManager).commitAsExternalChange();
            verifyNoMoreInteractions(wdAccessor, versioningManager);
        }

        @Test
        public void gitException() throws GitAPIException, IOException {
            doThrow(CanceledException.class).when(versioningManager).commitAllChanges("Commit configuration file and agent mapping changes");

            accessor.delete("test");

            verify(wdAccessor).delete("test");
            verify(versioningManager).commitAsExternalChange();
            verify(versioningManager).commitAllChanges("Commit configuration file and agent mapping changes");
            verifyNoMoreInteractions(wdAccessor, versioningManager);
        }
    }
}