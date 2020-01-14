package rocks.inspectit.ocelot.rest.file;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.manager.AbstractFileManager;
import rocks.inspectit.ocelot.file.FileMoveDescription;
import rocks.inspectit.ocelot.file.manager.ConfigurationFileManager;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MoveControllerTest {

    @Mock
    ConfigurationFileManager fileManager;

    @InjectMocks
    MoveController controller;

    @Nested
    class MoveFileOrDirectory {

        @Test
        void sourceWithLeadingSlash() throws Exception {
            controller.moveFileOrDirectory(FileMoveDescription.builder()
                    .source("/src")
                    .target("dest")
                    .build());
            verify(fileManager).move(eq("src"), eq("dest"));
        }

        @Test
        void targetWithLeadingSlash() throws Exception {
            controller.moveFileOrDirectory(FileMoveDescription.builder()
                    .source("src")
                    .target("/dest")
                    .build());
            verify(fileManager).move(eq("src"), eq("dest"));
        }

    }

}
