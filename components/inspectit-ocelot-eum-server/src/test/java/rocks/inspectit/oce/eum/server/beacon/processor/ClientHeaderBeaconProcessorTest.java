package rocks.inspectit.oce.eum.server.beacon.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@ExtendWith(MockitoExtension.class)
class ClientHeaderBeaconProcessorTest {

    @InjectMocks
    private ClientHeaderBeaconProcessor processor;

    private final String HEADER_PREFIX = "client.header.";

    @Nested
    public class Process {

        private final MockHttpServletRequest request = new MockHttpServletRequest();

        @BeforeEach
        void getCurrentRequest() {
            processor.requestSupplier = () -> {
                return request;
            };
        }

        @Test
        public void singleClientHeader() {
            request.addHeader("Accept-Encoding", "gzip, deflate");

            Beacon beacon = Beacon.of(Collections.singletonMap("key", "value"));
            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).hasSize(2)
                    .contains(entry("key", "value"), entry(HEADER_PREFIX + "Accept-Encoding", "gzip, deflate"));
        }

        @Test
        public void differentClientHeader() {
            request.addHeader("Accept-Encoding", "gzip,deflate");
            request.addHeader("Connection", "keep-alive");

            Beacon beacon = Beacon.of(Collections.singletonMap("key", "value"));
            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).hasSize(3)
                    .contains(entry("key", "value"), entry(HEADER_PREFIX + "Accept-Encoding", "gzip,deflate"), entry(HEADER_PREFIX + "Connection", "keep-alive"));
        }

        @Test
        public void multipleClientHeader() {
            request.addHeader("Accept-Encoding", "gzip");
            request.addHeader("Accept-Encoding", "deflate");

            Beacon beacon = Beacon.of(Collections.singletonMap("key", "value"));
            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).hasSize(2)
                    .contains(entry("key", "value"), entry(HEADER_PREFIX + "Accept-Encoding", "gzip,deflate"));
        }
        
    }
}