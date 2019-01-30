package rocks.inspectit.oce.core.instrumentation.config;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.data.DataSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.data.PropagationMode;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedDataProperties;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class DataPropertiesResolverTest {

    InstrumentationSettings testSettings = new InstrumentationSettings();

    @InjectMocks
    DataPropertiesResolver dpr = new DataPropertiesResolver();

    @Nested
    class Resolve {

        @Test
        void defaultSettingsForUnmentionedKeyCorrect() {
            ResolvedDataProperties dataProps = dpr.resolve(testSettings);
            assertThat(dataProps.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedDownGlobally("my_key")).isFalse();

            assertThat(dataProps.isPropagatedUpWithinJVM("my_key")).isFalse();
            assertThat(dataProps.isPropagatedUpGlobally("my_key")).isFalse();

            assertThat(dataProps.isTag("my_key")).isTrue();
        }

        @Test
        void defaultSettingsForUnmentionedLocalKeyCorrect() {
            ResolvedDataProperties dataProps = dpr.resolve(testSettings);
            assertThat(dataProps.isPropagatedDownWithinJVM("local_my_key")).isTrue();
            assertThat(dataProps.isPropagatedDownGlobally("local_my_key")).isFalse();

            assertThat(dataProps.isPropagatedUpWithinJVM("local_my_key")).isFalse();
            assertThat(dataProps.isPropagatedUpGlobally("local_my_key")).isFalse();

            assertThat(dataProps.isTag("local_my_key")).isTrue();
        }

        @Test
        void defaultSettingsForMentionedLocalKeyCorrect() {
            testSettings.setData(Maps.newHashMap("local_my_key", null));
            ResolvedDataProperties dataProps = dpr.resolve(testSettings);
            assertThat(dataProps.isPropagatedDownWithinJVM("local_my_key")).isFalse();
            assertThat(dataProps.isPropagatedDownGlobally("local_my_key")).isFalse();

            assertThat(dataProps.isPropagatedUpWithinJVM("local_my_key")).isFalse();
            assertThat(dataProps.isPropagatedUpGlobally("local_my_key")).isFalse();

            assertThat(dataProps.isTag("local_my_key")).isFalse();
        }

        @Test
        void defaultSettingsForMentionedNonLocalKeyCorrect() {
            testSettings.setData(Maps.newHashMap("my_key", null));
            ResolvedDataProperties dataProps = dpr.resolve(testSettings);
            assertThat(dataProps.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedDownGlobally("my_key")).isFalse();

            assertThat(dataProps.isPropagatedUpWithinJVM("my_key")).isFalse();
            assertThat(dataProps.isPropagatedUpGlobally("my_key")).isFalse();

            assertThat(dataProps.isTag("my_key")).isTrue();
        }


        @Test
        void noneDownPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setDownPropagation(PropagationMode.NONE);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            ResolvedDataProperties dataProps = dpr.resolve(testSettings);

            assertThat(dataProps.isPropagatedDownWithinJVM("my_key")).isFalse();
            assertThat(dataProps.isPropagatedDownGlobally("my_key")).isFalse();
        }

        @Test
        void jvmLocalDownPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setDownPropagation(PropagationMode.JVM_LOCAL);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            ResolvedDataProperties dataProps = dpr.resolve(testSettings);

            assertThat(dataProps.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedDownGlobally("my_key")).isFalse();
        }

        @Test
        void globalDownPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setDownPropagation(PropagationMode.GLOBAL);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            ResolvedDataProperties dataProps = dpr.resolve(testSettings);

            assertThat(dataProps.isPropagatedDownWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedDownGlobally("my_key")).isTrue();
        }

        @Test
        void noneUpPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setUpPropagation(PropagationMode.NONE);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            ResolvedDataProperties dataProps = dpr.resolve(testSettings);

            assertThat(dataProps.isPropagatedUpWithinJVM("my_key")).isFalse();
            assertThat(dataProps.isPropagatedUpGlobally("my_key")).isFalse();
        }

        @Test
        void jvmLocalUpPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setUpPropagation(PropagationMode.JVM_LOCAL);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            ResolvedDataProperties dataProps = dpr.resolve(testSettings);

            assertThat(dataProps.isPropagatedUpWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedUpGlobally("my_key")).isFalse();
        }

        @Test
        void globalUpPropagationConvertedCorrectly() {
            DataSettings ds = new DataSettings();
            ds.setUpPropagation(PropagationMode.GLOBAL);
            testSettings.setData(Maps.newHashMap("my_key", ds));

            ResolvedDataProperties dataProps = dpr.resolve(testSettings);

            assertThat(dataProps.isPropagatedUpWithinJVM("my_key")).isTrue();
            assertThat(dataProps.isPropagatedUpGlobally("my_key")).isTrue();
        }

    }

}
