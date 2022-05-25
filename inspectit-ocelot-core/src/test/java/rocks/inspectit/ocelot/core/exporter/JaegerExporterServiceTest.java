package rocks.inspectit.ocelot.core.exporter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.exporters.trace.JaegerExporterSettings;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class JaegerExporterServiceTest {

    @InjectMocks
    private JaegerExporterService service;

    @Nested
    class GetProtocol {

        @Test
        public void hasProtocol() {
            JaegerExporterSettings settings = new JaegerExporterSettings();
            settings.setProtocol(TransportProtocol.HTTP_PROTOBUF);

            TransportProtocol protocol = service.getProtocol(settings);

            assertThat(protocol).isEqualTo(TransportProtocol.HTTP_PROTOBUF);
        }

        @Test
        public void fallbackToGrpc() {
            JaegerExporterSettings settings = new JaegerExporterSettings();
            settings.setGrpc("url");

            TransportProtocol protocol = service.getProtocol(settings);

            assertThat(protocol).isEqualTo(TransportProtocol.GRPC);
        }

        @Test
        public void fallbackToThrift() {
            JaegerExporterSettings settings = new JaegerExporterSettings();
            settings.setUrl("url");

            TransportProtocol protocol = service.getProtocol(settings);

            assertThat(protocol).isEqualTo(TransportProtocol.HTTP_THRIFT);
        }

        @Test
        public void hasProtocolAndUrl() {
            JaegerExporterSettings settings = new JaegerExporterSettings();
            settings.setProtocol(TransportProtocol.HTTP_PROTOBUF);
            settings.setUrl("url");

            TransportProtocol protocol = service.getProtocol(settings);

            assertThat(protocol).isEqualTo(TransportProtocol.HTTP_PROTOBUF);
        }

        @Test
        public void hasProtocolAndGrpc() {
            JaegerExporterSettings settings = new JaegerExporterSettings();
            settings.setProtocol(TransportProtocol.HTTP_PROTOBUF);
            settings.setGrpc("url");

            TransportProtocol protocol = service.getProtocol(settings);

            assertThat(protocol).isEqualTo(TransportProtocol.HTTP_PROTOBUF);
        }
    }

}