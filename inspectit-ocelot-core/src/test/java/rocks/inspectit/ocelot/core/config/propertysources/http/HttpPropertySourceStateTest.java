package rocks.inspectit.ocelot.core.config.propertysources.http;

import com.google.common.collect.ImmutableMap;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.PropertySource;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;
import rocks.inspectit.ocelot.config.model.config.RetrySettings;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpPropertySourceStateTest {

    private HttpPropertySourceState state;

    @Nested
    public class Update {

        private MockWebServer mockServer;

        private HttpConfigSettings httpSettings;

        @BeforeEach
        public void setup() throws IOException {
            mockServer = new MockWebServer();
            mockServer.start();

            httpSettings = new HttpConfigSettings();
            httpSettings.setUrl(new URL(mockServer.url("/").toString()));
            httpSettings.setAttributes(new HashMap<>());
            httpSettings.setPersistenceFile(generateTempFilePath());
            httpSettings.setConnectionTimeout(Duration.ofSeconds(5));
            httpSettings.setSocketTimeout(Duration.ofSeconds(5));
            state = new HttpPropertySourceState("test-state", httpSettings);
        }

        @AfterEach
        public void shutdown() throws IOException {
            mockServer.shutdown();
        }

        @Test
        public void fetchingYaml() throws InterruptedException {
            String config = "inspectit:\n  service-name: test-name";
            MockResponse mockResponse = new MockResponse()
                    .setBody(config)
                    .setHeader("Content-Type", "application/x-yaml")
                    .setResponseCode(200);
            mockServer.enqueue(mockResponse);

            boolean updateResult = state.update(false);
            PropertySource result = state.getCurrentPropertySource();

            assertTrue(updateResult);
            assertThat(new File(httpSettings.getPersistenceFile())).hasContent(config);
            assertThat(result.getProperty("inspectit.service-name")).isEqualTo("test-name");

            RecordedRequest request = mockServer.takeRequest();

            assertThat(request).isNotNull();

            Set<String> headerKeys = request.getHeaders()
                    .toMultimap()
                    .keySet().stream()
                    .map(String::toUpperCase)
                    .filter(key -> key.startsWith("X-OCELOT-"))
                    .collect(Collectors.toSet());

            assertThat(headerKeys).containsOnly("X-OCELOT-AGENT-ID", "X-OCELOT-AGENT-VERSION", "X-OCELOT-JAVA-VERSION", "X-OCELOT-VM-NAME", "X-OCELOT-VM-VENDOR", "X-OCELOT-START-TIME", "X-OCELOT-HEALTH", "X-OCELOT-SERVICE-STATES-MAP");
        }

        @Test
        public void fetchingJson() {
            String json = "{\"inspectit\": {\"service-name\": \"test-name\"}}";
            MockResponse mockResponse = new MockResponse()
                    .setBody(json)
                    .setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                    .setResponseCode(200);
            mockServer.enqueue(mockResponse);

            boolean updateResult = state.update(false);
            PropertySource result = state.getCurrentPropertySource();

            assertTrue(updateResult);
            assertThat(result.getProperty("inspectit.service-name")).isEqualTo("test-name");
        }

        @Test
        public void fetchingEmptyResponse() {
            MockResponse mockResponse = new MockResponse()
                    .setBody("")
                    .setResponseCode(200);
            mockServer.enqueue(mockResponse);

            boolean updateResult = state.update(false);
            PropertySource result = state.getCurrentPropertySource();
            Properties source = (Properties) result.getSource();

            assertTrue(updateResult);
            assertThat(new File(httpSettings.getPersistenceFile())).hasContent("");
            assertTrue(source.isEmpty());
        }

        @Test
        public void multipleFetchingWithoutCaching() {
            String config = "{\"inspectit\": {\"service-name\": \"test-name\"}}";
            MockResponse mockResponse = new MockResponse()
                    .setBody(config)
                    .setResponseCode(200);
            mockServer.enqueue(mockResponse);
            mockServer.enqueue(mockResponse);

            boolean updateResultFirst = state.update(false);
            PropertySource resultFirst = state.getCurrentPropertySource();

            boolean updateResultSecond = state.update(false);
            PropertySource resultSecond = state.getCurrentPropertySource();

            assertTrue(updateResultFirst);
            assertTrue(updateResultSecond);
            assertNotSame(resultFirst, resultSecond);
            assertThat(resultFirst.getProperty("inspectit.service-name")).isEqualTo("test-name");
            assertThat(resultSecond.getProperty("inspectit.service-name")).isEqualTo("test-name");
            assertThat(new File(httpSettings.getPersistenceFile())).hasContent(config);
        }

        @Test
        public void usingLastModifiedHeader() {
            String config = "{\"inspectit\": {\"service-name\": \"test-name\"}}";
            MockResponse mockResponse1 = new MockResponse()
                    .setBody(config)
                    .setHeader("Last-Modified", "last_modified_header")
                    .setResponseCode(200);
            MockResponse mockResponse2 = new MockResponse()
                    .setHeader("If-Modified-Since", "last_modified_header")
                    .setResponseCode(304);
            mockServer.enqueue(mockResponse1);
            mockServer.enqueue(mockResponse2);

            boolean updateResultFirst = state.update(false);
            PropertySource resultFirst = state.getCurrentPropertySource();

            boolean updateResultSecond = state.update(false);
            PropertySource resultSecond = state.getCurrentPropertySource();

            assertTrue(updateResultFirst);
            assertFalse(updateResultSecond);
            assertSame(resultFirst, resultSecond);
            assertThat(resultFirst.getProperty("inspectit.service-name")).isEqualTo("test-name");
            assertThat(new File(httpSettings.getPersistenceFile())).hasContent(config);
        }

        @Test
        public void usingETagHeader() {
            String config = "{\"inspectit\": {\"service-name\": \"test-name\"}}";
            MockResponse mockResponse1 = new MockResponse()
                    .setBody(config)
                    .setHeader("ETag", "etag_header")
                    .setResponseCode(200);
            // regex required because this header can be different - e.g. Jetty adds "--gzip" to the ETag header value
            MockResponse mockResponse2 = new MockResponse()
                    .setHeader("If-None-Match", "etag_header.*")
                    .setResponseCode(304);
            mockServer.enqueue(mockResponse1);
            mockServer.enqueue(mockResponse2);

            boolean updateResultFirst = state.update(false);
            PropertySource resultFirst = state.getCurrentPropertySource();

            boolean updateResultSecond = state.update(false);
            PropertySource resultSecond = state.getCurrentPropertySource();

            assertTrue(updateResultFirst);
            assertFalse(updateResultSecond);
            assertSame(resultFirst, resultSecond);
            assertThat(resultFirst.getProperty("inspectit.service-name")).isEqualTo("test-name");
            assertThat(new File(httpSettings.getPersistenceFile())).hasContent(config);
        }

        @Test
        public void serverReturnsErrorNoFallback() throws IOException {
            Files.write(Paths.get(httpSettings.getPersistenceFile()), "test: testvalue".getBytes());
            MockResponse mockResponse = new MockResponse().setResponseCode(500);
            mockServer.enqueue(mockResponse);

            boolean updateResult = state.update(false);
            PropertySource result = state.getCurrentPropertySource();

            assertFalse(updateResult);
            assertThat(((Properties) result.getSource())).isEmpty();
            assertThat(new File(httpSettings.getPersistenceFile())).hasContent("test: testvalue");
        }

        @Test
        public void serverReturnsErrorWithFallback() throws IOException {
            Files.write(Paths.get(httpSettings.getPersistenceFile()), "test: testvalue".getBytes());
            MockResponse mockResponse = new MockResponse().setResponseCode(500);
            mockServer.enqueue(mockResponse);

            boolean updateResult = state.update(true);
            PropertySource result = state.getCurrentPropertySource();

            assertTrue(updateResult);
            assertThat(result.getProperty("test")).isEqualTo("testvalue");
            assertThat(new File(httpSettings.getPersistenceFile())).hasContent("test: testvalue");
        }

        @Test
        public void serverReturnsErrorWithoutFallbackFile() {
            MockResponse mockResponse = new MockResponse().setResponseCode(500);
            mockServer.enqueue(mockResponse);

            boolean updateResult = state.update(false);
            PropertySource result = state.getCurrentPropertySource();

            assertFalse(updateResult);
            assertThat(((Properties) result.getSource())).isEmpty();
            assertThat(new File(httpSettings.getPersistenceFile())).doesNotExist();
        }
    }

    @Nested
    public class GetEffectiveRequestUri {

        @Test
        void emptyParametersIgnored() throws Exception {
            HttpConfigSettings httpSettings = new HttpConfigSettings();
            httpSettings.setUrl(new URL("http://localhost:4242/endpoint"));

            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("a", null);
            attributes.put("b", "valb");
            attributes.put("c", "");
            httpSettings.setAttributes(attributes);

            state = new HttpPropertySourceState("test-state", httpSettings);

            assertThat(state.getEffectiveRequestUri().toString()).isEqualTo("http://localhost:4242/endpoint?b=valb");
        }

        @Test
        void existingParametersPreserved() throws Exception {
            HttpConfigSettings httpSettings = new HttpConfigSettings();
            httpSettings.setUrl(new URL("http://localhost:4242/endpoint?fixed=something"));
            httpSettings.setAttributes(ImmutableMap.of("service", "myservice"));

            state = new HttpPropertySourceState("test-state", httpSettings);

            assertThat(state.getEffectiveRequestUri()
                    .toString()).isEqualTo("http://localhost:4242/endpoint?fixed=something&service=myservice");
        }
    }

    @Nested
    public class Retries {

        private final MockWebServer mockServer = new MockWebServer();

        private final MockResponse successfulResponse = new MockResponse()
                .setBody("inspectit:\n  service-name: test-name")
                .setHeader("Content-Type", "application/x-yaml")
                .setResponseCode(200);

        private final MockResponse unsuccessfulResponse = new MockResponse().setResponseCode(500);

        @BeforeEach
        public void setup() throws IOException {
            mockServer.start();

            HttpConfigSettings httpSettings = new HttpConfigSettings();
            httpSettings.setUrl(new URL(mockServer.url("/").toString()));
            httpSettings.setAttributes(new HashMap<>());
            httpSettings.setConnectionTimeout(Duration.ofSeconds(5));
            httpSettings.setSocketTimeout(Duration.ofSeconds(5));
            RetrySettings retrySettings = new RetrySettings();
            retrySettings.setEnabled(true);
            retrySettings.setMaxAttempts(2);
            retrySettings.setInitialInterval(Duration.ofMillis(5));
            retrySettings.setMultiplier(BigDecimal.ONE);
            retrySettings.setRandomizationFactor(BigDecimal.valueOf(0.1));
            httpSettings.setRetry(retrySettings);

            state = new HttpPropertySourceState("retry-test-state", httpSettings);
        }

        @AfterEach
        public void shutdown() throws IOException {
            mockServer.shutdown();
        }

        @Test
        void updateReturnsTrueIfAvailable() {
            mockServer.enqueue(successfulResponse);

            boolean successfulUpdate = state.update(false);

            assertThat(successfulUpdate).isTrue();
        }

        @Test
        void updateReturnsTrueAfterSuccessfulRetry() {
            // First request fails
            mockServer.enqueue(unsuccessfulResponse);
            // Second request succeeds
            mockServer.enqueue(successfulResponse);

            boolean successfulUpdate = state.update(false);

            assertThat(successfulUpdate).isTrue();
        }

        @Test
        void failsIfMaxAttemptsIsReached() {
            mockServer.enqueue(unsuccessfulResponse);
            mockServer.enqueue(unsuccessfulResponse);

            boolean unsuccessfulUpdate = state.update(false);

            assertThat(unsuccessfulUpdate).isFalse();
        }

        @Test
        void failsIfServerIsNotAvailable() throws IOException {
            mockServer.shutdown();

            boolean unsuccessfulUpdate = state.update(false);

            assertThat(unsuccessfulUpdate).isFalse();
        }

        @Test
        void noRetriesIfFallingBackToFile() {
            mockServer.enqueue(unsuccessfulResponse);

            state.update(true);

            assertThat(mockServer.getRequestCount()).isEqualTo(1);
        }
    }

    @Nested
    public class SkipPersistenceFileWriteOnError {

        private MockWebServer mockServer;

        private HttpConfigSettings httpSettings;

        @BeforeEach
        public void setup() throws Exception {
            mockServer = new MockWebServer();
            mockServer.start();

            httpSettings = Mockito.spy(new HttpConfigSettings());
            httpSettings.setUrl(new URL(mockServer.url("/").toString()));
            httpSettings.setAttributes(new HashMap<>());
            httpSettings.setConnectionTimeout(Duration.ofSeconds(5));
            httpSettings.setSocketTimeout(Duration.ofSeconds(5));
            state = Mockito.spy(new HttpPropertySourceState("test-state", httpSettings));
        }

        @AfterEach
        public void shutdown() throws IOException {
            mockServer.shutdown();
        }

        @Test
        public void fileWritesSkippedOnError() {
            // "/dev/null/*inspectit-config" will fail on Unix and Windows systems
            when(httpSettings.getPersistenceFile()).thenReturn("/dev/null/*inspectit-config");
            MockResponse mockResponse = new MockResponse()
                    .setBody("{\"inspectit\": {\"service-name\": \"test-name\"}}")
                    .setResponseCode(200);
            mockServer.enqueue(mockResponse);
            mockServer.enqueue(mockResponse);

            assertTrue(state.update(false));
            assertFalse(state.isFirstFileWriteAttemptSuccessful());
            Mockito.verify(httpSettings, Mockito.times(1)).getPersistenceFile();
            assertTrue(state.update(false));
            Mockito.verify(httpSettings, Mockito.times(1)).getPersistenceFile();
        }

        @Test
        public void fileWritesContinuedOnSuccess() {
            when(httpSettings.getPersistenceFile()).thenReturn(generateTempFilePath());
            MockResponse mockResponse = new MockResponse()
                    .setBody("{\"inspectit\": {\"service-name\": \"test-name\"}}")
                            .setResponseCode(200);
            mockServer.enqueue(mockResponse);
            mockServer.enqueue(mockResponse);

            assertTrue(state.update(false));
            assertTrue(state.isFirstFileWriteAttemptSuccessful());
            Mockito.verify(httpSettings, Mockito.times(1)).getPersistenceFile();
            assertTrue(state.update(false));
            Mockito.verify(httpSettings, Mockito.times(2)).getPersistenceFile();

        }
    }

    private static String generateTempFilePath() {
        try {
            Path tempFile = Files.createTempFile("inspectit", "");
            Files.delete(tempFile);
            return tempFile.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
