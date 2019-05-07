package rocks.inspectit.ocelot.instrumentation.tracing;

import io.opencensus.trace.AttributeValue;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.TimeUnit;

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
}

