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
public class CachingDirectoryProxyTest {

    @InjectMocks
    CachingDirectoryProxy directoryCache;

    @Mock
    AutoCommitWorkingDirectoryProxy autoCommitWorkingDirectoryProxy;

    @Nested
    class ListFiles {

        @Test
        void emptyResult() {
            doReturn(Collections.emptyList()).when(autoCommitWorkingDirectoryProxy).listFiles(any());

            List<FileInfo> result = directoryCache.listFiles("");

            assertThat(result.isEmpty());
            assertThat(directoryCache.workspaceCache.asMap().size()).isEqualTo(1);
            verify(autoCommitWorkingDirectoryProxy).listFiles("");
            verifyNoMoreInteractions(autoCommitWorkingDirectoryProxy);
        }

        @Test
        void resultAlreadyCached() {
            doReturn(Collections.emptyList()).when(autoCommitWorkingDirectoryProxy).listFiles(any());

            //first call for caching
            directoryCache.listFiles("");
            List<FileInfo> result = directoryCache.listFiles("");

            assertThat(result.isEmpty());
            assertThat(directoryCache.workspaceCache.asMap().size()).isEqualTo(1);
            verify(autoCommitWorkingDirectoryProxy).listFiles("");
            verifyNoMoreInteractions(autoCommitWorkingDirectoryProxy);
        }

        @Test
        void catchesException() throws ExecutionException {
            LoadingCache<String, List<FileInfo>> mockCache = mock(LoadingCache.class);
            when(mockCache.get(any())).thenThrow(ExecutionException.class);
            directoryCache.workspaceCache = mockCache;

            List<FileInfo> result = directoryCache.listFiles("");

            assertThat(result.isEmpty());
        }
    }

    @Nested
    class WriteFile {

        @Test
        void invalidates() throws IOException {
            doNothing().when(autoCommitWorkingDirectoryProxy).writeFile(any(), any());
            LoadingCache<String, List<FileInfo>> mockCache = mock(LoadingCache.class);
            directoryCache.workspaceCache = mockCache;

            directoryCache.writeFile("","");

            verify(mockCache).invalidateAll();
            verifyNoMoreInteractions(mockCache);
        }
    }

    @Nested
    class CreateDirectory {

        @Test
        void invalidates() throws IOException {
            doNothing().when(autoCommitWorkingDirectoryProxy).createDirectory(any());
            LoadingCache<String, List<FileInfo>> mockCache = mock(LoadingCache.class);
            directoryCache.workspaceCache = mockCache;

            directoryCache.createDirectory("");

            verify(mockCache).invalidateAll();
            verifyNoMoreInteractions(mockCache);
        }
    }

    @Nested
    class Move {

        @Test
        void invalidates() throws IOException {
            doNothing().when(autoCommitWorkingDirectoryProxy).move(any(), any());
            LoadingCache<String, List<FileInfo>> mockCache = mock(LoadingCache.class);
            directoryCache.workspaceCache = mockCache;

            directoryCache.move("", "");

            verify(mockCache).invalidateAll();
            verifyNoMoreInteractions(mockCache);
        }
    }

    @Nested
    class Delete {

        @Test
        void invalidates() throws IOException {
            doNothing().when(autoCommitWorkingDirectoryProxy).delete(any());
            LoadingCache<String, List<FileInfo>> mockCache = mock(LoadingCache.class);
            directoryCache.workspaceCache = mockCache;

            directoryCache.delete("");

            verify(mockCache).invalidateAll();
            verifyNoMoreInteractions(mockCache);
        }
    }

}
