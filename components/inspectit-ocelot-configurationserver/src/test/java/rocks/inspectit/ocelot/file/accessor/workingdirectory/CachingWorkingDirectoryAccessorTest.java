package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import com.google.common.cache.LoadingCache;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CachingWorkingDirectoryAccessorTest {

    @InjectMocks
    CachingWorkingDirectoryAccessor directoryCache;

    @Mock
    AutoCommitWorkingDirectoryProxy autoCommitWorkingDirectoryProxy;

    @Nested
    class ListConfigurationFiles {

        @Test
        void emptyResult() {
            doReturn(Collections.emptyList()).when(autoCommitWorkingDirectoryProxy).listConfigurationFiles(any());

            List<FileInfo> result = directoryCache.listConfigurationFiles("");

            assertThat(result).isEmpty();
            assertThat(directoryCache.workspaceCache.asMap().size()).isEqualTo(1);
            verify(autoCommitWorkingDirectoryProxy).listConfigurationFiles("");
            verifyNoMoreInteractions(autoCommitWorkingDirectoryProxy);
        }

        @Test
        void resultAlreadyCached() {
            doReturn(Collections.emptyList()).when(autoCommitWorkingDirectoryProxy).listConfigurationFiles(any());

            //first call for caching
            directoryCache.listConfigurationFiles("");
            List<FileInfo> result = directoryCache.listConfigurationFiles("");

            assertThat(result).isEmpty();
            assertThat(directoryCache.workspaceCache.asMap().size()).isEqualTo(1);
            verify(autoCommitWorkingDirectoryProxy).listConfigurationFiles("");
            verifyNoMoreInteractions(autoCommitWorkingDirectoryProxy);
        }

        @Test
        void catchesException() throws ExecutionException {
            LoadingCache<String, List<FileInfo>> mockCache = mock(LoadingCache.class);
            when(mockCache.get(any())).thenThrow(ExecutionException.class);
            directoryCache.workspaceCache = mockCache;

            List<FileInfo> result = directoryCache.listConfigurationFiles("");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class WriteAgentMappings {

        @Test
        void invalidates() throws IOException {
            doNothing().when(autoCommitWorkingDirectoryProxy).writeAgentMappings(any());
            LoadingCache<String, List<FileInfo>> mockCache = mock(LoadingCache.class);
            directoryCache.workspaceCache = mockCache;

            directoryCache.writeAgentMappings("");

            verify(mockCache).invalidateAll();
            verifyNoMoreInteractions(mockCache);
        }
    }


    @Nested
    class WriteConfigurationFile {

        @Test
        void invalidates() throws IOException {
            doNothing().when(autoCommitWorkingDirectoryProxy).writeConfigurationFile(any(), any());
            LoadingCache<String, List<FileInfo>> mockCache = mock(LoadingCache.class);
            directoryCache.workspaceCache = mockCache;

            directoryCache.writeConfigurationFile("","");

            verify(mockCache).invalidateAll();
            verifyNoMoreInteractions(mockCache);
        }
    }

    @Nested
    class CreateConfigurationDirectory {

        @Test
        void invalidates() throws IOException {
            doNothing().when(autoCommitWorkingDirectoryProxy).createConfigurationDirectory(any());
            LoadingCache<String, List<FileInfo>> mockCache = mock(LoadingCache.class);
            directoryCache.workspaceCache = mockCache;

            directoryCache.createConfigurationDirectory("");

            verify(mockCache).invalidateAll();
            verifyNoMoreInteractions(mockCache);
        }
    }

    @Nested
    class MoveConfiguration {

        @Test
        void invalidates() throws IOException {
            doNothing().when(autoCommitWorkingDirectoryProxy).moveConfiguration(any(), any());
            LoadingCache<String, List<FileInfo>> mockCache = mock(LoadingCache.class);
            directoryCache.workspaceCache = mockCache;

            directoryCache.moveConfiguration("", "");

            verify(mockCache).invalidateAll();
            verifyNoMoreInteractions(mockCache);
        }
    }

    @Nested
    class DeleteConfiguration {

        @Test
        void invalidates() throws IOException {
            doNothing().when(autoCommitWorkingDirectoryProxy).deleteConfiguration(any());
            LoadingCache<String, List<FileInfo>> mockCache = mock(LoadingCache.class);
            directoryCache.workspaceCache = mockCache;

            directoryCache.deleteConfiguration("");

            verify(mockCache).invalidateAll();
            verifyNoMoreInteractions(mockCache);
        }
    }

}
