package rocks.inspectit.ocelot.rest.versioning;

import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class VersioningControllerIntTest extends IntegrationTestBase {

    @Nested
    class ListVersions {

        @Test
        public void emptyResponse() {
            ResponseEntity<WorkspaceVersion[]> result = authRest.getForEntity("/api/v1/versioning/list", WorkspaceVersion[].class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            WorkspaceVersion[] resultBody = result.getBody();
            assertThat(resultBody).hasSize(1)
                    .extracting(WorkspaceVersion::getAuthor, WorkspaceVersion::getMessage)
                    .contains(tuple("System", "Initializing Git repository"));
            assertThat(resultBody).allMatch(version -> ObjectId.isId(version.getId()));
        }

        @Test
        public void validResponse() {
            // create test files
            authRest.exchange("/api/v1/files/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<WorkspaceVersion[]> result = authRest.getForEntity("/api/v1/versioning/list", WorkspaceVersion[].class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            WorkspaceVersion[] resultBody = result.getBody();
            assertThat(resultBody).hasSize(3)
                    .extracting(WorkspaceVersion::getAuthor, WorkspaceVersion::getMessage)
                    .contains(tuple("admin", "Commit configuration file and agent mapping changes"), tuple("System", "Commit configuration file and agent mapping changes"), tuple("System", "Initializing Git repository"));
            assertThat(resultBody).allMatch(version -> ObjectId.isId(version.getId()));
        }
    }

}