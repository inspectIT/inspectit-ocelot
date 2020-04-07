package rocks.inspectit.ocelot.core.instrumentation.hook.tags;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.tracing.TracingSettings;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonTagsToAttributesActionTest {

    CommonTagsToAttributesAction action;

    @Mock
    CommonTagsManager commonTagsManager;

    @Mock
    Span span;

    @Nested
    class Never {

        @BeforeEach
        void init() {
            action = new CommonTagsToAttributesAction(commonTagsManager, TracingSettings.AddCommonTags.NEVER);
        }

        @Test
        void newSpan() {
            action.writeCommonTags(span, false, false);

            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void remoteParent() {
            action.writeCommonTags(span, true, false);

            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void localParent() {
            action.writeCommonTags(span, false, true);

            verifyNoMoreInteractions(span, commonTagsManager);
        }

    }

    @Nested
    class GlobalRoot {

        @BeforeEach
        void init() {
            action = new CommonTagsToAttributesAction(commonTagsManager, TracingSettings.AddCommonTags.ON_GLOBAL_ROOT);
        }

        @Test
        void newSpan() {
            when(commonTagsManager.getCommonTagValueMap()).thenReturn(Collections.singletonMap("key", "value"));

            action.writeCommonTags(span, false, false);

            verify(span).putAttribute("key", AttributeValue.stringAttributeValue("value"));
            verify(commonTagsManager).getCommonTagValueMap();
            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void remoteParent() {
            action.writeCommonTags(span, true, false);

            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void localParent() {
            action.writeCommonTags(span, false, true);

            verifyNoMoreInteractions(span, commonTagsManager);
        }

    }

    @Nested
    class LocalRoot {

        @BeforeEach
        void init() {
            action = new CommonTagsToAttributesAction(commonTagsManager, TracingSettings.AddCommonTags.ON_LOCAL_ROOT);
        }

        @Test
        void newSpan() {
            when(commonTagsManager.getCommonTagValueMap()).thenReturn(Collections.singletonMap("key", "value"));

            action.writeCommonTags(span, false, false);

            verify(span).putAttribute("key", AttributeValue.stringAttributeValue("value"));
            verify(commonTagsManager).getCommonTagValueMap();
            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void remoteParent() {
            when(commonTagsManager.getCommonTagValueMap()).thenReturn(Collections.singletonMap("key", "value"));

            action.writeCommonTags(span, true, false);

            verify(span).putAttribute("key", AttributeValue.stringAttributeValue("value"));
            verify(commonTagsManager).getCommonTagValueMap();
            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void localParent() {
            action.writeCommonTags(span, false, true);

            verifyNoMoreInteractions(span, commonTagsManager);
        }

    }

    @Nested
    class Always {

        @BeforeEach
        void init() {
            action = new CommonTagsToAttributesAction(commonTagsManager, TracingSettings.AddCommonTags.ALWAYS);
            when(commonTagsManager.getCommonTagValueMap()).thenReturn(Collections.singletonMap("key", "value"));
        }

        @Test
        void newSpan() {
            action.writeCommonTags(span, false, false);

            verify(span).putAttribute("key", AttributeValue.stringAttributeValue("value"));
            verify(commonTagsManager).getCommonTagValueMap();
            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void remoteParent() {
            action.writeCommonTags(span, true, false);

            verify(span).putAttribute("key", AttributeValue.stringAttributeValue("value"));
            verify(commonTagsManager).getCommonTagValueMap();
            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void localParent() {
            action.writeCommonTags(span, false, true);

            verify(span).putAttribute("key", AttributeValue.stringAttributeValue("value"));
            verify(commonTagsManager).getCommonTagValueMap();
            verifyNoMoreInteractions(span, commonTagsManager);
        }

    }


}