package rocks.inspectit.ocelot.file;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.CachingWorkingDirectoryAccessor;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class FileManagerTest {

    @Nested
    class GetWorkingDirectory {

        @Test
        public void getAccessor() throws GitAPIException, IOException {
            InspectitServerSettings settings = new InspectitServerSettings();
            settings.setWorkingDirectory("/test");
            ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
            FileManager manager = new FileManager(settings, eventPublisher, Runnable::run);

            AbstractWorkingDirectoryAccessor result = manager.getWorkingDirectory();

            assertThat(result).isInstanceOf(CachingWorkingDirectoryAccessor.class);
        }
    }
}
