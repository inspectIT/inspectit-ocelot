package inspectit.ocelot.configschemagenerator;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SchemaGeneratorTest {

    @Test
    void shouldNotThrowException() throws JsonMappingException {
        JsonSchema schema = SchemaGenerator.generateSchema();

        assertNotNull(schema);
    }
}
