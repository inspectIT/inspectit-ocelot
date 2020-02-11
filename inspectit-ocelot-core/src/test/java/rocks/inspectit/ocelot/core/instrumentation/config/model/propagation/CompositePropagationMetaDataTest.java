package rocks.inspectit.ocelot.core.instrumentation.config.model.propagation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompositePropagationMetaDataTest {

    @Mock
    PropagationMetaData parent;

    @Nested
    class Copy {

        @Test
        void verifySettingsOverridable() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setTag("root", true)
                    .setDownPropagation("root", PropagationMode.JVM_LOCAL)
                    .setUpPropagation("root", PropagationMode.JVM_LOCAL)
                    .build()
                    .copy()
                    .setTag("root", false)
                    .setUpPropagation("root", PropagationMode.GLOBAL)
                    .build();

            assertThat(result.isTag("root")).isFalse();
            assertThat(result.isPropagatedDownGlobally("root")).isFalse();
            assertThat(result.isPropagatedDownWithinJVM("root")).isTrue();
            assertThat(result.isPropagatedUpGlobally("root")).isTrue();
            assertThat(result.isPropagatedUpWithinJVM("root")).isTrue();
        }


        @Test
        void verifyNewSettingsCanBeAdded() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setTag("root", true)
                    .setDownPropagation("root", PropagationMode.JVM_LOCAL)
                    .setUpPropagation("root", PropagationMode.JVM_LOCAL)
                    .build()
                    .copy()
                    .setTag("child", true)
                    .setDownPropagation("child", PropagationMode.GLOBAL)
                    .setUpPropagation("child", PropagationMode.GLOBAL)
                    .build();

            assertThat(result.isTag("child")).isTrue();
            assertThat(result.isPropagatedDownGlobally("child")).isTrue();
            assertThat(result.isPropagatedDownWithinJVM("child")).isTrue();
            assertThat(result.isPropagatedUpGlobally("child")).isTrue();
            assertThat(result.isPropagatedUpWithinJVM("child")).isTrue();
        }
    }

    @Nested
    class SetDownPropagation {

        @Test
        void inheritedPropagation() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .build();

            assertThat(result.isPropagatedDownWithinJVM("my_key")).isFalse();
            verify(parent).isPropagatedDownWithinJVM(eq("my_key"));
            assertThat(result.isPropagatedDownGlobally("my_key")).isFalse();
            verify(parent).isPropagatedDownGlobally(eq("my_key"));
            verifyNoMoreInteractions(parent);
        }

        @Test
        void noPropagation() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setDownPropagation("my_key", PropagationMode.NONE)
                    .build();

            assertThat(result.isPropagatedDownWithinJVM("my_key")).isFalse();
            assertThat(result.isPropagatedDownGlobally("my_key")).isFalse();
            verifyZeroInteractions(parent);
        }

        @Test
        void jvmLocalPropagation() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setDownPropagation("my_key", PropagationMode.JVM_LOCAL)
                    .build();

            assertThat(result.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(result.isPropagatedDownGlobally("my_key")).isFalse();
            verifyZeroInteractions(parent);
        }

        @Test
        void globalPropagation() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setDownPropagation("my_key", PropagationMode.GLOBAL)
                    .build();

            assertThat(result.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(result.isPropagatedDownGlobally("my_key")).isTrue();
            verifyZeroInteractions(parent);
        }

        @Test
        void ensureBuilderOverridesRespected() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setDownPropagation("my_key", PropagationMode.GLOBAL)
                    .setDownPropagation("my_key", PropagationMode.NONE)
                    .build();

            assertThat(result.isPropagatedDownWithinJVM("my_key")).isFalse();
            assertThat(result.isPropagatedDownGlobally("my_key")).isFalse();
            verifyZeroInteractions(parent);
        }
    }


    @Nested
    class SetUpPropagation {

        @Test
        void inheritedPropagation() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .build();

            assertThat(result.isPropagatedUpWithinJVM("my_key")).isFalse();
            verify(parent).isPropagatedUpWithinJVM(eq("my_key"));
            assertThat(result.isPropagatedUpGlobally("my_key")).isFalse();
            verify(parent).isPropagatedUpGlobally(eq("my_key"));
            verifyNoMoreInteractions(parent);
        }

        @Test
        void noPropagation() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setUpPropagation("my_key", PropagationMode.NONE)
                    .build();

            assertThat(result.isPropagatedUpWithinJVM("my_key")).isFalse();
            assertThat(result.isPropagatedUpGlobally("my_key")).isFalse();
            verifyZeroInteractions(parent);
        }

        @Test
        void jvmLocalPropagation() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setUpPropagation("my_key", PropagationMode.JVM_LOCAL)
                    .build();

            assertThat(result.isPropagatedUpWithinJVM("my_key")).isTrue();
            assertThat(result.isPropagatedUpGlobally("my_key")).isFalse();
            verifyZeroInteractions(parent);
        }

        @Test
        void globalPropagation() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setUpPropagation("my_key", PropagationMode.GLOBAL)
                    .build();

            assertThat(result.isPropagatedUpWithinJVM("my_key")).isTrue();
            assertThat(result.isPropagatedUpGlobally("my_key")).isTrue();
            verifyZeroInteractions(parent);
        }

        @Test
        void ensureBuilderOverridesRespected() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setUpPropagation("my_key", PropagationMode.GLOBAL)
                    .setUpPropagation("my_key", PropagationMode.NONE)
                    .build();

            assertThat(result.isPropagatedUpWithinJVM("my_key")).isFalse();
            assertThat(result.isPropagatedUpGlobally("my_key")).isFalse();
            verifyZeroInteractions(parent);
        }
    }

    @Nested
    class SetTag {

        @Test
        void inheritedTag() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .build();

            assertThat(result.isTag("my_key")).isFalse();
            verify(parent).isTag(eq("my_key"));
            verifyNoMoreInteractions(parent);
        }

        @Test
        void isNotATag() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setTag("my_key", false)
                    .build();

            assertThat(result.isTag("my_key")).isFalse();
            verifyZeroInteractions(parent);
        }

        @Test
        void isTag() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setTag("my_key", true)
                    .build();

            assertThat(result.isTag("my_key")).isTrue();
            verifyZeroInteractions(parent);
        }

        @Test
        void overridesRespected() {
            PropagationMetaData result = CompositePropagationMetaData.builder(parent)
                    .setTag("my_key", true)
                    .setTag("my_key", false)
                    .build();

            assertThat(result.isTag("my_key")).isFalse();
            verifyZeroInteractions(parent);
        }
    }
}
