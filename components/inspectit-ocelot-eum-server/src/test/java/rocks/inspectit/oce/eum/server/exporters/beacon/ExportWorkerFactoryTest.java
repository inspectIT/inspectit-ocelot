package rocks.inspectit.oce.eum.server.exporters.beacon;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.exporters.beacon.ExportWorkerFactory.ExportWorker;
import rocks.inspectit.oce.eum.server.metrics.SelfMonitoringMetricManager;

import java.net.URI;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportWorkerFactoryTest {

    @InjectMocks
    private ExportWorkerFactory factory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EumServerConfiguration configuration;

    @Mock
    private BlockingQueue<Beacon> buffer;

    @Mock
    private SelfMonitoringMetricManager selfMonitoring;

    @Nested
    public class Initialize {

        @Test
        public void successfulWithoutAuthentication() {
            when(configuration.getExporters().getBeacons().getHttp().getEndpointUrl()).thenReturn("http://target:8080");

            factory.initialize();

            URI targetUrl = (URI) ReflectionTestUtils.getField(factory, "exportTargetUrl");
            assertThat(targetUrl).isEqualTo(URI.create("http://target:8080"));
            RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(factory, "restTemplate");
            assertThat(restTemplate.getInterceptors()).isEmpty();
        }

        @Test
        public void successfulWithAuthentication() {
            when(configuration.getExporters().getBeacons().getHttp().getEndpointUrl()).thenReturn("http://target:8080");
            when(configuration.getExporters().getBeacons().getHttp().getUsername()).thenReturn("user");
            when(configuration.getExporters().getBeacons().getHttp().getPassword()).thenReturn("passwd");

            factory.initialize();

            URI targetUrl = (URI) ReflectionTestUtils.getField(factory, "exportTargetUrl");
            assertThat(targetUrl).isEqualTo(URI.create("http://target:8080"));
            RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(factory, "restTemplate");
            assertThat(restTemplate.getInterceptors()).hasOnlyElementsOfType(BasicAuthenticationInterceptor.class);
            assertThat(restTemplate.getInterceptors()).extracting("username", "password")
                    .contains(tuple("user", "passwd"));
        }
    }

    @Nested
    public class GetWorker {

        @Test
        public void successful() {
            ExportWorker worker = factory.getWorker(buffer);

            BlockingQueue<Beacon> workerBuffer = (BlockingQueue<Beacon>) ReflectionTestUtils.getField(worker, "buffer");
            assertThat(worker).isNotNull();
            assertThat(workerBuffer).isSameAs(buffer);
        }
    }

    @Nested
    public class ExportWorker_run {

        @Mock
        private RestTemplate restTemplate;

        @Mock
        private ResponseEntity<Void> response;

        @Test
        public void successful() {
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            when(restTemplate.postForEntity(any(), any(), eq(Void.class))).thenReturn(response);
            ReflectionTestUtils.setField(factory, "restTemplate", restTemplate);

            ExportWorker worker = factory.getWorker(buffer);

            worker.run();

            verify(restTemplate).postForEntity(any(), eq(buffer), eq(Void.class));
            verifyNoMoreInteractions(restTemplate);
        }
    }
}