package rocks.inspectit.ocelot.core.instrumentation.actions.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class RegexCacheImplTest {

    private RegexCacheImpl regexCache;

    @BeforeEach
    void beforeEach() {
        regexCache = new RegexCacheImpl();
    }

    @Test
    void shouldMatchCorrectRegex() {
        boolean result = regexCache.matches("\\d+", "12345");

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotMatchIncorrectRegex() {
        boolean result = regexCache.matches("\\d+", "abc123");

        assertThat(result).isFalse();
    }

    @Test
    void shouldCacheCompiledPattern() {
        Pattern first = regexCache.pattern("\\w+");
        Pattern second = regexCache.pattern("\\w+");

        assertThat(first).isSameAs(second);
    }

    @Test
    void shouldReturnDifferentPatternsForDifferentRegexes() {
        Pattern first = regexCache.pattern("\\w+");
        Pattern second = regexCache.pattern("\\d+");

        assertThat(first).isNotSameAs(second);
    }
}
