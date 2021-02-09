package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import io.opencensus.trace.Span;
import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.util.IterableUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

public class SampledTraceTest {

    private static StackTrace createStackTrace(String... classMethods) {
        StackTraceElement[] elements = Arrays.stream(classMethods)
                .map(fqn -> new StackTraceElement(fqn.substring(0, fqn.lastIndexOf('.')), fqn.substring(fqn.lastIndexOf('.') + 1), null, -1))
                .toArray(StackTraceElement[]::new);
        ArrayUtils.reverse(elements);
        return new StackTrace(elements);
    }

    private static abstract class SampledTraceGenerator {

        private List<String> currentStack;

        private long fakeTime = 0;

        private SampledTrace traceToCreate;

        public SampledTraceGenerator(String... rootStackTrace) {
            currentStack = new ArrayList<>(Arrays.asList(rootStackTrace));
            StackTrace rootTrace = createStackTrace(rootStackTrace);
            traceToCreate = new SampledTrace(Mockito.mock(Span.class), () -> rootTrace);
        }

        protected void sample() {
            long time = fakeTime++;
            StackTrace trace = createStackTrace(currentStack.toArray(new String[]{}));
            traceToCreate.addStackTrace(trace, time);
        }

        protected void anonymousCall(String name, Runnable action) {
            currentStack.add(name); //add to fake call-stack
            action.run();
            currentStack.remove(currentStack.size() - 1);
        }

        protected void tracedCall(String name, Runnable action) {
            long entryTime = fakeTime++;
            PlaceholderSpan span = Mockito.mock(PlaceholderSpan.class);
            doReturn(entryTime).when(span).getStartTime();
            doReturn(name).when(span).getSpanName();

            currentStack.add(name); //add to fake call-stack
            String className = name.substring(0, name.lastIndexOf('.'));
            String methodName = name.substring(name.lastIndexOf('.') + 1);
            SampledTrace.MethodExitNotifier exitCallback = traceToCreate.newSpanStarted(span, className, methodName);

            action.run();

            currentStack.remove(currentStack.size() - 1); //pop from fake call stack
            long exitTime = fakeTime++;
            exitCallback.methodFinished(exitTime);
        }

        public SampledTrace doGenerate() {
            generate();
            return traceToCreate;
        }

        abstract protected void generate();
    }

    private static class InvocationCheck implements Consumer<Invocation> {

        private String expectedName;

        private List<InvocationCheck> childAsserts;

        private long entryTime;

        private long exitTime;

        public InvocationCheck(String expectedName, long entryTime, long exitTime, InvocationCheck... childAsserts) {
            this.entryTime = entryTime;
            this.exitTime = exitTime;
            this.expectedName = expectedName;
            this.childAsserts = Arrays.asList(childAsserts);
        }

        @Override
        public void accept(Invocation invocation) {
            if (invocation.getSampledMethod() != null) {
                assertThat(invocation.getSampledMethod().getClassName())
                        .isEqualTo(expectedName.substring(0, expectedName.lastIndexOf('.')));
                assertThat(invocation.getSampledMethod().getMethodName())
                        .isEqualTo(expectedName.substring(expectedName.lastIndexOf('.') + 1));
            } else {
                assertThat(invocation.getPlaceholderSpan().getSpanName()).isEqualTo(expectedName);
            }
            assertThat(invocation.getStart().getTimestamp()).isEqualTo(entryTime);
            assertThat(invocation.getEnd().getTimestamp()).isEqualTo(exitTime);
            assertThat(invocation.getChildren()).hasSize(childAsserts.size());
            Object[] children = IterableUtil.toArray(invocation.getChildren());
            for (int i = 0; i < children.length; i++) {
                childAsserts.get(i).accept((Invocation) children[i]);
            }
        }
    }

    @Nested
    class GenerateInvocations {

        @Test
        void singleSampleMethodsIgnored() {
            SampledTrace trace = new SampledTraceGenerator("RootA.myRootA", "RootA.myRootB") {
                @Override
                protected void generate() {
                    anonymousCall("Top.firstMethod", () -> {
                        anonymousCall("Child.meth1", () -> {
                            sample();
                        });
                        anonymousCall("Child.meth2", () -> {
                            sample();
                        });
                    });
                }
            }.doGenerate();

            Iterable<Invocation> invocations = trace.generateInvocations();
            assertThat(invocations).hasSize(1);
            assertThat(invocations).first().satisfies(
                    new InvocationCheck("Top.firstMethod", 0, 1)
            );
        }

        @Test
        void dualSampleMethodsRespected() {
            SampledTrace trace = new SampledTraceGenerator("RootA.myRootA", "RootA.myRootB") {
                @Override
                protected void generate() {
                    sample();
                    anonymousCall("Top.firstMethod", () -> {
                        anonymousCall("Child.meth0", () -> {
                            sample();
                        });
                        anonymousCall("Child.meth1", () -> {
                            sample();
                            sample();
                        });
                        anonymousCall("Child.meth2", () -> {
                            sample();
                            sample();
                        });
                    });
                }
            }.doGenerate();

            Iterable<Invocation> invocations = trace.generateInvocations();
            assertThat(invocations).hasSize(1);
            assertThat(invocations).first().satisfies(
                    new InvocationCheck("Top.firstMethod", 1, 5,
                            new InvocationCheck("Child.meth1", 2, 3),
                            new InvocationCheck("Child.meth2", 4, 5)
                    )
            );
        }

