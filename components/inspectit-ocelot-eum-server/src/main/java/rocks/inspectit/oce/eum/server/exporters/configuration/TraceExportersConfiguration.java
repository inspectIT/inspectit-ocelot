package rocks.inspectit.oce.eum.server.exporters.configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.exporters.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.ocelot.config.model.exporters.trace.JaegerExporterSettings;

@Configuration
public class TraceExportersConfiguration {

    @Autowired
    private EumServerConfiguration configuration;

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(value = "inspectit-eum-server.exporters.tracing.jaeger.enabled", havingValue = "true")
    public SpanExporter jaegerSpanExporter() {
        JaegerExporterSettings jaegerExporterSettings = configuration.getExporters().getTracing().getJaeger();

        UriComponents uri = UriComponentsBuilder.fromHttpUrl(jaegerExporterSettings.getUrl()).build();
        String host = uri.getHost();
        int port = uri.getPort();

        // TODO should config define the URI instead and we use forTarget() method here
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        return JaegerGrpcSpanExporter.newBuilder()
                .setChannel(channel)
                .setServiceName(jaegerExporterSettings.getServiceName())
                .build();
    }

}
