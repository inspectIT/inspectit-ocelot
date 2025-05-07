package inspectit.ocelot.configschemagenerator;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This class is only used by the Gradle task {@code generatedConfigSchema} to create a yaml schema file
 * for {@link InspectitConfig}. The schema file supports developers when writing an inspectIT configuration
 * in their IDE.
 */
public class SchemaGenerator {
    private static final Logger log = Logger.getLogger(SchemaGenerator.class.getName());

    /**
     * Generates JSON schema and creates a file at the specified path.
     *
     * @param args should contain path for the generated schema file
     */
    public static void main(String[] args) throws Exception {
        if(args.length < 1)
            throw new RuntimeException("Output path has to be specified!");

        JsonSchema configSchema = generateSchema();

        try {
            String outputPath = args[0];
            writeSchemaToFile(configSchema, outputPath);
        } catch (Exception e) {
            log.severe("Could not generate schema file: " + e.getMessage());
        }
    }

    /**
     * Generates the JSON schema for {@link InspectitConfigSchema}.
     * The camelCase of the Java property names are converted to kebab-case,
     * which is also used in the file editor of the configuration server.
     *
     * @return the generated schema
     */
    static JsonSchema generateSchema() throws JsonMappingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        JsonSchemaGenerator generator = new JsonSchemaGenerator(mapper);

        return generator.generateSchema(InspectitConfigSchema.class);
    }

    /**
     * Writes the provided schema into a file.
     *
     * @param schema the json schema
     * @param filePath the path to create the schema file
     */
    private static void writeSchemaToFile(JsonSchema schema, String filePath) throws IOException {
        File outputFile = new File(filePath);
        outputFile.getParentFile().mkdirs();

        YAMLMapper mapper = new YAMLMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, schema);
    }
}
