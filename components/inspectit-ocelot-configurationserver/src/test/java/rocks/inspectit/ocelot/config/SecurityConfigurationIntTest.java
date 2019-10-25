package rocks.inspectit.ocelot.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;


@TestPropertySource(properties = {
        "inspectit-config-server.default-user.name=master",
        "inspectit-config-server.default-user.password=foo",
        "inspectit-config-server.token-lifespan=2s"
})
public class SecurityConfigurationIntTest extends IntegrationTestBase {

    private static final String DIRECTORIES_URL = "/api/v1/directories";
    private static final String NEW_TOKEN_URL = "/api/v1/account/token";

    @Nested
    class AuthenticationActive {

        @Test
        void requestsWithoutAuthRejected() {
            ResponseEntity<?> resp = rest.getForEntity(DIRECTORIES_URL, String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }


    @Nested
    class BasicAuthentication {

        @Test
        void invalidUserRejected() {
            ResponseEntity<?> resp = rest
                    .withBasicAuth("admin", "foo")
                    .getForEntity(DIRECTORIES_URL, String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void invalidPasswordRejected() {
            ResponseEntity<?> resp = rest
                    .withBasicAuth("master", "bar")
                    .getForEntity(DIRECTORIES_URL, String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void correctAuthAccepted() {
            ResponseEntity<String> resp = rest
                    .withBasicAuth("master", "foo")
                    .getForEntity(DIRECTORIES_URL, String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isEqualTo("[]");
        }
    }


    @Nested
    class TokenAuthentication {

        private <T> ResponseEntity<T> getWithToken(String url, Class<T> respClass, String token) {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), respClass);
        }


        @Test
        void invalidCredentials() {
            ResponseEntity<?> resp = rest
                    .withBasicAuth("admin", "foo")
                    .getForEntity(NEW_TOKEN_URL, String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void tokenViaBasicAuth() {
            String token = rest
                    .withBasicAuth("master", "foo")
                    .getForObject(NEW_TOKEN_URL, String.class);

            assertThat(token).isNotEmpty();

            ResponseEntity<String> resp = getWithToken(DIRECTORIES_URL, String.class, token);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isEqualTo("[]");
        }

        @Test
        void tokenViaTokenAuth() {
            String token = rest
                    .withBasicAuth("master", "foo")
                    .getForObject(NEW_TOKEN_URL, String.class);

            assertThat(token).isNotEmpty();

            ResponseEntity<String> newTokenResp = getWithToken(NEW_TOKEN_URL, String.class, token);

            assertThat(newTokenResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            String newToken = newTokenResp.getBody();

            ResponseEntity<String> resp = getWithToken(DIRECTORIES_URL, String.class, newToken);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isEqualTo("[]");
        }


        @Test
        void tokenExpiration() throws InterruptedException {
            String token = rest
                    .withBasicAuth("master", "foo")
                    .getForObject(NEW_TOKEN_URL, String.class);

            assertThat(token).isNotEmpty();

            Thread.sleep(3000);

            ResponseEntity<String> resp = getWithToken(DIRECTORIES_URL, String.class, token);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void ensureTokenCannotChangePassword() throws InterruptedException {
            String token = rest
                    .withBasicAuth("master", "foo")
                    .getForObject(NEW_TOKEN_URL, String.class);

            assertThat(token).isNotEmpty();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<?> resp = rest.exchange("/api/v1/account/password", HttpMethod.GET, new HttpEntity<>(headers), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

}
