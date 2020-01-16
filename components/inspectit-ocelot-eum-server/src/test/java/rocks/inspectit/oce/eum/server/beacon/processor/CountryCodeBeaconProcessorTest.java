package rocks.inspectit.oce.eum.server.beacon.processor;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.configuration.model.EumTagsSettings;
import rocks.inspectit.oce.eum.server.utils.GeolocationResolver;
import rocks.inspectit.oce.eum.server.utils.IPUtils;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test of {@link CountryCodeBeaconProcessorTest}
 */
@ExtendWith(MockitoExtension.class)
class CountryCodeBeaconProcessorTest {

    @InjectMocks
    CountryCodeBeaconProcessor preProcessor;

    @Mock
    private GeolocationResolver geolocationResolver;

    @Spy
    private IPUtils ipUtils = new IPUtils();

    @Mock
    private EumServerConfiguration configuration;

    public static final String DEFAULT_IP_ADDRESS = "10.10.10.10";

    private EumTagsSettings tagsSettings = new EumTagsSettings();

    private Beacon beacon;

    @BeforeEach
    private void initializeBeacon() {
        beacon = Beacon.of(ImmutableMap.of("dummyMetric", "dummyValue"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(DEFAULT_IP_ADDRESS);
        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);
    }

    @Nested
    public class process {

        @Test
        public void addEmptyCountryCode() {
            when(geolocationResolver.getCountryCode(DEFAULT_IP_ADDRESS)).thenReturn("");
            when(configuration.getTags()).thenReturn(tagsSettings);

            Beacon b = preProcessor.process(beacon);
            assertThat(b.get(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE)).isEqualTo("");
        }

        @Test
        public void addGeoIPDBCountryCode() {
            when(geolocationResolver.getCountryCode(DEFAULT_IP_ADDRESS)).thenReturn("DE");
            when(configuration.getTags()).thenReturn(tagsSettings);

            Beacon b = preProcessor.process(beacon);
            assertThat(b.contains(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE)).isTrue();
            assertThat(b.get(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE)).isEqualTo("DE");
        }

        @Test
        public void addCustomLabelIPMatches() {
            tagsSettings.getCustomIPMapping().put("CUSTOM_TAG_1", Arrays.asList(new String[]{"10.10.10.10"}));
            when(configuration.getTags()).thenReturn(tagsSettings);

            Beacon b = preProcessor.process(beacon);
            assertThat(b.get(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE)).isEqualTo("CUSTOM_TAG_1");
        }

        @Test
        public void addGeoIPDBCountryCodeSecondPrio1() {
            tagsSettings.getCustomIPMapping().put("CUSTOM_TAG_1", Arrays.asList(new String[]{"10.10.10.11"}));

            when(geolocationResolver.getCountryCode(DEFAULT_IP_ADDRESS)).thenReturn("DE");
            when(configuration.getTags()).thenReturn(tagsSettings);

            Beacon b = preProcessor.process(beacon);
            assertThat(b.get(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE)).isEqualTo("DE");
        }

        @Test
        public void addGeoIPDBCountryCodeSecondPrio2() {
            tagsSettings.getCustomIPMapping().put("CUSTOM_TAG_1", Arrays.asList(new String[]{"10.11.0.0/16"}));

            when(geolocationResolver.getCountryCode(DEFAULT_IP_ADDRESS)).thenReturn("DE");
            when(configuration.getTags()).thenReturn(tagsSettings);

            Beacon b = preProcessor.process(beacon);
            assertThat(b.get(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE)).isEqualTo("DE");
        }

        @Test
        public void addCustomLabelCIDRMatches1() {
            tagsSettings.getCustomIPMapping().put("CUSTOM_TAG_1", Arrays.asList(new String[]{"10.10.0.0/16"}));
            when(configuration.getTags()).thenReturn(tagsSettings);

            Beacon b = preProcessor.process(beacon);
            assertThat(b.get(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE)).isEqualTo("CUSTOM_TAG_1");
        }

        @Test
        public void addCustomLabelCIDRMatches2() {
            tagsSettings.getCustomIPMapping().put("CUSTOM_TAG_1", Arrays.asList(new String[]{"10.11.0.0/16"}));
            tagsSettings.getCustomIPMapping().put("CUSTOM_TAG_2", Arrays.asList(new String[]{"10.10.0.0/16"}));
            when(configuration.getTags()).thenReturn(tagsSettings);

            Beacon b = preProcessor.process(beacon);
            assertThat(b.get(CountryCodeBeaconProcessor.TAG_COUNTRY_CODE)).isEqualTo("CUSTOM_TAG_2");
        }
    }
}
