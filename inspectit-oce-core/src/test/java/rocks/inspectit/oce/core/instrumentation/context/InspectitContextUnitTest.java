package rocks.inspectit.oce.core.instrumentation.context;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedDataProperties;
import rocks.inspectit.oce.core.tags.CommonTagsManager;
import rocks.inspectit.oce.core.testutils.GcUtils;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InspectitContextUnitTest {

    @Mock
    CommonTagsManager commonTags;

    @Mock
    ResolvedDataProperties propagation;

    Map<String, String> getCurrentTagsAsMap() {
        HashMap<String, String> result = new HashMap<>();
        InternalUtils.getTags(Tags.getTagger().getCurrentTagContext())
                .forEachRemaining(t -> result.put(t.getKey().getName(), t.getValue().asString()));
        return result;
    }

    @Nested
    public class DownPropagation {

        @Test
        void verifyCommonTagsExtracted() {
            HashMap<String, String> tags = new HashMap<>();
            tags.put("tagA", "valueA");
            tags.put("tagB", "valueB");
            when(commonTags.getCommonTagValueMap()).thenReturn(tags);

            InspectitContext ctx = InspectitContext.createAndEnter(commonTags, propagation);

            assertThat(ctx.getData("tagA")).isEqualTo("valueA");
            assertThat(ctx.getData("tagB")).isEqualTo("valueB");

            ctx.close();
            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyCommonTagsPropagatedAndOverwritable() {
            HashMap<String, String> tags = new HashMap<>();
            tags.put("tagA", "valueA");
            tags.put("tagB", "valueB");
            when(commonTags.getCommonTagValueMap()).thenReturn(tags);
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);

            InspectitContext ctxA = InspectitContext.createAndEnter(commonTags, propagation);
            ctxA.setData("tagB", "overwritten");
            InspectitContext ctxB = InspectitContext.createAndEnter(commonTags, propagation);

            assertThat(ctxB.getData("tagA")).isEqualTo("valueA");
            assertThat(ctxB.getData("tagB")).isEqualTo("overwritten");

            ctxB.close();
            ctxA.close();

            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyOverwritesAreLocal() {
            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());
            when(propagation.isPropagatedDownWithinJVM(any())).thenReturn(true);

            InspectitContext ctxA = InspectitContext.createAndEnter(commonTags, propagation);
            ctxA.setData("keyA", "ctxA_valueA");
            ctxA.setData("keyB", "ctxA_valueB");
            InspectitContext ctxB = InspectitContext.createAndEnter(commonTags, propagation);
            ctxB.setData("keyB", "ctxB_valueB");
            InspectitContext ctxC = InspectitContext.createAndEnter(commonTags, propagation);

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

            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyOverwritesHappenOnlyWhenConfigured() {
            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(eq("keyA"));
            doReturn(false).when(propagation).isPropagatedDownWithinJVM(eq("keyB"));

            InspectitContext ctxA = InspectitContext.createAndEnter(commonTags, propagation);
            ctxA.setData("keyA", "ctxA_valueA");
            ctxA.setData("keyB", "ctxA_valueB");
            InspectitContext ctxB = InspectitContext.createAndEnter(commonTags, propagation);
            ctxB.setData("keyB", "ctxB_valueB");
            InspectitContext ctxC = InspectitContext.createAndEnter(commonTags, propagation);

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

            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyNoMemoryLeakForAsyncCalls() {

            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());

            InspectitContext firstContext = InspectitContext.createAndEnter(commonTags, propagation);

            InspectitContext upPropagationBarrierContext = InspectitContext.createAndEnter(commonTags, propagation);
            WeakReference<InspectitContext> firstContextWeak = new WeakReference<>(firstContext);

            InspectitContext asyncContext = InspectitContext.createAndEnter(commonTags, propagation);

            upPropagationBarrierContext.close();
            firstContext.close();
            firstContext = null;

            GcUtils.waitUntilCleared(firstContextWeak);
            asyncContext.close();

        }

    }


    @Nested
    public class UpPropagation {

        @Test
        void verifyNewValuesPropagatedWhenConfigured() {
            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(eq("tag1"));
            doReturn(false).when(propagation).isPropagatedUpWithinJVM(eq("tag2"));

            InspectitContext ctxA = InspectitContext.createAndEnter(commonTags, propagation);
            InspectitContext ctxB = InspectitContext.createAndEnter(commonTags, propagation);
            InspectitContext ctxC = InspectitContext.createAndEnter(commonTags, propagation);

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

            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }
    }


    @Nested
    public class UpAndDownPropagation {

        @Test
        void verifyComplexTracePropagation() {
            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(eq("tag1"));
            doReturn(false).when(propagation).isPropagatedUpWithinJVM(eq("tag2"));
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(eq("tag1"));
            doReturn(false).when(propagation).isPropagatedDownWithinJVM(eq("tag2"));

            InspectitContext ctxA = InspectitContext.createAndEnter(commonTags, propagation);
            ctxA.setData("tag2", "ctxA_value2");
            InspectitContext ctxB = InspectitContext.createAndEnter(commonTags, propagation);
            ctxB.setData("tag2", "ctxB_value2");
            InspectitContext ctxC = InspectitContext.createAndEnter(commonTags, propagation);
            ctxC.setData("tag2", "ctxC_value2");

            ctxC.setData("tag1", "ctxC_value1");

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

            InspectitContext ctxC2 = InspectitContext.createAndEnter(commonTags, propagation);

            assertThat(ctxB.getData("tag1")).isEqualTo("ctxC_value1");
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

            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }
    }

    @Nested
    public class TagContextDownPropagation {

        @Test
        void verifyTagsExtractedOnRoot() {
            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isTag(any());

            TagContextBuilder tcb = Tags.getTagger().emptyBuilder()
                    .put(TagKey.create("myTag"), TagValue.create("myValue"));
            try (Scope tc = tcb.buildScoped()) {
                InspectitContext ctxA = InspectitContext.createAndEnter(commonTags, propagation);
                assertThat(ctxA.getData("myTag")).isEqualTo("myValue");

                ctxA.enterTagContextWithOnlyDownPropagatedData();
                assertThat(getCurrentTagsAsMap()).hasSize(1);
                assertThat(getCurrentTagsAsMap()).containsEntry("myTag", "myValue");
                ctxA.exitTagContextWithOnlyDownPropagation();

                ctxA.close();
            }

            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }

        @Test
        void verifyTagsExtractedWithinTrace() {
            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isTag(any());

            InspectitContext root = InspectitContext.createAndEnter(commonTags, propagation);
            root.setData("rootKey", "rootValue");

            root.enterTagContextWithOnlyDownPropagatedData();

            TagContextBuilder tcb = Tags.getTagger().emptyBuilder()
                    .put(TagKey.create("myTag"), TagValue.create("myValue"));
            try (Scope tc = tcb.buildScoped()) {
                InspectitContext ctxA = InspectitContext.createAndEnter(commonTags, propagation);
                assertThat(ctxA.getData("myTag")).isEqualTo("myValue");

                ctxA.enterTagContextWithOnlyDownPropagatedData();
                assertThat(getCurrentTagsAsMap()).hasSize(2);
                assertThat(getCurrentTagsAsMap()).containsEntry("myTag", "myValue");
                assertThat(getCurrentTagsAsMap()).containsEntry("rootKey", "rootValue");
                ctxA.exitTagContextWithOnlyDownPropagation();

                ctxA.close();
            }

            root.exitTagContextWithOnlyDownPropagation();
            root.close();
            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyDataOnlyPublishedAsTagWhenConfigured() {
            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isTag(eq("my_tag"));

            InspectitContext root = InspectitContext.createAndEnter(commonTags, propagation);
            root.setData("my_tag", "tagValue");
            root.setData("my_hidden", "hiddenValue");

            root.enterTagContextWithOnlyDownPropagatedData();

            assertThat(getCurrentTagsAsMap()).hasSize(1);
            assertThat(getCurrentTagsAsMap()).containsEntry("my_tag", "tagValue");

            root.exitTagContextWithOnlyDownPropagation();
            root.close();
            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyDataTypesPreservedWithinTrace() {
            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isTag(any());

            InspectitContext root = InspectitContext.createAndEnter(commonTags, propagation);
            root.setData("rootKey", "rootValue");
            root.setData("myTag", "rootValue");
            root.setData("longKey", 42L);

            root.enterTagContextWithOnlyDownPropagatedData();

            TagContextBuilder tcb = Tags.getTagger().currentBuilder()
                    .put(TagKey.create("myTag"), TagValue.create("myValue"));
            try (Scope tc = tcb.buildScoped()) {

                Map<String, String> currentTagsAsMap = getCurrentTagsAsMap();
                assertThat(currentTagsAsMap).containsEntry("longKey", "42");

                InspectitContext ctxA = InspectitContext.createAndEnter(commonTags, propagation);
                assertThat(ctxA.getData("myTag")).isEqualTo("myValue");
                assertThat(ctxA.getData("rootKey")).isEqualTo("rootValue");
                assertThat(ctxA.getData("longKey")).isEqualTo(42L);

                ctxA.close();
            }

            root.exitTagContextWithOnlyDownPropagation();
            root.close();
            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyCommonTagsPublished() {
            HashMap<String, String> tags = new HashMap<>();
            tags.put("tagA", "valueA");
            tags.put("tagB", "valueB");
            when(commonTags.getCommonTagValueMap()).thenReturn(tags);
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isTag(any());

            InspectitContext ctx = InspectitContext.createAndEnter(commonTags, propagation);
            ctx.enterTagContextWithOnlyDownPropagatedData();

            assertThat(getCurrentTagsAsMap()).hasSize(2);
            assertThat(getCurrentTagsAsMap()).containsEntry("tagA", "valueA");
            assertThat(getCurrentTagsAsMap()).containsEntry("tagB", "valueB");

            ctx.exitTagContextWithOnlyDownPropagation();
            ctx.close();
            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyLocalValuesPublishedCorrectly() {
            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());
            doReturn(false).when(propagation).isPropagatedDownWithinJVM(eq("local"));
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(eq("global"));
            doReturn(true).when(propagation).isTag(any());

            InspectitContext ctx = InspectitContext.createAndEnter(commonTags, propagation);
            ctx.setData("local", "localValue");
            ctx.setData("global", "globalValue");

            ctx.enterTagContextWithOnlyDownPropagatedData();

            assertThat(getCurrentTagsAsMap()).hasSize(1);
            assertThat(getCurrentTagsAsMap()).containsEntry("global", "globalValue");

            ctx.exitTagContextWithOnlyDownPropagation();
            try (Scope scope = ctx.enterTagScopeWithAllData()) {
                assertThat(getCurrentTagsAsMap()).hasSize(2);
                assertThat(getCurrentTagsAsMap()).containsEntry("local", "localValue");
                assertThat(getCurrentTagsAsMap()).containsEntry("global", "globalValue");
            }

            ctx.close();
            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }


        @Test
        void verifyUpPropagatedValuesAvailable() {
            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(any());
            doReturn(true).when(propagation).isTag(any());

            InspectitContext ctx = InspectitContext.createAndEnter(commonTags, propagation);
            ctx.setData("rootKey1", "rootValue1");
            ctx.setData("rootKey2", "rootValue2");

            ctx.enterTagContextWithOnlyDownPropagatedData();

            assertThat(getCurrentTagsAsMap()).hasSize(2);
            assertThat(getCurrentTagsAsMap()).containsEntry("rootKey1", "rootValue1");
            assertThat(getCurrentTagsAsMap()).containsEntry("rootKey2", "rootValue2");

            InspectitContext nested = InspectitContext.createAndEnter(commonTags, propagation);
            nested.setData("rootKey1", "nestedValue1");
            nested.setData("nestedKey2", "nestedValue2");
            nested.close();

            assertThat(getCurrentTagsAsMap()).hasSize(3);
            assertThat(getCurrentTagsAsMap()).containsEntry("rootKey1", "nestedValue1");
            assertThat(getCurrentTagsAsMap()).containsEntry("rootKey2", "rootValue2");
            assertThat(getCurrentTagsAsMap()).containsEntry("nestedKey2", "nestedValue2");

            ctx.exitTagContextWithOnlyDownPropagation();
            ctx.close();
            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }

        @Test
        void verifyUpPropagatedInterferenceHandled() {
            //this test case test how the context deals with the problem that values cannot be upropagated correctly
            // due to the fact that a custom user tagcontext was opened in between
            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());

            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isPropagatedUpWithinJVM(any());
            doReturn(true).when(propagation).isTag(any());

            InspectitContext ctx = InspectitContext.createAndEnter(commonTags, propagation);
            ctx.setData("rootKey1", "rootValue1");
            ctx.setData("rootKey2", "rootValue2");

            ctx.enterTagContextWithOnlyDownPropagatedData();

            try (Scope ts = Tags.getTagger().currentBuilder().put(TagKey.create("customKey"), TagValue.create("customValue")).buildScoped()) {
                InspectitContext nested = InspectitContext.createAndEnter(commonTags, propagation);
                nested.setData("rootKey1", "nestedValue1");
                nested.setData("nestedKey2", "nestedValue2");
                nested.close();

                InspectitContext nested2 = InspectitContext.createAndEnter(commonTags, propagation);

                nested2.enterTagContextWithOnlyDownPropagatedData();
                assertThat(getCurrentTagsAsMap()).hasSize(4);
                assertThat(getCurrentTagsAsMap()).containsEntry("rootKey1", "nestedValue1");
                assertThat(getCurrentTagsAsMap()).containsEntry("rootKey2", "rootValue2");
                assertThat(getCurrentTagsAsMap()).containsEntry("nestedKey2", "nestedValue2");
                assertThat(getCurrentTagsAsMap()).containsEntry("customKey", "customValue");

                nested2.exitTagContextWithOnlyDownPropagation();
                nested2.close();

            }


            ctx.exitTagContextWithOnlyDownPropagation();
            ctx.close();
            assertThat(InspectitContext.INSPECTIT_KEY.get()).isNull();
        }

        @Test
        void verifyOnlyPrimitiveDataUsedAsTag() {
            when(commonTags.getCommonTagValueMap()).thenReturn(Collections.emptyMap());
            doReturn(true).when(propagation).isPropagatedDownWithinJVM(any());
            doReturn(true).when(propagation).isTag(any());

            InspectitContext ctx = InspectitContext.createAndEnter(commonTags, propagation);
            ctx.setData("d1", "string");
            ctx.setData("d2", 1);
            ctx.setData("d3", 2L);
            ctx.setData("d4", 2.0);
            ctx.setData("d5", new HashMap<>());

            ctx.enterTagContextWithOnlyDownPropagatedData();

            assertThat(getCurrentTagsAsMap()).hasSize(4);
            assertThat(getCurrentTagsAsMap()).containsEntry("d1", "string");
            assertThat(getCurrentTagsAsMap()).containsEntry("d2", "1");
            assertThat(getCurrentTagsAsMap()).containsEntry("d3", "2");
            assertThat(getCurrentTagsAsMap()).containsEntry("d4", "2.0");

            ctx.exitTagContextWithOnlyDownPropagation();
            try (Scope scope = ctx.enterTagScopeWithAllData()) {

                assertThat(getCurrentTagsAsMap()).hasSize(4);
                assertThat(getCurrentTagsAsMap()).containsEntry("d1", "string");
                assertThat(getCurrentTagsAsMap()).containsEntry("d2", "1");
                assertThat(getCurrentTagsAsMap()).containsEntry("d3", "2");
                assertThat(getCurrentTagsAsMap()).containsEntry("d4", "2.0");
            }
            ctx.close();


        }
    }


}
