package rocks.inspectit.ocelot.rest.configuration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;

import static org.assertj.core.api.Assertions.assertThat;

class PromotionControllerIntTest extends IntegrationTestBase {

    @Nested
    class GetPromotions {

        @Test
        public void noPromotionsAvailable() {
            ResponseEntity<WorkspaceDiff> result = authRest.getForEntity("/api/v1/configuration/promotions", WorkspaceDiff.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getDiffEntries()).isEmpty();
        }

        @Test
        public void getPromotionFiles() {
            authRest.exchange("/api/v1/files/src/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<WorkspaceDiff> result = authRest.getForEntity("/api/v1/configuration/promotions", WorkspaceDiff.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getDiffEntries()).hasSize(1);
        }
    }
}