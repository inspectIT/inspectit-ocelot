package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class SampledTraceTest {


    private StackTrace createStackTrace(String... classMethods) {
        StackTraceElement[] elements = Arrays.stream(classMethods)
                .map(fqn -> new StackTraceElement(fqn.substring(0, fqn.lastIndexOf('.')), fqn.substring(fqn.lastIndexOf('.') + 1), null, -1))
                .toArray(StackTraceElement[]::new);
        ArrayUtils.reverse(elements);
        return new StackTrace(elements);
    }

    @Nested
    class Add {

        @Test
        void addMultipleCallsAtOnce() {
            StackTrace start = createStackTrace("Main.main", "Root.root");
            StackTrace first = createStackTrace("Main.main", "Root.root", "A.a", "B.b");
            StackTrace second = createStackTrace("Main.main", "Root.root", "A.a", "B.b", "C.c");

            SampledTrace trace = new SampledTrace(null, start, 100);
            trace.add(first, 200);
            trace.add(second, 300);

            SampledSpan root = trace.getRoot();
            assertThat(root.getChildren()).hasSize(1);
            SampledSpan child = root.getLastChild();
            assertThat(child.getChildren()).hasSize(1);
            SampledSpan grandChild = child.getLastChild();
            assertThat(grandChild.getChildren()).isEmpty();

            assertThat(root.isEnded()).isFalse();
            assertThat(child.isEnded()).isFalse();
            assertThat(grandChild.isEnded()).isFalse();

            assertThat(root.getEntryTime()).isEqualTo(100);
            assertThat(child.getEntryTime()).isEqualTo(200);
            assertThat(grandChild.getEntryTime()).isEqualTo(200);

            assertThat(root.getSimpleName()).isEqualTo("Root.root");
            assertThat(child.getSimpleName()).isEqualTo("A.a");
            assertThat(grandChild.getSimpleName()).isEqualTo("B.b");
        }


        @Test
        void singleSampleCallsIgnored() {
            StackTrace start = createStackTrace("Root.root");
            StackTrace first = createStackTrace("Root.root", "A.a", "B.b");
            StackTrace second = createStackTrace("Root.root");

            SampledTrace trace = new SampledTrace(null, start, 100);
            trace.add(first, 200);
            trace.add(second, 300);

            SampledSpan root = trace.getRoot();
            assertThat(root.getChildren()).isEmpty();
        }


        @Test
        void reoccurringCallsSplit() {
            StackTrace start = createStackTrace("Root.root");
            StackTrace[] traces = {
                    createStackTrace("Root.root", "A.a"),
                    createStackTrace("Root.root", "A.a", "B.b"),
                    createStackTrace("Root.root"),
                    createStackTrace("Root.root", "A.a", "B.b"),
                    createStackTrace("Root.root", "A.a"),
            };
            SampledTrace trace = new SampledTrace(null, start, 100);
            for (int i = 0; i < traces.length; i++) {
                trace.add(traces[i], i * 100 + 200);
            }

            SampledSpan root = trace.getRoot();
            assertThat(root.getChildren()).hasSize(2);
            SampledSpan firstChild = root.getChildren().get(0);
            SampledSpan secondChild = root.getChildren().get(1);

            assertThat(firstChild.isEnded()).isTrue();
            assertThat(firstChild.getSimpleName()).isEqualTo("A.a");
            assertThat(firstChild.getEntryTime()).isEqualTo(200);
            assertThat(firstChild.getExitTime()).isEqualTo(300);
            assertThat(firstChild.getChildren()).isEmpty();

            assertThat(secondChild.isEnded()).isFalse();
            assertThat(secondChild.getSimpleName()).isEqualTo("A.a");
            assertThat(secondChild.getEntryTime()).isEqualTo(500);
            assertThat(secondChild.getChildren()).isEmpty();
        }
    }

    @Nested
    class End {

        @Test
        void endWithoutSamples() {
            StackTrace start = createStackTrace("Main.main", "Root.root");
            SampledTrace trace = new SampledTrace(null, start, 100);

            trace.end();

            SampledSpan root = trace.getRoot();

            assertThat(root.getChildren()).isEmpty();
            assertThat(root.isEnded()).isTrue();
            assertThat(root.getExitTime()).isEqualTo(100);
        }

        @Test
        void endWithoutSignificantSamples() {
            StackTrace start = createStackTrace("Main.main", "Root.root");
            StackTrace first = createStackTrace("Main.main", "Root.root", "A.a");
            StackTrace second = createStackTrace("Main.main", "Root.root", "B.b");

            SampledTrace trace = new SampledTrace(null, start, 100);
            trace.add(first, 200);
            trace.add(second, 300);

            trace.end();

            SampledSpan root = trace.getRoot();

            assertThat(root.getChildren()).isEmpty();
            assertThat(root.isEnded()).isTrue();
            assertThat(root.getExitTime()).isEqualTo(300);
        }


        @Test
        void endPreventsFurtherModification() {
            StackTrace start = createStackTrace("Main.main", "Root.root");
            StackTrace first = createStackTrace("Main.main", "Root.root", "A.a");
            StackTrace second = createStackTrace("Main.main", "Root.root", "A.a", "B.b");
            StackTrace third = createStackTrace("Main.main", "Root.root", "A.a", "B.b");

            SampledTrace trace = new SampledTrace(null, start, 100);
            trace.add(first, 200);
            trace.add(second, 300);
            trace.end();
            trace.add(third, 400);

            SampledSpan root = trace.getRoot();

            assertThat(root.getChildren()).hasSize(1);
            assertThat(root.isEnded()).isTrue();
            assertThat(root.getExitTime()).isEqualTo(300);

            SampledSpan child = root.getLastChild();
            assertThat(child.getChildren()).isEmpty();
            assertThat(child.isEnded()).isTrue();
            assertThat(child.getExitTime()).isEqualTo(300);
        }
    }
}
