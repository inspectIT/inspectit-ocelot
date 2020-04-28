package rocks.inspectit.ocelot.file;


import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AbstractWorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.AutoCommitWorkingDirectoryProxy;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.WorkingDirectoryAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class FileManagerTest {

    @Nested
    class GetWorkingDirectory {

        @Test
        public void getAccessor() {
            AutoCommitWorkingDirectoryProxy accessor = mock(AutoCommitWorkingDirectoryProxy.class);
            FileManager manager = new FileManager(accessor);

            AbstractWorkingDirectoryAccessor result = manager.getWorkingDirectory();

            assertThat(result).isSameAs(accessor);
        }
    }
}
