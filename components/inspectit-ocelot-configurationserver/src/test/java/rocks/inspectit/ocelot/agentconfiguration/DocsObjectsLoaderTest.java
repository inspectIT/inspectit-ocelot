package rocks.inspectit.ocelot.agentconfiguration;

import inspectit.ocelot.configdocsgenerator.model.AgentDocumentation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static rocks.inspectit.ocelot.agentconfiguration.DocsObjectsLoader.OCELOT_DEFAULT_CONFIG_PREFIX;

public class DocsObjectsLoaderTest {

    private final String srcYamlWithDocsObject = """
            inspectit:
              instrumentation:
                scopes:
                  s_jdbc_statement_execute:
                    docs:
                      description: 'Scope for executed JDBC statements.'
                    methods:
                      - name: execute
            """;

    private final String srcYamlWithoutDocsObjects = """
            inspectit:
              metrics:
                disk:
                  enabled:
                    free: true
            """;


    @Nested
    class LoadObjects {
        @Test
        void verifyLoadObjectsSuccessful() throws IOException {
            Set<String> objects = DocsObjectsLoader.loadObjects(srcYamlWithDocsObject);

            assertTrue(objects.contains("s_jdbc_statement_execute"));
        }

        @Test
        void verifyLoadObjectsEmptyDocs() throws IOException {
            Set<String> objects = DocsObjectsLoader.loadObjects(srcYamlWithoutDocsObjects);

            assertTrue(objects.isEmpty());
        }

        @Test
        void verifyLoadObjectsEmptyString() throws IOException {
            Set<String> objects = DocsObjectsLoader.loadObjects("");

            assertTrue(objects.isEmpty());
        }

        @Test
        void verifyLoadObjectsInvalidConfig() {
            assertThrows(NoSuchElementException.class, () -> DocsObjectsLoader.loadObjects("invalid-config"));
        }

        @Test
        void verifyLoadObjectsNull() {
            assertThrows(IllegalArgumentException.class, () -> DocsObjectsLoader.loadObjects(null));
        }
    }

    @Nested
    class DefaultAgentDocumentation {
        @Test
        void verifyLoadDefaultAgentDocumentations() {
            String file = "test.yml";
            String fileWithPrefix = OCELOT_DEFAULT_CONFIG_PREFIX + file;
            Map<String, String> configs = new HashMap<>();
            configs.put(file, srcYamlWithDocsObject);

            Set<AgentDocumentation> documentations = DocsObjectsLoader.loadDefaultAgentDocumentations(configs);
            assertTrue(documentations.stream().anyMatch(doc -> doc.getFilePath().equals(fileWithPrefix)));
            assertTrue(documentations.stream().anyMatch(doc -> doc.getObjects().equals(Collections.singleton("s_jdbc_statement_execute"))));
        }

        @Test
        void verifyLoadDefaultAgentDocumentationsEmptyDocs() {
            String file = "test.yml";
            String fileWithPrefix = OCELOT_DEFAULT_CONFIG_PREFIX + file;
            Map<String, String> configs = new HashMap<>();
            configs.put(file, srcYamlWithoutDocsObjects);

            Set<AgentDocumentation> documentations = DocsObjectsLoader.loadDefaultAgentDocumentations(configs);
            assertTrue(documentations.stream().anyMatch(doc -> doc.getFilePath().equals(fileWithPrefix)));
            assertTrue(documentations.stream().anyMatch(doc -> doc.getObjects().equals(Collections.emptySet())));
        }

        @Test
        void verifyLoadDefaultAgentDocumentationsEmptyMap() {
            Set<AgentDocumentation> documentations = DocsObjectsLoader.loadDefaultAgentDocumentations(new HashMap<>());

            assertTrue(documentations.isEmpty());
        }
    }
}
