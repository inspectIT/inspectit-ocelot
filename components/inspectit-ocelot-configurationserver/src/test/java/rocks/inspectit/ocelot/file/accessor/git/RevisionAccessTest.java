package rocks.inspectit.ocelot.file.accessor.git;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
class RevisionAccessTest {

    private RevisionAccess revisionAccess;

    @Mock
    private Repository repository;

    @Mock
    private RevCommit revCommit;

    @BeforeEach
    void init() {
        revisionAccess = new RevisionAccess(repository, revCommit, false);
    }

    @Nested
    class VerifyPath {

        @Test
        public void validPath() {
            String result = revisionAccess.verifyPath("files", "test");

            assertThat(result).isEqualTo("files/test");
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
                    .isThrownBy(() -> revisionAccess.verifyPath("files", "../test/"))
                    .withMessage("User path escapes the base path: ../test/");
        }
    }

}