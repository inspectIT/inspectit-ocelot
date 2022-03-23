package rocks.inspectit.ocelot.rest.file;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.file.FileMoveDescription;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"grpc.server.port=-1"})
class MoveControllerIntTest extends IntegrationTestBase {

    @Nested
    class MoveFileOrDirectory {

        @Test
        public void srcNotExisting() {
            HttpEntity<FileMoveDescription> request = new HttpEntity<>(FileMoveDescription.builder()
                    .source("src")
                    .target("trgt")
                    .build());
            ResponseEntity<Void> result = authRest.exchange("/api/v1/move", HttpMethod.PUT, request, Void.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        public void targetExists() {
            // create test files
            authRest.exchange("/api/v1/files/src/file.yml", HttpMethod.PUT, null, Void.class);
            authRest.exchange("/api/v1/files/trgt/file.yml", HttpMethod.PUT, null, Void.class);

            HttpEntity<FileMoveDescription> request = new HttpEntity<>(FileMoveDescription.builder()
                    .source("src")
                    .target("trgt")
                    .build());
            ResponseEntity<Void> result = authRest.exchange("/api/v1/move", HttpMethod.PUT, request, Void.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        public void successfulMove() {
            // create test files
            authRest.exchange("/api/v1/files/src/file.yml", HttpMethod.PUT, null, Void.class);

            assertThat(Paths.get(settings.getWorkingDirectory(), "files/src/file.yml")).exists();
            assertThat(Paths.get(settings.getWorkingDirectory(), "files/trgt/file.yml")).doesNotExist();

            HttpEntity<FileMoveDescription> request = new HttpEntity<>(FileMoveDescription.builder()
                    .source("src")
                    .target("trgt")
                    .build());
            ResponseEntity<Void> result = authRest.exchange("/api/v1/move", HttpMethod.PUT, request, Void.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(Paths.get(settings.getWorkingDirectory(), "files/src/file.yml")).doesNotExist();
            assertThat(Paths.get(settings.getWorkingDirectory(), "files/trgt/file.yml")).exists();
        }
    }
}