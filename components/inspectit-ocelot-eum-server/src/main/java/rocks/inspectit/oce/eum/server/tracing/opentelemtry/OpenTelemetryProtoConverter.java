package rocks.inspectit.oce.eum.server.tracing.opentelemtry;

import com.google.protobuf.ByteString;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.InstrumentationLibrary;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.OcelotRERSProxy;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
            //TODO
            // final Map<String, AttributeValue> resourceAttributes = toOtAttributes(resource1.getAttributesList());
            Attributes resourceAttributes = Attributes.empty();
            final Resource resource = Resource.create(resourceAttributes);

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
                        SpanData spanData = buildSpan(span, resource, instrumentationLibraryInfo);
                        //                        SpanData.Builder spanBuilder = getSpanBuilder(span);
                        //                        spanBuilder.setResource(resource);
                        //                        spanBuilder.setInstrumentationLibraryInfo(instrumentationLibraryInfo);
                        //                        result.add(spanBuilder.build());
                        result.add(spanData);
                    } catch (Exception e) {
                        log.warn("Error converting OT proto span {} to span data.", span, e);
                    }
                });
            });
        });

        return result;
    }

    private SpanData buildSpan(Span span, Resource resource, InstrumentationLibraryInfo instrumentationLibraryInfo) {

        String traceId = TraceId.fromBytes(span.getTraceId().toByteArray());
        String spanId = SpanId.fromBytes(span.getSpanId().toByteArray());

        //        String traceState = span.getTraceState();
        TraceState traceState = TraceState.getDefault();
        SpanContext spanContext = SpanContext.create(traceId, spanId, TraceFlags.getDefault(), traceState);

        String parentSpanId = SpanId.fromBytes(span.getSpanId().toByteArray());
        SpanContext parentSpanContext = SpanContext.create(traceId, parentSpanId, TraceFlags.getDefault(), traceState);

        String name = span.getName();
        SpanKind spanKind = toOtSpanKind(span.getKind());

        List<LinkData> links = Collections.emptyList();

        Attributes attributes = Attributes.empty();

        ReadableSpan readableSpan = OcelotRERSProxy.create(spanContext, name, instrumentationLibraryInfo, spanKind, parentSpanContext, null, resource, attributes, links, -1, -1L);

        return readableSpan.toSpanData();
    }

    //    static SpanData.Builder getSpanBuilder(SpanOrBuilder span) {
    //        SpanData.Builder builder = SpanData.newBuilder();
    //
    //        // TODO TraceState currently not supported
    //        toOtTraceId(span.getTraceId()).ifPresent(builder::setTraceId);
    //        toOtSpanId(span.getSpanId()).ifPresent(builder::setSpanId);
    //        toOtSpanId(span.getParentSpanId()).ifPresent(builder::setParentSpanId);
    //
    //        builder.setName(span.getName());
    //        builder.setKind(toOtSpanKind(span.getKind()));
    //        builder.setStartEpochNanos(span.getStartTimeUnixNano());
    //        builder.setEndEpochNanos(span.getEndTimeUnixNano());
    //        builder.setHasEnded(span.getEndTimeUnixNano() != 0);
    //
    //        // attributes
    //        Map<String, AttributeValue> attributesMap = toOtAttributes(span.getAttributesList());
    //        builder.setAttributes(attributesMap);
    //        builder.setTotalAttributeCount(span.getDroppedAttributesCount() + attributesMap.size());
    //
    //        // events
    //        List<SpanData.TimedEvent> events = toOtEvents(span.getEventsList());
    //        builder.setTimedEvents(events);
    //        builder.setTotalRecordedEvents(span.getDroppedEventsCount() + events.size());
    //
    //        // links
    //        List<SpanData.Link> links = toOtLink(span.getLinksList());
    //        builder.setLinks(links);
    //        builder.setTotalRecordedLinks(span.getDroppedLinksCount() + links.size());
    //
    //
    //        // status only if we can map
    //        if (span.hasStatus()) {
    //            toOtStatus(span.getStatus()).ifPresent(builder::setStatus);
    //        }
    //
    //        return builder;
    //    }
    //
    //    static Optional<Status> toOtStatus(io.opentelemetry.proto.trace.v1.Status status) {
    //        return Arrays.stream(Status.CanonicalCode.values())
    //                .filter(canonicalCode -> canonicalCode.value() == status.getCodeValue())
    //                .findFirst()
    //                .map(canonicalCode -> canonicalCode.toStatus().withDescription(status.getMessage()));
    //    }
    //
