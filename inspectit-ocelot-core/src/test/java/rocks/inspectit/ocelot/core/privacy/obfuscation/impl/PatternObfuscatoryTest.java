package rocks.inspectit.ocelot.core.privacy.obfuscation.impl;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatternObfuscatoryTest {

    @Nested
    class PutSpanAttribute {

        @Mock
        Span span;

        Pattern p1 = Pattern.compile("[a-z]+");

        Pattern p2 = Pattern.compile("[0-9]+");

        @Test
        public void keyOnlyNoObfuscation() {
            PatternObfuscatory.PatternEntry.PatternEntryBuilder entryBuilder = PatternObfuscatory.PatternEntry.builder();
            entryBuilder.pattern(p1);
            entryBuilder.checkKey(true);
            PatternObfuscatory patternObfuscatory = new PatternObfuscatory(Collections.singleton(entryBuilder.build()));

            patternObfuscatory.putSpanAttribute(span, "011", "belgrade");

            verify(span).setAttribute(AttributeKey.stringKey("011"), "belgrade");
            verifyNoMoreInteractions(span);
        }

        @Test
        public void keyOnlyObfuscation() {
            PatternObfuscatory.PatternEntry.PatternEntryBuilder entryBuilder = PatternObfuscatory.PatternEntry.builder();
            entryBuilder.pattern(p1);
            entryBuilder.checkKey(true);
            PatternObfuscatory patternObfuscatory = new PatternObfuscatory(Collections.singleton(entryBuilder.build()));

            patternObfuscatory.putSpanAttribute(span, "abc", "belgrade");

            verify(span).setAttribute(AttributeKey.stringKey("abc"), "***");
            verifyNoMoreInteractions(span);
        }

        @Test
        public void keyOnlyObfuscationMapShortCut() {
            PatternObfuscatory.PatternEntry.PatternEntryBuilder entryBuilder = PatternObfuscatory.PatternEntry.builder();
            entryBuilder.pattern(p1);
            entryBuilder.checkKey(true);
            PatternObfuscatory patternObfuscatory = new PatternObfuscatory(Collections.singleton(entryBuilder.build()));

            patternObfuscatory.putSpanAttribute(span, "abc", "belgrade");

            verify(span).setAttribute(AttributeKey.stringKey("abc"), "***");
            verifyNoMoreInteractions(span);

            patternObfuscatory.putSpanAttribute(span, "abc", "stuttgart");

            verify(span, times(2)).setAttribute(AttributeKey.stringKey("abc"), "***");
            verifyNoMoreInteractions(span);
        }

        @Test
        public void keyOnlyObfuscationWithReplaceRegex() {
            PatternObfuscatory.PatternEntry.PatternEntryBuilder entryBuilder = PatternObfuscatory.PatternEntry.builder();
            entryBuilder.pattern(p1);
            entryBuilder.checkKey(true);
            entryBuilder.replaceRegex("grad");
            PatternObfuscatory patternObfuscatory = new PatternObfuscatory(Collections.singleton(entryBuilder.build()));

            patternObfuscatory.putSpanAttribute(span, "abc", "belgrade");

            verify(span).setAttribute(AttributeKey.stringKey("abc"), "***");
            verifyNoMoreInteractions(span);
        }

        @Test
        public void dataOnlyNoObfuscation() {
            PatternObfuscatory.PatternEntry.PatternEntryBuilder entryBuilder = PatternObfuscatory.PatternEntry.builder();
            entryBuilder.pattern(p1);
            entryBuilder.checkData(true);
            PatternObfuscatory patternObfuscatory = new PatternObfuscatory(Collections.singleton(entryBuilder.build()));

            patternObfuscatory.putSpanAttribute(span, "belgrade", "011");

            verify(span).setAttribute(AttributeKey.stringKey("belgrade"), "011");
            verifyNoMoreInteractions(span);
        }

        @Test
        public void dataOnlyObfuscation() {
            PatternObfuscatory.PatternEntry.PatternEntryBuilder entryBuilder = PatternObfuscatory.PatternEntry.builder();
            entryBuilder.pattern(p1);
            entryBuilder.checkData(true);
            PatternObfuscatory patternObfuscatory = new PatternObfuscatory(Collections.singleton(entryBuilder.build()));

            patternObfuscatory.putSpanAttribute(span, "belgrade", "abc");

            verify(span).setAttribute(AttributeKey.stringKey("belgrade"), "***");
            verifyNoMoreInteractions(span);
        }

        @Test
        public void dataOnlyObfuscationWithReplaceRegex() {
            PatternObfuscatory.PatternEntry.PatternEntryBuilder entryBuilder = PatternObfuscatory.PatternEntry.builder();
            entryBuilder.pattern(p1);
            entryBuilder.checkData(true);
            entryBuilder.replaceRegex("[ac]");
            PatternObfuscatory patternObfuscatory = new PatternObfuscatory(Collections.singleton(entryBuilder.build()));

            patternObfuscatory.putSpanAttribute(span, "belgrade", "abc");

            verify(span).setAttribute(AttributeKey.stringKey("belgrade"), "***");
            verifyNoMoreInteractions(span);
        }

        @Test
        public void mixed() {
            // numbers everywhere, lower-case letters only in key
            PatternObfuscatory.PatternEntry.PatternEntryBuilder entryBuilder1 = PatternObfuscatory.PatternEntry.builder();
            entryBuilder1.pattern(p1);
            entryBuilder1.checkKey(true);
            PatternObfuscatory.PatternEntry.PatternEntryBuilder entryBuilder2 = PatternObfuscatory.PatternEntry.builder();
            entryBuilder2.pattern(p2);
            entryBuilder2.checkKey(true);
            entryBuilder2.checkData(true);
            entryBuilder2.replaceRegex("[47]");

            PatternObfuscatory patternObfuscatory = new PatternObfuscatory(Arrays.asList(entryBuilder1.build(), entryBuilder2.build()));

            patternObfuscatory.putSpanAttribute(span, "abc", "abc");
            patternObfuscatory.putSpanAttribute(span, "efg", "012");
            patternObfuscatory.putSpanAttribute(span, "ABC", "abc");
            patternObfuscatory.putSpanAttribute(span, "ABC", "ABC");
            patternObfuscatory.putSpanAttribute(span, "012", "abc");
            patternObfuscatory.putSpanAttribute(span, "345", "678");
            patternObfuscatory.putSpanAttribute(span, "ABC", "234");

            verify(span).setAttribute(AttributeKey.stringKey("abc"), "***");
            verify(span).setAttribute(AttributeKey.stringKey("efg"), "***");
            verify(span).setAttribute(AttributeKey.stringKey("ABC"), "abc");
            verify(span).setAttribute(AttributeKey.stringKey("ABC"), "ABC");
            verify(span).setAttribute(AttributeKey.stringKey("012"), "***");
            verify(span).setAttribute(AttributeKey.stringKey("345"), "***");
            verify(span).setAttribute(AttributeKey.stringKey("ABC"), "***");
            verifyNoMoreInteractions(span);
        }

    }

}