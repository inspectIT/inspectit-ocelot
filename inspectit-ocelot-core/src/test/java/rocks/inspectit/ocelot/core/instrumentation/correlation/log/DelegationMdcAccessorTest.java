package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class DelegationMdcAccessorTest {

    private DelegationMdcAccessor delegationAccessor;

    private final String EXISTING_KEY = "KEY_EXIST";

    private final String EXISTING_VALUE = "TEST";

    private final String NON_EXISTING_KEY = "NON_KEY_EXIST";

    @BeforeEach
    public void beforeEach() {
        FakeMdc fakeMdc = new FakeMdc();
        fakeMdc.put(EXISTING_KEY, EXISTING_VALUE);

        delegationAccessor = new DelegationMdcAccessor(fakeMdc::put, fakeMdc::get, fakeMdc::remove) {
            @Override
            public boolean isEnabled(TraceIdMDCInjectionSettings settings) {
                return false;
            }
        };
    }

    @Nested
    public class Get {

        @Test
        public void existingKey() {
            Object result = delegationAccessor.get(EXISTING_KEY);

            assertThat(result).isSameAs(EXISTING_VALUE);
        }

        @Test
        public void nonExistingKey() {
            Object result = delegationAccessor.get(NON_EXISTING_KEY);

            assertThat(result).isNull();
        }
    }

    @Nested
    public class Remove {

        @Test
        public void successful() {
            Object before = delegationAccessor.get(EXISTING_KEY);

            delegationAccessor.remove(EXISTING_KEY);

            Object after = delegationAccessor.get(EXISTING_KEY);

            assertThat(before).isSameAs(EXISTING_VALUE);
            assertThat(after).isNull();
        }
    }

    @Nested
    public class Put {

        @Test
        public void overwriteValue() {
            Object before = delegationAccessor.get(EXISTING_KEY);

            delegationAccessor.put(EXISTING_KEY, "test");

            Object after = delegationAccessor.get(EXISTING_KEY);

            assertThat(before).isSameAs(EXISTING_VALUE);
            assertThat(after).isSameAs("test");
        }
    }

    @Nested
    public class Inject {

        @Test
        public void injectNullWhenValueExists() {
            Object beforeInjection = delegationAccessor.get(EXISTING_KEY);

            InjectionScope scope = delegationAccessor.inject(EXISTING_KEY, null);

            Object afterInjection = delegationAccessor.get(EXISTING_KEY);

            scope.close();

            Object afterReset = delegationAccessor.get(EXISTING_KEY);

            assertThat(beforeInjection).isSameAs(EXISTING_VALUE);
            assertThat(afterInjection).isNull();
            assertThat(afterReset).isSameAs(EXISTING_VALUE);
        }

        @Test
        public void injectNullWhenNothingExists() {
            Object beforeInjection = delegationAccessor.get(NON_EXISTING_KEY);

            InjectionScope scope = delegationAccessor.inject(NON_EXISTING_KEY, null);

            Object afterInjection = delegationAccessor.get(NON_EXISTING_KEY);

            scope.close();

            Object afterReset = delegationAccessor.get(NON_EXISTING_KEY);

            assertThat(beforeInjection).isNull();
            assertThat(afterInjection).isNull();
            assertThat(afterReset).isNull();
        }

        @Test
        public void injectValueWhenValueExists() {
            Object beforeInjection = delegationAccessor.get(EXISTING_KEY);

            InjectionScope scope = delegationAccessor.inject(EXISTING_KEY, "test");

            Object afterInjection = delegationAccessor.get(EXISTING_KEY);

            scope.close();

            Object afterReset = delegationAccessor.get(EXISTING_KEY);

            assertThat(beforeInjection).isSameAs(EXISTING_VALUE);
            assertThat(afterInjection).isEqualTo("test");
            assertThat(afterReset).isSameAs(EXISTING_VALUE);
        }

        @Test
        public void injectValueWhenNothingExists() {
            Object beforeInjection = delegationAccessor.get(NON_EXISTING_KEY);

            InjectionScope scope = delegationAccessor.inject(NON_EXISTING_KEY, "test");

            Object afterInjection = delegationAccessor.get(NON_EXISTING_KEY);

            scope.close();

            Object afterReset = delegationAccessor.get(NON_EXISTING_KEY);

            assertThat(beforeInjection).isNull();
            assertThat(afterInjection).isEqualTo("test");
            assertThat(afterReset).isNull();
        }
    }
}