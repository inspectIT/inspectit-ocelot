package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters.MDCAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
public class MDCAccessTest {

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
        access.activeAdapters.clear();
    }

    @Nested
    class Put {

        @Test
        void verifyUndoOrderReversed() {
            access.activeAdapters.put(Long.class, adapterA);
            access.activeAdapters.put(Double.class, adapterB);
            access.activeAdapters.put(Byte.class, adapterC);

            List<Object> setOrder = new ArrayList<>();
            List<Object> undoOrder = new ArrayList<>();

            Answer<MDCAdapter.Undo> orderRemember = (invoc) -> {
                setOrder.add(invoc.getMock());
                MDCAdapter.Undo undo = Mockito.mock(MDCAdapter.Undo.class);
                doAnswer((invoc2) -> undoOrder.add(invoc.getMock())).when(undo).undoChange();
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
}
