package rocks.inspectit.oce.eum.server.utils;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.beacon.processor.CountryCodeBeaconProcessor;
import rocks.inspectit.oce.eum.server.metrics.BeaconMetricManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test of {@link GeolocationResolver}
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
public class GeolocationResolverIntTest {

    private static String URL_KEY = "u";
    private static String SUT_URL = "http://test.com/login";
    private static String BEACON_KEY_NAME = "t_page";

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    BeaconMetricManager beaconMetricManager;

    @Captor
    ArgumentCaptor<Beacon> beaconCaptor;

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

    /**
     * The application should process a beacon, where the tag COUNTRY_CODE is set to DE
     *
     * @throws Exception
     */
    @Test
    public void expectCountryCodeToBeSet() throws Exception {
        Map<String, String> beacon = getBasicBeacon();
        beacon.put(BEACON_KEY_NAME, "12");

        sendBeacon(beacon, "94.186.169.18");

        verify(beaconMetricManager).processBeacon(beaconCaptor.capture());
        assertThat(beaconCaptor.getValue().get(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE)).isEqualTo("DE");
    }

    /**
     * The application should process a beacon, where the tag COUNTRY_CODE is not set
     *
     * @throws Exception
     */
    @Test
    public void expectCountryCodeToBeNotSetNotResolvableIP() throws Exception {
        Map<String, String> beacon = getBasicBeacon();
        beacon.put(BEACON_KEY_NAME, "12");

        sendBeacon(beacon, "127.0.0.1");

        verify(beaconMetricManager).processBeacon(beaconCaptor.capture());
        assertThat(beaconCaptor.getValue().get(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE)).isEmpty();
    }

    /**
     * The application should process a beacon, where the tag COUNTRY_CODE is not set
     *
     * @throws Exception
     */
    @Test
    public void expectCountryCodeToBeNotSetWrongFormattedIP() throws Exception {
        Map<String, String> beacon = getBasicBeacon();
        beacon.put(BEACON_KEY_NAME, "12");

        sendBeacon(beacon, "127.0.0.0.1");

        verify(beaconMetricManager).processBeacon(beaconCaptor.capture());
        assertThat(beaconCaptor.getValue().get(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE)).isEmpty();
    }

}