        @Test
        void multipleRoots() {
            SampledTrace trace = new SampledTraceGenerator("RootA.myRootA", "RootA.myRootB") {
                @Override
                protected void generate() {
                    anonymousCall("Top.firstMethod", () -> {
                        sample();
                        sample();
                    });
                    tracedCall("Hello.secondMethod", () -> {
                    });
                }
            }.doGenerate();

            Iterable<Invocation> invocations = trace.generateInvocations();
            assertThat(invocations).hasSize(2);
            assertThat(invocations).first().satisfies(
                    new InvocationCheck("Top.firstMethod", 0, 1)
            );
            assertThat(invocations).last().satisfies(
                    new InvocationCheck("Hello.secondMethod", 2, 3)
            );
        }

        @Test
        void instrumentedMethodWithSampledChild() {
            SampledTrace trace = new SampledTraceGenerator("RootA.myRootA", "RootA.myRootB") {
                @Override
                protected void generate() {
                    tracedCall("Instr.methA", () -> {
                        anonymousCall("Sampled.methB", () -> {
                            sample();
                            anonymousCall("Sampled.methC", () -> {
                                sample();
                                sample();
                            });
                        });
                    });
                }
            }.doGenerate();

            Iterable<Invocation> invocations = trace.generateInvocations();
            assertThat(invocations).hasSize(1);
            assertThat(invocations).first().satisfies(
                    new InvocationCheck("Instr.methA", 0, 4,
                            new InvocationCheck("Sampled.methB", 1, 3,
                                    new InvocationCheck("Sampled.methC", 2, 3)))
            );
        }

        @Test
        void nestedInstrumentations() {
            SampledTrace trace = new SampledTraceGenerator("RootA.myRootA", "RootA.myRootB") {
                @Override
                protected void generate() {
                    anonymousCall("Top.firstMethod", () -> {
                        sample();
                        tracedCall("A.a1", () -> {
                            tracedCall("B.b1", () -> {
                                sample();
                            });
                        });
                    });
                }
            }.doGenerate();

            Iterable<Invocation> invocations = trace.generateInvocations();
            assertThat(invocations).hasSize(1);
            assertThat(invocations).first().satisfies(
                    new InvocationCheck("Top.firstMethod", 0, 5,
                            new InvocationCheck("A.a1", 1, 5,
                                    new InvocationCheck("B.b1", 2, 4)
                            )
                    )
            );
        }

        @Test
        void instrumentedMethodWithSingleSampleParent() {
            SampledTrace trace = new SampledTraceGenerator("RootA.myRootA", "RootA.myRootB") {
                @Override
                protected void generate() {
                    anonymousCall("Top.firstMethod", () -> {
                        tracedCall("A.a1", () -> {
                            sample();
                        });
                    });
                }
            }.doGenerate();

            Iterable<Invocation> invocations = trace.generateInvocations();
            assertThat(invocations).hasSize(1);
            assertThat(invocations).first().satisfies(
                    new InvocationCheck("Top.firstMethod", 0, 2,
                            new InvocationCheck("A.a1", 0, 2)
                    )
            );
        }

        @Test
        void instrumentedMethodWithMultipleSampleParent() {
            SampledTrace trace = new SampledTraceGenerator("RootA.myRootA", "RootA.myRootB") {
                @Override
                protected void generate() {
                    anonymousCall("Top.firstMethod", () -> {
                        anonymousCall("Blub.secondMethod", () -> {
                            tracedCall("A.a1", () -> {
                                sample();
                            });
                        });
                    });
                }
            }.doGenerate();

            Iterable<Invocation> invocations = trace.generateInvocations();
            assertThat(invocations).hasSize(1);
            assertThat(invocations).first().satisfies(
                    new InvocationCheck("Top.firstMethod", 0, 2,
                            new InvocationCheck("Blub.secondMethod", 0, 2,
                                    new InvocationCheck("A.a1", 0, 2)
                            )
                    )
            );
        }

        @Test
        void instrumentedMethodWithExtendedParent() {
            SampledTrace trace = new SampledTraceGenerator("RootA.myRootA", "RootA.myRootB") {
                @Override
                protected void generate() {
                    anonymousCall("Top.firstMethod", () -> {
                        tracedCall("A.a1", () -> {
                            sample();
                        });
                        sample();
                        sample();
                    });
                }
            }.doGenerate();

            Iterable<Invocation> invocations = trace.generateInvocations();
            assertThat(invocations).hasSize(1);
            assertThat(invocations).first().satisfies(
                    new InvocationCheck("Top.firstMethod", 0, 4,
                            new InvocationCheck("A.a1", 0, 2)
                    )
            );
        }

        @Test
        void recursiveInstrumentations() {
            SampledTrace trace = new SampledTraceGenerator("RootA.myRootA", "RootA.myRootB") {
                @Override
                protected void generate() {
                    anonymousCall("Top.firstMethod", () -> {
                        tracedCall("Do.recurse", () -> {
                            anonymousCall("Middle.myMethod", () -> {
                                tracedCall("Do.recurse", () -> {
                                    sample();
                                });
                            });
                        });
                    });
                }
            }.doGenerate();

            Iterable<Invocation> invocations = trace.generateInvocations();
            assertThat(invocations).hasSize(1);
            assertThat(invocations).first().satisfies(
                    new InvocationCheck("Top.firstMethod", 0, 4,
                            new InvocationCheck("Do.recurse", 0, 4,
                                    new InvocationCheck("Middle.myMethod", 1, 3,
                                            new InvocationCheck("Do.recurse", 1, 3))
                            )
                    )
            );
        }

    }

}
