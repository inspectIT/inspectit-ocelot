package rocks.inspectit.ocelot.rest.file;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.WorkingDirectoryAccessor;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

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

    @InjectMocks
    private DirectoryController controller;

    @Nested
    class ListContents {

        @Test
        public void nullResult() {
            when(fileManager.getWorkingDirectory()).thenReturn(accessor);
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            when(accessor.listConfigurationFiles(any())).thenReturn(Collections.emptyList());

            Collection<FileInfo> result = controller.listContents(null, request);

            verify(accessor).listConfigurationFiles("target");
            verifyNoMoreInteractions(accessor);
            assertThat(result).isEmpty();
        }

        @Test
        public void emptyResult() {
            when(fileManager.getWorkingDirectory()).thenReturn(accessor);
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            when(accessor.listConfigurationFiles("target")).thenReturn(Collections.emptyList());

            Collection<FileInfo> result = controller.listContents(null, request);

            verify(accessor).listConfigurationFiles("target");
            verifyNoMoreInteractions(accessor);
            assertThat(result).isEmpty();
        }

        @Test
        public void validResponse() {
            when(fileManager.getWorkingDirectory()).thenReturn(accessor);
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            FileInfo fileInfo = mock(FileInfo.class);
            when(accessor.listConfigurationFiles("target")).thenReturn(Collections.singletonList(fileInfo));

            Collection<FileInfo> result = controller.listContents(null, request);

            verify(fileManager).getWorkingDirectory();
            verify(accessor).listConfigurationFiles("target");
            verifyNoMoreInteractions(fileManager, accessor);
            assertThat(result).containsExactly(fileInfo);
        }

        @Test
        public void listLiveVersion() {
            when(fileManager.getLiveRevision()).thenReturn(revisionAccess);
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            FileInfo fileInfo = mock(FileInfo.class);
            when(revisionAccess.listConfigurationFiles("target")).thenReturn(Collections.singletonList(fileInfo));

            Collection<FileInfo> result = controller.listContents("live", request);

            verify(fileManager).getLiveRevision();
            verify(revisionAccess).listConfigurationFiles("target");
            verifyNoMoreInteractions(fileManager, revisionAccess);
            assertThat(result).containsExactly(fileInfo);
        }

        @Test
        public void idResponse() {
            when(fileManager.getCommitWithId("123")).thenReturn(revisionAccess);
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            FileInfo fileInfo = mock(FileInfo.class);
            when(revisionAccess.listConfigurationFiles("target")).thenReturn(Collections.singletonList(fileInfo));

            Collection<FileInfo> result = controller.listContents("123", request);

            verify(fileManager).getCommitWithId("123");
            verify(revisionAccess).listConfigurationFiles("target");
            verifyNoMoreInteractions(fileManager, revisionAccess);
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

            controller.createNewDirectory(request);

            verify(wdAccessor).createConfigurationDirectory("target");
            verifyNoMoreInteractions(wdAccessor);
        }
    }

    @Nested
    class DeleteDirectory {

        @Test
        public void successful() throws IOException {
            when(fileManager.getWorkingDirectory()).thenReturn(wdAccessor);
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");

            controller.deleteDirectory(request);

            verify(wdAccessor).deleteConfiguration("target");
            verifyNoMoreInteractions(wdAccessor);
        }
    }
}