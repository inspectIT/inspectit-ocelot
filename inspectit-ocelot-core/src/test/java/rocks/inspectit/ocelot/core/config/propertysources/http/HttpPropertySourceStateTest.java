package rocks.inspectit.ocelot.core.config.propertysources.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.MultiValue;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.PropertySource;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpPropertySourceStateTest {

    private HttpPropertySourceState state;

    @Nested
    public class Update {

        private WireMockServer mockServer;

        private HttpConfigSettings httpSettings;

        @BeforeEach
        public void setup() throws MalformedURLException {
            mockServer = new WireMockServer(options().dynamicPort());
            mockServer.start();

            httpSettings = new HttpConfigSettings();
            httpSettings.setUrl(new URL("http://localhost:" + mockServer.port() + "/"));
            httpSettings.setAttributes(new HashMap<>());
            httpSettings.setPersistenceFile(generateTempFilePath());
            state = new HttpPropertySourceState("test-state", httpSettings);
        }


        @AfterEach
        public void teardown() {
            mockServer.stop();
        }

        @Test
        public void fetchingYaml() {
            String config = "inspectit:\n  service-name: test-name";

            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(config)));

            boolean updateResult = state.update(false);
            PropertySource result = state.getCurrentPropertySource();

            assertTrue(updateResult);
            assertThat(new File(httpSettings.getPersistenceFile())).hasContent(config);
            assertThat(result.getProperty("inspectit.service-name")).isEqualTo("test-name");

            List<ServeEvent> requests = mockServer.getServeEvents().getRequests();
            assertThat(requests).hasSize(1);

            List<String> headerKeys = requests.get(0).getRequest().getHeaders().all().stream()
                    .map(MultiValue::key)
                    .filter(key -> key.startsWith("X-OCELOT-"))
                    .collect(Collectors.toList());

            assertThat(headerKeys).containsOnly(
                    "X-OCELOT-AGENT-VERSION",
                    "X-OCELOT-JAVA-VERSION",
                    "X-OCELOT-VM-NAME",
                    "X-OCELOT-VM-VERSION",
                    "X-OCELOT-START-TIME"
            );
        }

        @Test
        public void fetchingJson() {
            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"inspectit\": {\"service-name\": \"test-name\"}}")));

            boolean updateResult = state.update(false);
            PropertySource result = state.getCurrentPropertySource();

            assertTrue(updateResult);
            assertThat(result.getProperty("inspectit.service-name")).isEqualTo("test-name");
        }

        @Test
        public void fetchingEmptyResponse() {
            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("")));

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
            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(config)));

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
            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(config)
                            .withHeader("Last-Modified", "last_modified_header")));
            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .withHeader("If-Modified-Since", equalTo("last_modified_header"))
                    .willReturn(aResponse()
                            .withStatus(304)));

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
            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(config)
                            .withHeader("ETag", "etag_header")));
            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .withHeader("If-None-Match", matching("etag_header.*")) // regex required because this header can be different - e.g. Jetty adds "--gzip" to the ETag header value
                    .willReturn(aResponse()
                            .withStatus(304)));

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

            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .willReturn(aResponse()
                            .withStatus(500)));

            boolean updateResult = state.update(false);
            PropertySource result = state.getCurrentPropertySource();

            assertFalse(updateResult);
            assertThat(((Properties) result.getSource())).isEmpty();
            assertThat(new File(httpSettings.getPersistenceFile())).hasContent("test: testvalue");
        }

        @Test
        public void serverReturnsErrorWithFallback() throws IOException {
            Files.write(Paths.get(httpSettings.getPersistenceFile()), "test: testvalue".getBytes());

            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .willReturn(aResponse()
                            .withStatus(500)));

            boolean updateResult = state.update(true);
            PropertySource result = state.getCurrentPropertySource();

            assertTrue(updateResult);
            assertThat(result.getProperty("test")).isEqualTo("testvalue");
            assertThat(new File(httpSettings.getPersistenceFile())).hasContent("test: testvalue");
        }


        @Test
        public void serverReturnsErrorWithoutFallbackFile() throws IOException {
            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .willReturn(aResponse()
                            .withStatus(500)));

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

            assertThat(state.getEffectiveRequestUri().toString()).isEqualTo("http://localhost:4242/endpoint?fixed=something&service=myservice");
        }
    }


    @Nested
    public class SkipPersistenceFileWriteOnError {

        private WireMockServer mockServer;

        private HttpConfigSettings httpSettings;

        @BeforeEach
        public void setup() throws Exception {
            mockServer = new WireMockServer(options().dynamicPort());
            mockServer.start();

            httpSettings = Mockito.spy(new HttpConfigSettings());
            httpSettings.setUrl(new URL("http://localhost:" + mockServer.port() + "/"));
            httpSettings.setAttributes(new HashMap<>());
            state = Mockito.spy(new HttpPropertySourceState("test-state", httpSettings));
        }


        @AfterEach
        public void teardown() {
            mockServer.stop();
        }


        @Test
        public void fileWritesSkippedOnError() {
            // "/dev/null/*inspectit-config" will fail on Unix and Windows systems
            when(httpSettings.getPersistenceFile()).thenReturn("/dev/null/*inspectit-config");

            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"inspectit\": {\"service-name\": \"test-name\"}}")));

            assertTrue(state.update(false));
            assertFalse(state.isFirstFileWriteAttemptSuccessful());
            Mockito.verify(httpSettings, Mockito.times(1)).getPersistenceFile();
            assertTrue(state.update(false));
            Mockito.verify(httpSettings, Mockito.times(1)).getPersistenceFile();

        }

        @Test
        public void fileWritesContinuedOnSuccess() {
            when(httpSettings.getPersistenceFile()).thenReturn(generateTempFilePath());
            mockServer.stubFor(get(urlPathEqualTo("/"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"inspectit\": {\"service-name\": \"test-name\"}}")));

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