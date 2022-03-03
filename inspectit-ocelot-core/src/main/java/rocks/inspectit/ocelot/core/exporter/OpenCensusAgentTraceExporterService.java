package rocks.inspectit.ocelot.core.exporter;

import io.opencensus.common.Duration;
import io.opencensus.exporter.trace.ocagent.OcAgentTraceExporter;
import io.opencensus.exporter.trace.ocagent.OcAgentTraceExporterConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.model.exporters.trace.OpenCensusAgentTraceExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

@Component
@Slf4j
public class OpenCensusAgentTraceExporterService extends DynamicallyActivatableService {

    public OpenCensusAgentTraceExporterService() {
        super("exporters.tracing.openCensusAgent", "tracing.enabled");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        OpenCensusAgentTraceExporterSettings openCensusAgent = conf.getExporters().getTracing().getOpenCensusAgent();
        if (conf.getTracing().isEnabled() && !openCensusAgent.getEnabled().isDisabled()) {
            if (StringUtils.hasText(openCensusAgent.getAddress())) {
                return true;
            } else if (openCensusAgent.getEnabled().equals(ExporterEnabledState.ENABLED)) {
                log.warn("OpenCensus Tracing Exporter is enabled but 'address' is not set.");
            }
        }
        return false;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        try {
            OpenCensusAgentTraceExporterSettings settings = configuration.getExporters()
                    .getTracing()
                    .getOpenCensusAgent();
            log.info("Starting OpenCensus Agent Trace exporter");
            OcAgentTraceExporter.createAndRegister(OcAgentTraceExporterConfiguration.builder()
                    .setEndPoint(settings.getAddress())
                    .setServiceName(settings.getServiceName())
                    .setUseInsecure(settings.isUseInsecure())
                    .setRetryInterval(Duration.fromMillis(settings.getReconnectionPeriod().toMillis()))
                    .build());
            return true;
        } catch (Throwable t) {
            log.error("Error creating OpenCensus Agent Trace exporter", t);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        log.info("Stopping OpenCensus Agent Trace exporter");
        try {
            OcAgentTraceExporter.unregister();
        } catch (Throwable t) {
            log.error("Error disabling OpenCensus Agent Trace exporter", t);
        }
        return true;
    }
}
