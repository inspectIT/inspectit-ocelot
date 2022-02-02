package rocks.inspectit.ocelot.rest.yamlhighlighter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for {@link HighlightRulesMapController}
 */
public class HighlightRulesMapControllerIntTest extends IntegrationTestBase {

    final static String JSON = "{start={object-attributes={inspectit={type=object, object-attributes=" + HighlightRulesMapController.generateMap(InspectitConfig.class) + "}},type=object}}";

    static JsonParser jsonParser = new JsonParser();

    @Test
    void testGetHighlightingRulesMap() {

        // get expected JSON
        JsonObject expected = jsonParser.parse(JSON).getAsJsonObject();

        // get highlighting rules map
        ResponseEntity<Object> result = authRest.getForEntity("/api/v1/highlight-rules", Object.class);

        // parse to JSON
        JsonObject obj = jsonParser.parse(result.getBody().toString()).getAsJsonObject();

        // assert that the returned body is a JsonObject and that it equals the expected JSON
        assertThat(obj.isJsonObject()).isTrue();
        assertThat(expected).isEqualTo(obj);

    }

    @Test
    void testGenerateMap() {
        // TODO: implement test
    }
}
