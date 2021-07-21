package rocks.inspectit.ocelot.rest.configuration;

import org.eclipse.jgit.diff.DiffEntry;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.file.versioning.model.ConfigurationPromotion;
import rocks.inspectit.ocelot.file.versioning.model.SimpleDiffEntry;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class PromotionControllerIntTest extends IntegrationTestBase {

    @Nested
    class GetPromotions {

        @Test
        public void noPromotionsAvailable() {
            ResponseEntity<WorkspaceDiff> result = authRest.getForEntity("/api/v1/configuration/promotions", WorkspaceDiff.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getEntries()).isEmpty();
        }

        @Test
        public void getPromotionFiles() {
            authRest.exchange("/api/v1/files/src/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<WorkspaceDiff> result = authRest.getForEntity("/api/v1/configuration/promotions", WorkspaceDiff.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getEntries()).containsExactly(SimpleDiffEntry.builder()
                    .file("/src/file.yml")
                    .type(DiffEntry.ChangeType.ADD)
                    .authors(Collections.singletonList(settings.getDefaultUser().getName()))
                    .build());
        }
    }

    @Nested
    class PromoteConfiguration {

        @Test
        public void promoteFiles() {
            authRest.exchange("/api/v1/files/src/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<WorkspaceDiff> diff = authRest.getForEntity("/api/v1/configuration/promotions", WorkspaceDiff.class);

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setFiles(Collections.singletonList("/src/file.yml"));
            promotion.setLiveCommitId(diff.getBody().getLiveCommitId());
            promotion.setWorkspaceCommitId(diff.getBody().getWorkspaceCommitId());
            promotion.setCommitMessage("test");

            ResponseEntity<String> result = authRest.exchange("/api/v1/configuration/promote", HttpMethod.POST, new HttpEntity<>(promotion), String.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo("{\"result\":\"SYNCHRONIZATION_FAILED\"}");
        }

        @Test
        public void promoteNoFiles() {
            authRest.exchange("/api/v1/files/src/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<WorkspaceDiff> diff = authRest.getForEntity("/api/v1/configuration/promotions", WorkspaceDiff.class);

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setFiles(Collections.emptyList());
            promotion.setLiveCommitId(diff.getBody().getLiveCommitId());
            promotion.setWorkspaceCommitId(diff.getBody().getWorkspaceCommitId());
            promotion.setCommitMessage("test");

            ResponseEntity<Void> result = authRest.exchange("/api/v1/configuration/promote", HttpMethod.POST, new HttpEntity<>(promotion), Void.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        public void emptyPromotionMessage() {
            authRest.exchange("/api/v1/files/src/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<WorkspaceDiff> diff = authRest.getForEntity("/api/v1/configuration/promotions", WorkspaceDiff.class);

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setFiles(Collections.emptyList());
            promotion.setLiveCommitId(diff.getBody().getLiveCommitId());
            promotion.setWorkspaceCommitId(diff.getBody().getWorkspaceCommitId());
            promotion.setCommitMessage("");

            ResponseEntity<Void> result = authRest.exchange("/api/v1/configuration/promote", HttpMethod.POST, new HttpEntity<>(promotion), Void.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        public void nullPromotionMessage() {
            authRest.exchange("/api/v1/files/src/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<WorkspaceDiff> diff = authRest.getForEntity("/api/v1/configuration/promotions", WorkspaceDiff.class);

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setFiles(Collections.emptyList());
            promotion.setLiveCommitId(diff.getBody().getLiveCommitId());
            promotion.setWorkspaceCommitId(diff.getBody().getWorkspaceCommitId());
            promotion.setCommitMessage(null);

            ResponseEntity<Void> result = authRest.exchange("/api/v1/configuration/promote", HttpMethod.POST, new HttpEntity<>(promotion), Void.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        public void conflictingPromotion() {
            authRest.exchange("/api/v1/files/src/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<WorkspaceDiff> diff = authRest.getForEntity("/api/v1/configuration/promotions", WorkspaceDiff.class);

            ConfigurationPromotion promotion = new ConfigurationPromotion();
            promotion.setFiles(Collections.singletonList("/src/file.yml"));
            promotion.setLiveCommitId(diff.getBody().getLiveCommitId());
            promotion.setWorkspaceCommitId(diff.getBody().getWorkspaceCommitId());
            promotion.setCommitMessage("test");

            authRest.exchange("/api/v1/configuration/promote", HttpMethod.POST, new HttpEntity<>(promotion), String.class);

            // conflicting promotion
            ResponseEntity<String> result = authRest.exchange("/api/v1/configuration/promote", HttpMethod.POST, new HttpEntity<>(promotion), String.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }
}