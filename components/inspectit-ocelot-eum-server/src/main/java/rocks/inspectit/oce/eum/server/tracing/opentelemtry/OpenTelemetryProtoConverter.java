package rocks.inspectit.oce.eum.server.tracing.opentelemtry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.InstrumentationLibrary;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.OcelotSpanUtils;
import io.opentelemetry.sdk.trace.data.SpanData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@Slf4j
public class OpenTelemetryProtoConverter {

    /**
     * Converts open-telemetry proto data to the open-telemetry SDK span data.
     *
     * @param data data to convert
     *
     * @return Non-null collection of {@link SpanData}
     */
    public Collection<SpanData> convert(ExportTraceServiceRequest data) {
        List<SpanData> result = new ArrayList<>();
        data.getResourceSpansList().forEach(resourceSpans -> {
            // convert resource
            Attributes attributes = OcelotSpanUtils.toAttributes(resourceSpans.getResource().getAttributesList());
            final Resource resource = Resource.create(attributes);

            // then iterate all instrumentation libs
            resourceSpans.getInstrumentationLibrarySpansList().forEach(instrumentationLibrarySpans -> {
                // convert instrumentation lib
                InstrumentationLibrary instrumentationLibrary = instrumentationLibrarySpans.getInstrumentationLibrary();
                InstrumentationLibraryInfo instrumentationLibraryInfo = InstrumentationLibraryInfo.create(instrumentationLibrary
                        .getName(), instrumentationLibrary.getVersion());

                // then iterate all spans
                instrumentationLibrarySpans.getSpansList().forEach(span -> {
                    // create builder, add resource and inst. lib info, then add to results
                    try {
                        SpanData spanData = OcelotSpanUtils.createSpanData(span, resource, instrumentationLibraryInfo);
                        result.add(spanData);
                    } catch (Exception e) {
                        log.warn("Error converting OT proto span {} to span data.", span, e);
                    }
                });
            });
        });

        return result;
    }
}
