package rocks.inspectit.ocelot.core.config.util;

import com.fasterxml.jackson.core.JsonParseException;
import org.apache.http.entity.ContentType;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import rocks.inspectit.ocelot.core.config.propertysources.http.RawProperties;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

public class PropertyUtilsTest {

    String json = "{\"arr\": [{\"x\":42},{\"y\":7},[\"A\",\"B\"]],nested:{\"name\":\"blub\",\"complex.key\":true}}";

    String yaml = "root:\n  sub-child:\n    value: true\n  second: 42";

    @Nested
    public class ReadJson {

        @Test
        public void readJson() throws Exception {

            Properties result = PropertyUtils.readJson(json);

            assertThat(result).hasSize(6);
            assertThat(result).containsEntry("arr[0].x", 42);
            assertThat(result).containsEntry("arr[1].y", 7);
            assertThat(result).containsEntry("arr[2][0]", "A");
            assertThat(result).containsEntry("arr[2][1]", "B");
            assertThat(result).containsEntry("nested.name", "blub");
            assertThat(result).containsEntry("nested.complex.key", true);

        }
    }

    @Nested
    public class ReadYaml {

        @Test
        public void readYaml() {

            Properties result = PropertyUtils.readYaml(yaml);

            assertThat(result).hasSize(2);
            assertThat(result).containsEntry("root.sub-child.value", true);
            assertThat(result).containsEntry("root.second", 42);

        }
    }

    @Nested
    class Read {

        @Test
        public void read() throws IOException {

            RawProperties confJ = new RawProperties(json, ContentType.APPLICATION_JSON);

            // test that json is correctly parsed
            Properties resultJ1 = PropertyUtils.read(confJ);
            assertThat(resultJ1).hasSize(6);
            assertThat(resultJ1).containsEntry("arr[0].x", 42);
            Properties resultJ2 = PropertyUtils.read(json, ContentType.APPLICATION_JSON);
            assertThat(resultJ1).isEqualTo(resultJ2);

            // test that yaml is correctly parsed
            RawProperties confY = new RawProperties(yaml, ContentType.parse("application/x-yaml"));
            Properties resultY = PropertyUtils.read(confY);
            assertThat(resultY).hasSize(2);
            assertThat(resultY).containsEntry("root.sub-child.value", true);
            assertThat(resultY).isEqualTo(PropertyUtils.read(yaml, "application/x-yaml"));

            // test MIME type mismatch
            Assertions.assertThrows(JsonParseException.class, () -> PropertyUtils.read(yaml, ContentType.APPLICATION_JSON));

            Properties resultN = PropertyUtils.read(json, "application/x-yaml");
            assertThat(resultJ1).isEqualTo(resultN);

        }
    }

    /**
     * Testing the MIME type sensitive reading of properties.
     * These tests only work when using the mockito-inline artifact instead of mockito-core as they are using static mocks.
     * Since mockito-inline does not work with IBM JDK 8, these tests are ignored.
     */
    @Nested
    class ReadMimeTypeSensitive {

        @Test
        @Ignore
        public void readJsonMimeTypeSensitive() {

            try (MockedStatic<PropertyUtils> theMock = Mockito.mockStatic(PropertyUtils.class, CALLS_REAL_METHODS)) {
                PropertyUtils.read(new RawProperties(json, ContentType.APPLICATION_JSON));

                // verify that only json-related methods have been called
                theMock.verify(() -> PropertyUtils.read(Mockito.any(RawProperties.class)));
                theMock.verify(() -> PropertyUtils.read(anyString(), Mockito.any(ContentType.class)));
                theMock.verify(() -> PropertyUtils.readJson(anyString()));
                theMock.verify(() -> PropertyUtils.readJsonFromStream(Mockito.any()));
                theMock.verifyNoMoreInteractions();
            } catch (Exception e) {
                System.err.println("The test readYamlMimeTypeSensitive is ignored as we are not using mockito-inline. See exception for more details");
                e.printStackTrace();
            }
        }

        @Test
        @Ignore
        public void readYamlMimeTypeSensitive() {
            try (MockedStatic<PropertyUtils> theMock = Mockito.mockStatic(PropertyUtils.class, CALLS_REAL_METHODS)) {
                PropertyUtils.read(new RawProperties(yaml, ContentType.parse("application/x-yaml")));

                // verify that only read and readYaml, and  readYamlFiles have been called
                theMock.verify(() -> PropertyUtils.read(Mockito.any(RawProperties.class)));
                theMock.verify(() -> PropertyUtils.read(anyString(), Mockito.any(ContentType.class)));
                theMock.verify(() -> PropertyUtils.readYaml(anyString()));
                theMock.verify(() -> PropertyUtils.readYamlFiles(Mockito.any()));
            }
            catch(Exception e){
                System.err.println("The test readYamlMimeTypeSensitive is ignored as we are not using mockito-inline. See exception for more details");
                e.printStackTrace();
            }

        }
    }
}