package io.opentelemetry.sdk.trace;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.proto.trace.v1.Status;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OcelotSpanUtilsTest {

    @Nested
    class CreateSpanContext {

        @Test
        public void validContext() {
            SpanContext result = OcelotSpanUtils.createSpanContext("03c2a546267d1e90d70269bdc02babef", "c29e6dd2a1e1e7ae");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getTraceId()).isEqualTo("03c2a546267d1e90d70269bdc02babef");
            assertThat(result.getSpanId()).isEqualTo("c29e6dd2a1e1e7ae");
        }

        @Test
        public void emptySpanId() {
            SpanContext result = OcelotSpanUtils.createSpanContext("03c2a546267d1e90d70269bdc02babef", "");

            assertThat(result.isValid()).isFalse();
        }
    }

    @Nested
    class ToStatusCode {

        @Test
        public void toStatusCode() {
            assertThat(OcelotSpanUtils.toStatusCode(Status.StatusCode.STATUS_CODE_OK)).isEqualTo(StatusCode.OK);
            assertThat(OcelotSpanUtils.toStatusCode(Status.StatusCode.STATUS_CODE_ERROR)).isEqualTo(StatusCode.ERROR);
            assertThat(OcelotSpanUtils.toStatusCode(Status.StatusCode.STATUS_CODE_UNSET)).isEqualTo(StatusCode.UNSET);
            assertThat(OcelotSpanUtils.toStatusCode(Status.StatusCode.UNRECOGNIZED)).isNull();
        }
    }
}