package rocks.inspectit.ocelot.rest.file;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.DirectoryCache;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.WorkingDirectoryAccessor;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DirectoryControllerTest {

    @Mock
    private FileManager fileManager;

    @Mock
    private WorkingDirectoryAccessor wdAccessor;

    @Mock
    private RevisionAccess revisionAccess;

    @Mock
    private WorkingDirectoryAccessor accessor;

    @Mock
    private DirectoryCache directoryCache;

    @InjectMocks
    private DirectoryController controller;

    @Nested
    class ListContents {

        @Test
        public void nullResult() throws ExecutionException {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            when(directoryCache.get("working", "target")).thenReturn(Collections.emptyList());

            Collection<FileInfo> result = controller.listContents(null, request);

            verify(directoryCache).get("working", "target");
            verifyNoMoreInteractions(directoryCache);
            assertThat(result).isEmpty();
        }

        @Test
        public void emptyResult() throws ExecutionException {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            when(directoryCache.get("working", "target")).thenReturn(Collections.emptyList());

            Collection<FileInfo> result = controller.listContents(null, request);

            verify(directoryCache).get("working", "target");
            verifyNoMoreInteractions(directoryCache);
            assertThat(result).isEmpty();
        }

        @Test
        public void validResponse() throws ExecutionException {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            FileInfo fileInfo = mock(FileInfo.class);
            when(directoryCache.get("working", "target")).thenReturn(Collections.singletonList(fileInfo));

            Collection<FileInfo> result = controller.listContents(null, request);

            verify(directoryCache).get("working", "target");
            verifyNoMoreInteractions(directoryCache);
            assertThat(result).containsExactly(fileInfo);
        }

        @Test
        public void listLiveVersion() throws ExecutionException {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            FileInfo fileInfo = mock(FileInfo.class);
            when(directoryCache.get("live", "target")).thenReturn(Collections.singletonList(fileInfo));

            Collection<FileInfo> result = controller.listContents("live", request);

            verify(directoryCache).get("live", "target");
            verifyNoMoreInteractions(directoryCache);
            assertThat(result).containsExactly(fileInfo);
        }

        @Test
        public void idResponse() throws ExecutionException {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            FileInfo fileInfo = mock(FileInfo.class);
            when(directoryCache.get("123", "target")).thenReturn(Collections.singletonList(fileInfo));

            Collection<FileInfo> result = controller.listContents("123", request);

            verify(directoryCache).get("123", "target");
            verifyNoMoreInteractions(directoryCache);
            assertThat(result).containsExactly(fileInfo);
        }
    }

    @Nested
    class CreateNewDirectory {

        @Test
        public void successful() throws IOException {
            when(fileManager.getWorkingDirectory()).thenReturn(wdAccessor);
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            doNothing().when(directoryCache).invalidate(any());

            controller.createNewDirectory(request);

            verify(wdAccessor).createConfigurationDirectory("target");
            verify(directoryCache).invalidate("working");
            verifyNoMoreInteractions(wdAccessor, directoryCache);

        }
    }

    @Nested
    class DeleteDirectory {

        @Test
        public void successful() throws IOException {
            when(fileManager.getWorkingDirectory()).thenReturn(wdAccessor);
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            doNothing().when(directoryCache).invalidate(any());

            controller.deleteDirectory(request);

            verify(wdAccessor).deleteConfiguration("target");
            verify(directoryCache).invalidate("working");
            verifyNoMoreInteractions(wdAccessor, directoryCache);
        }
    }
}