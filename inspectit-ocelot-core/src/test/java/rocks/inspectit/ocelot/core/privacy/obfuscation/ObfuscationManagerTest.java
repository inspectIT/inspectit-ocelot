package rocks.inspectit.ocelot.core.privacy.obfuscation;

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
import rocks.inspectit.ocelot.config.model.privacy.PrivacySettings;
import rocks.inspectit.ocelot.config.model.privacy.obfuscation.ObfuscationPattern;
import rocks.inspectit.ocelot.config.model.privacy.obfuscation.ObfuscationSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.privacy.obfuscation.impl.NoopObfuscatory;
import rocks.inspectit.ocelot.core.privacy.obfuscation.impl.PatternObfuscatory;

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

    @Nested
    class Constructor {

        @Test
        public void obfuscatoryNotNull() {
            Supplier<IObfuscatory> obfuscatorySupplier = obfuscationManager.obfuscatorySupplier();
            assertThat(obfuscatorySupplier.get())
                    .isNotNull()
                    .isInstanceOf(NoopObfuscatory.class);
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
            assertThat(obfuscatorySupplier.get())
                    .isNotNull()
                    .isInstanceOf(NoopObfuscatory.class);
        }

        @Test
        public void patternCompileError() {
            ObfuscationPattern obfuscationPattern = new ObfuscationPattern();
            obfuscationPattern.setPattern("[[a-z]");

            when(obfuscationSettings.isEnabled()).thenReturn(true);
            when(obfuscationSettings.getPatterns()).thenReturn(Collections.singletonList(obfuscationPattern));

            obfuscationManager.update();

            Supplier<IObfuscatory> obfuscatorySupplier = obfuscationManager.obfuscatorySupplier();
            assertThat(obfuscatorySupplier.get())
                    .isNotNull()
                    .isInstanceOf(NoopObfuscatory.class);
        }

        @Test
        public void happyPath() {
            ObfuscationPattern obfuscationPattern1 = new ObfuscationPattern();
            obfuscationPattern1.setPattern("[a-z]+");
            obfuscationPattern1.setCheckKey(true);
            ObfuscationPattern obfuscationPattern2 = new ObfuscationPattern();
            obfuscationPattern2.setPattern("[0-9]+");
            obfuscationPattern2.setCheckData(true);

            when(obfuscationSettings.isEnabled()).thenReturn(true);
            when(obfuscationSettings.getPatterns()).thenReturn(Arrays.asList(obfuscationPattern1, obfuscationPattern2));

            obfuscationManager.update();

            Supplier<IObfuscatory> obfuscatorySupplier = obfuscationManager.obfuscatorySupplier();
            IObfuscatory obfuscatory = obfuscatorySupplier.get();
            assertThat(obfuscatory)
                    .isNotNull()
                    .isInstanceOf(PatternObfuscatory.class);

            // there is no way for me to test the correct patterns passed to the obfuscatory
            // then to actually invoke the obfuscatory
            Span span = mock(Span.class);

            obfuscatory.putSpanAttribute(span, "abc", "abc");
            obfuscatory.putSpanAttribute(span, "ABC", "abc");
            obfuscatory.putSpanAttribute(span, "DEF", "123");

            verify(span, never()).putAttribute("abc", AttributeValue.stringAttributeValue("abc"));
            verify(span, never()).putAttribute("DEF", AttributeValue.stringAttributeValue("123"));
            verify(span).putAttribute("ABC", AttributeValue.stringAttributeValue("abc"));
            verify(span).putAttribute(eq("abc"), any());
            verify(span).putAttribute(eq("ABC"), any());
        }

        @Test
        public void happyPathCaseInsensitive() {
            ObfuscationPattern obfuscationPattern1 = new ObfuscationPattern();
            obfuscationPattern1.setPattern("[a-z]+");
            obfuscationPattern1.setCheckKey(true);

            when(obfuscationSettings.isEnabled()).thenReturn(true);
            when(obfuscationSettings.isCaseInsensitive()).thenReturn(true);
            when(obfuscationSettings.getPatterns()).thenReturn(Collections.singletonList(obfuscationPattern1));

            obfuscationManager.update();

            Supplier<IObfuscatory> obfuscatorySupplier = obfuscationManager.obfuscatorySupplier();
            IObfuscatory obfuscatory = obfuscatorySupplier.get();
            assertThat(obfuscatory)
                    .isNotNull()
                    .isInstanceOf(PatternObfuscatory.class);

            // there is no way for me to test the correct patterns passed to the obfuscatory
            // then to actually invoke the obfuscatory
            Span span = mock(Span.class);

            obfuscatory.putSpanAttribute(span, "abc", "abc");
            obfuscatory.putSpanAttribute(span, "ABC", "abc");

            verify(span, never()).putAttribute("abc", AttributeValue.stringAttributeValue("abc"));
            verify(span, never()).putAttribute("ABC", AttributeValue.stringAttributeValue("abc"));
            verify(span).putAttribute(eq("abc"), any());
            verify(span).putAttribute(eq("ABC"), any());
        }

    }

}