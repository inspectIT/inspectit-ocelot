package rocks.inspectit.oce.eum.server.exporters;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test of PrometheusExporterService
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class PrometheusExporterServiceIntTest {

    private static final int HTTP_TIMEOUT = 1000;
    private static String URL_KEY = "u";
    private static String SUT_URL = "http://test.com/login";
    private static String  METRIC_NAME = "page_ready_time";
    private static String  BEACON_KEY_NAME = "t_page";
    private static String  FAKE_BEACON_KEY_NAME = "does_not_exist";

    @Autowired
    protected MockMvc mockMvc;

    private static CloseableHttpClient testClient;

    @Before
    public void initClient() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        testClient = builder.build();
    }

    /**
     * Sends beacon to mocked endpoint /beacon
     * @param beacon
     * @throws Exception
     */
    private void sendBeacon(Map<String, String> beacon) throws Exception {
        List<NameValuePair> params = beacon.entrySet().stream().map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        mockMvc.perform(post("/beacon").contentType(MediaType.APPLICATION_FORM_URLENCODED).content(EntityUtils.toString(new UrlEncodedFormEntity(params)))).andExpect(status().isAccepted());
    }

    private Map<String, String> getBasicBeacon(){
        Map<String, String> beacon = new HashMap<>();
        beacon.put(URL_KEY, SUT_URL);
        return  beacon;
    }

    void assertGet200(String url) throws Exception {
        int statusCode = testClient.execute(new HttpGet(url)).getStatusLine().getStatusCode();

        assertThat(statusCode).isEqualTo(200);
    }

    @After
    public void closeClient() throws Exception {
        testClient.close();
    }

    @Test
    public void testDefaultSettings() throws Exception {
        assertGet200("http://localhost:8888/metrics");
    }

    /**
     * The application should expose no view, since no beacon entry maps to the default implementation.
     * @throws Exception
     */
    @Test
    public void expectNoViews() throws Exception {
        Map<String, String> beacon =getBasicBeacon();
        beacon.put(FAKE_BEACON_KEY_NAME, "Fake Value");
        sendBeacon(beacon);

        HttpResponse response = testClient.execute(new HttpGet("http://localhost:8888/metrics)"));
        ResponseHandler responseHandler = new BasicResponseHandler();
        assertThat(responseHandler.handleResponse(response).toString()).doesNotContain("Fake Value");
        assertGet200("http://localhost:8888/metrics");
    }

    /**
     * The application should expose one view, since one beacon entry maps to the default implementation.
     * @throws Exception
     */
    @Test
    public void expectOneView() throws Exception {
        Map<String, String> beacon = getBasicBeacon();
        beacon.put(BEACON_KEY_NAME, "12");

        sendBeacon(beacon);

        HttpResponse response = testClient.execute(new HttpGet("http://localhost:8888/metrics)"));
        ResponseHandler responseHandler = new BasicResponseHandler();
        assertThat(responseHandler.handleResponse(response).toString()).contains(METRIC_NAME).contains(SUT_URL).contains("12");
    }
}
