package io.opentelemetry.sdk.trace;

import io.opencensus.trace.AttributeValue;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.internal.SystemClock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OcelotRERSProxy {

    private static final Context NOOP_CONTEXT = new Context() {
        @Nullable
        @Override
        public <V> V get(ContextKey<V> key) {
            return null;
        }

        @Override
        public <V> Context with(ContextKey<V> k1, V v1) {
            return null;
        }
    };

    private static final SpanProcessor NOOP_SPAN_PROCESSOR = new SpanProcessor() {
        @Override
        public void onStart(Context parentContext, ReadWriteSpan span) {
        }

        @Override
        public boolean isStartRequired() {
            return false;
        }

        @Override
        public void onEnd(ReadableSpan span) {
        }

        @Override
        public boolean isEndRequired() {
            return false;
        }
    };

    public static RecordEventsReadableSpan create(SpanContext context, String name, InstrumentationLibraryInfo instrumentationLibraryInfo, SpanKind kind, @Nullable SpanContext parentSpanContext, SpanLimits spanLimits, Resource resource, Attributes attributes, List<LinkData> links, int totalRecordedLinks, long startEpochNanos, long endTime, List<Span.Event> eventsList, Status status) {
        // convert attributes map to AttributesMap
        Map<AttributeKey<?>, Object> map = attributes.asMap();
        AttributesMap attributesMap = new AttributesMap(map.size());
        attributesMap.putAll(map);

        // create span
        RecordEventsReadableSpan span = RecordEventsReadableSpan.startSpan(context, name, instrumentationLibraryInfo, kind, parentSpanContext, NOOP_CONTEXT, spanLimits, NOOP_SPAN_PROCESSOR, SystemClock
                .getInstance(), resource, attributesMap, links, totalRecordedLinks, startEpochNanos);

        // add event to the span
        addEvents(span, eventsList);

        StatusCode statusCode = toStatusCode(status.getCode());
        if (statusCode != null) {
            span.setStatus(statusCode, status.getMessage());
        }

        // set end time
        if (endTime > 0) {
            span.end(endTime, TimeUnit.NANOSECONDS);
        }

        return span;
    }

    private static StatusCode toStatusCode(Status.StatusCode code) {
        switch (code) {
            case STATUS_CODE_UNSET:
                return StatusCode.UNSET;
            case STATUS_CODE_OK:
                return StatusCode.OK;
            case STATUS_CODE_ERROR:
                return StatusCode.ERROR;
            case UNRECOGNIZED:
            default:
                return null;
        }
    }

    private static void addEvents(RecordEventsReadableSpan span, List<Span.Event> eventsList) {
        if (CollectionUtils.isEmpty(eventsList)) {
            return;
        }

        for (Span.Event event : eventsList) {
            long timeUnixNano = event.getTimeUnixNano();

            // skip events which starts before the actual span
            if (timeUnixNano < span.getStartEpochNanos()) {
                continue;
            }

            Attributes attributes = toOtAttributes_rename(event.getAttributesList());
            span.addEvent(event.getName(), attributes, timeUnixNano, TimeUnit.NANOSECONDS);
        }
    }

    private static Attributes toOtAttributes_rename(List<KeyValue> attributesList) {
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

    private static AttributeKey toAttributeKey_rename(KeyValue attribute) {
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

    private static Object toOtAttributeValue_rename(AnyValue anyValue) {
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
}
