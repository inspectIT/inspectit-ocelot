package inspectit.ocelot.configschemagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class InspectitSchemaGeneratorTest {

    @Test
    void shouldNotThrowException() {
        JsonNode schema = InspectitSchemaGenerator.generateSchema();

        assertNotNull(schema);
    }
}
