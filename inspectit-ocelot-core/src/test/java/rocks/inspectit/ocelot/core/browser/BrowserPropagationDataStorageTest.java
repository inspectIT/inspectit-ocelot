package rocks.inspectit.ocelot.core.browser;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.instrumentation.browser.BrowserPropagationDataStorage;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextUtil;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BrowserPropagationDataStorageTest {

    @Mock
    PropagationMetaData propagation;

    BrowserPropagationDataStorage dataStorage;

    @BeforeEach
    void createDataStorage() {
        dataStorage = BrowserPropagationDataStorage.getInstance();
    }

    @AfterEach
    void clearDataStorage() {
        dataStorage.clearData();
    }


    @Nested
    public class WriteBrowserPropagationData {
        @Test
        void verifyNoDataHasBeenWritten() {
            when(propagation.isPropagatedWithBrowser(any())).thenReturn(false);
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctx.makeActive();
            ctx.setData("keyA", "valueA");


            assertThat(dataStorage.readData()).isEmpty();

            ctx.close();
            assertThat(dataStorage.readData()).isEmpty();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyDataHasBeenWritten() {
            when(propagation.isPropagatedWithBrowser(eq("keyA"))).thenReturn(true);
            when(propagation.isPropagatedWithBrowser(eq("keyB"))).thenReturn(false);
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctx.makeActive();
            ctx.setData("keyA", "valueA");
            ctx.setData("keyB", "valueB");


            assertThat(dataStorage.readData()).isEmpty();

            ctx.close();
            assertThat(dataStorage.readData()).containsKey("keyA");
            assertThat(dataStorage.readData()).doesNotContainKey("keyB");
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyDataHasBeenOverwritten() {
            when(propagation.isPropagatedWithBrowser(any())).thenReturn(true);
            Map<String, Object> oldData = new HashMap<>();
            oldData.put("keyA", "value0");
            dataStorage.writeData(oldData);
            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxA.makeActive();
            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxB.makeActive();

            ctxA.setData("keyA", "value1");
            ctxB.setData("keyA", "value2");
            assertThat(dataStorage.readData()).containsEntry("keyA", "value0");

            ctxB.close();
            assertThat(dataStorage.readData()).doesNotContainValue("value0");
            assertThat(dataStorage.readData()).containsEntry("keyA", "value2");

            ctxA.close();
            assertThat(dataStorage.readData()).doesNotContainValue("value2");
            assertThat(dataStorage.readData()).containsEntry("keyA", "value1");
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }
    }

    @Nested
    public class ReadBrowserPropagationData {

        @Test
        void verifyNoDownPropagation() {
            when(propagation.isPropagatedWithBrowser(any())).thenReturn(true);
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(false);
            Map<String, Object> data = new HashMap<>();
            data.put("keyA", "valueA");
            dataStorage.writeData(data);
            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxA.makeActive();
            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxB.makeActive();

            assertThat(ctxA.getData("keyA")).isNull();
            assertThat(ctxB.getData("keyA")).isNull();

            ctxB.close();
            assertThat(ctxB.getData("keyA")).isNull();

            ctxA.close();
            assertThat(ctxA.getData("keyA")).isNull();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyDownPropagation() {
            when(propagation.isPropagatedWithBrowser(any())).thenReturn(true);
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);
            Map<String, Object> data = new HashMap<>();
            data.put("keyA", "valueA");
            dataStorage.writeData(data);
            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxA.makeActive();
            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxB.makeActive();
            InspectitContextImpl ctxC = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxC.makeActive();

            assertThat(ctxA.getData("keyA")).isEqualTo("valueA");
            assertThat(ctxB.getData("keyA")).isEqualTo("valueA");

            ctxC.setData("keyA", "valueB");
            assertThat(ctxC.getData("keyA")).isEqualTo("valueB");

            ctxC.close();
            assertThat(ctxB.getData("keyA")).isEqualTo("valueA");

            ctxB.close();
            assertThat(ctxA.getData("keyA")).isEqualTo("valueA");

            ctxA.close();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyUpPropagation() {
            when(propagation.isPropagatedWithBrowser(any())).thenReturn(true);
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);
            when(propagation.isPropagatedUpWithinJVM(eq("keyB"))).thenReturn(true);
            Map<String, Object> data = new HashMap<>();
            data.put("keyA", "valueA");
            data.put("keyB", "valueB");
            dataStorage.writeData(data);
            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxA.makeActive();
            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxB.makeActive();
            InspectitContextImpl ctxC = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxC.makeActive();

            assertThat(ctxA.getData("keyA")).isEqualTo("valueA");
            assertThat(ctxA.getData("keyB")).isEqualTo("valueB");

            assertThat(ctxB.getData("keyA")).isEqualTo("valueA");
            assertThat(ctxB.getData("keyB")).isEqualTo("valueB");

            ctxC.setData("keyB", "valueZ");
            assertThat(ctxC.getData("keyA")).isEqualTo("valueA");
            assertThat(ctxC.getData("keyB")).isEqualTo("valueZ");

            ctxC.close();
            assertThat(ctxB.getData("keyB")).isEqualTo("valueZ");

            ctxB.close();
            assertThat(ctxA.getData("keyB")).isEqualTo("valueZ");

            ctxA.close();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
            assertThat(dataStorage.readData()).containsEntry("keyB", "valueZ");
        }
    }
}
