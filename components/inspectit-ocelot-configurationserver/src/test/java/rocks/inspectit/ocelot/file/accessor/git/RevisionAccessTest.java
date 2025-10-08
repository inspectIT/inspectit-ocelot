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

    RevisionAccess revisionAccess;

    @Mock
    Repository repository;

    @Mock
    RevCommit revCommit;

    @BeforeEach
    void init() {
        revisionAccess = new RevisionAccess(repository, revCommit, false);
    }

    @Nested
    class VerifyPath {

        @Test
        void validPath() {
            String result = revisionAccess.verifyPath("files", "test");

            assertThat(result).isEqualTo("files/test");
        }

        @Test
        void emptyBase() {
            String result = revisionAccess.verifyPath("", "dir/..");

            assertThat(result).isEmpty();
        }

        @Test
        void nullBase() {
            String result = revisionAccess.verifyPath("", "dir/..");

            assertThat(result).isEmpty();
        }

        @Test
        void invalidPath() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> revisionAccess.verifyPath("files", "../test/"))
                    .withMessage("User path escapes the base path: ../test/");
        }

        @Test
        void backslashPath() {
            String result = revisionAccess.verifyPath("files", "test\\skywalker");

            assertThat(result).isEqualTo("files/test/skywalker");
        }
    }
}