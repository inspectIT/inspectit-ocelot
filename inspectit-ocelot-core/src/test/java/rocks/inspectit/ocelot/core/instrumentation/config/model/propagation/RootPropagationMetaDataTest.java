package rocks.inspectit.ocelot.core.instrumentation.config.model.propagation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;

import static org.assertj.core.api.Assertions.assertThat;

public class RootPropagationMetaDataTest {

    @Nested
    class Copy {

        @Test
        void testDefaultSettings() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .setTag("root", true)
                    .setDownPropagation("root", PropagationMode.JVM_LOCAL)
                    .setUpPropagation("root", PropagationMode.JVM_LOCAL)
                    .build()
                    .copy()
                    .setTag("child", true)
                    .setDownPropagation("child", PropagationMode.GLOBAL)
                    .setUpPropagation("child", PropagationMode.GLOBAL)
                    .build();

            assertThat(result.isTag("not_present")).isFalse();
            assertThat(result.isPropagatedDownGlobally("not_present")).isFalse();
            assertThat(result.isPropagatedDownWithinJVM("not_present")).isFalse();
            assertThat(result.isPropagatedUpGlobally("not_present")).isFalse();
            assertThat(result.isPropagatedUpWithinJVM("not_present")).isFalse();
        }


        @Test
        void verifySettingsOverridable() {
            PropagationMetaData result = RootPropagationMetaData.builder()
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
            PropagationMetaData result = RootPropagationMetaData.builder()
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
        void defaultPropagation() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .build();

            assertThat(result.isPropagatedDownWithinJVM("my_key")).isFalse();
            assertThat(result.isPropagatedDownGlobally("my_key")).isFalse();
        }

        @Test
        void noPropagation() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .setDownPropagation("my_key", PropagationMode.NONE)
                    .build();

            assertThat(result.isPropagatedDownWithinJVM("my_key")).isFalse();
            assertThat(result.isPropagatedDownGlobally("my_key")).isFalse();
        }

        @Test
        void jvmLocalPropagation() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .setDownPropagation("my_key", PropagationMode.JVM_LOCAL)
                    .build();

            assertThat(result.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(result.isPropagatedDownGlobally("my_key")).isFalse();
        }

        @Test
        void globalPropagation() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .setDownPropagation("my_key", PropagationMode.GLOBAL)
                    .build();

            assertThat(result.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(result.isPropagatedDownGlobally("my_key")).isTrue();
        }

        @Test
        void ensureOverridesRespected() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .setDownPropagation("my_key", PropagationMode.GLOBAL)
                    .setDownPropagation("my_key", PropagationMode.NONE)
                    .build();

            assertThat(result.isPropagatedDownWithinJVM("my_key")).isFalse();
            assertThat(result.isPropagatedDownGlobally("my_key")).isFalse();
        }

    }


    @Nested
    class SetUpPropagation {

        @Test
        void defaultPropagation() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .build();

            assertThat(result.isPropagatedUpWithinJVM("my_key")).isFalse();
            assertThat(result.isPropagatedUpGlobally("my_key")).isFalse();
        }

        @Test
        void noPropagation() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .setUpPropagation("my_key", PropagationMode.NONE)
                    .build();

            assertThat(result.isPropagatedUpWithinJVM("my_key")).isFalse();
            assertThat(result.isPropagatedUpGlobally("my_key")).isFalse();
        }

        @Test
        void jvmLocalPropagation() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .setUpPropagation("my_key", PropagationMode.JVM_LOCAL)
                    .build();

            assertThat(result.isPropagatedUpWithinJVM("my_key")).isTrue();
            assertThat(result.isPropagatedUpGlobally("my_key")).isFalse();
        }

        @Test
        void globalPropagation() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .setUpPropagation("my_key", PropagationMode.GLOBAL)
                    .build();

            assertThat(result.isPropagatedUpWithinJVM("my_key")).isTrue();
            assertThat(result.isPropagatedUpGlobally("my_key")).isTrue();
        }

        @Test
        void ensureOverridesRespected() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .setUpPropagation("my_key", PropagationMode.GLOBAL)
                    .setUpPropagation("my_key", PropagationMode.NONE)
                    .build();

            assertThat(result.isPropagatedUpWithinJVM("my_key")).isFalse();
            assertThat(result.isPropagatedUpGlobally("my_key")).isFalse();
        }
    }

    @Nested
    class SetTag {

        @Test
        void defaultTag() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .build();

            assertThat(result.isTag("my_key")).isFalse();
        }

        @Test
        void isNotATag() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .setTag("my_key", false)
                    .build();

            assertThat(result.isTag("my_key")).isFalse();
        }

        @Test
        void isTag() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .setTag("my_key", true)
                    .build();

            assertThat(result.isTag("my_key")).isTrue();
        }

        @Test
        void overridesRespected() {
            PropagationMetaData result = RootPropagationMetaData.builder()
                    .setTag("my_key", true)
                    .setTag("my_key", false)
                    .build();

            assertThat(result.isTag("my_key")).isFalse();
        }
    }
}
