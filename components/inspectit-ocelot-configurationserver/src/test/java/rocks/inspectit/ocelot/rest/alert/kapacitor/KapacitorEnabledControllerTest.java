package rocks.inspectit.ocelot.rest.alert.kapacitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.KapacitorState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class KapacitorEnabledControllerTest extends KapacitorControllerTestBase {

    private KapacitorEnabledController controller;

    @BeforeEach
    void setup() {
        controller = super.createWithMock(KapacitorEnabledController::new);
    }

    @Nested
    class GetState {

        @Test
        void notConfigured() {
            controller.kapacitorRestTemplate = null;
            KapacitorState result = controller.getState();

            assertThat(result.isEnabled()).isFalse();
            assertThat(result.isKapacitorOnline()).isFalse();
        }

        @Test
        void notReachable() {
            mockKapacitor.expect(requestTo("/kapacitor/v1/ping"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withServerError());

            KapacitorState result = controller.getState();

            assertThat(result.isEnabled()).isTrue();
            assertThat(result.isKapacitorOnline()).isFalse();
            mockKapacitor.verify();
        }

        @Test
        void reachable() {
            mockKapacitor.expect(requestTo("/kapacitor/v1/ping"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.NOT_MODIFIED));

            KapacitorState result = controller.getState();

            assertThat(result.isEnabled()).isTrue();
            assertThat(result.isKapacitorOnline()).isFalse();
            mockKapacitor.verify();
        }
    }
}
