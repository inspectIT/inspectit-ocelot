package rocks.inspectit.ocelot.file.accessor.git;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
class RevisionAccessTest {

    @InjectMocks
    private RevisionAccess revisionAccess;

    @Mock
    private Repository repository;

    @Mock
    private RevCommit revCommit;

    @Nested
    class VerifyPath {

        @Test
        public void validPath() {
            String result = revisionAccess.verifyPath("files", "files/test/");

            assertThat(result).isEqualTo(Paths.get("files", "test").toString());
        }

        @Test
        public void emptyBase() {
            String result = revisionAccess.verifyPath("", "dir/..");

            assertThat(result).isEmpty();
        }

        @Test
        public void nullBase() {
            String result = revisionAccess.verifyPath("", "dir/..");

            assertThat(result).isEmpty();
        }

        @Test
        public void invalidPath() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> revisionAccess.verifyPath("files", "outside/test/"))
                    .withMessage("User path escapes the base path: outside/test/");
        }
    }

}