package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters.MDCAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class MDCAccessTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    InspectitEnvironment inspectitEnv;

    @InjectMocks
    MDCAccess access;

    @Mock
    MDCAdapter adapterA;

    @Mock
    MDCAdapter adapterB;

    @Mock
    MDCAdapter adapterC;

    @BeforeEach
    void reset() {
        access.availableAdapters.clear();
    }

    @Nested
    class Put {

        @Test
        void verifyUndoOrderReversed() {
            access.enabledAdapters = ImmutableSet.of(adapterA, adapterB, adapterC);

            List<Object> setOrder = new ArrayList<>();
            List<Object> undoOrder = new ArrayList<>();

            Answer<MDCAccess.Undo> orderRemember = (invoc) -> {
                setOrder.add(invoc.getMock());
                MDCAccess.Undo undo = Mockito.mock(MDCAccess.Undo.class);
                doAnswer((invoc2) -> undoOrder.add(invoc.getMock())).when(undo).close();
                return undo;
            };
            doAnswer(orderRemember).when(adapterA).set(eq("key"), eq("value"));
            doAnswer(orderRemember).when(adapterB).set(eq("key"), eq("value"));
            doAnswer(orderRemember).when(adapterC).set(eq("key"), eq("value"));

            MDCAccess.Undo undo = access.put("key", "value");
            assertThat(setOrder).containsExactlyInAnyOrder(adapterA, adapterB, adapterC);
            assertThat(undoOrder).isEmpty();

            undo.close();

            assertThat(setOrder).containsExactlyInAnyOrder(adapterA, adapterB, adapterC);
            List<Object> reverseUndo = new ArrayList<>(undoOrder);
            Collections.reverse(reverseUndo);
            assertThat(reverseUndo).containsExactlyElementsOf(setOrder);

        }

    }

    @Nested
    class UpdateEnabledAdapters {

        @Test
        void verifyUndoOrderReversed() {
            access.availableAdapters.put(Byte.class, adapterA);
            access.availableAdapters.put(Short.class, adapterB);
            access.availableAdapters.put(Integer.class, adapterC);

            doReturn(false).when(adapterA).isEnabledForConfig(any());
            doReturn(true).when(adapterB).isEnabledForConfig(any());
            doReturn(true).when(adapterC).isEnabledForConfig(any());

            access.updateEnabledAdaptersSet();

            assertThat(access.enabledAdapters).containsExactlyInAnyOrder(adapterB, adapterC);
        }

    }
}
