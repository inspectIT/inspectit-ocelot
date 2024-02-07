package rocks.inspectit.ocelot.agentconfiguration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static rocks.inspectit.ocelot.agentconfiguration.DocsObjectsLoader.OCELOT_DEFAULT_CONFIG_PREFIX;

public class DocsObjectsLoaderTest {

    private final String srcYaml = """
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

    @Test
    void verifyLoadObjectsSuccessful() throws IOException {
        Set<String> objects = DocsObjectsLoader.loadObjects(srcYaml);

        assertTrue(objects.contains("s_jdbc_statement_execute"));
    }

    @Test
    void verifyLoadObjectsEmpty() throws IOException {
        Set<String> objects = DocsObjectsLoader.loadObjects(srcYamlWithoutDocsObjects);

        assertTrue(objects.isEmpty());
    }

    @Test
    void verifyLoadThrowsException() {
        assertThrows(NoSuchElementException.class, () -> DocsObjectsLoader.loadObjects("invalid-config"));
    }

    @Test
    void verifyLoadDefaultDocsObjectsByFile() {
        String file = "test.yml";
        String fileWithPrefix = OCELOT_DEFAULT_CONFIG_PREFIX + file;
        Map<String, String> configs = new HashMap<>();
        configs.put(file, srcYaml);

        Map<String, Set<String>> docsObjectsByFile = DocsObjectsLoader.loadDefaultDocsObjectsByFile(configs);
        assertTrue(docsObjectsByFile.containsKey(fileWithPrefix));
        assertTrue(docsObjectsByFile.containsValue(Collections.singleton("s_jdbc_statement_execute")));
    }
}
