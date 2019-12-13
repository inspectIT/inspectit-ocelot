package rocks.inspectit.ocelot.config.loaders;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigFileLoaderTest {
    @Nested
    public class GetDefaultConfig {
        @Test
        void GetDefaultConfig() throws IOException {
            String testPath1 = "basics.yml";
            String testContent1 = "i:\n" +
                    "  am:\n" +
                    "    a:\n" +
                    "      basic: test.yml";
            String testPath2 = "subfolder\\anotherSubFolder\\defaultSubfolder.yml";
            String testContent2 = "i:\n" +
                    "  am:\n" +
                    "    a:\n" +
                    "      basic: test-yml\n" +
                    "      in-a: subfolfder\n" +
                    "      in: another subfolder";

            Map<String, String> output = ConfigFileLoader.getDefaultConfigFiles();

            assertThat(output.size()).isEqualTo(2);
            assertThat(output.containsKey(testPath1)).isEqualTo(true);
            assertThat(output.containsKey(testPath2)).isEqualTo(true);
            assertThat(output.get(testPath1)).isEqualTo(testContent1);
            assertThat(output.get(testPath2)).isEqualTo(testContent2);
        }
    }

    @Nested
    public class GetFallBackConfig {
        @Test
        void getFallbackConfig() throws IOException {
            String testPath1 = "fallback.yml";
            String testContent1 = "i:\n" +
                    "  am:\n" +
                    "    a:\n" +
                    "      fallback: test.yml";
            String testPath2 = "subfolder\\anotherSubFolder\\fallbackSubfolder.yml";
            String testContent2 = "i:\n" +
                    "  am:\n" +
                    "    a:\n" +
                    "      fallback: test-yml\n" +
                    "      in-a: subfolfder\n" +
                    "      in: another subfolder";

            Map<String, String> output = ConfigFileLoader.getFallbackConfigFiles();

            assertThat(output.size()).isEqualTo(2);
            assertThat(output.containsKey(testPath1)).isEqualTo(true);
            assertThat(output.containsKey(testPath2)).isEqualTo(true);
            assertThat(output.get(testPath1)).isEqualTo(testContent1);
            assertThat(output.get(testPath2)).isEqualTo(testContent2);
        }
    }
}