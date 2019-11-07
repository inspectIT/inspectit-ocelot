package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SampledSpanTest {
    
    @Nested
    class AddChild {

        @Test
        void verifyChildAddedLast() {
            StackTraceElement method = new StackTraceElement("PackageLessClass", "foo", "some.java", 42);
            SampledSpan span = new SampledSpan(method, 100);

            SampledSpan childA = new SampledSpan(method, 100);
            SampledSpan childB = new SampledSpan(method, 150);
            SampledSpan childC = new SampledSpan(method, 200);

            span.addChild(childA);
            span.addChild(childB);
            span.addChild(childC);

            assertThat(span.getChildren()).containsExactly(childA, childB, childC);
            assertThat(span.getLastChild()).isSameAs(childC);
        }

        @Test
        void testComplexName() {
            StackTraceElement method = new StackTraceElement("some.package.MyClass$0$1", "bar", "some.java", 42);
            SampledSpan span = new SampledSpan(method, 100);

            assertThat(span.getSimpleName()).isEqualTo("MyClass$0$1.bar");
        }
    }

    @Nested
    class EndWithAllChildren {

        @Test
        public void endWithoutChildren() {
            StackTraceElement method = new StackTraceElement("class", "foo", "some.java", 42);
            SampledSpan span = new SampledSpan(method, 100);
            span.endWithAllChildren(200);

            assertThat(span.isEnded()).isTrue();
            assertThat(span.getEntryTime()).isEqualTo(100);
            assertThat(span.getExitTime()).isEqualTo(200);
        }

        @Test
        public void endWithChildren() {
            StackTraceElement method = new StackTraceElement("class", "foo", "some.java", 42);

            SampledSpan root = new SampledSpan(method, 100);

            SampledSpan endedChild = new SampledSpan(method, 150);
            root.addChild(endedChild);
            endedChild.endWithAllChildren(160);

            SampledSpan openChild = new SampledSpan(method, 200);
            root.addChild(openChild);

            SampledSpan grandChild = new SampledSpan(method, 210);
            openChild.addChild(grandChild);


            root.endWithAllChildren(250);

            assertThat(root.isEnded()).isTrue();
            assertThat(openChild.isEnded()).isTrue();
            assertThat(grandChild.isEnded()).isTrue();

            assertThat(root.getEntryTime()).isEqualTo(100);
            assertThat(root.getExitTime()).isEqualTo(250);
            assertThat(endedChild.getEntryTime()).isEqualTo(150);
            assertThat(endedChild.getExitTime()).isEqualTo(160);
            assertThat(openChild.getEntryTime()).isEqualTo(200);
            assertThat(openChild.getExitTime()).isEqualTo(250);
            assertThat(grandChild.getEntryTime()).isEqualTo(210);
            assertThat(grandChild.getExitTime()).isEqualTo(250);
        }

    }

    @Nested
    class GetFullName {

        @Test
        void testSimpleName() {
            StackTraceElement method = new StackTraceElement("PackageLessClass", "foo", "some.java", 42);
            SampledSpan span = new SampledSpan(method, 100);

            assertThat(span.getFullName()).isEqualTo("PackageLessClass.foo");
        }

        @Test
        void testComplexName() {
            StackTraceElement method = new StackTraceElement("some.package.MyClass$0$1", "bar", "some.java", 42);
            SampledSpan span = new SampledSpan(method, 100);

            assertThat(span.getFullName()).isEqualTo("some.package.MyClass$0$1.bar");
        }
    }

    @Nested
    class GetSimpleName {

        @Test
        void testSimpleName() {
            StackTraceElement method = new StackTraceElement("PackageLessClass", "foo", "some.java", 42);
            SampledSpan span = new SampledSpan(method, 100);

            assertThat(span.getSimpleName()).isEqualTo("PackageLessClass.foo");
        }

        @Test
        void testComplexName() {
            StackTraceElement method = new StackTraceElement("some.package.MyClass$0$1", "bar", "some.java", 42);
            SampledSpan span = new SampledSpan(method, 100);

            assertThat(span.getSimpleName()).isEqualTo("MyClass$0$1.bar");
        }
    }

    @Nested
    class GetSourceFile {

        @Test
        void noSourceAvailable() {
            StackTraceElement method = new StackTraceElement("PackageLessClass", "foo", null, 42);
            SampledSpan span = new SampledSpan(method, 100);

            assertThat(span.getSourceFile()).isNull();
        }

        @Test
        void sourceWithoutLine() {
            StackTraceElement method = new StackTraceElement("some.package.MyClass$0$1", "bar", "some.java", -1);
            SampledSpan span = new SampledSpan(method, 100);

            assertThat(span.getSourceFile()).isEqualTo("some.java");
        }

        @Test
        void sourceWithLine() {
            StackTraceElement method = new StackTraceElement("some.package.MyClass$0$1", "bar", "some.java", 42);
            SampledSpan span = new SampledSpan(method, 100);

            assertThat(span.getSourceFile()).isEqualTo("some.java:42");
        }
    }
}
