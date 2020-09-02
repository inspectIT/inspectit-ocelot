package rocks.inspectit.ocelot.rest.file;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.file.FileData;
import rocks.inspectit.ocelot.file.FileInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class FileControllerIntTest extends IntegrationTestBase {

    @Nested
    class ListContent {

        @Test
        public void listFileFromWorkspace() {
            authRest.exchange("/api/v1/files/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<FileInfo[]> result = authRest.getForEntity("/api/v1/directories/", FileInfo[].class);

            FileInfo[] resultBody = result.getBody();
            assertThat(resultBody)
                    .hasSize(1)
                    .extracting(FileInfo::getName, FileInfo::getType, FileInfo::getChildren)
                    .contains(tuple("file.yml", FileInfo.Type.FILE, null));

            ResponseEntity<FileData> result2 = authRest.getForEntity("/api/v1/files/file.yml", FileData.class);

            FileData resultBody2 = result2.getBody();
            assertThat(resultBody2).isNotNull();
            assertThat(resultBody2.getContent()).isBlank();
        }

        @Test
        public void listFileFromLiveWorkspace() {

            authRest.exchange("/api/v1/files/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<FileInfo[]> result = authRest.getForEntity("/api/v1/directories/", FileInfo[].class);

            FileInfo[] resultBody = result.getBody();
            assertThat(resultBody)
                    .hasSize(1)
                    .extracting(FileInfo::getName, FileInfo::getType, FileInfo::getChildren)
                    .contains(tuple("file.yml", FileInfo.Type.FILE, null));

            ResponseEntity<Error> result1 = authRest.getForEntity("/api/v1/files/file.yml?version=live/",Error.class);

            assertThat(result1.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
