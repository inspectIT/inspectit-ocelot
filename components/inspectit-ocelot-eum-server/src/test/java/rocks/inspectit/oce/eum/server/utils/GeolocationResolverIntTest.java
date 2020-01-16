package rocks.inspectit.oce.eum.server.utils;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import rocks.inspectit.oce.eum.server.beacon.processor.CountryCodeBeaconProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test of {@link GeolocationResolver}
 */
@SpringBootTest
@AutoConfigureMockMvc
public class GeolocationResolverIntTest {

    private static String URL_KEY = "u";
    private static String SUT_URL = "http://test.com/login";
    private static String BEACON_KEY_NAME = "t_page";

    @Autowired
    protected MockMvc mockMvc;

    private static CloseableHttpClient testClient;

    @BeforeEach
    public void initClient() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        testClient = builder.build();
    }

    /**
     * Sends beacon to mocked endpoint /beacon
     *
     * @param beacon
     * @throws Exception
     */
    private void sendBeacon(Map<String, String> beacon, String requesterIP) throws Exception {
        List<NameValuePair> params = beacon.entrySet().stream().map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        mockMvc.perform(post("/beacon").header("X-Forwarded-For", requesterIP).contentType(MediaType.APPLICATION_FORM_URLENCODED).content(EntityUtils.toString(new UrlEncodedFormEntity(params)))).andExpect(status().isOk());
    }

    private Map<String, String> getBasicBeacon() {
        Map<String, String> beacon = new HashMap<>();
        beacon.put(URL_KEY, SUT_URL);
        return beacon;
    }

    @AfterEach
    public void closeClient() throws Exception {
        testClient.close();
    }

    /**
     * The application should expose a view, where the tag COUNTRY_CODE is set to DE
     *
     * @throws Exception
     */
    @Test
    public void expectCountryCodeToBeSet() throws Exception {
        Map<String, String> beacon = getBasicBeacon();
        beacon.put(BEACON_KEY_NAME, "12");

        sendBeacon(beacon, "94.186.169.18");

        HttpResponse response = testClient.execute(new HttpGet("http://localhost:8888/metrics)"));
        ResponseHandler responseHandler = new BasicResponseHandler();
        assertThat(responseHandler.handleResponse(response).toString()).contains(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE + "=" + "\"DE\"");
    }

    /**
     * The application should expose a view, where the tag COUNTRY_CODE is not set
     *
     * @throws Exception
     */
    @Test
    public void expectCountryCodeToBeNotSetNotResolvableIP() throws Exception {
        Map<String, String> beacon = getBasicBeacon();
        beacon.put(BEACON_KEY_NAME, "12");

        sendBeacon(beacon, "127.0.0.1");

        HttpResponse response = testClient.execute(new HttpGet("http://localhost:8888/metrics)"));
        ResponseHandler responseHandler = new BasicResponseHandler();
        assertThat(responseHandler.handleResponse(response).toString()).contains(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE + "=" + "\"\"");
    }

    /**
     * The application should expose a view, where the tag COUNTRY_CODE is not set
     *
     * @throws Exception
     */
    @Test
    public void expectCountryCodeToBeNotSetWrongFormattedIP() throws Exception {
        Map<String, String> beacon = getBasicBeacon();
        beacon.put(BEACON_KEY_NAME, "12");

        sendBeacon(beacon, "wrong-formatted-ip");

        HttpResponse response = testClient.execute(new HttpGet("http://localhost:8888/metrics)"));
        ResponseHandler responseHandler = new BasicResponseHandler();
        assertThat(responseHandler.handleResponse(response).toString()).contains(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE + "=" + "\"\"");
    }
}
