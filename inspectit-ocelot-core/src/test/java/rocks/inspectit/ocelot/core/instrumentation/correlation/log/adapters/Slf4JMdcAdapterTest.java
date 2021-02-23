package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.DelegationMdcAccessor;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Slf4JMdcAdapterTest {

    // dummy MDC class - representing 'org.slf4j.MDC'
    public static class Slf4J_MDC {
        public static String get(String key) {
            return null;
        }

        public static void remove(String key) {
        }

        public static void put(String key, String value) {
        }
    }

    @InjectMocks
    private Slf4JMdcAdapter adapter;

    @Nested
    public class GetGetMethod {

        @Test
        public void findGetMethod() throws NoSuchMethodException {
            Method method = adapter.getGetMethod(Slf4J_MDC.class);

            assertThat(method).isEqualTo(Slf4J_MDC.class.getMethod("get", String.class));
        }
    }

    @Nested
    public class GetPutMethod {

        @Test
        public void findGetMethod() throws NoSuchMethodException {
            Method method = adapter.getPutMethod(Slf4J_MDC.class);

            assertThat(method).isEqualTo(Slf4J_MDC.class.getMethod("put", String.class, String.class));
        }
    }

    @Nested
    public class GetRemoveMethod {

        @Test
        public void findGetMethod() throws NoSuchMethodException {
            Method method = adapter.getRemoveMethod(Slf4J_MDC.class);

            assertThat(method).isEqualTo(Slf4J_MDC.class.getMethod("remove", String.class));
        }
    }

    @Nested
    public class Wrap {

        @Mock
        private TraceIdMDCInjectionSettings settings;

        @Mock
        private BiConsumer<String, Object> putConsumer;

        @Mock
        private Function<String, Object> getFunction;

        @Mock
        private Consumer<String> removeConsumer;

        @Test
        public void isEnabled() {
            when(settings.isSlf4jEnabled()).thenReturn(true);

            DelegationMdcAccessor delegationAccessor = adapter.wrap(putConsumer, getFunction, removeConsumer);

            boolean result = delegationAccessor.isEnabled(settings);

            assertThat(result).isTrue();
        }

        @Test
        public void isDisabled() {
            when(settings.isSlf4jEnabled()).thenReturn(false);

            DelegationMdcAccessor delegationAccessor = adapter.wrap(putConsumer, getFunction, removeConsumer);

            boolean result = delegationAccessor.isEnabled(settings);

            assertThat(result).isFalse();
        }
    }
}