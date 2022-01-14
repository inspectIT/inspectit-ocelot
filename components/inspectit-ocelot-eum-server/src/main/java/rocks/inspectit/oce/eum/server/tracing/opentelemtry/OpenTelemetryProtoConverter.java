package rocks.inspectit.oce.eum.server.tracing.opentelemtry;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.utils.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Component
@Slf4j
public class OpenTelemetryProtoConverter {

    @Autowired
    private EumServerConfiguration configuration;

    @VisibleForTesting
    Supplier<HttpServletRequest> requestSupplier = RequestUtils::getCurrentRequest;

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

            Map<String, String> customSpanAttributes = getCustomSpanAttributes();

            resourceSpans.getInstrumentationLibrarySpansList()
                    .stream()
                    .flatMap(librarySpans -> toSpanData(librarySpans, resource, customSpanAttributes))
                    .forEach(result::add);
        }

        return result;
    }

    /**
     * @return Converts an {@link InstrumentationLibrarySpans} instance to a stream of individual {@link SpanData} instances.
     */
    private Stream<SpanData> toSpanData(InstrumentationLibrarySpans librarySpans, Resource resource, Map<String, String> customSpanAttributes) {
        InstrumentationLibrary library = librarySpans.getInstrumentationLibrary();
        InstrumentationLibraryInfo libraryInfo = InstrumentationLibraryInfo.create(library.getName(), library.getVersion());

        return librarySpans.getSpansList()
                .stream()
                .map(protoSpan -> OcelotSpanUtils.createSpanData(protoSpan, resource, libraryInfo, customSpanAttributes))
                .filter(Objects::nonNull);
    }

    /**
     * @return Returns the current {@link HttpServletRequest} or <code>null</code> in case no request exists.
     */
    @VisibleForTesting
    Map<String, String> getCustomSpanAttributes() {
        HttpServletRequest request = requestSupplier.get();
        if (request == null) {
            return Collections.emptyMap();
        }

        String clientIp = request.getRemoteAddr();

        if (configuration.getExporters() != null && configuration.getExporters()
                .getTracing() != null && configuration.getExporters().getTracing().isMaskSpanIpAddresses()) {
            clientIp = anonymizeIpAddress(clientIp);
        }

        return ImmutableMap.of("client.ip", clientIp);
    }

    /**
     * Masks the given IP address and returns it. If the IP is a IPv4 address, the last 8 bits will be mask. In case
     * of an IPv6, the last 48 bits will be mask.
     *
     * @param ipAddress the unmasked IP address
     *
     * @return the masked IP address
     */
    private String anonymizeIpAddress(String ipAddress) {
        int index = ipAddress.lastIndexOf(".");
        if (index <= 0) {
            // ipv6
            String[] quartets = ipAddress.split(":");
            quartets[5] = "0";
            quartets[6] = "0";
            quartets[7] = "0";
            return String.join(":", quartets);
        } else {
            // ipv4
            return ipAddress.substring(0, index) + ".0";
        }
    }
}
