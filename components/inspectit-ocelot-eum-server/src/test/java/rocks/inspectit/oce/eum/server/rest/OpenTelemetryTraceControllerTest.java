package rocks.inspectit.oce.eum.server.rest;

import com.google.common.io.CharStreams;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
class OpenTelemetryTraceControllerTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Value("classpath:ot-export-trace-v0.7.0.json")
    private Resource resource;

    @MockBean
    SpanExporter spanExporter;


    @Nested
    class Spans {

        @Captor
        ArgumentCaptor<Collection<SpanData>> spanCaptor;


        @Test
        public void empty() {
            ResponseEntity<Void> result = restTemplate.postForEntity("/spans", null, Void.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        public void badData() {
            ResponseEntity<Void> result = restTemplate.postForEntity("/spans", "{\"bad\": \"data'\"}", Void.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        public void happyPath() throws Exception {
            try (final Reader reader = new InputStreamReader(resource.getInputStream())) {
                String json = CharStreams.toString(reader);

                ResponseEntity<Void> result = restTemplate.postForEntity("/spans", json, Void.class);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

                verify(spanExporter).export(spanCaptor.capture());
                assertThat(spanCaptor.getValue()).hasSize(2)
                        .allSatisfy(data -> {
                            assertThat(data.getTraceId()).isNotNull();
                            assertThat(data.getSpanId()).isNotNull();
                            assertThat(data.getKind()).isNotNull();
                            assertThat(data.getName()).isNotNull();
                            assertThat(data.getStartEpochNanos()).isGreaterThan(0);
                            assertThat(data.getEndEpochNanos()).isGreaterThan(0);
                            assertThat(data.getHasEnded()).isTrue();
                            assertThat(data.getAttributes()).isNotEmpty();
                            assertThat(data.getTimedEvents()).isNotEmpty();
                        });
            }
        }

    }

}