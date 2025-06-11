package rocks.inspectit.ocelot.core.exporter;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.SocketUtils;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.instrumentation.browser.BrowserPropagationSessionStorage;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
public class BrowserPropagationHttpExporterServiceIntTest extends SpringTestBase {

    @Autowired
    private BrowserPropagationHttpExporterService exporterService;
    private BrowserPropagationSessionStorage sessionStorage;
    private PropagationMetaData propagation;
    private CloseableHttpClient testClient;

    private static final String sessionID = "test=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-4942-453a-9243-7d8422803604";
    private static final String host = "127.0.0.1";
    private static String url;

    private static final String path = "/inspectit";
    private static final String sessionIDHeader = "Session-Id";
    private static final String allowedOrigin = "localhost";

    @BeforeEach
    void prepareTest() throws IOException {
        startServer();
        propagation = createPropagation();
        testClient = createHttpClient();
        writeDataIntoStorage();
    }

    @AfterEach
    void clearDataStorage() {
        sessionStorage.clearDataStorages();
    }

    void startServer() throws IOException {
        int port = SocketUtils.findAvailableTcpPort();
        BrowserPropagationHandler handler = new BrowserPropagationHandler(sessionIDHeader, Collections.singletonList(allowedOrigin));
        exporterService.startServer(host, port, path, handler);
        url = "http://" + host + ":" + port + path;
    }

    CloseableHttpClient createHttpClient(){
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(requestBuilder.build());
        return builder.build();
    }

