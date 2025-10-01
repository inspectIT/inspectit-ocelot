package rocks.inspectit.ocelot.core.instrumentation.context;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.correlation.noop.NoopLogTraceCorrelator;
import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.instrumentation.context.propagation.BaggagePropagation;
import rocks.inspectit.ocelot.core.instrumentation.context.session.PropagationDataStorage;
import rocks.inspectit.ocelot.core.instrumentation.context.session.PropagationSessionStorage;
import rocks.inspectit.ocelot.core.testutils.GcUtils;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext.REMOTE_PARENT_SPAN_CONTEXT_KEY;
import static rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext.REMOTE_SESSION_ID;

@ExtendWith(MockitoExtension.class)
public class InspectitContextImplTest extends SpringTestBase {

    @Mock
    PropagationMetaData propagation;

    @Mock
    PropagationSessionStorage sessionStorage;

    Map<String, String> getCurrentAttributesAsMap() {
        Map<String, String> result = new HashMap<>();
        Baggage current = Baggage.current();
        current.asMap().forEach((key, valueEntry) -> result.put(key, valueEntry.getValue()));
        return result;
    }

    @Nested
    public class GetAndClearCurrentRemoteSpanContext {

        @Test
        void verifyNullIfNoSpanSet() {
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(new HashMap<>(), propagation, sessionStorage, false);

            assertThat(ctx.getAndClearCurrentRemoteSpanContext()).isNull();

            ctx.close();
        }

        @Test
        void verifyCleared() {
            SpanContext span = OpenTelemetryUtils.getTracer().spanBuilder("blub").startSpan().getSpanContext();
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(new HashMap<>(), propagation, sessionStorage, false);
            ctx.setData(REMOTE_PARENT_SPAN_CONTEXT_KEY, span);

            SpanContext result = ctx.getAndClearCurrentRemoteSpanContext();

            assertThat(ctx.getAndClearCurrentRemoteSpanContext()).isNull();
            assertThat(result).isSameAs(span);
            ctx.close();
        }

    }

    @Nested
    public class CreateRemoteParentContext {

        @Test
        void verifyTraceContextFormat() {
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(new HashMap<>(), propagation, sessionStorage, false);
            String traceContext = ctx.createRemoteParentContext();
            String w3cFormat = "00-([0-9a-f]{32})-([0-9a-f]{16})-01";

            assertThat(traceContext.matches(w3cFormat)).isTrue();
            ctx.close();
        }
    }

    @Nested
    public class EnterSpan {

        @Spy
        NoopLogTraceCorrelator traceCorrelator;

        @BeforeEach
        void setup() {
            Instances.logTraceCorrelator = traceCorrelator;

        }

        @AfterEach
        void reset() {
            Instances.logTraceCorrelator = NoopLogTraceCorrelator.INSTANCE;
        }

        @Test
        void spanEntered() {
            Span mySpan = OpenTelemetryUtils.getTracer().spanBuilder("blub").startSpan();

            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(new HashMap<>(), propagation, sessionStorage, false);
            assertThat(ctx.hasEnteredSpan()).isFalse();

            Scope scope = mySpan.makeCurrent();
            ctx.setSpanScope(scope);

            ctx.makeActive();

            assertThat(ctx.hasEnteredSpan()).isTrue();
            assertThat(Span.current()).isSameAs(mySpan);

            ctx.close();

            assertThat(Span.current()).isNotSameAs(mySpan);
        }

    }

    @Nested
    public class DownPropagation {