//    static Optional<String> toOtTraceId(ByteString traceId) {
//        // at least 16 bytes required here
//        return Optional.ofNullable(traceId.toByteArray())
//                .filter(array -> array.length >= 16)
//                .map(array -> TraceId.fromBytes(array));
//    }
//
//    static Optional<String> toOtSpanId(ByteString spanId) {
//        // at least 8 bytes required here
//        return Optional.ofNullable(spanId.toByteArray())
//                .filter(array -> array.length >= 8)
//                .map(array -> SpanId.fromBytes(array));
//    }

    //
//        static List<SpanData.Link> toOtLink(List<Span.Link> linksList) {
//            if (CollectionUtils.isEmpty(linksList)) {
//                return Collections.emptyList();
//            }
//
//            return linksList.stream()
//                    .flatMap(link -> {
//                        // TODO TraceState currently not supported
//                        // TODO increase dropped links count if we ignore a link?
//                        Optional<TraceId> traceId = toOtTraceId(link.getTraceId());
//                        Optional<SpanId> spanId = toOtSpanId(link.getSpanId());
//                        if (traceId.isPresent() && spanId.isPresent()) {
//                            SpanContext context = SpanContext.create(traceId.get(), spanId.get(), TraceFlags.getDefault(), TraceState.getDefault());
//                            Map<String, AttributeValue> attributesMap = toOtAttributes(link.getAttributesList());
//                            int totalAttributes = attributesMap.size() + link.getDroppedAttributesCount();
//                            return Stream.of(SpanData.Link.create(context, attributesMap, totalAttributes));
//                        } else {
//                            return Stream.empty();
//                        }
//                    })
//                    .collect(Collectors.toList());
//
//        }
    //
    //    static List<SpanData.TimedEvent> toOtEvents(List<Span.Event> eventsList) {
    //        if (CollectionUtils.isEmpty(eventsList)) {
    //            return Collections.emptyList();
    //        }
    //
    //        return eventsList.stream()
    //                .map(event -> {
    //                    Map<String, AttributeValue> attributesMap = toOtAttributes(event.getAttributesList());
    //                    int totalAttributes = attributesMap.size() + event.getDroppedAttributesCount();
    //                    return SpanData.TimedEvent.create(event.getTimeUnixNano(), event.getName(), attributesMap, totalAttributes);
    //                })
    //                .collect(Collectors.toList());
    //    }
    //
    //    static Map<String, AttributeValue> toOtAttributes(List<AttributeKeyValue> attributesList) {
    //        if (CollectionUtils.isEmpty(attributesList)) {
    //            return Collections.emptyMap();
    //        }
    //
    //        Map<String, AttributeValue> result = new HashMap<>(attributesList.size());
    //        attributesList.forEach(a -> toOtAttributeValue(a).ifPresent(value -> result.put(a.getKey(), value)));
    //        return result;
    //    }
    //
    //    static Optional<AttributeValue> toOtAttributeValue(AttributeKeyValue attribute) {
    //        switch (attribute.getType()) {
    //            case INT:
    //                return Optional.of(AttributeValue.longAttributeValue(attribute.getIntValue()));
    //            case BOOL:
    //                return Optional.of(AttributeValue.booleanAttributeValue(attribute.getBoolValue()));
    //            case DOUBLE:
    //                return Optional.of(AttributeValue.doubleAttributeValue(attribute.getDoubleValue()));
    //            case STRING:
    //                return Optional.of(AttributeValue.stringAttributeValue(attribute.getStringValue()));
    //        }
    //        return Optional.empty();
    //    }
    //
    private SpanKind toOtSpanKind(Span.SpanKind spanKind) {
        switch (spanKind) {
            case SPAN_KIND_INTERNAL:
                return SpanKind.INTERNAL;
            case SPAN_KIND_SERVER:
                return SpanKind.SERVER;
            case SPAN_KIND_CLIENT:
                return SpanKind.CLIENT;
            case SPAN_KIND_PRODUCER:
                return SpanKind.PRODUCER;
            case SPAN_KIND_CONSUMER:
                return SpanKind.CONSUMER;
        }

        // default value if we can not map
        return SpanKind.INTERNAL;
    }

}
