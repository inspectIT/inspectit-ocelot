package rocks.inspectit.ocelot.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DirectoryCacheTest {

    @InjectMocks
    DirectoryCache directoryCache;

    @Mock
    FileManager fileManager;

    @BeforeEach
    void setup(){
        directoryCache.postConstruct();
    }

    @Nested
    class Get {

        @Test
        void TestWorkingDirectory() throws ExecutionException {
            AbstractWorkingDirectoryAccessor mockDirectoryAccessor = mock(AbstractWorkingDirectoryAccessor.class);
            FileInfo mockFileInfo = mock(FileInfo.class);
            List<FileInfo> infoList = Collections.singletonList(mockFileInfo);
            when(mockDirectoryAccessor.listConfigurationFiles(any())).thenReturn(infoList);
            doReturn(mockDirectoryAccessor).when(fileManager).getWorkingDirectory();

            List<FileInfo> result = directoryCache.get("working", "testPath");

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0)).isEqualTo(mockFileInfo);
            assertThat(directoryCache.workingDirectoryCache.asMap()).containsOnlyKeys("testPath");
            verify(mockDirectoryAccessor).listConfigurationFiles("testPath");
            verify(fileManager).getWorkingDirectory();
        }

        @Test
        void TestLiveDirectory() throws ExecutionException {
            RevisionAccess mockRevisionAccess = mock(RevisionAccess.class);
            FileInfo mockFileInfo = mock(FileInfo.class);
            List<FileInfo> infoList = Collections.singletonList(mockFileInfo);
            when(mockRevisionAccess.listConfigurationFiles(any())).thenReturn(infoList);
            doReturn(mockRevisionAccess).when(fileManager).getLiveRevision();

            List<FileInfo> result = directoryCache.get("live", "");

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0)).isEqualTo(mockFileInfo);
            assertThat(directoryCache.versioningDirectoryCache.asMap()).containsOnlyKeys("live");
            verify(mockRevisionAccess).listConfigurationFiles("");
            verify(fileManager).getLiveRevision();
        }

        @Test
        void TestCommitDirectory() throws ExecutionException {
            RevisionAccess mockRevisionAccess = mock(RevisionAccess.class);
            FileInfo mockFileInfo = mock(FileInfo.class);
            List<FileInfo> infoList = Collections.singletonList(mockFileInfo);
            when(mockRevisionAccess.listConfigurationFiles(any())).thenReturn(infoList);
            doReturn(mockRevisionAccess).when(fileManager).getCommitWithId(any());

            List<FileInfo> result = directoryCache.get("someCommit", "");

            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0)).isEqualTo(mockFileInfo);
            assertThat(directoryCache.versioningDirectoryCache.asMap()).containsOnlyKeys("someCommit");
            verify(mockRevisionAccess).listConfigurationFiles("");
            verify(fileManager).getCommitWithId("someCommit");
        }
    }

    @Nested
    class Invalidate {

        @Test
        void invalidateWorkingDirectory() throws ExecutionException {
            //Add entry to cache
            AbstractWorkingDirectoryAccessor mockDirectoryAccessor = mock(AbstractWorkingDirectoryAccessor.class);
            FileInfo mockFileInfo = mock(FileInfo.class);
            List<FileInfo> infoList = Collections.singletonList(mockFileInfo);
            when(mockDirectoryAccessor.listConfigurationFiles(any())).thenReturn(infoList);
            doReturn(mockDirectoryAccessor).when(fileManager).getWorkingDirectory();
            directoryCache.get("working", "");

            directoryCache.invalidate("working");

            assertThat(directoryCache.workingDirectoryCache.size()).isEqualTo(0);
        }

        @Test
        void invalidateLiveDirectory() throws ExecutionException {
            //Add entry to cache
            RevisionAccess mockRevisionAccess = mock(RevisionAccess.class);
            FileInfo mockFileInfo = mock(FileInfo.class);
            List<FileInfo> infoList = Collections.singletonList(mockFileInfo);
            when(mockRevisionAccess.listConfigurationFiles(any())).thenReturn(infoList);
            doReturn(mockRevisionAccess).when(fileManager).getLiveRevision();
            directoryCache.get("live", "");

            directoryCache.invalidate("live");

            assertThat(directoryCache.versioningDirectoryCache.size()).isEqualTo(0);
        }

        @Test
        void invalidateCommitDirectory() throws ExecutionException {
            //Add entry to cache
            RevisionAccess mockRevisionAccess = mock(RevisionAccess.class);
            FileInfo mockFileInfo = mock(FileInfo.class);
            List<FileInfo> infoList = Collections.singletonList(mockFileInfo);
            when(mockRevisionAccess.listConfigurationFiles(any())).thenReturn(infoList);
            doReturn(mockRevisionAccess).when(fileManager).getCommitWithId(any());
            directoryCache.get("someCommit", "");

            directoryCache.invalidate("someCommit");

            assertThat(directoryCache.versioningDirectoryCache.size()).isEqualTo(0);
        }
    }

}
