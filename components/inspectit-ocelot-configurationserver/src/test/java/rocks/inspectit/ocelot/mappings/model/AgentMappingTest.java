package rocks.inspectit.ocelot.mappings.model;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AgentMappingTest {

    private AgentMapping mapping;

    @BeforeEach
    public void beforeEach() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("first", "01234");
        attributes.put("second", "AnAttribute");
        List<String> sources = new ArrayList<>();
        sources.add("/one");
        sources.add("/two/a.yml");
        sources.add("/two/b.yml");
        mapping = new AgentMapping("test-mapping", sources, attributes);
    }

    @Nested
    class MatchesAttributes {

        @Test
        public void sameAttributes() {
            Map<String, String> attributes = ImmutableMap.of("first", "01234", "second", "AnAttribute");

            boolean result = mapping.matchesAttributes(attributes);

            assertThat(result).isTrue();
        }

        @Test
        public void differentCasing() {
            Map<String, String> attributes = ImmutableMap.of("first", "01234", "second", "anaTTribute");

            boolean result = mapping.matchesAttributes(attributes);

            assertThat(result).isTrue();
        }

        @Test
        public void differentAttributes() {
            Map<String, String> attributes = ImmutableMap.of("first", "00000", "second", "AnAttribute");

            boolean result = mapping.matchesAttributes(attributes);

            assertThat(result).isFalse();
        }

        @Test
        public void emptyAttributes() {
            Map<String, String> attributes = Collections.emptyMap();

            boolean result = mapping.matchesAttributes(attributes);

            assertThat(result).isFalse();
        }

    }

}