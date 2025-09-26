package rocks.inspectit.ocelot.core.instrumentation.hook.tags;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
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
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonAttributesToSpanAttributesManagerTest {

    @InjectMocks
    CommonAttributesToSpanAttributesManager manager;

    @Mock
    InspectitEnvironment env;

    @Mock
    CommonAttributesManager commonAttributesManager;

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
            manager.writeCommonAttributes(span, false, false);

            verifyNoMoreInteractions(span, commonAttributesManager);
        }

        @Test
        void remoteParent() {
            manager.writeCommonAttributes(span, true, false);

            verifyNoMoreInteractions(span, commonAttributesManager);
        }

        @Test
        void localParent() {
            manager.writeCommonAttributes(span, false, true);

            verifyNoMoreInteractions(span, commonAttributesManager);
        }

    }

    @Nested
    class GlobalRoot {

        @BeforeEach
        void init() {
            when(tracingSettings.getAddCommonAttributes()).thenReturn(TracingSettings.AddCommonAttributes.ON_GLOBAL_ROOT);
            manager.update();
        }

        @Test
        void newSpan() {
            when(commonAttributesManager.getCommonAttributeValueMap()).thenReturn(Collections.singletonMap("key", "value"));

            manager.writeCommonAttributes(span, false, false);

            verify(span).setAttribute(AttributeKey.stringKey("key"), "value");
            verify(commonAttributesManager).getCommonAttributeValueMap();
            verifyNoMoreInteractions(span, commonAttributesManager);
        }

        @Test
        void remoteParent() {
            manager.writeCommonAttributes(span, true, false);

            verifyNoMoreInteractions(span, commonAttributesManager);
        }

        @Test
        void localParent() {
            manager.writeCommonAttributes(span, false, true);

            verifyNoMoreInteractions(span, commonAttributesManager);
        }

    }

    @Nested
    class LocalRoot {

        @BeforeEach
        void init() {
            when(tracingSettings.getAddCommonAttributes()).thenReturn(TracingSettings.AddCommonAttributes.ON_LOCAL_ROOT);
            manager.update();
        }

        @Test
        void newSpan() {
            when(commonAttributesManager.getCommonAttributeValueMap()).thenReturn(Collections.singletonMap("key", "value"));

            manager.writeCommonAttributes(span, false, false);

            verify(span).setAttribute(AttributeKey.stringKey("key"), "value");
            verify(commonAttributesManager).getCommonAttributeValueMap();
            verifyNoMoreInteractions(span, commonAttributesManager);
        }

        @Test
        void remoteParent() {
            when(commonAttributesManager.getCommonAttributeValueMap()).thenReturn(Collections.singletonMap("key", "value"));

            manager.writeCommonAttributes(span, true, false);

            verify(span).setAttribute(AttributeKey.stringKey("key"), "value");
            verify(commonAttributesManager).getCommonAttributeValueMap();
            verifyNoMoreInteractions(span, commonAttributesManager);
        }

        @Test
        void localParent() {
            manager.writeCommonAttributes(span, false, true);

            verifyNoMoreInteractions(span, commonAttributesManager);
        }

    }

    @Nested
    class Always {

        @BeforeEach
        void init() {
            when(commonAttributesManager.getCommonAttributeValueMap()).thenReturn(Collections.singletonMap("key", "value"));
            when(tracingSettings.getAddCommonAttributes()).thenReturn(TracingSettings.AddCommonAttributes.ALWAYS);
            manager.update();
        }

        @Test
        void newSpan() {
            manager.writeCommonAttributes(span, false, false);

            verify(span).setAttribute(AttributeKey.stringKey("key"), "value");
            verify(commonAttributesManager).getCommonAttributeValueMap();
            verifyNoMoreInteractions(span, commonAttributesManager);
        }

        @Test
        void remoteParent() {
            manager.writeCommonAttributes(span, true, false);

            verify(span).setAttribute(AttributeKey.stringKey("key"), "value");
            verify(commonAttributesManager).getCommonAttributeValueMap();
            verifyNoMoreInteractions(span, commonAttributesManager);
        }

        @Test
        void localParent() {
            manager.writeCommonAttributes(span, false, true);

            verify(span).setAttribute(AttributeKey.stringKey("key"), "value");
            verify(commonAttributesManager).getCommonAttributeValueMap();
            verifyNoMoreInteractions(span, commonAttributesManager);
        }

    }

}