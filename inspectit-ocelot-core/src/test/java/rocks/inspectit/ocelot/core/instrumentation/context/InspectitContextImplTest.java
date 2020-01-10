package rocks.inspectit.ocelot.core.instrumentation.context;

import io.grpc.Context;
import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import io.opencensus.trace.Span;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracing;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.bootstrap.correlation.noop.NoopLogTraceCorrelator;
import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;
import rocks.inspectit.ocelot.core.testutils.GcUtils;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InspectitContextImplTest {

    @Mock
    PropagationMetaData propagation;

    Map<String, String> getCurrentTagsAsMap() {
        HashMap<String, String> result = new HashMap<>();
        InternalUtils.getTags(Tags.getTagger().getCurrentTagContext())
                .forEachRemaining(t -> result.put(t.getKey().getName(), t.getValue().asString()));
        return result;
    }

    @Nested
    public class GetAndClearCurrentRemoteSpanContext {

        @Test
        void verifyNullIfNoSpanSet() {
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(new HashMap<>(), propagation, false);

            assertThat(ctx.getAndClearCurrentRemoteSpanContext()).isNull();
        }

        @Test
        void verifyCleared() {
            SpanContext span = Tracing.getTracer().spanBuilder("blub").startSpan().getContext();
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(new HashMap<>(), propagation, false);
            ctx.setData(InternalInspectitContext.REMOTE_PARENT_SPAN_CONTEXT_KEY, span);

            SpanContext result = ctx.getAndClearCurrentRemoteSpanContext();

            assertThat(ctx.getAndClearCurrentRemoteSpanContext()).isNull();
            assertThat(result).isSameAs(span);
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
            Span mySpan = Tracing.getTracer().spanBuilder("blub").startSpan();

            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(new HashMap<>(), propagation, false);
            assertThat(ctx.enteredSpan()).isFalse();

            ctx.enterSpan(mySpan);

            verify(traceCorrelator).startCorrelatedSpanScope(any());

            ctx.makeActive();

            assertThat(ctx.enteredSpan()).isTrue();
            assertThat(Tracing.getTracer().getCurrentSpan()).isSameAs(mySpan);

            ctx.close();

            assertThat(Tracing.getTracer().getCurrentSpan()).isNotSameAs(mySpan);
        }


    }


    @Nested
    public class DownPropagation {

        @Test
        void verifyCommonTagsExtracted() {
            HashMap<String, String> tags = new HashMap<>();
            tags.put("tagA", "valueA");
            tags.put("tagB", "valueB");

            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(tags, propagation, false);
            ctx.makeActive();

            assertThat(ctx.getData("tagA")).isEqualTo("valueA");
            assertThat(ctx.getData("tagB")).isEqualTo("valueB");

            ctx.close();
            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyCommonTagsPropagatedAndOverwritable() {
            HashMap<String, String> tags = new HashMap<>();
            tags.put("tagA", "valueA");
            tags.put("tagB", "valueB");
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);

            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(tags, propagation, false);
            ctxA.setData("tagB", "overwritten");
            ctxA.makeActive();

            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(tags, propagation, false);
            ctxB.makeActive();

            assertThat(ctxB.getData("tagA")).isEqualTo("valueA");
            assertThat(ctxB.getData("tagB")).isEqualTo("overwritten");

            ctxB.close();
            ctxA.close();

            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyOverwritesAreLocal() {
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);

            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxA.setData("keyA", "ctxA_valueA");
            ctxA.setData("keyB", "ctxA_valueB");
            ctxA.makeActive();

            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxB.setData("keyB", "ctxB_valueB");
            ctxB.makeActive();

            InspectitContextImpl ctxC = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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

            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyOverwritesHappenOnlyWhenConfigured() {
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(eq("keyA"));
            doReturn(false).when(propagation).isPropagatedDownWithinJVM(eq("keyB"));

            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxA.setData("keyA", "ctxA_valueA");
            ctxA.setData("keyB", "ctxA_valueB");
            ctxA.makeActive();

            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxB.setData("keyB", "ctxB_valueB");
            ctxB.makeActive();

            InspectitContextImpl ctxC = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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

            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyContextReleasedWhenAllChildrenAreClosed() {

            InspectitContextImpl firstContext = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            firstContext.makeActive();

            InspectitContextImpl secondContext = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            secondContext.makeActive();
            WeakReference<InspectitContextImpl> firstContextWeak = new WeakReference<>(firstContext);

            InspectitContextImpl openRemainingContext = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);

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

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            root.setData("tag", "rootValue");
            root.makeActive();
            root.setData("tag", "invisibleValue");

            AtomicReference<Object> tagValue = new AtomicReference<>();
            Thread asyncTask = new Thread(Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            root.setData("tag", "rootValue");
            root.makeActive();
            root.setData("tag", "invisibleValue");

            AtomicReference<Object> tagValue = new AtomicReference<>();
            Thread asyncTask = new Thread(Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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
        void verifyDownPropagationForChildrenOnSameThreadWithRootClosed() throws Exception {
            lenient().doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            root.setData("tag", "rootValue");
            root.makeActive();
            root.setData("tag", "invisibleValue");

            AtomicReference<Object> tagValue = new AtomicReference<>();
            Runnable delayedTask = Context.current().wrap(() -> {
                InspectitContextImpl delayedChild = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
                tagValue.set(delayedChild.getData("tag"));
                delayedChild.makeActive();
                delayedChild.close();
            });

            root.close();

            delayedTask.run();

            assertThat(tagValue.get()).isEqualTo("rootValue");
        }

    }


    @Nested
    public class UpPropagation {

        @Test
        void verifyNewValuesPropagatedWhenConfigured() {
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(eq("tag1"));
            doReturn(false).when(propagation).isPropagatedUpWithinJVM(eq("tag2"));

            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxA.makeActive();
            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxB.makeActive();
            InspectitContextImpl ctxC = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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

            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyNoUpPropagationForChildrenOnDifferentThreadWithRootNotClosed() throws Exception {
            lenient().doReturn(true).when(propagation).isPropagatedUpWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            root.setData("tag", "rootValue");
            root.makeActive();

            Thread asyncTask = new Thread(Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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
            lenient().doReturn(true).when(propagation).isPropagatedUpWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            root.setData("tag", "rootValue");
            root.makeActive();

            Thread asyncTask = new Thread(Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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
        void verifyNoUpPropagationForChildrenOnSameThreadWithRootClosed() throws Exception {
            lenient().doReturn(true).when(propagation).isPropagatedUpWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            root.setData("tag", "rootValue");
            root.makeActive();

            Runnable delayedTask = Context.current().wrap(() -> {
                InspectitContextImpl delayedChild = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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

            InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxA.setData("tag2", "ctxA_value2");
            ctxA.makeActive();
            InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            ctxB.setData("tag2", "ctxB_value2");
            ctxB.makeActive();
            InspectitContextImpl ctxC = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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

            InspectitContextImpl ctxC2 = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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

            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }

        @Test
        void verifyUpPropagatedValuesInvisibleForChildrenOnDifferentThreadWithRootNotClosed() throws Exception {
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(any());
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            root.setData("tag", "rootValue");
            root.makeActive();

            InspectitContextImpl syncChild = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            syncChild.setData("tag", "syncChildValue");
            syncChild.makeActive();
            syncChild.close();

            AtomicReference<Object> asyncTaskTagValue = new AtomicReference<>();
            Thread asyncTask = new Thread(Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            root.setData("tag", "rootValue");
            root.makeActive();

            InspectitContextImpl syncChild = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            syncChild.setData("tag", "syncChildValue");
            syncChild.makeActive();
            syncChild.close();

            AtomicReference<Object> asyncTaskTagValue = new AtomicReference<>();
            Thread asyncTask = new Thread(Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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
        void verifyUpPropagatedValuesInvisibleForChildrenOnSameThreadWithRootClosed() throws Exception {
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(any());
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            root.setData("tag", "rootValue");
            root.makeActive();

            InspectitContextImpl syncChild = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
            syncChild.setData("tag", "syncChildValue");
            syncChild.makeActive();
            syncChild.close();

            AtomicReference<Object> asyncTaskTagValue = new AtomicReference<>();
            Runnable asyncTask = Context.current().wrap(() -> {
                InspectitContextImpl asyncChild = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, false);
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
    public class TagContextDownPropagation {

        @Test
        void verifyTagsExtractedOnRoot() {
            doAnswer((invocation) -> PropagationMetaData.builder()).when(propagation).copy();

            TagContextBuilder tcb = Tags.getTagger().emptyBuilder()
                    .put(TagKey.create("myTag"), TagValue.create("myValue"));
            try (Scope tc = tcb.buildScoped()) {
                InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
                assertThat(ctxA.getData("myTag")).isEqualTo("myValue");

                ctxA.makeActive();

                assertThat(getCurrentTagsAsMap()).hasSize(1);
                assertThat(getCurrentTagsAsMap()).containsEntry("myTag", "myValue");

                ctxA.close();
            }

            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyTagPropagationPreservedOnRoot() {
            doAnswer((invocation) -> PropagationMetaData.builder()).when(propagation).copy();

            TagContextBuilder tcb = Tags.getTagger().emptyBuilder()
                    .put(TagKey.create("myTag"), TagValue.create("myValue"));
            try (Scope tc = tcb.buildScoped()) {
                InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
                ctxA.makeActive();

                InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
                ctxB.makeActive();

                assertThat(getCurrentTagsAsMap()).hasSize(1);
                assertThat(getCurrentTagsAsMap()).containsEntry("myTag", "myValue");

                ctxB.close();
                ctxA.close();
            }

            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }

        @Test
        void verifyTagsExtractedWithinTrace() {
            doAnswer((invocation) -> PropagationMetaData.builder()
                    .setTag("rootKey", true)
                    .setDownPropagation("rootKey", PropagationMode.JVM_LOCAL)).when(propagation).copy();
            doReturn(true).when(propagation).isTag(eq("rootKey"));
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(eq("rootKey"));

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
            root.setData("rootKey", "rootValue");

            root.makeActive();

            TagContextBuilder tcb = Tags.getTagger().emptyBuilder()
                    .put(TagKey.create("myTag"), TagValue.create("myValue"));
            try (Scope tc = tcb.buildScoped()) {
                InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
                assertThat(ctxA.getData("myTag")).isEqualTo("myValue");

                ctxA.makeActive();
                assertThat(getCurrentTagsAsMap()).hasSize(2);
                assertThat(getCurrentTagsAsMap()).containsEntry("myTag", "myValue");
                assertThat(getCurrentTagsAsMap()).containsEntry("rootKey", "rootValue");

                ctxA.close();
            }

            root.close();
            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }

        @Test
        void verifyTagPropagationPreservedWithinTrace() {
            doAnswer((invocation) -> PropagationMetaData.builder()
                    .setTag("rootKey", true)
                    .setDownPropagation("rootKey", PropagationMode.JVM_LOCAL)).when(propagation).copy();
            doReturn(true).when(propagation).isTag(eq("rootKey"));
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(eq("rootKey"));

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
            root.setData("rootKey", "rootValue");

            root.makeActive();

            TagContextBuilder tcb = Tags.getTagger().emptyBuilder()
                    .put(TagKey.create("myTag"), TagValue.create("myValue"));
            try (Scope tc = tcb.buildScoped()) {
                InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
                ctxA.makeActive();

                InspectitContextImpl ctxB = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
                assertThat(ctxB.getData("myTag")).isEqualTo("myValue");
                ctxB.makeActive();

                assertThat(getCurrentTagsAsMap()).hasSize(2);
                assertThat(getCurrentTagsAsMap()).containsEntry("myTag", "myValue");
                assertThat(getCurrentTagsAsMap()).containsEntry("rootKey", "rootValue");

                ctxB.close();
                ctxA.close();
            }

            root.close();
            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyDataOnlyPublishedAsTagWhenConfigured() {
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isTag(eq("my_tag"));

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
            root.setData("my_tag", "tagValue");
            root.setData("my_hidden", "hiddenValue");

            root.makeActive();

            assertThat(getCurrentTagsAsMap()).hasSize(1);
            assertThat(getCurrentTagsAsMap()).containsEntry("my_tag", "tagValue");

            root.close();
            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyDataTypesPreservedWithinTrace() {
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isTag(any());

            InspectitContextImpl root = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
            root.setData("rootKey", "rootValue");
            root.setData("myTag", "rootValue");
            root.setData("longKey", 42L);

            root.makeActive();

            TagContextBuilder tcb = Tags.getTagger().currentBuilder()
                    .put(TagKey.create("myTag"), TagValue.create("myValue"));
            try (Scope tc = tcb.buildScoped()) {

                Map<String, String> currentTagsAsMap = getCurrentTagsAsMap();
                assertThat(currentTagsAsMap).containsEntry("longKey", "42");

                InspectitContextImpl ctxA = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
                ctxA.makeActive();
                assertThat(ctxA.getData("myTag")).isEqualTo("myValue");
                assertThat(ctxA.getData("rootKey")).isEqualTo("rootValue");
                assertThat(ctxA.getData("longKey")).isEqualTo(42L);

                ctxA.close();
            }

            root.close();
            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyCommonTagsPublished() {
            HashMap<String, String> tags = new HashMap<>();
            tags.put("tagA", "valueA");
            tags.put("tagB", "valueB");
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isTag(any());

            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(tags, propagation, true);
            ctx.makeActive();

            assertThat(getCurrentTagsAsMap()).hasSize(2);
            assertThat(getCurrentTagsAsMap()).containsEntry("tagA", "valueA");
            assertThat(getCurrentTagsAsMap()).containsEntry("tagB", "valueB");

            ctx.close();
            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyLocalValuesPublishedCorrectly() {
            doReturn(false).when(propagation).isPropagatedDownWithinJVM(eq("local"));
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(eq("global"));
            doReturn(true).when(propagation).isTag(any());

            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
            ctx.setData("local", "localValue");
            ctx.setData("global", "globalValue");

            ctx.makeActive();

            assertThat(getCurrentTagsAsMap()).hasSize(1);
            assertThat(getCurrentTagsAsMap()).containsEntry("global", "globalValue");

            try (Scope scope = ctx.enterFullTagScope()) {
                assertThat(getCurrentTagsAsMap()).hasSize(2);
                assertThat(getCurrentTagsAsMap()).containsEntry("local", "localValue");
                assertThat(getCurrentTagsAsMap()).containsEntry("global", "globalValue");
            }

            ctx.close();
            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyUpPropagatedValuesOnlyAvailableInFullTagScope() {
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(any());
            doReturn(true).when(propagation).isTag(any());

            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
            ctx.setData("rootKey1", "rootValue1");
            ctx.setData("rootKey2", "rootValue2");

            ctx.makeActive();

            assertThat(getCurrentTagsAsMap()).hasSize(2);
            assertThat(getCurrentTagsAsMap()).containsEntry("rootKey1", "rootValue1");
            assertThat(getCurrentTagsAsMap()).containsEntry("rootKey2", "rootValue2");

            InspectitContextImpl nested = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
            nested.makeActive();
            nested.setData("rootKey1", "nestedValue1");
            nested.setData("nestedKey2", "nestedValue2");
            nested.close();

            assertThat(getCurrentTagsAsMap()).hasSize(2);
            assertThat(getCurrentTagsAsMap()).containsEntry("rootKey1", "rootValue1");
            assertThat(getCurrentTagsAsMap()).containsEntry("rootKey2", "rootValue2");

            try (Scope sc = ctx.enterFullTagScope()) {

                assertThat(getCurrentTagsAsMap()).hasSize(3);
                assertThat(getCurrentTagsAsMap()).containsEntry("rootKey1", "nestedValue1");
                assertThat(getCurrentTagsAsMap()).containsEntry("rootKey2", "rootValue2");
                assertThat(getCurrentTagsAsMap()).containsEntry("nestedKey2", "nestedValue2");
            }

            ctx.close();
            assertThat(InspectitContextImpl.INSPECTIT_KEY.get()).isNull();
        }

        @Test
        void verifyOnlyPrimitiveDataUsedAsTag() {
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isTag(any());

            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
            ctx.setData("d1", "string");
            ctx.setData("d2", 1);
            ctx.setData("d3", 2L);
            ctx.setData("d4", 2.0);
            ctx.setData("d5", new HashMap<>());

            ctx.makeActive();

            assertThat(getCurrentTagsAsMap()).hasSize(4);
            assertThat(getCurrentTagsAsMap()).containsEntry("d1", "string");
            assertThat(getCurrentTagsAsMap()).containsEntry("d2", "1");
            assertThat(getCurrentTagsAsMap()).containsEntry("d3", "2");
            assertThat(getCurrentTagsAsMap()).containsEntry("d4", "2.0");

            try (Scope scope = ctx.enterFullTagScope()) {

                assertThat(getCurrentTagsAsMap()).hasSize(4);
                assertThat(getCurrentTagsAsMap()).containsEntry("d1", "string");
                assertThat(getCurrentTagsAsMap()).containsEntry("d2", "1");
                assertThat(getCurrentTagsAsMap()).containsEntry("d3", "2");
                assertThat(getCurrentTagsAsMap()).containsEntry("d4", "2.0");
            }
            ctx.close();
        }
    }


    @Nested
    public class SpanActivation {

        @Test
        void verifySpanAttachedAndDetached() {
            InspectitContextImpl ctx = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), propagation, true);
            Span sp = Tracing.getTracer().spanBuilder("blub").startSpan();
            ctx.enterSpan(sp);
            ctx.makeActive();

            Span span = Tracing.getTracer().getCurrentSpan();

            ctx.close();
            Span endSpan = Tracing.getTracer().getCurrentSpan();

            assertThat(span).isSameAs(sp);
            assertThat(endSpan).isNotSameAs(sp);
        }

    }


}
