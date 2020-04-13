package rocks.inspectit.ocelot.security.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MultiValueMap;
import rocks.inspectit.ocelot.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "inspectit-config-server.default-user.name=master",
        "inspectit-config-server.security.auth-proxy.enabled=true",
        "inspectit-config-server.security.auth-proxy.principal-request-header=someauthheader",
})
class AuthProxySecurityConfigurationIntTest extends IntegrationTestBase {

    private static final String DIRECTORIES_URL = "/api/v1/directories";

    @Nested
    class AuthenticationActive {

        @Test
        void requestsWithoutAuthRejected() {
            ResponseEntity<?> resp = rest.getForEntity(DIRECTORIES_URL, String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }


    @Nested
    class PreAuthentication {

        @Test
        void invalidUserRejected() {
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("someauthheader", "someuser");

            ResponseEntity<?> resp = rest
                    .exchange(DIRECTORIES_URL, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void invalidHeaderRejected() {
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("hackersheader", "master");

            ResponseEntity<?> resp = rest
                    .exchange(DIRECTORIES_URL, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void correctAuthAccepted() {
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("someauthheader", "master");

            ResponseEntity<?> resp = rest
                    .exchange(DIRECTORIES_URL, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isEqualTo("[]");
        }
    }

}