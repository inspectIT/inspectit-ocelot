package rocks.inspectit.ocelot.rest.file;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.DirectoryCache;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.FileMoveDescription;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MoveControllerTest {

    @Mock
    FileManager fileManager;

    @Mock
    AbstractWorkingDirectoryAccessor fileAccessor;

    @Mock
    DirectoryCache directoryCache;

    @InjectMocks
    MoveController controller;

    @Nested
    class MoveFileOrDirectory {

        @Test
        void sourceWithLeadingSlash() throws Exception {
            when(fileManager.getWorkingDirectory()).thenReturn(fileAccessor);

            controller.moveFileOrDirectory(FileMoveDescription.builder()
                    .source("/src")
                    .target("dest")
                    .build());

            verify(fileManager).getWorkingDirectory();
            verify(fileAccessor).moveConfiguration(eq("src"), eq("dest"));
            verify(directoryCache).invalidate("working");
            verifyNoMoreInteractions(fileManager, fileAccessor, directoryCache);
        }

        @Test
        void targetWithLeadingSlash() throws Exception {
            when(fileManager.getWorkingDirectory()).thenReturn(fileAccessor);

            controller.moveFileOrDirectory(FileMoveDescription.builder()
                    .source("src")
                    .target("/dest")
                    .build());

            verify(fileManager).getWorkingDirectory();
            verify(fileAccessor).moveConfiguration(eq("src"), eq("dest"));
            verify(directoryCache).invalidate("working");
            verifyNoMoreInteractions(fileManager, fileAccessor, directoryCache);
        }

    }

}
