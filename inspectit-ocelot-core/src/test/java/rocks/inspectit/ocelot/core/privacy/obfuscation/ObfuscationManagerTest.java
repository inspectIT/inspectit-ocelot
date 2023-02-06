package rocks.inspectit.ocelot.core.privacy.obfuscation;

import io.opencensus.internal.NoopScope;
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
import rocks.inspectit.ocelot.config.model.privacy.PrivacySettings;
import rocks.inspectit.ocelot.config.model.privacy.obfuscation.ObfuscationPattern;
import rocks.inspectit.ocelot.config.model.privacy.obfuscation.ObfuscationSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.privacy.obfuscation.impl.NoopObfuscatory;
import rocks.inspectit.ocelot.core.privacy.obfuscation.impl.PatternObfuscatory;
import rocks.inspectit.ocelot.core.privacy.obfuscation.impl.SelfMonitoringDelegatingObfuscatory;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObfuscationManagerTest {

    @InjectMocks
    ObfuscationManager obfuscationManager;

    @Mock
    InspectitEnvironment env;

    @Mock
    SelfMonitoringService selfMonitoringService;

    @Nested
    class Constructor {

        @Test
        public void obfuscatoryNotNull() {
            Supplier<IObfuscatory> obfuscatorySupplier = obfuscationManager.obfuscatorySupplier();
            assertThat(obfuscatorySupplier.get()).isNotNull().isInstanceOf(NoopObfuscatory.class);
        }
    }

    @Nested
    class Update {

        @Mock
        InspectitConfig config;

        @Mock
        PrivacySettings privacySettings;

        @Mock
        ObfuscationSettings obfuscationSettings;

        @BeforeEach
        public void init() {
            when(privacySettings.getObfuscation()).thenReturn(obfuscationSettings);
            when(config.getPrivacy()).thenReturn(privacySettings);
            when(env.getCurrentConfig()).thenReturn(config);
        }

        @Test
        public void notEnabled() {
            when(obfuscationSettings.isEnabled()).thenReturn(false);

            obfuscationManager.update();

            Supplier<IObfuscatory> obfuscatorySupplier = obfuscationManager.obfuscatorySupplier();
            assertThat(obfuscatorySupplier.get()).isNotNull().isInstanceOf(NoopObfuscatory.class);
        }

        @Test
        public void patternCompileError() {
            ObfuscationPattern obfuscationPattern = new ObfuscationPattern();
            obfuscationPattern.setPattern("[[a-z]");

            when(obfuscationSettings.isEnabled()).thenReturn(true);
            when(obfuscationSettings.getPatterns()).thenReturn(Collections.singletonList(obfuscationPattern));

            obfuscationManager.update();

            Supplier<IObfuscatory> obfuscatorySupplier = obfuscationManager.obfuscatorySupplier();
            assertThat(obfuscatorySupplier.get()).isNotNull().isInstanceOf(NoopObfuscatory.class);
        }

        @Test
        public void happyPath() {
            ObfuscationPattern obfuscationPattern1 = new ObfuscationPattern();
            obfuscationPattern1.setPattern("[a-z]+");
            obfuscationPattern1.setCheckKey(true);
            obfuscationPattern1.setCaseInsensitive(false);
            ObfuscationPattern obfuscationPattern2 = new ObfuscationPattern();
            obfuscationPattern2.setPattern("[0-9]+");
            obfuscationPattern2.setCheckData(true);

            when(obfuscationSettings.isEnabled()).thenReturn(true);
            when(obfuscationSettings.getPatterns()).thenReturn(Arrays.asList(obfuscationPattern1, obfuscationPattern2));

            obfuscationManager.update();

            Supplier<IObfuscatory> obfuscatorySupplier = obfuscationManager.obfuscatorySupplier();
            IObfuscatory obfuscatory = obfuscatorySupplier.get();
            assertThat(obfuscatory).isNotNull().isInstanceOf(PatternObfuscatory.class);

            // there is no way for me to test the correct patterns passed to the obfuscatory
            // then to actually invoke the obfuscatory
            Span span = mock(Span.class);

            obfuscatory.putSpanAttribute(span, "abc", "abc");
            obfuscatory.putSpanAttribute(span, "ABC", "abc");
            obfuscatory.putSpanAttribute(span, "DEF", "123");

            verify(span).setAttribute(AttributeKey.stringKey("abc"), "***");
            verify(span).setAttribute(AttributeKey.stringKey("ABC"), "abc");
            verify(span).setAttribute(AttributeKey.stringKey("DEF"), "***");
            verifyNoMoreInteractions(span);
        }

        @Test
        public void happyPathWithReplacementRegex() {
            ObfuscationPattern obfuscationPattern = new ObfuscationPattern();
            obfuscationPattern.setPattern("[a-z]+");
            obfuscationPattern.setCheckKey(true);
            obfuscationPattern.setCaseInsensitive(false);
            obfuscationPattern.setReplaceRegex("[b-z]+");

            when(obfuscationSettings.isEnabled()).thenReturn(true);
            when(obfuscationSettings.getPatterns()).thenReturn(Collections.singletonList(obfuscationPattern));

            obfuscationManager.update();

            Supplier<IObfuscatory> obfuscatorySupplier = obfuscationManager.obfuscatorySupplier();
            IObfuscatory obfuscatory = obfuscatorySupplier.get();
            assertThat(obfuscatory).isNotNull().isInstanceOf(PatternObfuscatory.class);

            // there is no way for me to test the correct patterns passed to the obfuscatory
            // then to actually invoke the obfuscatory
            Span span = mock(Span.class);

            obfuscatory.putSpanAttribute(span, "abc", "abc");
            obfuscatory.putSpanAttribute(span, "ABC", "abc");
            obfuscatory.putSpanAttribute(span, "DEF", "123");

            verify(span).setAttribute(AttributeKey.stringKey("abc"), "***");
            verify(span).setAttribute(AttributeKey.stringKey("ABC"), "abc");
            verify(span).setAttribute(AttributeKey.stringKey("DEF"), "123");
        }

        @Test
        public void happyPathCaseInsensitive() {
            ObfuscationPattern obfuscationPattern1 = new ObfuscationPattern();
            obfuscationPattern1.setPattern("[a-z]+");
            obfuscationPattern1.setCheckKey(true);
            obfuscationPattern1.setCaseInsensitive(true);

            when(obfuscationSettings.isEnabled()).thenReturn(true);
            when(obfuscationSettings.getPatterns()).thenReturn(Collections.singletonList(obfuscationPattern1));

            obfuscationManager.update();

            Supplier<IObfuscatory> obfuscatorySupplier = obfuscationManager.obfuscatorySupplier();
            IObfuscatory obfuscatory = obfuscatorySupplier.get();
            assertThat(obfuscatory).isNotNull().isInstanceOf(PatternObfuscatory.class);

            // there is no way for me to test the correct patterns passed to the obfuscatory
            // then to actually invoke the obfuscatory
            Span span = mock(Span.class);

            obfuscatory.putSpanAttribute(span, "abc", "abc");
            obfuscatory.putSpanAttribute(span, "ABC", "abc");

            verify(span, never()).setAttribute(AttributeKey.stringKey("abc"), "abc");
            verify(span, never()).setAttribute(AttributeKey.stringKey("ABC"), "abc");
            verify(span).setAttribute(eq(AttributeKey.stringKey("abc")), any());
            verify(span).setAttribute(eq(AttributeKey.stringKey("ABC")), any());
        }

        @Test
        public void happyPathWithSelfMonitoring() {
            ObfuscationPattern obfuscationPattern1 = new ObfuscationPattern();
            obfuscationPattern1.setPattern("[a-z]+");
            obfuscationPattern1.setCheckKey(true);
            obfuscationPattern1.setCaseInsensitive(false);
            ObfuscationPattern obfuscationPattern2 = new ObfuscationPattern();
            obfuscationPattern2.setPattern("[0-9]+");
            obfuscationPattern2.setCheckData(true);

            when(selfMonitoringService.isSelfMonitoringEnabled()).thenReturn(true);
            when(selfMonitoringService.withDurationSelfMonitoring(any())).thenReturn(NoopScope.getInstance());
            when(obfuscationSettings.isEnabled()).thenReturn(true);
            when(obfuscationSettings.getPatterns()).thenReturn(Arrays.asList(obfuscationPattern1, obfuscationPattern2));

            obfuscationManager.update();

            Supplier<IObfuscatory> obfuscatorySupplier = obfuscationManager.obfuscatorySupplier();
            IObfuscatory obfuscatory = obfuscatorySupplier.get();
            assertThat(obfuscatory).isNotNull().isInstanceOf(SelfMonitoringDelegatingObfuscatory.class);

            // there is no way for me to test the correct patterns passed to the obfuscatory
            // then to actually invoke the obfuscatory
            Span span = mock(Span.class);

            obfuscatory.putSpanAttribute(span, "abc", "abc");
            obfuscatory.putSpanAttribute(span, "ABC", "abc");
            obfuscatory.putSpanAttribute(span, "DEF", "123");

            verify(span).setAttribute(AttributeKey.stringKey("abc"), "***");
            verify(span).setAttribute(AttributeKey.stringKey("ABC"), "abc");
            verify(span).setAttribute(AttributeKey.stringKey("DEF"), "***");
            verifyNoMoreInteractions(span);

            verify(selfMonitoringService).isSelfMonitoringEnabled();
            verify(selfMonitoringService, times(3)).withDurationSelfMonitoring("PatternObfuscatory");
            verifyNoMoreInteractions(selfMonitoringService);
        }

        @Test
        public void testSupportedValues() {
            ObfuscationPattern obfuscationPattern1 = new ObfuscationPattern();
            obfuscationPattern1.setPattern("username");

            when(obfuscationSettings.isEnabled()).thenReturn(true);
            when(obfuscationSettings.getPatterns()).thenReturn(Collections.singletonList(obfuscationPattern1));

            obfuscationManager.update();

            Supplier<IObfuscatory> obfuscatorySupplier = obfuscationManager.obfuscatorySupplier();
            IObfuscatory obfuscatory = obfuscatorySupplier.get();
            assertThat(obfuscatory).isNotNull().isInstanceOf(PatternObfuscatory.class);

            // there is no way for me to test the correct patterns passed to the obfuscatory
            // then to actually invoke the obfuscatory
            Span span = mock(Span.class);

            // String
            obfuscatory.putSpanAttribute(span, "username", "abc");
            verify(span).setAttribute(AttributeKey.stringKey("username"), "***");
            verifyNoMoreInteractions(span);

            // exception
            obfuscatory.putSpanAttribute(span, "exception", new RuntimeException("runtime exception"));
            verify(span).setAttribute(AttributeKey.stringKey("exception"), "java.lang.RuntimeException: runtime exception");
            verifyNoMoreInteractions(span);

            // Long and Integer
            obfuscatory.putSpanAttribute(span, "long", Long.valueOf(42));
            verify(span).setAttribute(AttributeKey.longKey("long"), 42l);
            obfuscatory.putSpanAttribute(span, "integer", Integer.valueOf(1337));
            verify(span).setAttribute(AttributeKey.longKey("integer"), 1337l);
            verifyNoMoreInteractions(span);

            // Double and Float
            obfuscatory.putSpanAttribute(span, "double", Double.valueOf(1.338d));
            verify(span).setAttribute(AttributeKey.doubleKey("double"), 1.338d);
            obfuscatory.putSpanAttribute(span, "float", Float.valueOf(1.337f));
            // TODO: do we want to avoid the 'added' precision by double? Then we need to change it accordingly in the IObfuscatory.
            verify(span).setAttribute(eq(AttributeKey.doubleKey("float")), anyDouble());
            verifyNoMoreInteractions(span);

            // Number -> Double
            obfuscatory.putSpanAttribute(span, "number", new Number() {
                @Override
                public int intValue() {
                    return 55;
                }

                @Override
                public long longValue() {
                    return 55;
                }

                @Override
                public float floatValue() {
                    return 55;
                }

                @Override
                public double doubleValue() {
                    return 55;
                }
            });
            verify(span).setAttribute(AttributeKey.doubleKey("number"), 55.0d);
            verifyNoMoreInteractions(span);

            // TODO: Boolean

            // Object -> String
            obfuscatory.putSpanAttribute(span, "object", new Object() {
                @Override
                public String toString() {
                    return "this-is-object";
                }
            });
            verify(span).setAttribute(AttributeKey.stringKey("object"), "this-is-object");

            verifyNoMoreInteractions(span);
        }
    }

}