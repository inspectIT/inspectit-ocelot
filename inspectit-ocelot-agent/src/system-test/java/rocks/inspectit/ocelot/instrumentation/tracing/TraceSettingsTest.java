package rocks.inspectit.ocelot.instrumentation.tracing;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.export.SpanData;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TraceSettingsTest extends TraceTestBase {


    void rootA() {
        attributesSetterWithoutSpan();
    }

    String attributesSetterWithoutSpan() {
        return "Hello A!";
    }

    @Test
    void testAttributeWritingToParentSpan() {

        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
        rootA();

        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(1)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).endsWith("TraceSettingsTest.rootA");
                            assertThat(sp.getAttributes().getAttributeMap())
                                    .hasSize(2)
                                    .containsEntry("entry", AttributeValue.stringAttributeValue("const"))
                                    .containsEntry("exit", AttributeValue.stringAttributeValue("Hello A!"));

                        })

        );

    }


    void rootB(boolean captureAttributes) {
        attributesSetterWithoutSpanWithConditions(captureAttributes);
    }

    String attributesSetterWithoutSpanWithConditions(boolean captureAttributes) {
        return "Hello B!";
    }

    @Test
    void testConditionalAttributeWriting() {

        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
        rootB(false);
        rootB(true);

        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(1)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).endsWith("TraceSettingsTest.rootB");
                            assertThat(sp.getAttributes().getAttributeMap())
                                    .hasSize(0);
                        })

        );

        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(1)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).endsWith("TraceSettingsTest.rootB");
                            assertThat(sp.getAttributes().getAttributeMap())
                                    .hasSize(2)
                                    .containsEntry("entry", AttributeValue.stringAttributeValue("const"))
                                    .containsEntry("exit", AttributeValue.stringAttributeValue("Hello B!"));

                        })

        );

    }


    void conditionalRoot(boolean startSpan) {
        nestedC();
    }

    void nestedC() {
    }

    @Test
    void testConditionalSpanCreation() {

        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
        conditionalRoot(false);
        conditionalRoot(true);

        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(1)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).endsWith("TraceSettingsTest.nestedC");
                            assertThat(sp.getParentSpanId()).isNull();
                        })

        );
        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(2)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).endsWith("TraceSettingsTest.conditionalRoot");
                            assertThat(sp.getParentSpanId()).isNull();
                        })
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).endsWith("TraceSettingsTest.nestedC");
                            assertThat(sp.getParentSpanId()).isNotNull();
                        })

        );

    }


    void namedA(String name) {
        namedB("second");
    }

    void namedB(String name) {
    }

    @Test
    void testSpanNameCustomization() {

        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, 15, TimeUnit.SECONDS);
        namedA("first");

        assertTraceExported((spans) ->
                assertThat(spans)
                        .hasSize(2)
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).isEqualTo("first");
                            assertThat(sp.getParentSpanId()).isNull();
                        })
                        .anySatisfy((sp) -> {
                            assertThat(sp.getName()).isEqualTo("second");
                            assertThat(sp.getParentSpanId()).isNotNull();
                        })

        );

    }

    static class AsyncTask {
        void doAsync(String att1, String att2, String att3, boolean isFinished) {
        }
    }

    @Test
    void testInterleavedAsyncSpans() throws Exception {

        TestUtils.waitForClassInstrumentation(AsyncTask.class, 15, TimeUnit.SECONDS);

        //all method calls of each task will result in a single span
        AsyncTask first = new AsyncTask();
        AsyncTask second = new AsyncTask();

        //interleave the asynchronous tasks and check that the
        first.doAsync("a1", null, null, false);
        Thread.sleep(10);
        second.doAsync("b1", null, null, false);
        second.doAsync(null, "b2", null, false);
        Thread.sleep(10);
        first.doAsync(null, "a2", null, true);
        Thread.sleep(10);
        second.doAsync(null, null, "b3", true);

        assertSpansExported(spans -> {
            List<SpanData> asyncSpans = spans.stream()
                    .filter(s -> s.getName().equals("AsyncTask.doAsync"))
                    .collect(Collectors.toList());
            assertThat(asyncSpans).hasSize(2);

            SpanData firstSpan = asyncSpans.get(0);
            SpanData secondSpan = asyncSpans.get(1);

            //order the spans by time
            if (secondSpan.getStartTimestamp().compareTo(secondSpan.getStartTimestamp()) < 0) {
                SpanData temp = firstSpan;
                firstSpan = secondSpan;
                secondSpan = temp;
            }

            //ensure that all method invocations have been combined to single spans
            assertThat(firstSpan.getAttributes().getAttributeMap())
                    .hasSize(2)
                    .containsEntry("1", AttributeValue.stringAttributeValue("a1"))
                    .containsEntry("2", AttributeValue.stringAttributeValue("a2"));
            assertThat(secondSpan.getAttributes().getAttributeMap())
                    .hasSize(3)
                    .containsEntry("1", AttributeValue.stringAttributeValue("b1"))
                    .containsEntry("2", AttributeValue.stringAttributeValue("b2"))
                    .containsEntry("3", AttributeValue.stringAttributeValue("b3"));

            //ensure that the timings are valid
            assertThat(firstSpan.getEndTimestamp()).isLessThan(secondSpan.getEndTimestamp());
            assertThat(secondSpan.getStartTimestamp()).isLessThan(firstSpan.getEndTimestamp());
        });


    }
}

