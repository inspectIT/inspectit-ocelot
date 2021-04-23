package rocks.inspectit.oce.eum.server.tracing.opentelemtry;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationLibrary;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.OcelotRERSProxy;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

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
            Attributes attributes = toOtAttributes_rename(resourceSpans.getResource().getAttributesList());
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
                        SpanData spanData = buildSpan(span, resource, instrumentationLibraryInfo);
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

        String traceId = toIdString(span.getTraceId());
        String spanId = toIdString(span.getSpanId());

        //TODO - TraceState currently not supported
        SpanContext spanContext = SpanContext.create(traceId, spanId, TraceFlags.getDefault(), TraceState.getDefault());

        SpanContext parentSpanContext;
        if (span.getParentSpanId().isEmpty()) {
            parentSpanContext = SpanContext.getInvalid();
        } else {
            String parentSpanId = toIdString(span.getParentSpanId());
            parentSpanContext = SpanContext.create(traceId, parentSpanId, TraceFlags.getDefault(), TraceState.getDefault());
        }

        String name = span.getName();
        SpanKind spanKind = toOtSpanKind(span.getKind());

        List<LinkData> links = toLinkData(span.getLinksList());
        int totalRecordedLinks = span.getLinksCount() + span.getDroppedLinksCount();

        long startTime = span.getStartTimeUnixNano();
        long endTime = span.getEndTimeUnixNano();
        Status status = span.getStatus();

        Attributes spanAttributes = toOtAttributes_rename(span.getAttributesList());

        List<Span.Event> eventsList = span.getEventsList();
        SpanLimits spanLimits = SpanLimits.getDefault();

        ReadableSpan readableSpan = OcelotRERSProxy.create(spanContext, name, instrumentationLibraryInfo, spanKind, parentSpanContext, spanLimits, resource, spanAttributes, links, totalRecordedLinks, startTime, endTime, eventsList, status);

        return readableSpan.toSpanData();
    }

    private String toIdString(ByteString bytes) {
        // the id has to be base64 encoded, because it is assumed that the received data is base64 encoded, which is not the case.
        // see: com.google.protobuf.util.JsonFormat.ParserImpl.parseBytes
        return BaseEncoding.base64().encode(bytes.toByteArray());
    }

    private List<LinkData> toLinkData(List<Span.Link> linksList) {
        if (CollectionUtils.isEmpty(linksList)) {
            return Collections.emptyList();
        }

        return linksList.stream().map(link -> {
            //TODO - TraceState currently not supported
            String spanId = toIdString(link.getSpanId());
            String traceId = toIdString(link.getTraceId());

            SpanContext context = SpanContext.create(traceId, spanId, TraceFlags.getDefault(), TraceState.getDefault());
            Attributes attributes = toOtAttributes_rename(link.getAttributesList());
            int totalAttributes = attributes.size() + link.getDroppedAttributesCount();

            return LinkData.create(context, attributes, totalAttributes);
        }).collect(Collectors.toList());

    }

    private Attributes toOtAttributes_rename(List<KeyValue> attributesList) {
        if (CollectionUtils.isEmpty(attributesList)) {
            return Attributes.empty();
        }

        AttributesBuilder builder = Attributes.builder();

        attributesList.forEach(attribute -> {
            AttributeKey attributeKey = toAttributeKey_rename(attribute);
            Object value = toOtAttributeValue_rename(attribute.getValue());
            if (value != null) {
                builder.put(attributeKey, value);
            }
        });

        return builder.build();
    }

    private AttributeKey toAttributeKey_rename(KeyValue attribute) {
        String key = attribute.getKey();
        AnyValue.ValueCase valueCase = attribute.getValue().getValueCase();
        switch (valueCase) {
            case STRING_VALUE:
                return AttributeKey.stringKey(key);
            case BOOL_VALUE:
                return AttributeKey.booleanKey(key);
            case INT_VALUE:
                return AttributeKey.longKey(key);
            case DOUBLE_VALUE:
                return AttributeKey.doubleKey(key);
            case ARRAY_VALUE:
                return AttributeKey.stringArrayKey(key);
        }
        return null;
    }

    private Object toOtAttributeValue_rename(AnyValue anyValue) {
        AnyValue.ValueCase valueCase = anyValue.getValueCase();
        switch (valueCase) {
            case STRING_VALUE:
                return anyValue.getStringValue();
            case BOOL_VALUE:
                return anyValue.getBoolValue();
            case INT_VALUE:
                return anyValue.getIntValue();
            case DOUBLE_VALUE:
                return anyValue.getDoubleValue();
            case ARRAY_VALUE:
                return anyValue.getArrayValue();
            case KVLIST_VALUE:
                return anyValue.getKvlistValue();
        }
        return null;
    }

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