    void writeDataIntoStorage() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        sessionStorage = BrowserPropagationSessionStorage.get();
        sessionStorage.getOrCreateDataStorage(sessionID, propagation).writeData(data);
    }

    @Nested
    class GetEndpoint {

        @Test
        void verifyGetEndpoint() throws IOException, ParseException {
            ClassicHttpRequest getRequest = ClassicRequestBuilder.get().setUri(url).build();
            getRequest.setHeader("Origin", allowedOrigin);
            getRequest.setHeader(sessionIDHeader, sessionID);

            CloseableHttpResponse response = testClient.execute(getRequest);
            int statusCode = response.getCode();
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);

            assertThat(statusCode).isEqualTo(200);
            assertThat(responseBody).isEqualTo("[{\"key\":\"value\"}]");
            EntityUtils.consume(entity);
            response.close();
        }

        @Test
        void verifyGetEndpointWithoutSessionID() throws IOException {
            ClassicHttpRequest getRequest = ClassicRequestBuilder.get().setUri(url).build();
            getRequest.setHeader("Origin", allowedOrigin);

            CloseableHttpResponse response = testClient.execute(getRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(400);
            response.close();
        }

        @Test
        void verifyGetEndpointWithWrongSessionID() throws IOException {
            ClassicHttpRequest getRequest = ClassicRequestBuilder.get().setUri(url).build();
            getRequest.setHeader("Origin", allowedOrigin);
            getRequest.setHeader(sessionIDHeader, "###WrongSessionID###");

            CloseableHttpResponse response = testClient.execute(getRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(404);
            response.close();
        }

        @Test
        void verifyGetEndpointWithoutCorrectSessionIDKey() throws IOException {
            ClassicHttpRequest getRequest = ClassicRequestBuilder.get().setUri(url).build();
            getRequest.setHeader("Origin", allowedOrigin);
            getRequest.setHeader(sessionIDHeader + "-1", sessionID);

            CloseableHttpResponse response = testClient.execute(getRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(400);
            response.close();
        }

        @Test
        void verifyGetEndpointWithoutAllowedOrigin() throws IOException {
            ClassicHttpRequest getRequest = ClassicRequestBuilder.get().setUri(url).build();
            getRequest.setHeader("Origin", "www.example.com");

            CloseableHttpResponse response = testClient.execute(getRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(403);
            response.close();
        }
    }

    @Nested
    class PutEndpoint {

        @Test
        void verifyPutEndpointWithCorrectData() throws IOException {
            ClassicHttpRequest putRequest = ClassicRequestBuilder.put().setUri(url).build();
            String requestBody = "[{\"newKey\":\"newValue\"}]";
            StringEntity requestEntity = new StringEntity(requestBody);
            putRequest.setEntity(requestEntity);
            putRequest.setHeader("Origin", allowedOrigin);
            putRequest.setHeader(sessionIDHeader, sessionID);

            CloseableHttpResponse response = testClient.execute(putRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(200);
            assertThat(sessionStorage.getOrCreateDataStorage(sessionID, propagation).readData()).containsEntry("newKey", "newValue");
            response.close();
        }

        @Test
        void verifyPutEndpointWithIncorrectData() throws IOException {
            ClassicHttpRequest putRequest = ClassicRequestBuilder.put().setUri(url).build();
            String requestBody = "##WrongDataFormat##";
            StringEntity requestEntity = new StringEntity(requestBody);
            putRequest.setEntity(requestEntity);
            putRequest.setHeader("Origin", allowedOrigin);
            putRequest.setHeader(sessionIDHeader, sessionID);

            CloseableHttpResponse response = testClient.execute(putRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(400);
            response.close();
        }

        @Test
        void verifyPutEndpointWithoutSessionID() throws IOException {
            ClassicHttpRequest putRequest = ClassicRequestBuilder.put().setUri(url).build();
            String requestBody = "[{\"newKey\":\"newValue\"}]";
            StringEntity requestEntity = new StringEntity(requestBody);
            putRequest.setEntity(requestEntity);
            putRequest.setHeader("Origin", allowedOrigin);

            CloseableHttpResponse response = testClient.execute(putRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(400);
            assertThat(sessionStorage.getOrCreateDataStorage(sessionID, propagation).readData()).doesNotContainEntry("newKey", "newValue");
            response.close();
        }

        @Test
        void verifyPutEndpointWithWrongSessionID() throws IOException {
            ClassicHttpRequest putRequest = ClassicRequestBuilder.put().setUri(url).build();
            String requestBody = "[{\"newKey\":\"newValue\"}]";
            StringEntity requestEntity = new StringEntity(requestBody);
            putRequest.setEntity(requestEntity);
            putRequest.setHeader("Origin", allowedOrigin);
            putRequest.setHeader(sessionIDHeader, "###WrongSessionID###");

            CloseableHttpResponse response = testClient.execute(putRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(404);
            assertThat(sessionStorage.getOrCreateDataStorage(sessionID, propagation).readData()).doesNotContainEntry("newKey", "newValue");
            response.close();
        }

        @Test
        void verifyPutEndpointWithoutCorrectSessionIDKey() throws IOException {
            ClassicHttpRequest putRequest = ClassicRequestBuilder.put().setUri(url).build();
            String requestBody = "[{\"newKey\":\"newValue\"}]";
            StringEntity requestEntity = new StringEntity(requestBody);
            putRequest.setEntity(requestEntity);
            putRequest.setHeader("Origin", allowedOrigin);
            putRequest.setHeader(sessionIDHeader + "-1", sessionID);

            CloseableHttpResponse response = testClient.execute(putRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(400);
            assertThat(sessionStorage.getOrCreateDataStorage(sessionID, propagation).readData()).doesNotContainEntry("newKey", "newValue");
            response.close();
        }

        @Test
        void verifyPutEndpointWithoutAllowedOrigin() throws IOException {
            ClassicHttpRequest putRequest = ClassicRequestBuilder.put().setUri(url).build();
            putRequest.setHeader("Origin", "www.example.com");

            CloseableHttpResponse response = testClient.execute(putRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(403);
            response.close();
        }
    }

    @Nested
    class OptionsEndpoint {

        @Test
        void verifyOptionsEndpointSuccessfulGet() throws IOException {
            ClassicHttpRequest optionsRequest = ClassicRequestBuilder.options().setUri(url).build();
            optionsRequest.setHeader("Origin", allowedOrigin);
            optionsRequest.setHeader("access-control-request-method", "GET");
            optionsRequest.setHeader("access-control-request-headers", sessionIDHeader);

            CloseableHttpResponse response = testClient.execute(optionsRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(200);
            response.close();
        }

        @Test
        void verifyOptionsEndpointSuccessfulPut() throws IOException {
            ClassicHttpRequest optionsRequest = ClassicRequestBuilder.options().setUri(url).build();
            optionsRequest.setHeader("Origin", allowedOrigin);
            optionsRequest.setHeader("access-control-request-method", "PUT");
            optionsRequest.setHeader("access-control-request-headers", sessionIDHeader);

            CloseableHttpResponse response = testClient.execute(optionsRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(200);
            response.close();
        }

        @Test
        void verifyOptionsEndpointWithMissingHeaders() throws IOException {
            ClassicHttpRequest optionsRequest = ClassicRequestBuilder.options().setUri(url).build();
            optionsRequest.setHeader("Origin", allowedOrigin);

            CloseableHttpResponse response = testClient.execute(optionsRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(403);
            response.close();
        }

        @Test
        void verifyOptionsEndpointWithoutAllowedOrigin() throws IOException {
            ClassicHttpRequest optionsRequest = ClassicRequestBuilder.options().setUri(url).build();
            optionsRequest.setHeader("Origin", "www.example.com");

            CloseableHttpResponse response = testClient.execute(optionsRequest);
            int statusCode = response.getCode();

            assertThat(statusCode).isEqualTo(403);
            response.close();
        }
    }

    private PropagationMetaData createPropagation() {
        return PropagationMetaData.builder()
                .setBrowserPropagation("key", true)
                .setBrowserPropagation("newKey", true)
                .build();
    }
}
