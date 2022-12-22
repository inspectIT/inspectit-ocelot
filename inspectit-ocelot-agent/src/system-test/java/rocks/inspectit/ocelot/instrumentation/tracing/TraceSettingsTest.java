package rocks.inspectit.ocelot.instrumentation.tracing;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TraceSettingsTest extends TraceTestBase {

    String attributesSetter() {
        return "Hello A!";
    }

    @Test
    void testAttributeWritingToParentSpan() {

        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);
        attributesSetter();

        assertTraceExported((spans) -> assertThat(spans).hasSize(1).anySatisfy((sp) -> {
                    assertThat(sp.getName()).endsWith("TraceSettingsTest.attributesSetter");
                    assertThat(sp.getAttributes().asMap()).hasSize(7)
                            .containsEntry(AttributeKey.stringKey("entry"), "const")
                            .containsEntry(AttributeKey.stringKey("exit"), "Hello A!")
                            .containsEntry(AttributeKey.stringKey("toObfuscate"), "***")
                            .containsEntry(AttributeKey.stringKey("anything"), "***")
                            // plus include all common tags (service + key validation only)
                            .containsEntry(AttributeKey.stringKey("service"), "systemtest")
                            .containsKeys(AttributeKey.stringKey("host"), AttributeKey.stringKey("host_address"));
                })

        );

    }

    String attributesSetterWithConditions(boolean captureAttributes) {
        return "Hello B!";
    }

    @Test
    void testConditionalAttributeWriting() {

        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);
        attributesSetterWithConditions(false);
        attributesSetterWithConditions(true);

        assertTraceExported((spans) -> assertThat(spans).hasSize(1).anySatisfy((sp) -> {
                    assertThat(sp.getName()).endsWith("TraceSettingsTest.attributesSetterWithConditions");
                    assertThat(sp.getAttributes().asMap()).hasSize(3)
                            .containsKeys(AttributeKey.stringKey("service"), AttributeKey.stringKey("host"), AttributeKey.stringKey("host_address"));
                })

        );

        assertTraceExported((spans) -> assertThat(spans).hasSize(1).anySatisfy((sp) -> {
                    assertThat(sp.getName()).endsWith("TraceSettingsTest.attributesSetterWithConditions");
                    assertThat(sp.getAttributes().asMap()).hasSize(5)
                            .containsEntry(AttributeKey.stringKey("entry"), "const")
                            .containsEntry(AttributeKey.stringKey("exit"), "Hello B!")
                            .containsKeys(AttributeKey.stringKey("service"), AttributeKey.stringKey("host"), AttributeKey.stringKey("host_address"));
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

        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);
        conditionalRoot(false);
        conditionalRoot(true);

        assertTraceExported((spans) -> assertThat(spans).hasSize(1).anySatisfy((sp) -> {
                    assertThat(sp.getName()).endsWith("TraceSettingsTest.nestedC");
                    assertThat(SpanId.isValid(sp.getParentSpanId())).isFalse();
                })

        );
        assertTraceExported((spans) -> assertThat(spans).hasSize(2).anySatisfy((sp) -> {
                    assertThat(sp.getName()).endsWith("TraceSettingsTest.conditionalRoot");
                    assertThat(SpanId.isValid(sp.getParentSpanId())).isFalse();
                }).anySatisfy((sp) -> {
                    assertThat(sp.getName()).endsWith("TraceSettingsTest.nestedC");
                    assertThat(SpanId.isValid(sp.getParentSpanId())).isTrue();
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

        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);
        namedA("first");

        assertTraceExported((spans) -> assertThat(spans).hasSize(2).anySatisfy((sp) -> {
                    assertThat(sp.getName()).isEqualTo("first");
                    assertThat(SpanId.isValid(sp.getParentSpanId())).isFalse();
                }).anySatisfy((sp) -> {
                    assertThat(sp.getName()).isEqualTo("second");
                    assertThat(SpanId.isValid(sp.getParentSpanId())).isTrue();
                })

        );

    }

    @Test
    void testNoCommonTagsOnChild() {
        TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);

        namedA("whatever");

        assertTraceExported((spans) -> assertThat(spans).hasSize(2).anySatisfy((sp) -> {
                    assertThat(SpanId.isValid(sp.getParentSpanId())).isFalse();
                    assertThat(sp.getAttributes().asMap()).hasSize(3);
                }).anySatisfy((sp) -> {
                    assertThat(SpanId.isValid(sp.getParentSpanId())).isTrue();
                    assertThat(sp.getAttributes().asMap()).hasSize(0);
                })

        );

    }

    static class AsyncTask {

        void doAsync(String att1, String att2, String att3, boolean isFinished) {
        }
    }

    @Test
    void testInterleavedAsyncSpans() throws Exception {

        TestUtils.waitForClassInstrumentation(AsyncTask.class, true, 15, TimeUnit.SECONDS);

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
            List<io.opentelemetry.sdk.trace.data.SpanData> asyncSpans = spans.stream()
                    .filter(s -> s.getName().equals("AsyncTask.doAsync"))
                    .collect(Collectors.toList());
            assertThat(asyncSpans).hasSize(2);

            io.opentelemetry.sdk.trace.data.SpanData firstSpan = asyncSpans.get(0);
            io.opentelemetry.sdk.trace.data.SpanData secondSpan = asyncSpans.get(1);

            //order the spans by time
            if (secondSpan.getStartEpochNanos() - firstSpan.getStartEpochNanos() < 0) {
                SpanData temp = firstSpan;
                firstSpan = secondSpan;
                secondSpan = temp;
            }

            //ensure that all method invocations have been combined to single spans
            assertThat(firstSpan.getAttributes().asMap()).hasSize(5)
                    .containsEntry(AttributeKey.stringKey("1"), "a1")
                    .containsEntry(AttributeKey.stringKey("2"), "a2")
                    .containsKeys(AttributeKey.stringKey("service"), AttributeKey.stringKey("host"), AttributeKey.stringKey("host_address"));
            assertThat(secondSpan.getAttributes().asMap()).hasSize(6)
                    .containsEntry(AttributeKey.stringKey("1"), "b1")
                    .containsEntry(AttributeKey.stringKey("2"), "b2")
                    .containsEntry(AttributeKey.stringKey("3"), "b3")
                    .containsKeys(AttributeKey.stringKey("service"), AttributeKey.stringKey("host"), AttributeKey.stringKey("host_address"));

            //ensure that the timings are valid
            assertThat(firstSpan.getEndEpochNanos()).isLessThan(secondSpan.getEndEpochNanos());
            assertThat(secondSpan.getStartEpochNanos()).isLessThan(firstSpan.getEndEpochNanos());
        });
    }

    void samplingTestEndMarker(String id) {
    }

    void fixedSamplingRateTest(String id) {
    }

    void dynamicSamplingRateTest(String id, Object rate) {
    }

    void nestedSamplingTestRoot(Double rootProbability, Double nestedProbability) {
        nestedSamplingTestNested(nestedProbability);
        nestedSamplingTestNestedDefault();
    }

    void nestedSamplingTestNested(Double nestedProbability) {
    }

    /**
     * Runs with the default sample probability
     */
    void nestedSamplingTestNestedDefault() {
    }

    @Nested
    class Sampling {

        @Test
        void testFixedSpanSamplingRate() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);
            for (int i = 0; i < 10000; i++) {
                fixedSamplingRateTest("fixed");
            }
            samplingTestEndMarker("fixed_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) -> assertThat(spans).anySatisfy((sp) -> {
                assertThat(sp.getName()).isEqualTo("fixed_end");
            }));

            long numSpans = getExportedSpans().stream().filter(sp -> sp.getName().equals("fixed")).count();
            //the number of spans lies with a probability greater than 99.999% +-300 around the mean of 0.5 * 10000
            assertThat(numSpans).isGreaterThan(4700).isLessThan(5300);
        }

        @Test
        void dynamicSampleRate_low() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);
            for (int i = 0; i < 10000; i++) {
                dynamicSamplingRateTest("dynamic_0.2", 0.2);
            }
            samplingTestEndMarker("dynamic_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) -> assertThat(spans).anySatisfy((sp) -> {
                assertThat(sp.getName()).isEqualTo("dynamic_end");
            }));

            //the number of spans lies with a probability greater than 99.999% +-300 around the mean of 0.2 * 10000
            long numSpans02 = getExportedSpans().stream().filter(sp -> sp.getName().equals("dynamic_0.2")).count();
            assertThat(numSpans02).isGreaterThan(1700).isLessThan(2300);
        }

        @Test
        void dynamicSampleRate_high() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);
            for (int i = 0; i < 10000; i++) {
                dynamicSamplingRateTest("dynamic_0.7", 0.7);
            }
            samplingTestEndMarker("dynamic_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) -> assertThat(spans).anySatisfy((sp) -> {
                assertThat(sp.getName()).isEqualTo("dynamic_end");
            }));

            //the number of spans lies with a probability greater than 99.999% +-300 around the mean of 0.7 * 10000
            long numSpans07 = getExportedSpans().stream().filter(sp -> sp.getName().equals("dynamic_0.7")).count();
            assertThat(numSpans07).isGreaterThan(6700).isLessThan(7300);
        }

        @Test
        void dynamicSampleRate_invalidRate() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);
            for (int i = 0; i < 10000; i++) {
                dynamicSamplingRateTest("invalid", "not a number! haha!");
            }
            samplingTestEndMarker("dynamic_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) -> assertThat(spans).anySatisfy((sp) -> {
                assertThat(sp.getName()).isEqualTo("dynamic_end");
            }));

            //ensure that an invalid probability is equal to "never sample"
            long numSpansInvalid = getExportedSpans().stream().filter(sp -> sp.getName().equals("invalid")).count();
            assertThat(numSpansInvalid).isEqualTo(10000L);
        }

        @Test
        void dynamicSampleRate_null() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);
            for (int i = 0; i < 10000; i++) {
                dynamicSamplingRateTest("null", null);
            }
            samplingTestEndMarker("dynamic_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) -> assertThat(spans).anySatisfy((sp) -> {
                assertThat(sp.getName()).isEqualTo("dynamic_end");
            }));

            //ensure that an invalid probability is equal to "never sample"
            long numSpansNull = getExportedSpans().stream().filter(sp -> sp.getName().equals("null")).count();
            assertThat(numSpansNull).isEqualTo(10000L);
        }

        @Test
        void testNestedZeroSamplingProbability() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);

            nestedSamplingTestRoot(1.0, 0.0);

            samplingTestEndMarker("nested_zero_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) -> assertThat(spans).anySatisfy((sp) -> {
                assertThat(sp.getName()).isEqualTo("nested_zero_end");
            }));

            assertTraceExported((spans) -> assertThat(spans).hasSize(3).anySatisfy((sp) -> {
                        assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestRoot");
                        assertThat(SpanId.isValid(sp.getParentSpanId())).isFalse();
                    }).anySatisfy((sp) -> {
                        assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestNested");
                        assertThat(SpanId.isValid(sp.getParentSpanId())).isTrue();
                    }).anySatisfy((sp) -> {
                        assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestNestedDefault");
                        assertThat(SpanId.isValid(sp.getParentSpanId())).isTrue();
                    })

            );
        }

        @Test
        void testNestedOneSamplingProbability() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 30, TimeUnit.SECONDS);
            Instances.openTelemetryController.setSampler("TRACE_ID_RATIO_BASED", 1.0);
            nestedSamplingTestRoot(0.0, 1.0);

            samplingTestEndMarker("nested_one_end");
            Instances.openTelemetryController.flush();
            Instances.openTelemetryController.setSampler("PARENT_BASED", 1.0);
            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) -> assertThat(spans).anySatisfy((sp) -> {
                assertThat(sp.getName()).isEqualTo("nested_one_end");
            }));
            assertThat(getExportedSpans()).noneSatisfy(sp -> assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestRoot"))
                    .noneSatisfy(sp -> assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestNestedDefault"))
                    .anySatisfy(sp -> assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestNested"));
        }

        @Test
        void testNestedNullSamplingProbability() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);

            nestedSamplingTestRoot(0.0, null);

            samplingTestEndMarker("nested_null_end");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) -> assertThat(spans).anySatisfy((sp) -> {
                assertThat(sp.getName()).isEqualTo("nested_null_end");
            }));

            assertThat(getExportedSpans()).noneSatisfy(sp -> assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestRoot"))
                    .noneSatisfy(sp -> assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestNestedDefault"))
                    .noneSatisfy(sp -> assertThat(sp.getName()).isEqualTo("TraceSettingsTest.nestedSamplingTestNested"));
        }

    }

    void withErrorStatus(Object status) {
    }

    void withoutErrorStatus() {
    }

    @Nested
    class ErrorStatus {

        @BeforeEach
        void waitForInstrumentation() {
            TestUtils.waitForClassInstrumentation(TraceSettingsTest.class, true, 15, TimeUnit.SECONDS);
        }

        @Test
        void testWithoutErrorStatus() {
            withoutErrorStatus();

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) -> assertThat(spans).hasSize(1).anySatisfy((sp) -> {
                assertThat(sp.getName()).isEqualTo("TraceSettingsTest.withoutErrorStatus");
                assertThat(sp.getStatus()).isEqualTo(StatusData.unset());
            }));
        }

        @Test
        void testNullErrorStatus() {
            withErrorStatus(null);

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) -> assertThat(spans).hasSize(1).anySatisfy((sp) -> {
                assertThat(sp.getName()).isEqualTo("TraceSettingsTest.withErrorStatus");
                assertThat(sp.getStatus()).isEqualTo(StatusData.unset());
            }));
        }

        @Test
        void testFalseErrorStatus() {
            withErrorStatus(false);

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) -> assertThat(spans).hasSize(1).anySatisfy((sp) -> {
                assertThat(sp.getName()).isEqualTo("TraceSettingsTest.withErrorStatus");
                assertThat(sp.getStatus()).isEqualTo(StatusData.unset());
            }));
        }

        @Test
        void testNonNullErrorStatus() {
            withErrorStatus("foo");

            //wait for the end marker, this ensures that all sampled spans are also exported
            assertTraceExported((spans) -> assertThat(spans).hasSize(1).anySatisfy((sp) -> {
                assertThat(sp.getName()).isEqualTo("TraceSettingsTest.withErrorStatus");
                assertThat(sp.getStatus()).isEqualTo(StatusData.error());
            }));
        }
    }

}

