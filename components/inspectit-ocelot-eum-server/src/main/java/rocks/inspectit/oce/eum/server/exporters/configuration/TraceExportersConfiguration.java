package rocks.inspectit.oce.eum.server.exporters.configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import rocks.inspectit.oce.eum.server.configuration.model.EumExportersSettings;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.ocelot.config.model.exporters.trace.JaegerExporterSettings;
import rocks.inspectit.ocelot.config.model.exporters.trace.TraceExportersSettings;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Configuration
@Slf4j
public class TraceExportersConfiguration {

    @Autowired
    private EumServerConfiguration configuration;

    @PostConstruct
    public void logWrongJaegerConfig() {
        Optional.ofNullable(configuration.getExporters())
                .map(EumExportersSettings::getTracing)
                .map(TraceExportersSettings::getJaeger)
                .filter((jaeger) -> !jaeger.getEnabled().isDisabled())
                .ifPresent(settings -> {
                    if (StringUtils.hasText(settings.getUrl()) && !StringUtils.hasText(settings.getGrpc())) {
                        log.warn("In order to use Jaeger span exporter, please specify the grpc API endpoint property instead of the url.");
                    }
                });
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty({"inspectit-eum-server.exporters.tracing.jaeger.enabled", "inspectit-eum-server.exporters.tracing.jaeger.grpc"})
    @ConditionalOnExpression("(NOT new String('${inspectit-eum-server.exporters.tracing.jaeger.enabled}').toUpperCase().equals(T(rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState).DISABLED.toString())) AND (new String('${inspectit-eum-server.exporters.tracing.jaeger.grpc}').length() > 0)")
    public SpanExporter jaegerSpanExporter() {
        JaegerExporterSettings jaegerExporterSettings = configuration.getExporters().getTracing().getJaeger();

        ManagedChannel channel = ManagedChannelBuilder.forTarget(jaegerExporterSettings.getGrpc())
                .usePlaintext()
                .build();

        log.info("Starting Jaeger Exporter on grpc '{}'", jaegerExporterSettings.getGrpc());
        System.setProperty("otel.resource.attributes", "service.name=" + jaegerExporterSettings.getServiceName());

        return JaegerGrpcSpanExporter.builder().setChannel(channel).build();
    }

}
