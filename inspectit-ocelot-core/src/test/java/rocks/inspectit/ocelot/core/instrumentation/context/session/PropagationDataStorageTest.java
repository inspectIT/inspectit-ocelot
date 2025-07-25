package rocks.inspectit.ocelot.core.instrumentation.context.session;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.instrumentation.context.propagation.ContextPropagation;
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
import static rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext.REMOTE_SESSION_ID;

@ExtendWith(MockitoExtension.class)
@DirtiesContext
public class PropagationDataStorageTest extends SpringTestBase {

    @Mock
    PropagationMetaData propagation;

    @Autowired
    PropagationSessionStorage sessionStorage;

    Map<String, String> headers;

    private final String sessionId = "test=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-4942-453a-9243-7d8422803604";

    @BeforeEach
    void prepareTest() {
        sessionStorage.setPropagation(propagation);

        String sessionIdHeader = "Session-Id";
        ContextPropagation.get().setSessionIdHeader(sessionIdHeader);
        // Create HTTP header to pass it to the initial InspectIT-Context
        headers = new HashMap<>();
        headers.put(sessionIdHeader, sessionId);
    }

    @AfterEach
    void clearDataStorage() {
        sessionStorage.clearDataStorages();
    }

    @Nested
    public class WriteSessionData {

        @Test
        void verifyNoDataHasBeenWritten() {
            when(propagation.isStoredForSession(any())).thenReturn(false);
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
            ctx.readDownPropagationHeaders(headers);
            ctx.makeActive();
            ctx.setData("keyA", "valueA");

            PropagationDataStorage dataStorage = sessionStorage.getDataStorage(sessionId);
            assertThat(dataStorage.readData()).isEmpty();

            ctx.close();
            assertThat(dataStorage.getSize()).isZero();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyDataHasBeenWritten() {
            when(propagation.isStoredForSession(anyString())).thenReturn(false);
            when(propagation.isStoredForSession(eq("keyA"))).thenReturn(true);
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
            ctx.readDownPropagationHeaders(headers);
            ctx.makeActive();
            ctx.setData("keyA", "valueA");
            ctx.setData("keyB", "valueB");

            PropagationDataStorage dataStorage = sessionStorage.getDataStorage(sessionId);
            assertThat(dataStorage.readData()).isEmpty();

            ctx.close();
            assertThat(dataStorage.readData()).containsKey("keyA");
            assertThat(dataStorage.readData()).doesNotContainKey("keyB");
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyDataHasBeenOverwritten() {
            when(propagation.isStoredForSession(any())).thenReturn(true);
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);
            PropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            Map<String, Object> oldData = new HashMap<>();
            oldData.put("keyA", "value0");
            dataStorage.writeData(oldData);

            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
            ctxA.readDownPropagationHeaders(headers);
            ctxA.makeActive();
            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
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
        void verifyTagLimit() {
            when(propagation.isStoredForSession(any())).thenReturn(true);
            PropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            Map<String, Object> dummyMap = IntStream.rangeClosed(1, 130).boxed()
                    .collect(Collectors.toMap(i -> "key"+i, i -> "value"+i));
            dataStorage.writeData(dummyMap);

            assertThat(dataStorage.getSize()).isLessThanOrEqualTo(128);
        }

        @Test
        void verifyValidEntries() {
            when(propagation.isStoredForSession(any())).thenReturn(true);
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
            ctx.readDownPropagationHeaders(headers);
            ctx.makeActive();
            // Create too long key and value
            String dummyKey = IntStream.range(1, 130).mapToObj(i -> "x").collect(Collectors.joining());
            String dummyValue = IntStream.range(1, 2050).mapToObj(i -> "y").collect(Collectors.joining());

            //System.out.println(dummyKey.length() + " : " + dummyValue.length());

            ctx.setData(dummyKey, dummyValue);
            PropagationDataStorage dataStorage = sessionStorage.getDataStorage(sessionId);
            assertThat(dataStorage.readData()).doesNotContainEntry(dummyKey, dummyValue);

            ctx.close();
            assertThat(dataStorage.readData()).doesNotContainEntry(dummyKey, dummyValue);
        }

        @Test
        void verifySessionIdHasNotBeenWritten() {
            Map<String, Object> data = new HashMap<>();
            data.put(REMOTE_SESSION_ID, "value");

            PropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            dataStorage.writeData(data);

            assertThat(dataStorage.readData()).isEmpty();
        }

        @Test
        void verifyDataHasBeenDownPropagatedToLateDataStorage() {
            when(propagation.isStoredForSession(any())).thenReturn(true);
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);
            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
            ctxA.setData("keyA", "valueA");
            ctxA.makeActive();

            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
            ctxB.setData(REMOTE_SESSION_ID, sessionId);
            ctxB.setData("keyB", "valueB");
            ctxB.makeActive();
            ctxB.close();

            PropagationDataStorage dataStorage = sessionStorage.getDataStorage(sessionId);
            assertThat(dataStorage.readData()).containsEntry("keyA", "valueA");
            assertThat(dataStorage.readData()).containsEntry("keyB", "valueB");

            ctxA.close();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }
    }

    @Nested
    public class ReadSessionData {

        @Test
        void verifySessionDataUpdated() {
            when(propagation.isStoredForSession(any())).thenReturn(true);
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);
            PropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
            Map<String, Object> data = new HashMap<>();
            data.put("keyA", "valueA");
            dataStorage.writeData(data);
            ctxA.readDownPropagationHeaders(headers);
            ctxA.makeActive();
            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
            ctxB.makeActive();
            InspectitContextImpl ctxC = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
            ctxC.makeActive();

            assertThat(ctxA.getData("keyA")).isEqualTo("valueA");
            assertThat(ctxB.getData("keyA")).isEqualTo("valueA");

            ctxC.setData("keyA", "valueB");
            assertThat(ctxC.getData("keyA")).isEqualTo("valueB");

            ctxC.close();
            assertThat(ctxB.getData("keyA")).isEqualTo("valueB");

            ctxB.close();
            assertThat(ctxA.getData("keyA")).isEqualTo("valueB");

            ctxA.close();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyUpPropagation() {
            when(propagation.isStoredForSession(any())).thenReturn(true);
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);
            when(propagation.isPropagatedUpWithinJVM(eq("keyB"))).thenReturn(true);
            PropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(sessionId);
            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
            Map<String, Object> data = new HashMap<>();
            data.put("keyA", "valueA");
            data.put("keyB", "valueB");
            dataStorage.writeData(data);
            ctxA.readDownPropagationHeaders(headers);
            ctxA.makeActive();
            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
            ctxB.makeActive();
            InspectitContextImpl ctxC = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, sessionStorage, false);
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
