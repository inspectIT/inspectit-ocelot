package rocks.inspectit.ocelot.rest.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
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
    private WorkingDirectoryAccessor accessor;

    @InjectMocks
    private DirectoryController controller;

    @BeforeEach
    public void beforeEach() {
        when(fileManager.getWorkingDirectory()).thenReturn(accessor);
    }

    @Nested
    class ListContents {

        @Test
        public void nullResult() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            when(accessor.listConfigurationFiles(any())).thenReturn(Collections.emptyList());

            Collection<FileInfo> result = controller.listContents(request);

            verify(accessor).listConfigurationFiles("target");
            verifyNoMoreInteractions(accessor);
            assertThat(result).isEmpty();
        }

        @Test
        public void emptyResult() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            when(accessor.listConfigurationFiles("target")).thenReturn(Collections.emptyList());

            Collection<FileInfo> result = controller.listContents(request);

            verify(accessor).listConfigurationFiles("target");
            verifyNoMoreInteractions(accessor);
            assertThat(result).isEmpty();
        }

        @Test
        public void validResponse() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");
            FileInfo fileInfo = mock(FileInfo.class);
            when(accessor.listConfigurationFiles("target")).thenReturn(Collections.singletonList(fileInfo));

            Collection<FileInfo> result = controller.listContents(request);

            verify(accessor).listConfigurationFiles("target");
            verifyNoMoreInteractions(accessor);
            assertThat(result).containsExactly(fileInfo);
        }
    }

    @Nested
    class CreateNewDirectory {

        @Test
        public void successful() throws IOException {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");

            controller.createNewDirectory(request);

            verify(accessor).createConfigurationDirectory("target");
            verifyNoMoreInteractions(accessor);
        }
    }

    @Nested
    class DeleteDirectory {

        @Test
        public void successful() throws IOException {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(anyString())).thenReturn("/api/target", "/api/**");

            controller.deleteDirectory(request);

            verify(accessor).deleteConfiguration("target");
            verifyNoMoreInteractions(accessor);
        }
    }
}