        @Test
        void verifyCommonAttributesExtracted() {
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("tagA", "valueA");
            attributes.put("tagB", "valueB");

            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(attributes, propagation, sessionStorage, false);
            ctx.makeActive();

            assertThat(ctx.getData("tagA")).isEqualTo("valueA");
            assertThat(ctx.getData("tagB")).isEqualTo("valueB");

            ctx.close();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyCommonAttributesPropagatedAndOverwritable() {
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("tagA", "valueA");
            attributes.put("tagB", "valueB");
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);

            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(attributes, propagation, sessionStorage, false);
            ctxA.setData("tagB", "overwritten");
            ctxA.makeActive();

            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(attributes, propagation, sessionStorage, false);
            ctxB.makeActive();

            assertThat(ctxB.getData("tagA")).isEqualTo("valueA");
            assertThat(ctxB.getData("tagB")).isEqualTo("overwritten");

            ctxB.close();
            ctxA.close();

            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyOverwritesAreLocal() {
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);

            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxA.setData("keyA", "ctxA_valueA");
            ctxA.setData("keyB", "ctxA_valueB");
            ctxA.makeActive();

            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxB.setData("keyB", "ctxB_valueB");
            ctxB.makeActive();

            InspectitContextImpl ctxC = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxC.makeActive();

            assertThat(ctxA.getData("keyA")).isEqualTo("ctxA_valueA");
            assertThat(ctxA.getData("keyB")).isEqualTo("ctxA_valueB");

            assertThat(ctxB.getData("keyA")).isEqualTo("ctxA_valueA");
            assertThat(ctxB.getData("keyB")).isEqualTo("ctxB_valueB");

            assertThat(ctxC.getData("keyA")).isEqualTo("ctxA_valueA");
            assertThat(ctxC.getData("keyB")).isEqualTo("ctxB_valueB");

            ctxC.close();
            ctxB.close();
            //ensure no up propagation took place
            assertThat(ctxA.getData("keyA")).isEqualTo("ctxA_valueA");
            assertThat(ctxA.getData("keyB")).isEqualTo("ctxA_valueB");
            ctxA.close();

            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyOverwritesHappenOnlyWhenConfigured() {
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(eq("keyA"));
            doReturn(false).when(propagation).isPropagatedDownWithinJVM(eq("keyB"));

            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxA.setData("keyA", "ctxA_valueA");
            ctxA.setData("keyB", "ctxA_valueB");
            ctxA.makeActive();

            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxB.setData("keyB", "ctxB_valueB");
            ctxB.makeActive();

            InspectitContextImpl ctxC = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxC.makeActive();

            assertThat(ctxA.getData("keyA")).isEqualTo("ctxA_valueA");
            assertThat(ctxA.getData("keyB")).isEqualTo("ctxA_valueB");

            assertThat(ctxB.getData("keyA")).isEqualTo("ctxA_valueA");
            assertThat(ctxB.getData("keyB")).isEqualTo("ctxB_valueB");

            assertThat(ctxC.getData("keyA")).isEqualTo("ctxA_valueA");
            assertThat(ctxC.getData("keyB")).isNull();

            ctxC.close();
            ctxB.close();
            //ensure no up propagation took place
            assertThat(ctxA.getData("keyA")).isEqualTo("ctxA_valueA");
            assertThat(ctxA.getData("keyB")).isEqualTo("ctxA_valueB");
            ctxA.close();

            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyContextReleasedWhenAllChildrenAreClosed() {

            InspectitContextImpl firstContext = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            firstContext.makeActive();

            InspectitContextImpl secondContext = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            secondContext.makeActive();
            WeakReference<InspectitContextImpl> firstContextWeak = new WeakReference<>(firstContext);

            InspectitContextImpl openRemainingContext = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);

            secondContext.close();
            firstContext.close();
            firstContext = null;

            GcUtils.waitUntilCleared(firstContextWeak);

            openRemainingContext.makeActive();
            openRemainingContext.close();
        }

        @Test
        void verifyDownPropagationForChildrenOnDifferentThreadWithRootNotClosed() throws Exception {
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            root.setData("tag", "rootValue");
            root.makeActive();
            root.setData("tag", "invisibleValue");

            AtomicReference<Object> tagValue = new AtomicReference<>();
            Thread asyncTask = new Thread(Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
                tagValue.set(asyncChild.getData("tag"));
                asyncChild.makeActive();
                asyncChild.close();
            }));

            asyncTask.start();
            asyncTask.join();

            root.close();

            assertThat(tagValue.get()).isEqualTo("rootValue");
        }

