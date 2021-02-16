package rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.bootstrap.correlation.MdcAccessor;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.DelegationMdcAccessor;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Log4J1MdcAdapterTest {

    // dummy MDC class - representing 'org.apache.log4j.MDC'
    public static class LOG4J1_MDC {
        public static Object get(String key) {
            return null;
        }

        public static void remove(String key) {
        }

        public static void put(String key, Object value) {
        }
    }

    @InjectMocks
    private Log4J1MdcAdapter adapter;

    @Nested
    public class GetGetMethod {

        @Test
        public void findGetMethod() throws NoSuchMethodException {
            Method method = adapter.getGetMethod(LOG4J1_MDC.class);

            assertThat(method).isEqualTo(LOG4J1_MDC.class.getMethod("get", String.class));
        }
    }

    @Nested
    public class GetPutMethod {

        @Test
        public void findGetMethod() throws NoSuchMethodException {
            Method method = adapter.getPutMethod(LOG4J1_MDC.class);

            assertThat(method).isEqualTo(LOG4J1_MDC.class.getMethod("put", String.class, Object.class));
        }
    }

    @Nested
    public class GetRemoveMethod {

        @Test
        public void findGetMethod() throws NoSuchMethodException {
            Method method = adapter.getRemoveMethod(LOG4J1_MDC.class);

            assertThat(method).isEqualTo(LOG4J1_MDC.class.getMethod("remove", String.class));
        }
    }

    @Nested
    public class Wrap {

        @Mock
        private TraceIdMDCInjectionSettings settings;

        @Mock
        private MdcAccessor accessor;

        @Test
        public void isEnabled() {
            when(settings.isLog4j1Enabled()).thenReturn(true);

            DelegationMdcAccessor delegationAccessor = adapter.wrap(accessor);

            boolean result = delegationAccessor.isEnabled(settings);

            assertThat(result).isTrue();
        }

        @Test
        public void isDisabled() {
            when(settings.isLog4j1Enabled()).thenReturn(false);

            DelegationMdcAccessor delegationAccessor = adapter.wrap(accessor);

            boolean result = delegationAccessor.isEnabled(settings);

            assertThat(result).isFalse();
        }
    }
}