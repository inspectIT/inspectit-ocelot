package rocks.inspectit.ocelot.config.loaders;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.io.File;
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
            String testPath2 = "subfolder/anotherSubFolder/defaultSubfolder.yml";
            String testContent2 = "i:\n" +
                    "  am:\n" +
                    "    a:\n" +
                    "      basic: test-yml\n" +
                    "      in-a: subfolfder\n" +
                    "      in: another subfolder";

            Map<String, String> output = ConfigFileLoader.getDefaultConfigFiles();

            assertThat(output).hasSize(2);
            assertThat(output).containsKey(testPath1);
            assertThat(output).containsKey(testPath2);
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
            String testPath2 = "subfolder/anotherSubFolder/fallbackSubfolder.yml";
            String testContent2 = "i:\n" +
                    "  am:\n" +
                    "    a:\n" +
                    "      fallback: test-yml\n" +
                    "      in-a: subfolfder\n" +
                    "      in: another subfolder";

            Map<String, String> output = ConfigFileLoader.getFallbackConfigFiles();

            assertThat(output).hasSize(2);
            assertThat(output).containsKey(testPath1);
            assertThat(output).containsKey(testPath2);
            assertThat(output.get(testPath1)).isEqualTo(testContent1);
            assertThat(output.get(testPath2)).isEqualTo(testContent2);
        }
    }

    @Nested
    public class GetDefaultResources {
        @Test
        void getDefaultResource() throws IOException {
            String filePath1 = "config/default/basics.yml]";
            filePath1 = filePath1.replace("/", File.separator);
            String filePath2 = "config/default/subfolder/anotherSubFolder/defaultSubfolder.yml]";
            filePath2 = filePath2.replace("/", File.separator);

            Resource[] output = ConfigFileLoader.getDefaultResources();

            assertThat(output).hasSize(2);
            assertThat(output[0].getDescription()).endsWith(filePath1);
            assertThat(output[1].getDescription()).endsWith(filePath2);
        }
    }

    @Nested
    public class GetFallbackResources {
        @Test
        void getFallbackResource() throws IOException {
            String filePath1 = "config/fallback/fallback.yml]";
            filePath1 = filePath1.replace("/", File.separator);
            String filePath2 = "config/fallback/subfolder/anotherSubFolder/fallbackSubfolder.yml]";
            filePath2 = filePath2.replace("/", File.separator);

            Resource[] output = ConfigFileLoader.getFallBackResources();

            assertThat(output).hasSize(2);
            assertThat(output[0].getDescription()).endsWith(filePath1);
            assertThat(output[1].getDescription()).endsWith(filePath2);
        }
    }
}