package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@NoArgsConstructor
public class TraceExportersSettings {

    @Valid
    private JaegerExporterSettings jaeger;

    @Valid
    private ZipkinExporterSettings zipkin;

    @Valid
    private LoggingTraceExporterSettings logging;

    @Valid
    private OtlpGrpcTraceExporterSettings otlpGrpc;
}
