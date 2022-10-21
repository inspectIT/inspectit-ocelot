package rocks.inspectit.ocelot.rest.yamlhighlighter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;
import rocks.inspectit.ocelot.config.model.instrumentation.scope.AdvancedScopeSettings;
import springfox.documentation.spring.web.json.Json;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for {@link HighlightRulesMapController}
 */
public class HighlightRulesMapControllerIntTest extends IntegrationTestBase {

    @Autowired
    HighlightRulesMapController controller;

    @Nested
    public class GetHighlightingRulesMapTest {
        @Test
        void testGetHighlightingRulesMap() {

            JsonParser jsonParser = new JsonParser();
            Gson gson = new Gson();

            final String JSON = gson.toJson(controller.generateMap(InspectitConfig.class));

            // get expected JSON
            JsonObject expected = jsonParser.parse(JSON).getAsJsonObject();

            // get highlighting rules map
            ResponseEntity<Object> result = authRest.getForEntity("/api/v1/highlight-rules", Object.class);

            // parse to JSON
            final String RESPONSE_JSON = gson.toJson(result.getBody());
            JsonObject obj = jsonParser.parse(RESPONSE_JSON).getAsJsonObject();

            // assert that the returned body is a JsonObject and that it equals the expected JSON
            assertThat(obj.isJsonObject()).isTrue();
            assertThat(obj).isEqualTo(expected);

        }
    }

    @Nested
    public class GenerateMapTest {

        // Using a test-class all branches except for one in the generateMap method are visited at least once.
        @Test
        void withTestClass() throws JsonProcessingException {

            // Build expected Map from JSON String using Jackson
            final String advancedScopeSettingsExpected = String.format(
                    "{\"instrument-only-inherited-methods\":{\"%s\":\"%s\"}, \"disable-safety-mechanisms\":{\"%s\":\"%s\"}}",
                    HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_TEXT,
                    HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_TEXT);

            final String expectedJson = String.format("{\"object-map\":{\"%s\":\"%s\", \"%s\":\"%s\"},",
                    HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_MAP,
                    HighlightRulesMapController.KEY_MAP_CONTENT_TYPE, HighlightRulesMapController.VALUE_TYPE_YAML) +
                    String.format("\"advanced-scope-settings-map\":{\"%s\":\"%s\", \"%s\":\"%s\", \"%s\":%s},",
                            HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_MAP,
                            HighlightRulesMapController.KEY_MAP_CONTENT_TYPE, HighlightRulesMapController.VALUE_TYPE_OBJECT,
                            HighlightRulesMapController.KEY_MAP_CONTENTS, advancedScopeSettingsExpected) +
                    String.format("\"string-map\":{\"%s\":\"%s\", \"%s\":\"%s\"},",
                            HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_MAP,
                            HighlightRulesMapController.KEY_MAP_CONTENT_TYPE, HighlightRulesMapController.VALUE_TYPE_TEXT) +
                    String.format("\"object-list\":{\"%s\":\"%s\", \"%s\":\"%s\"},",
                            HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_LIST,
                            HighlightRulesMapController.KEY_LIST_CONTENT_TYPE, HighlightRulesMapController.VALUE_TYPE_YAML) +
                    String.format("\"advanced-scope-settings-list\":{\"%s\":\"%s\", \"%s\":\"%s\", \"%s\":%s},",
                            HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_LIST,
                            HighlightRulesMapController.KEY_LIST_CONTENT_TYPE, HighlightRulesMapController.VALUE_TYPE_OBJECT,
                            HighlightRulesMapController.KEY_LIST_CONTENTS, advancedScopeSettingsExpected) +
                    String.format("\"boolean-list\":{\"%s\":\"%s\", \"%s\":\"%s\"},",
                            HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_LIST,
                            HighlightRulesMapController.KEY_LIST_CONTENT_TYPE, HighlightRulesMapController.VALUE_TYPE_TEXT) +
                    String.format("\"object\":{\"%s\":\"%s\"},",
                            HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_YAML) +
                    String.format("\"propagation-mode\":{\"%s\":\"%s\", \"%s\":[\"NONE\", \"JVM_LOCAL\", \"GLOBAL\"]},",
                            HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_ENUM,
                            HighlightRulesMapController.KEY_ENUM_VALUES) +
                    String.format("\"advanced-scope-settings\":{\"%s\":\"%s\", \"%s\":%s},",
                            HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_OBJECT,
                            HighlightRulesMapController.KEY_OBJECT_ATTRIBUTES, advancedScopeSettingsExpected) +
                    String.format("\"a-boolean\":{\"%s\":\"%s\"},",
                            HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_TEXT)+
                    String.format("\"protocol\":{\"%s\":\"%s\", \"%s\": [\"\", \"grpc\", \"http/thrift\", \"http/protobuf\"]}}",
                            HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_ENUM,
                            HighlightRulesMapController.KEY_ENUM_VALUES);

            Map<String,Object> expected = new ObjectMapper().readValue(expectedJson, Map.class);

            // Generate the Map using generateMap
            Map<String, Object> result = controller.generateMap(TestClass.class);

            // Compare the two Maps
            assertThat(result).isEqualTo(expected);
        }

        // Since the type Java is only inserted into the map when the class GenericActionSettings is handled
        // a separate test-case is needed to test this.
        @Test
        void testValueTypeJava() throws JsonProcessingException {
            // Build expected Map from JSON String using Jackson
            final String expectedJson = String.format("{\"%s\":\"%s\"},",
                    HighlightRulesMapController.KEY_TYPE, HighlightRulesMapController.VALUE_TYPE_JAVA);

            Map<String,Object> expected = new ObjectMapper().readValue(expectedJson, Map.class);

            // Generate the Map using generateMap
            Map<String, Object> result = controller.generateMap(GenericActionSettings.class);

            // Compare actual content of result Map with the expected content
            assertThat(result.get("value")).isEqualTo(expected);
            assertThat(result.get("value-body")).isEqualTo(expected);
        }
    }

    private static class TestClass{

        Map<String, Object>  objectMap;
        Map<String, AdvancedScopeSettings> advancedScopeSettingsMap;
        Map<String, String> stringMap;

        List<Object> objectList;
        List<AdvancedScopeSettings> advancedScopeSettingsList;
        List<Boolean> booleanList;

        Object object;

        PropagationMode propagationMode;

        AdvancedScopeSettings advancedScopeSettings;

        Boolean aBoolean;

        TransportProtocol protocol;

    }
}
