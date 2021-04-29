package rocks.inspectit.oce.eum.server.tracing.opentelemtry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.InstrumentationLibrary;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.OcelotSpanUtils;
import io.opentelemetry.sdk.trace.data.SpanData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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

        for (ResourceSpans resourceSpans : data.getResourceSpansList()) {
            // create general span resources, e.g. sdk-version, service-name, ...
            Attributes attributes = OcelotSpanUtils.toAttributes(resourceSpans.getResource().getAttributesList());
            final Resource resource = Resource.create(attributes);

            resourceSpans.getInstrumentationLibrarySpansList()
                    .stream()
                    .flatMap(librarySpans -> toSpanData(librarySpans, resource))
                    .forEach(result::add);
        }

        return result;
    }

    /**
     * @return Converts an {@link InstrumentationLibrarySpans} instance to a stream of individual {@link SpanData} instances.
     */
    private Stream<SpanData> toSpanData(InstrumentationLibrarySpans librarySpans, Resource resource) {
        InstrumentationLibrary library = librarySpans.getInstrumentationLibrary();
        InstrumentationLibraryInfo libraryInfo = InstrumentationLibraryInfo.create(library.getName(), library.getVersion());

        return librarySpans.getSpansList()
                .stream()
                .map(protoSpan -> OcelotSpanUtils.createSpanData(protoSpan, resource, libraryInfo))
                .filter(Objects::nonNull);
    }
}