        @Test
        void verifyDownPropagationForChildrenOnDifferentThreadWithRootClosed() throws Exception {
            lenient().doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            root.setData("tag", "rootValue");
            root.makeActive();
            root.setData("tag", "invisibleValue");

            AtomicReference<Object> tagValue = new AtomicReference<>();
            Thread asyncTask = new Thread(Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
                tagValue.set(asyncChild.getData("tag"));
                asyncChild.makeActive();
                asyncChild.close();
            }));

            root.close();

            asyncTask.start();
            asyncTask.join();

            assertThat(tagValue.get()).isEqualTo("rootValue");
        }

        @Test
        void verifyDownPropagationForChildrenOnSameThreadWithRootClosed() {
            lenient().doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            root.setData("tag", "rootValue");
            root.makeActive();
            root.setData("tag", "invisibleValue");

            AtomicReference<Object> tagValue = new AtomicReference<>();
            Runnable delayedTask = Context.current().wrap(() -> {
                InspectitContextImpl delayedChild = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
                tagValue.set(delayedChild.getData("tag"));
                delayedChild.makeActive();
                delayedChild.close();
            });

            root.close();

            delayedTask.run();

            assertThat(tagValue.get()).isEqualTo("rootValue");
        }

        @Test
        void verifySessionIdDownPropagationViaSessionIdHeaderWhenBaggageAlsoExists() {
            String sessionIdHeader = "Session-Id"; // We use the default header
            String expectedSessionId = "test123456789987654321";
            String baggageSessionId = "remote_session_id=test333456789987654333,wallah=was-geht";
            Map<String, String> headers = new HashMap<>();
            headers.put(sessionIdHeader, expectedSessionId);
            headers.put(BaggagePropagation.BAGGAGE_HEADER, baggageSessionId);

            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxA.readDownPropagationHeaders(headers);
            ctxA.makeActive();

            assertThat(ctxA.getData(REMOTE_SESSION_ID)).isEqualTo(expectedSessionId);

            ctxA.close();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }
    }

    @Nested
    public class UpPropagation {

        @Test
        void verifyNewValuesPropagatedWhenConfigured() {
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(eq("tag1"));
            doReturn(false).when(propagation).isPropagatedUpWithinJVM(eq("tag2"));

            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxA.makeActive();
            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxB.makeActive();
            InspectitContextImpl ctxC = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxC.makeActive();

            ctxC.setData("tag1", "ctxC_value1");
            ctxC.setData("tag2", "ctxC_value2");

            assertThat(ctxA.getData("tag1")).isNull();
            assertThat(ctxA.getData("tag2")).isNull();

            assertThat(ctxB.getData("tag1")).isNull();
            assertThat(ctxB.getData("tag2")).isNull();

            assertThat(ctxC.getData("tag1")).isEqualTo("ctxC_value1");
            assertThat(ctxC.getData("tag2")).isEqualTo("ctxC_value2");

            ctxC.close();

            assertThat(ctxA.getData("tag1")).isNull();
            assertThat(ctxA.getData("tag2")).isNull();

            assertThat(ctxB.getData("tag1")).isEqualTo("ctxC_value1");
            assertThat(ctxB.getData("tag2")).isNull();

            ctxB.close();

            assertThat(ctxA.getData("tag1")).isEqualTo("ctxC_value1");
            assertThat(ctxA.getData("tag2")).isNull();

            ctxA.close();

            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyNoUpPropagationForChildrenOnDifferentThreadWithRootNotClosed() throws Exception {
            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            root.setData("tag", "rootValue");
            root.makeActive();

            Thread asyncTask = new Thread(Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
                asyncChild.setData("tag", "asyncChildValue");
                asyncChild.makeActive();
                asyncChild.close();
            }));

            asyncTask.start();
            asyncTask.join();

            root.close();

            assertThat(root.getData("tag")).isEqualTo("rootValue");
        }

        @Test
        void verifyNoUpPropagationForChildrenOnDifferentThreadWithRootClosed() throws Exception {
            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            root.setData("tag", "rootValue");
            root.makeActive();

            Thread asyncTask = new Thread(Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
                asyncChild.setData("tag", "asyncChildValue");
                asyncChild.makeActive();
                asyncChild.close();
            }));

            root.close();

            asyncTask.start();
            asyncTask.join();

            assertThat(root.getData("tag")).isEqualTo("rootValue");
        }

        @Test
        void verifyNoUpPropagationForChildrenOnSameThreadWithRootClosed() {
            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            root.setData("tag", "rootValue");
            root.makeActive();

            Runnable delayedTask = Context.current().wrap(() -> {
                InspectitContextImpl delayedChild = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
                delayedChild.setData("tag", "asyncChildValue");
                delayedChild.makeActive();
                delayedChild.close();
            });

            root.close();

            delayedTask.run();

            assertThat(root.getData("tag")).isEqualTo("rootValue");
        }
    }

    @Nested
    public class UpAndDownPropagation {

        @Test
        void verifyComplexTracePropagation() {
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(eq("tag1"));
            doReturn(false).when(propagation).isPropagatedUpWithinJVM(eq("tag2"));
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(eq("tag1"));
            doReturn(false).when(propagation).isPropagatedDownWithinJVM(eq("tag2"));

            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxA.setData("tag2", "ctxA_value2");
            ctxA.makeActive();
            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxB.setData("tag2", "ctxB_value2");
            ctxB.makeActive();
            InspectitContextImpl ctxC = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxC.setData("tag2", "ctxC_value2");
            ctxC.setData("tag1", "ctxC_value1");
            ctxC.makeActive();

            assertThat(ctxA.getData("tag1")).isNull();
            assertThat(ctxA.getData("tag2")).isEqualTo("ctxA_value2");

            assertThat(ctxB.getData("tag1")).isNull();
            assertThat(ctxB.getData("tag2")).isEqualTo("ctxB_value2");

            assertThat(ctxC.getData("tag1")).isEqualTo("ctxC_value1");
            assertThat(ctxC.getData("tag2")).isEqualTo("ctxC_value2");

            ctxC.close();

            assertThat(ctxA.getData("tag1")).isNull();
            assertThat(ctxA.getData("tag2")).isEqualTo("ctxA_value2");

            assertThat(ctxB.getData("tag1")).isEqualTo("ctxC_value1");
            assertThat(ctxB.getData("tag2")).isEqualTo("ctxB_value2");

            InspectitContextImpl ctxC2 = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            ctxC2.makeActive();

            assertThat(ctxB.getData("tag1")).isEqualTo("ctxC_value1");
            //up propagation is visible to newly opened synchronous children
            assertThat(ctxC2.getData("tag1")).isEqualTo("ctxC_value1");
            assertThat(ctxC2.getData("tag2")).isNull();

            ctxC2.setData("tag1", "ctxC2_value1");
            ctxC2.close();

            assertThat(ctxB.getData("tag1")).isEqualTo("ctxC2_value1");
            assertThat(ctxB.getData("tag2")).isEqualTo("ctxB_value2");

            ctxB.close();

            assertThat(ctxA.getData("tag1")).isEqualTo("ctxC2_value1");
            assertThat(ctxA.getData("tag2")).isEqualTo("ctxA_value2");

            ctxA.close();

            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyUpPropagatedValuesInvisibleForChildrenOnDifferentThreadWithRootNotClosed() throws Exception {
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(any());
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            root.setData("tag", "rootValue");
            root.makeActive();

            InspectitContextImpl syncChild = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            syncChild.setData("tag", "syncChildValue");
            syncChild.makeActive();
            syncChild.close();

            AtomicReference<Object> asyncTaskTagValue = new AtomicReference<>();
            Thread asyncTask = new Thread(Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
                asyncTaskTagValue.set(asyncChild.getData("tag"));
                asyncChild.makeActive();
                asyncChild.close();
            }));

            asyncTask.start();
            asyncTask.join();

            root.close();

            assertThat(asyncTaskTagValue.get()).isEqualTo("rootValue");
            assertThat(root.getData("tag")).isEqualTo("syncChildValue");
        }

        @Test
        void verifyUpPropagatedValuesInvisibleForChildrenOnDifferentThreadWithRootClosed() throws Exception {
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(any());
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            root.setData("tag", "rootValue");
            root.makeActive();

            InspectitContextImpl syncChild = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            syncChild.setData("tag", "syncChildValue");
            syncChild.makeActive();
            syncChild.close();

            AtomicReference<Object> asyncTaskTagValue = new AtomicReference<>();
            Thread asyncTask = new Thread(Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
                asyncTaskTagValue.set(asyncChild.getData("tag"));
                asyncChild.makeActive();
                asyncChild.close();
            }));

            root.close();

            asyncTask.start();
            asyncTask.join();

            assertThat(asyncTaskTagValue.get()).isEqualTo("rootValue");
            assertThat(root.getData("tag")).isEqualTo("syncChildValue");
        }

        @Test
        void verifyUpPropagatedValuesInvisibleForChildrenOnSameThreadWithRootClosed() {
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(any());
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            root.setData("tag", "rootValue");
            root.makeActive();

            InspectitContextImpl syncChild = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
            syncChild.setData("tag", "syncChildValue");
            syncChild.makeActive();
            syncChild.close();

            AtomicReference<Object> asyncTaskTagValue = new AtomicReference<>();
            Runnable asyncTask = Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, false);
                asyncTaskTagValue.set(asyncChild.getData("tag"));
                asyncChild.makeActive();
                asyncChild.close();
            });

            root.close();

            asyncTask.run();

            assertThat(asyncTaskTagValue.get()).isEqualTo("rootValue");
            assertThat(root.getData("tag")).isEqualTo("syncChildValue");
        }
    }

    @Nested
    public class BaggageDownPropagation {

        @Test
        void verifyAttributesExtractedOnRoot() {
            Baggage baggage = Baggage.builder()
                    .put("myTag", "myValue")
                    .build();
            try (Scope scope = baggage.makeCurrent()) {
                InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
                assertThat(ctxA.getData("myTag")).isEqualTo("myValue");

                ctxA.makeActive();

                assertThat(getCurrentAttributesAsMap()).hasSize(1);
                assertThat(getCurrentAttributesAsMap()).containsEntry("myTag", "myValue");

                ctxA.close();
            }

            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyAttributePropagationPreservedOnRoot() {
            Baggage baggage = Baggage.builder()
                    .put("myTag", "myValue")
                    .build();
            try (Scope scope = baggage.makeCurrent()) {
                InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
                ctxA.makeActive();

                InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
                ctxB.makeActive();

                assertThat(getCurrentAttributesAsMap()).hasSize(1);
                assertThat(getCurrentAttributesAsMap()).containsEntry("myTag", "myValue");

                ctxB.close();
                ctxA.close();
            }

            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyAttributesExtractedWithinTrace() {
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
            root.setData("rootKey", "rootValue");

            root.makeActive();

            Baggage baggage = Baggage.builder()
                    .put("myTag", "myValue")
                    .build();
            try (Scope scope = baggage.makeCurrent()) {
                InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
                assertThat(ctxA.getData("myTag")).isEqualTo("myValue");

                ctxA.makeActive();
                assertThat(getCurrentAttributesAsMap()).hasSize(2);
                assertThat(getCurrentAttributesAsMap()).containsEntry("myTag", "myValue");
                assertThat(getCurrentAttributesAsMap()).containsEntry("rootKey", "rootValue");

                ctxA.close();
            }

            root.close();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyAttributePropagationPreservedWithinTrace() {
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
            root.setData("rootKey", "rootValue");

            root.makeActive();

            Baggage baggage = Baggage.builder()
                    .put("myTag", "myValue")
                    .build();
            try (Scope scope = baggage.makeCurrent()) {
                InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
                ctxA.makeActive();

                InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
                assertThat(ctxB.getData("myTag")).isEqualTo("myValue");
                ctxB.makeActive();

                assertThat(getCurrentAttributesAsMap()).hasSize(2);
                assertThat(getCurrentAttributesAsMap()).containsEntry("myTag", "myValue");
                assertThat(getCurrentAttributesAsMap()).containsEntry("rootKey", "rootValue");

                ctxB.close();
                ctxA.close();
            }

            root.close();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyDataTypesPreservedWithinTrace() {
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
            root.setData("rootKey", "rootValue");
            root.setData("myTag", "rootValue");
            root.setData("longKey", 42L);

            root.makeActive();

            Baggage baggage = Baggage.current()
                    .toBuilder()
                    .put("myTag", "myValue")
                    .build();
            try (Scope scope = baggage.makeCurrent()) {

                Map<String, String> currentTagsAsMap = getCurrentAttributesAsMap();
                assertThat(currentTagsAsMap).containsEntry("longKey", "42");

                InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
                ctxA.makeActive();
                assertThat(ctxA.getData("myTag")).isEqualTo("myValue");
                assertThat(ctxA.getData("rootKey")).isEqualTo("rootValue");
                assertThat(ctxA.getData("longKey")).isEqualTo(42L);

                ctxA.close();
            }

            root.close();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }

        @Test
        void verifyCommonAttributesPublished() {
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("tagA", "valueA");
            attributes.put("tagB", "valueB");
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(attributes, propagation, sessionStorage, true);
            ctx.makeActive();

            assertThat(getCurrentAttributesAsMap()).hasSize(2);
            assertThat(getCurrentAttributesAsMap()).containsEntry("tagA", "valueA");
            assertThat(getCurrentAttributesAsMap()).containsEntry("tagB", "valueB");

            ctx.close();
            assertThat(ContextUtil.currentInspectitContext()).isNull();
        }
    }

    @Nested
    public class SpanActivation {

        @Test
        void verifySpanAttachedAndDetached() {
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
            Span sp = OpenTelemetryUtils.getTracer().spanBuilder("blub").startSpan();
            ctx.setSpanScope(Context.current().with(sp).makeCurrent());
            ctx.makeActive();

            Span span = Span.current();

            ctx.close();
            Span endSpan = Span.current();

            assertThat(span).isSameAs(sp);
            assertThat(endSpan).isNotSameAs(sp);
        }

    }

    @Nested
    public class SessionStorage {

        private static final String SESSION = "session";

        private static final String KEY = "my-key";

        private static final String VALUE = "my-value";

        private static final String SESSION_VALUE = "my-session-value";

        @Mock
        private PropagationDataStorage dataStorage;

        @Test
        void shouldReadDataFromSessionStorage() {;
            doReturn(dataStorage).when(sessionStorage).getOrCreateDataStorage(SESSION);
            doReturn(SESSION_VALUE).when(dataStorage).readData(KEY);

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
            root.setData(REMOTE_SESSION_ID, SESSION);

            root.makeActive();
            Object value = root.getData(KEY);

            assertThat(value).isEqualTo(SESSION_VALUE);
            root.close();
        }

        @Test
        void shouldKeepContextDataAndUpdateStorageData() {
            Map<String, Object> storageDate = new HashMap<>();
            storageDate.put(KEY, SESSION_VALUE);
            Map<String, Object> expectedUpdate = new HashMap<>();
            expectedUpdate.put(REMOTE_SESSION_ID, SESSION);
            expectedUpdate.put(KEY, VALUE);

            doReturn(dataStorage).when(sessionStorage).getOrCreateDataStorage(SESSION);
            lenient().doReturn(storageDate).when(dataStorage).readData(KEY);

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
            root.setData(KEY, VALUE);
            root.setData(REMOTE_SESSION_ID, SESSION);

            root.makeActive();

            assertThat(root.getData(KEY)).isEqualTo(VALUE);

            root.close();

            assertThat(root.getData(KEY)).isEqualTo(VALUE);
            verify(dataStorage, times(2)).writeData(expectedUpdate);
        }

        @Test
        void shouldNotReadDataWithoutSessionId() {
            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(emptyMap(), propagation, sessionStorage, true);
            root.makeActive();
            root.close();

            verifyNoInteractions(dataStorage);
        }
    }
}
