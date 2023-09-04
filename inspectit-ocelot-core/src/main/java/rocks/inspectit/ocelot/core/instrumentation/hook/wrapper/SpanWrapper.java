package rocks.inspectit.ocelot.core.instrumentation.hook.wrapper;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Slf4j
public class SpanWrapper implements Span {

    /**
     * Object, which is wrapped by this class
     */
    private Span span;

    /**
     * Additional Autocloseable to define custom close()-functions
     */
    private AutoCloseable autoCloseable;

    @Override
    public Scope makeCurrent() {
        Scope scope = span.makeCurrent();
        return new ScopeWrapper(scope, autoCloseable);
    }

    @Override
    public <T> Span setAttribute(AttributeKey<T> key, T value) {
        return span.setAttribute(key, value);
    }

    @Override
    public Span addEvent(String name, Attributes attributes) {
        return span.addEvent(name, attributes);
    }

    @Override
    public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
        return span.addEvent(name, attributes, timestamp, unit);
    }

    @Override
    public Span setStatus(StatusCode statusCode, String description) {
        return span.setStatus(statusCode, description);
    }

    @Override
    public Span recordException(Throwable exception, Attributes additionalAttributes) {
        return span.recordException(exception, additionalAttributes);
    }

    @Override
    public Span updateName(String name) {
        return span.updateName(name);
    }

    @Override
    public void end() {
        span.end();
    }

    @Override
    public void end(long timestamp, TimeUnit unit) {
        span.end(timestamp, unit);
    }

    @Override
    public SpanContext getSpanContext() {
        return span.getSpanContext();
    }

    @Override
    public boolean isRecording() {
        return span.isRecording();
    }
}
