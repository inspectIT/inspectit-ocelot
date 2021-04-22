package io.opentelemetry.sdk.trace;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

import java.util.List;

public class OcelotSpanWrapperProxy {

    public static SpanData create(RecordEventsReadableSpan delegate, List<LinkData> links, List<EventData> events, Attributes attributes, int totalAttributeCount, int totalRecordedEvents, StatusData status, String name, long endEpochNanos, boolean hasEnded) {
        return AutoValue_SpanWrapper.create(delegate, links, events, attributes, totalAttributeCount, totalRecordedEvents, status, name, endEpochNanos, hasEnded);
    }

}
