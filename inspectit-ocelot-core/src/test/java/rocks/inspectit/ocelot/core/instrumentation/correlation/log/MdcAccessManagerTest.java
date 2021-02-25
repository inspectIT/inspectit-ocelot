package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class MdcAccessManagerTest {

    @InjectMocks
    private MdcAccessManager manager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitEnvironment environment;

    @Mock
    private MdcAccessor mdcAccessorOne;

    @Mock
    private MdcAccessor mdcAccessorTwo;

    @Mock
    private MdcAccessor mdcAccessorThree;

    @Nested
    public class InjectValue {

        @Mock
        private InjectionScope scopeOne;

        @Mock
        private InjectionScope scopeTwo;

        @Mock
        private InjectionScope scopeThree;

        @Test
        public void injectAndResetInReverseOrder() {
            manager.activeMdcAccessors = Arrays.asList(
                    mdcAccessorOne,
                    mdcAccessorTwo,
                    mdcAccessorThree
            );

            when(mdcAccessorOne.inject(anyString(), anyString())).thenReturn(scopeOne);
            when(mdcAccessorTwo.inject(anyString(), anyString())).thenReturn(scopeTwo);
            when(mdcAccessorThree.inject(anyString(), anyString())).thenReturn(scopeThree);

            InjectionScope scope = manager.injectValue("key", "value");

            InOrder inOrder = inOrder(mdcAccessorOne, mdcAccessorTwo, mdcAccessorThree, scopeOne, scopeTwo, scopeThree);
            inOrder.verify(mdcAccessorOne).inject("key", "value");
            inOrder.verify(mdcAccessorTwo).inject("key", "value");
            inOrder.verify(mdcAccessorThree).inject("key", "value");

            scope.close();

            inOrder.verify(scopeThree).close();
            inOrder.verify(scopeTwo).close();
            inOrder.verify(scopeOne).close();
            verifyNoMoreInteractions(scopeOne, scopeTwo, scopeThree, mdcAccessorOne, mdcAccessorTwo, mdcAccessorThree);
        }
    }

    @Nested
    public class UpdateActiveMdcAccessors {

        @Test
        public void updateAccessors() {
            manager.availableMdcAccessors = ImmutableMap.of(
                    Byte.class, mdcAccessorOne,
                    Short.class, mdcAccessorTwo,
                    Integer.class, mdcAccessorThree
            );

            when(mdcAccessorOne.isEnabled(any())).thenReturn(true);
            when(mdcAccessorTwo.isEnabled(any())).thenReturn(false);
            when(mdcAccessorThree.isEnabled(any())).thenReturn(true);
            when(mdcAccessorOne.getTargetMdcClass()).thenReturn(new WeakReference<>(Byte.class));
            when(mdcAccessorThree.getTargetMdcClass()).thenReturn(new WeakReference<>(Integer.class));

            // activate
            manager.updateActiveMdcAccessors();

            assertThat(manager.activeMdcAccessors).containsOnly(mdcAccessorOne, mdcAccessorThree);

            when(mdcAccessorThree.isEnabled(any())).thenReturn(false);

            // deactivate
            manager.updateActiveMdcAccessors();

            assertThat(manager.activeMdcAccessors).containsOnly(mdcAccessorOne);
        }
    }
}