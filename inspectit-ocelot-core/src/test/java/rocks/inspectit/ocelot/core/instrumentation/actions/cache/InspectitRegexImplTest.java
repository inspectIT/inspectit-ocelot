package rocks.inspectit.ocelot.core.instrumentation.actions.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class InspectitRegexImplTest {

    private InspectitRegexImpl regex;

    @BeforeEach
    void beforeEach() {
        regex = new InspectitRegexImpl();
    }

    @Test
    void shouldMatchCorrectRegex() {
        boolean result = regex.matches("\\d+", "12345");

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotMatchIncorrectRegex() {
        boolean result = regex.matches("\\d+", "abc123");

        assertThat(result).isFalse();
    }

    @Test
    void shouldCacheCompiledPattern() {
        Pattern first = regex.pattern("\\w+");
        Pattern second = regex.pattern("\\w+");

        assertThat(first).isSameAs(second);
    }

    @Test
    void shouldReturnDifferentPatternsForDifferentRegexes() {
        Pattern first = regex.pattern("\\w+");
        Pattern second = regex.pattern("\\d+");

        assertThat(first).isNotSameAs(second);
    }
}
