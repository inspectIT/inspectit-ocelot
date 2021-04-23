package io.opentelemetry.sdk.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.internal.SystemClock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.LinkData;

import javax.annotation.Nullable;
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

    public static RecordEventsReadableSpan create(SpanContext context, String name, InstrumentationLibraryInfo instrumentationLibraryInfo, SpanKind kind, @Nullable SpanContext parentSpanContext, SpanLimits spanLimits, Resource resource, Attributes attributes, List<LinkData> links, int totalRecordedLinks, long startEpochNanos, long endTime) {
        // convert attributes map to AttributesMap
        Map<AttributeKey<?>, Object> map = attributes.asMap();
        AttributesMap attributesMap = new AttributesMap(map.size());
        attributesMap.putAll(map);

        // create span
        RecordEventsReadableSpan span = RecordEventsReadableSpan.startSpan(context, name, instrumentationLibraryInfo, kind, parentSpanContext, NOOP_CONTEXT, spanLimits, NOOP_SPAN_PROCESSOR, SystemClock
                .getInstance(), resource, attributesMap, links, totalRecordedLinks, startEpochNanos);

        // set end time
        span.end(endTime, TimeUnit.NANOSECONDS);

        return span;
    }

}
