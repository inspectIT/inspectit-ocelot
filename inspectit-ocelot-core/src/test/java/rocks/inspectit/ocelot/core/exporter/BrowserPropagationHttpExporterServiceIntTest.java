package rocks.inspectit.ocelot.core.exporter;

import de.flapdoodle.embed.process.runtime.Network;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.instrumentation.browser.BrowserPropagationSessionStorage;

import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"inspectit.exporters.tags.http.time-to-live=8"})
@DirtiesContext
public class BrowserPropagationHttpExporterServiceIntTest extends SpringTestBase {

    @Autowired
    private BrowserPropagationHttpExporterService exporterService;
    private BrowserPropagationSessionStorage sessionStorage;
    private CloseableHttpClient testClient;

    private static final String sessionID = "test=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-4942-453a-9243-7d8422803604";
    private static final String host = "127.0.0.1";
    private static int port;
    private static final String path = "/inspectit";

    @BeforeEach
    void prepareTest() throws IOException {
        startServer();
        testClient = createHttpClient();
        writeDataIntoStorage();
    }

    @AfterEach
    void clearDataStorage() {
        sessionStorage.clearDataStorages();
    }

    void startServer() throws IOException {
        port = Network.getFreeServerPort();
        HttpServlet servlet = new BrowserPropagationServlet();
        exporterService.startServer(host, port, path, servlet);
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
        sessionStorage = BrowserPropagationSessionStorage.getInstance();
        sessionStorage.getOrCreateDataStorage(sessionID).writeData(data);
    }

    @Nested
    class GetEndpoint {
        @Test
        void verifyGetEndpoint() throws IOException {
            String url = "http://" + host + ":" + port + path;
            HttpGet getRequest = new HttpGet(url);
            getRequest.setHeader("cookie", sessionID);
            CloseableHttpResponse response = testClient.execute(getRequest);

            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);

            assertThat(statusCode).isEqualTo(200);
            assertThat(responseBody).isEqualTo("[{\"key\":\"value\"}]");
            EntityUtils.consume(entity);
            response.close();
        }

        @Test
        void verifyGetEndpointWithoutSessionID() throws IOException {
            String url = "http://" + host + ":" + port + path;
            HttpGet getRequest = new HttpGet(url);

            CloseableHttpResponse response = testClient.execute(getRequest);

            int statusCode = response.getStatusLine().getStatusCode();
            assertThat(statusCode).isEqualTo(400);
            response.close();
        }

        @Test
        void verifyGetEndpointWithWrongSessionID() throws IOException {
            String url = "http://" + host + ":" + port + path;
            HttpGet getRequest = new HttpGet(url);
            getRequest.setHeader("cookie", "###WrongSessionID###");
            CloseableHttpResponse response = testClient.execute(getRequest);

            int statusCode = response.getStatusLine().getStatusCode();
            assertThat(statusCode).isEqualTo(404);
            response.close();
        }
    }

    @Nested
    class PutEndpoint {

        @Test
        void verifyPutEndpointWithCorrectData() throws IOException {
            String url = "http://" + host + ":" + port + path;
            HttpPut putRequest = new HttpPut(url);
            String requestBody = "[{\"newKey\":\"newValue\"}]";
            StringEntity requestEntity = new StringEntity(requestBody);
            putRequest.setEntity(requestEntity);
            putRequest.setHeader("cookie", sessionID);

            CloseableHttpResponse response = testClient.execute(putRequest);
            int statusCode = response.getStatusLine().getStatusCode();

            assertThat(statusCode).isEqualTo(200);
            assertThat(sessionStorage.getOrCreateDataStorage(sessionID).readData()).containsEntry("newKey", "newValue");
            response.close();
        }

        @Test
        void verifyPutEndpointWithIncorrectData() throws IOException {
            String url = "http://" + host + ":" + port + path;
            HttpPut putRequest = new HttpPut(url);
            String requestBody = "##WrongDataFormat##";
            StringEntity requestEntity = new StringEntity(requestBody);
            putRequest.setEntity(requestEntity);
            putRequest.setHeader("cookie", sessionID);

            CloseableHttpResponse response = testClient.execute(putRequest);
            int statusCode = response.getStatusLine().getStatusCode();

            assertThat(statusCode).isEqualTo(400);
            response.close();
        }

        @Test
        void verifyPutEndpointWithoutSessionID() throws IOException {
            String url = "http://" + host + ":" + port + path;
            HttpPut putRequest = new HttpPut(url);
            String requestBody = "[{\"newKey\":\"newValue\"}]";
            StringEntity requestEntity = new StringEntity(requestBody);
            putRequest.setEntity(requestEntity);

            CloseableHttpResponse response = testClient.execute(putRequest);
            int statusCode = response.getStatusLine().getStatusCode();

            assertThat(statusCode).isEqualTo(400);
            assertThat(sessionStorage.getOrCreateDataStorage(sessionID).readData()).doesNotContainEntry("newKey", "newValue");
            response.close();
        }

        @Test
        void verifyPutEndpointWithWrongSessionID() throws IOException {
            String url = "http://" + host + ":" + port + path;
            HttpPut putRequest = new HttpPut(url);
            String requestBody = "[{\"newKey\":\"newValue\"}]";
            StringEntity requestEntity = new StringEntity(requestBody);
            putRequest.setEntity(requestEntity);
            putRequest.setHeader("cookie", "###WrongSessionID###");

            CloseableHttpResponse response = testClient.execute(putRequest);
            int statusCode = response.getStatusLine().getStatusCode();

            assertThat(statusCode).isEqualTo(404);
            assertThat(sessionStorage.getOrCreateDataStorage(sessionID).readData()).doesNotContainEntry("newKey", "newValue");
            response.close();
        }

        @Test
        @Disabled("Fails in Github Actions")
        void verifyPutEndpointTimeToLive() throws IOException, InterruptedException {
            String url = "http://" + host + ":" + port + path;
            HttpPut putRequest = new HttpPut(url);
            String requestBody = "[{\"newKey\":\"newValue\"}]";
            StringEntity requestEntity = new StringEntity(requestBody);
            putRequest.setEntity(requestEntity);
            putRequest.setHeader("cookie", sessionID);

            CloseableHttpResponse response = testClient.execute(putRequest);
            assertThat(sessionStorage.getOrCreateDataStorage(sessionID).readData()).containsEntry("newKey", "newValue");

            // 10s (Clean-Up-Frequency) + 10s (Buffer)
            TimeUnit.SECONDS.sleep(20);
            assertThat(sessionStorage.getOrCreateDataStorage(sessionID).readData()).isEmpty();
            response.close();
        }
    }
}
