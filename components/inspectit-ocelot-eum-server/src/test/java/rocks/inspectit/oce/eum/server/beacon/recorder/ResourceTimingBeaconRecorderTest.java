package rocks.inspectit.oce.eum.server.beacon.recorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opencensus.tags.Tags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.metrics.MeasuresAndViewsManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceTimingBeaconRecorderTest {

    ResourceTimingBeaconRecorder recorder;

    @Mock
    MeasuresAndViewsManager measuresAndViewsManager;

    @Mock
    EumServerConfiguration configuration;

    ObjectMapper objectMapper;

    @BeforeEach
    public void init() {
        lenient().when(measuresAndViewsManager.getTagContext(any())).thenReturn(Tags.getTagger().emptyBuilder());

        objectMapper = new ObjectMapper();
        recorder = new ResourceTimingBeaconRecorder(objectMapper, measuresAndViewsManager, configuration);
    }

    @Nested
    class Record {

        @Captor
        ArgumentCaptor<Map<String, String>> tagsCaptor;

        @Test
        public void noResourceTimingInfo() {
            Beacon beacon = Beacon.of(Collections.emptyMap());

            recorder.record(beacon);

            verifyNoMoreInteractions(measuresAndViewsManager);
        }

        @Test
        public void notAJson() {
            Beacon beacon = Beacon.of(Collections.singletonMap("restiming", "This is not a valid json"));

            recorder.record(beacon);

            verifyNoMoreInteractions(measuresAndViewsManager);
        }

        @Test
        public void badCompression() {
            // intentionally change the initiator
            String json = "" + "{\n" + "  \"https://www.google.de/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png\": \"!!\"\n" + "}";
            Map<String, String> fields = new HashMap<>();
            fields.put("u", "http://myhost/somepage.html");
            fields.put("restiming", json);
            Beacon beacon = Beacon.of(fields);

            recorder.record(beacon);

            verifyNoMoreInteractions(measuresAndViewsManager);
        }

        @Test
        public void nestedResources() {
            String json = "" + "{\n" + "  \"http\": {\n" + "    \"://myhost/\": {\n" + "      \"|\": \"6,2\",\n" + "      \"boomerang/plugins/\": {\n" + "        \"r\": {\n" + "          \"t.js\": \"32o,2u,2q,25*1d67,9s,wdu*20\",\n" + "          \"estiming.js\": \"02p,2w,2p,24*1efk,9z,y2i*20\"\n" + "        }\n" + "      }\n" + "    },\n" + "    \"s://www.google.de/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png\": \"*02k,7k,1y,a2|180,3l\"\n" + "  }\n" + "}";
            Map<String, String> fields = new HashMap<>();
            fields.put("u", "http://myhost/somepage.html");
            fields.put("restiming", json);
            Beacon beacon = Beacon.of(fields);

            recorder.record(beacon);

            verify(measuresAndViewsManager, atLeastOnce()).getTagContext(tagsCaptor.capture());
            verify(measuresAndViewsManager).recordMeasure(eq("resource_time"), any(), eq(2));
            verify(measuresAndViewsManager).recordMeasure(eq("resource_time"), any(), eq(102));
            verify(measuresAndViewsManager).recordMeasure(eq("resource_time"), any(), eq(104));
            verify(measuresAndViewsManager).recordMeasure(eq("resource_time"), any(), eq(129));
            verifyNoMoreInteractions(measuresAndViewsManager);

            assertThat(tagsCaptor.getAllValues()).hasSize(4)
                    // |
                    .anySatisfy(map -> assertThat(map).hasSize(3)
                            .containsEntry("initiatorType", "HTML")
                            .containsEntry("crossOrigin", "false")
                            .containsEntry("cached", "true"))
                    // t.js
                    .anySatisfy(map -> assertThat(map).hasSize(3)
                            .containsEntry("initiatorType", "SCRIPT")
                            .containsEntry("crossOrigin", "false")
                            .containsEntry("cached", "false"))
                    // estiming.js
                    .anySatisfy(map -> assertThat(map).hasSize(3)
                            .containsEntry("initiatorType", "OTHER")
                            .containsEntry("crossOrigin", "false")
                            .containsEntry("cached", "false"))
                    // googlelogo_color_272x92dp.png
                    .anySatisfy(map -> assertThat(map).hasSize(2)
                            .containsEntry("initiatorType", "IMG")
                            .containsEntry("crossOrigin", "true"));

        }

        @Test
        public void pipedData() {
            // intentionally change the initiator
            String json = "" + "{\n" + "  \"https://www.google.de/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png\": \"*02k,7k,1y,a2|180,3l|280,4l\"\n" + "}";
            Map<String, String> fields = new HashMap<>();
            fields.put("u", "http://myhost/somepage.html");
            fields.put("restiming", json);
            Beacon beacon = Beacon.of(fields);

            recorder.record(beacon);

            verify(measuresAndViewsManager, atLeastOnce()).getTagContext(tagsCaptor.capture());
            verify(measuresAndViewsManager).recordMeasure(eq("resource_time"), any(), eq(129));
            verify(measuresAndViewsManager).recordMeasure(eq("resource_time"), any(), eq(165));
            verifyNoMoreInteractions(measuresAndViewsManager);

            assertThat(tagsCaptor.getAllValues()).hasSize(2)
                    // first
                    .anySatisfy(map -> assertThat(map).hasSize(2)
                            .containsEntry("initiatorType", "IMG")
                            .containsEntry("crossOrigin", "true"))
                    // second
                    .anySatisfy(map -> assertThat(map).hasSize(2)
                            .containsEntry("initiatorType", "LINK")
                            .containsEntry("crossOrigin", "true"));
        }

        @Test
        public void onlyAdditionalData() {
            // intentionally change the initiator
            String json = "" + "{\n" + "  \"https://www.google.de/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png\": \"*02k,7k,1y,a2\"\n" + "}";
            Map<String, String> fields = new HashMap<>();
            fields.put("u", "http://myhost/somepage.html");
            fields.put("restiming", json);
            Beacon beacon = Beacon.of(fields);

            recorder.record(beacon);

            verifyNoMoreInteractions(measuresAndViewsManager);
        }

        @Test
        public void wrongInitiator() {
            // intentionally change the initiator
            String json = "" + "{\n" + "  \"https://www.google.de/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png\": \"w80,3l\"\n" + "}";
            Map<String, String> fields = new HashMap<>();
            fields.put("u", "http://myhost/somepage.html");
            fields.put("restiming", json);
            Beacon beacon = Beacon.of(fields);

            recorder.record(beacon);

            verify(measuresAndViewsManager, atLeastOnce()).getTagContext(tagsCaptor.capture());
            verify(measuresAndViewsManager).recordMeasure(eq("resource_time"), any(), eq(129));
            verifyNoMoreInteractions(measuresAndViewsManager);

            assertThat(tagsCaptor.getAllValues()).hasSize(1)
                    // first
                    .anySatisfy(map -> assertThat(map).hasSize(2)
                            .containsEntry("initiatorType", "OTHER")
                            .containsEntry("crossOrigin", "true"));
        }

        @Test
        public void noResponseEnd() {
            // intentionally change the initiator
            String json = "" + "{\n" + "  \"https://www.google.de/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png\": \"180\"\n" + "}";
            Map<String, String> fields = new HashMap<>();
            fields.put("u", "http://myhost/somepage.html");
            fields.put("restiming", json);
            Beacon beacon = Beacon.of(fields);

            recorder.record(beacon);

            verify(measuresAndViewsManager, atLeastOnce()).getTagContext(tagsCaptor.capture());
            verify(measuresAndViewsManager).recordMeasure(eq("resource_time"), any(), eq(0));
            verifyNoMoreInteractions(measuresAndViewsManager);

            assertThat(tagsCaptor.getAllValues()).hasSize(1)
                    // first
                    .anySatisfy(map -> assertThat(map).hasSize(2)
                            .containsEntry("initiatorType", "IMG")
                            .containsEntry("crossOrigin", "true"));
        }

    }

}
