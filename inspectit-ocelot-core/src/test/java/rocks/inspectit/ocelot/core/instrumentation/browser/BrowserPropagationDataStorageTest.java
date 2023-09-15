package rocks.inspectit.ocelot.core.instrumentation.browser;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextUtil;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BrowserPropagationDataStorageTest extends SpringTestBase {

    @Mock
    PropagationMetaData propagation;

    BrowserPropagationSessionStorage sessionStorage;

    Map<String, String> headers;

    private static final String sessionIdHeader = "Cookie";
    private static final String sessionId = "test=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-4942-453a-9243-7d8422803604";

    @BeforeEach
    void prepareTest() {
        // Create session storage to store BrowserPropagationDataStorages
        sessionStorage = BrowserPropagationSessionStorage.getInstance();
        // Create HTTP header to pass it to the initial InspectIT-Context
        headers = new HashMap<>();
        headers.put(sessionIdHeader, sessionId);
    }

    @AfterEach
    void clearDataStorage() {
        sessionStorage.clearDataStorages();
    }


    @Nested
    public class WriteBrowserPropagationData {
        @Test
        void verifyNoDataHasBeenWritten() {
            when(propagation.isPropagatedWithBrowser(any())).thenReturn(false);
            BrowserPropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctx.readDownPropagationHeaders(headers);
            ctx.makeActive();
            ctx.setData("keyA", "valueA");

            assertThat(dataStorage.readData()).isEmpty();

            ctx.close();
            assertThat(dataStorage.readData()).isEmpty();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyDataHasBeenWritten() {
            when(propagation.isPropagatedWithBrowser(anyString())).thenReturn(false);
            when(propagation.isPropagatedWithBrowser(eq("keyA"))).thenReturn(true);
            BrowserPropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctx.readDownPropagationHeaders(headers);
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
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);
            BrowserPropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            Map<String, Object> oldData = new HashMap<>();
            oldData.put("keyA", "value0");
            dataStorage.writeData(oldData);
            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxA.readDownPropagationHeaders(headers);
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

        @Test
        void verifyAttributeCountLimit() {
            when(propagation.isPropagatedWithBrowser(any())).thenReturn(true);
            BrowserPropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            Map<String, Object> dummyMap = IntStream.rangeClosed(1, 128).boxed()
                    .collect(Collectors.toMap(i -> "key"+i, i -> "value"+i));
            dataStorage.writeData(dummyMap);
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctx.readDownPropagationHeaders(headers);
            ctx.makeActive();

            ctx.setData("key1", "value321");
            ctx.setData("keyABC", "valueABC");
            assertThat(dataStorage.readData()).doesNotContainEntry("key1", "value321");
            assertThat(dataStorage.readData()).doesNotContainEntry("keyABC", "valueABC");

            ctx.close();
            assertThat(dataStorage.readData()).doesNotContainEntry("key1", "value321");
            assertThat(dataStorage.readData()).doesNotContainEntry("keyABC", "valueABC");
            assertThat(dataStorage.readData().size()).isEqualTo(128);
        }

        @Test
        void verifyValidEntries() {
            when(propagation.isPropagatedWithBrowser(any())).thenReturn(true);
            BrowserPropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctx.readDownPropagationHeaders(headers);
            ctx.makeActive();
            String dummyKey = IntStream.range(1, 130).mapToObj(i -> "x").collect(Collectors.joining());
            String dummyValue = IntStream.range(1,2050).mapToObj(i -> "y").collect(Collectors.joining());

            System.out.println(dummyKey.length() + " : " + dummyValue.length());

            ctx.setData(dummyKey, dummyValue);
            assertThat(dataStorage.readData()).doesNotContainEntry(dummyKey, dummyValue);

            ctx.close();
            assertThat(dataStorage.readData()).doesNotContainEntry(dummyKey, dummyValue);
        }
    }

    @Nested
    public class ReadBrowserPropagationData {

        @Test
        void verifyNoDownPropagation() {
            when(propagation.isPropagatedWithBrowser(any())).thenReturn(true);
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(false);
            when(propagation.isPropagatedDownWithinJVM(eq("remote_session_id"))).thenReturn(true);
            BrowserPropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            Map<String, Object> data = new HashMap<>();
            data.put("keyA", "valueA");
            dataStorage.writeData(data);
            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxA.readDownPropagationHeaders(headers);
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
            BrowserPropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            Map<String, Object> data = new HashMap<>();
            data.put("keyA", "valueA");
            dataStorage.writeData(data);
            ctxA.readDownPropagationHeaders(headers);
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
            BrowserPropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            Map<String, Object> data = new HashMap<>();
            data.put("keyA", "valueA");
            data.put("keyB", "valueB");
            dataStorage.writeData(data);
            ctxA.readDownPropagationHeaders(headers);
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
