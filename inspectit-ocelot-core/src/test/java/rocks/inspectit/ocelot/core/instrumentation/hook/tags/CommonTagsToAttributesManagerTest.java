package rocks.inspectit.ocelot.core.instrumentation.hook.tags;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.tracing.TracingSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonTagsToAttributesManagerTest {

    @InjectMocks
    CommonTagsToAttributesManager manager;

    @Mock
    InspectitEnvironment env;

    @Mock
    CommonTagsManager commonTagsManager;

    @Mock
    Span span;

    @Mock
    TracingSettings tracingSettings;

    @Mock
    InspectitConfig config;

    @BeforeEach
    void init() {
        lenient().when(env.getCurrentConfig()).thenReturn(config);
        lenient().when(config.getTracing()).thenReturn(tracingSettings);
    }

    @Nested
    class Never {

        // never is default

        @Test
        void newSpan() {
            manager.writeCommonTags(span, false, false);

            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void remoteParent() {
            manager.writeCommonTags(span, true, false);

            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void localParent() {
            manager.writeCommonTags(span, false, true);

            verifyNoMoreInteractions(span, commonTagsManager);
        }

    }

    @Nested
    class GlobalRoot {

        @BeforeEach
        void init() {
            when(tracingSettings.getAddCommonTags()).thenReturn(TracingSettings.AddCommonTags.ON_GLOBAL_ROOT);
            manager.update();
        }

        @Test
        void newSpan() {
            when(commonTagsManager.getCommonTagValueMap()).thenReturn(Collections.singletonMap("key", "value"));

            manager.writeCommonTags(span, false, false);

            verify(span).putAttribute("key", AttributeValue.stringAttributeValue("value"));
            verify(commonTagsManager).getCommonTagValueMap();
            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void remoteParent() {
            manager.writeCommonTags(span, true, false);

            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void localParent() {
            manager.writeCommonTags(span, false, true);

            verifyNoMoreInteractions(span, commonTagsManager);
        }

    }

    @Nested
    class LocalRoot {

        @BeforeEach
        void init() {
            when(tracingSettings.getAddCommonTags()).thenReturn(TracingSettings.AddCommonTags.ON_LOCAL_ROOT);
            manager.update();
        }


        @Test
        void newSpan() {
            when(commonTagsManager.getCommonTagValueMap()).thenReturn(Collections.singletonMap("key", "value"));

            manager.writeCommonTags(span, false, false);

            verify(span).putAttribute("key", AttributeValue.stringAttributeValue("value"));
            verify(commonTagsManager).getCommonTagValueMap();
            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void remoteParent() {
            when(commonTagsManager.getCommonTagValueMap()).thenReturn(Collections.singletonMap("key", "value"));

            manager.writeCommonTags(span, true, false);

            verify(span).putAttribute("key", AttributeValue.stringAttributeValue("value"));
            verify(commonTagsManager).getCommonTagValueMap();
            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void localParent() {
            manager.writeCommonTags(span, false, true);

            verifyNoMoreInteractions(span, commonTagsManager);
        }

    }

    @Nested
    class Always {

        @BeforeEach
        void init() {
            when(commonTagsManager.getCommonTagValueMap()).thenReturn(Collections.singletonMap("key", "value"));
            when(tracingSettings.getAddCommonTags()).thenReturn(TracingSettings.AddCommonTags.ALWAYS);
            manager.update();
        }

        @Test
        void newSpan() {
            manager.writeCommonTags(span, false, false);

            verify(span).putAttribute("key", AttributeValue.stringAttributeValue("value"));
            verify(commonTagsManager).getCommonTagValueMap();
            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void remoteParent() {
            manager.writeCommonTags(span, true, false);

            verify(span).putAttribute("key", AttributeValue.stringAttributeValue("value"));
            verify(commonTagsManager).getCommonTagValueMap();
            verifyNoMoreInteractions(span, commonTagsManager);
        }

        @Test
        void localParent() {
            manager.writeCommonTags(span, false, true);

            verify(span).putAttribute("key", AttributeValue.stringAttributeValue("value"));
            verify(commonTagsManager).getCommonTagValueMap();
            verifyNoMoreInteractions(span, commonTagsManager);
        }

    }


}