package rocks.inspectit.ocelot.core.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@NoArgsConstructor
public class TraceExportersSettings {

    @Valid
    private JaegerExporterSettings jaeger;

    @Valid
    private ZipKinExporterSettings zipkin;
}
