package rocks.inspectit.ocelot.configschema;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import rocks.inspectit.ocelot.config.ui.UISettings;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.beans.PropertyDescriptor;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ConfigurationSchemaProviderTest {

    ConfigurationSchemaProvider provider = new ConfigurationSchemaProvider();

    private static PropertyDescriptor getFirstProperty(Object obj) {
        return Arrays.stream(BeanUtils.getPropertyDescriptors(obj.getClass()))
                .filter(prop -> prop.getWriteMethod() != null)
                .findFirst()
                .get();
    }

    private class NestedProp {

        private String simple;

        public void setSimple(String str) {
        }

    }

    @Nested
    class ToDescription {

        @Nested
        class Name {

            @Test
            void simpleName() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    private int simple;

                    public void setSimple(int i) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isNotEmpty();
                ConfigurationPropertyDescription desc = result.get();

                assertThat(desc.getPropertyName()).isEqualTo("simple");
                assertThat(desc.getReadableName()).isEqualTo("Simple");
            }

            @Test
            void complexName() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    private int notSoSimple;

                    public void setNotSoSimple(int i) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isNotEmpty();
                ConfigurationPropertyDescription desc = result.get();

                assertThat(desc.getPropertyName()).isEqualTo("not-so-simple");
                assertThat(desc.getReadableName()).isEqualTo("Not So Simple");
            }

            @Test
            void customNameOnField() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    @UISettings(name = "custom NAME")
                    private int simple;

                    public void setSimple(int i) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isNotEmpty();
                ConfigurationPropertyDescription desc = result.get();

                assertThat(desc.getPropertyName()).isEqualTo("simple");
                assertThat(desc.getReadableName()).isEqualTo("custom NAME");
            }

            @Test
            void customNameOnSetter() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    private int simple;

                    @UISettings(name = "custom NAME")
                    public void setSimple(int i) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isNotEmpty();
                ConfigurationPropertyDescription desc = result.get();

                assertThat(desc.getPropertyName()).isEqualTo("simple");
                assertThat(desc.getReadableName()).isEqualTo("custom NAME");
            }
        }

        @Nested
        class Exclusion {

            @Test
            void excludeOnField() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    @UISettings(exclude = true)
                    private int simple;

                    public void setSimple(int i) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isEmpty();
            }

            @Test
            void excludeOnSetter() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    private int simple;

                    @UISettings(exclude = true)
                    public void setSimple(int i) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isEmpty();
            }

            @Test
            void excludeOnMapType() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    private Map<String, String> simple;

                    public void setSimple(Map<String, String> map) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isEmpty();
            }

            @Test
            void excludeOnCollectionsType() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    private List<String> simple;

                    public void setSimple(List<String> list) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isEmpty();
            }
        }

        @Nested
        class Type {

            @Test
            void compositeType() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    public void setComp(NestedProp p) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isNotEmpty();

                ConfigurationPropertyDescription desc = result.get();
                assertThat(desc.getType()).isEqualTo(ConfigurationPropertyType.COMPOSITE);
                assertThat(desc.getPropertyName()).isEqualTo("comp");
                assertThat(desc.getChildren()).hasSize(1).anySatisfy((child) -> {
                    assertThat(child.getPropertyName()).isEqualTo("simple");
                    assertThat(child.getType()).isEqualTo(ConfigurationPropertyType.STRING);
                });
            }

            @Test
            void simpleType() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    public void setMyInt(short i) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isNotEmpty();

                ConfigurationPropertyDescription desc = result.get();
                assertThat(desc.getType()).isEqualTo(ConfigurationPropertyType.INTEGER);
                assertThat(desc.getPropertyName()).isEqualTo("my-int");
            }
        }

        @Nested
        class Nullability {

            @Test
            void primitiveNutNullable() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    public void setMyInt(short i) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isNotEmpty();

                ConfigurationPropertyDescription desc = result.get();
                assertThat(desc.isNullable()).isFalse();
            }

            @Test
            void compositeNullable() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    public void setComp(NestedProp p) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isNotEmpty();

                ConfigurationPropertyDescription desc = result.get();
                assertThat(desc.isNullable()).isTrue();
            }

            @Test
            void wrapperNullable() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    public void setProp(Boolean b) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isNotEmpty();

                ConfigurationPropertyDescription desc = result.get();
                assertThat(desc.isNullable()).isTrue();
            }

            @Test
            void notNullAnnotation() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    @NotNull Boolean prop;

                    public void setProp(Boolean b) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isNotEmpty();

                ConfigurationPropertyDescription desc = result.get();
                assertThat(desc.isNullable()).isFalse();
            }

            @Test
            void notBlankAnnotation() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    @NotBlank String prop;

                    public void setProp(String b) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isNotEmpty();

                ConfigurationPropertyDescription desc = result.get();
                assertThat(desc.isNullable()).isFalse();
            }

            @Test
            void notEmptyAnnotation() {
                PropertyDescriptor prop = getFirstProperty(new Object() {
                    @NotEmpty String prop;

                    public void setProp(String b) {
                    }
                });

                Optional<ConfigurationPropertyDescription> result = provider.toDescription(prop);
                assertThat(result).isNotEmpty();

                ConfigurationPropertyDescription desc = result.get();
                assertThat(desc.isNullable()).isFalse();
            }
        }
    }

    enum TestEnum {
        VAL_A, VAL_B, VAL_C
    }

    @Nested
    class SetTerminalType {

        @Test
        void enumType() {
            ConfigurationPropertyDescription result = provider.setTerminalType(TestEnum.class, ConfigurationPropertyDescription
                    .builder()).build();

            assertThat(result.getType()).isEqualTo(ConfigurationPropertyType.ENUM);
            assertThat(result.getEnumValues()).containsExactly("VAL_A", "VAL_B", "VAL_C");
        }

        @Test
        void booleanType() {
            ConfigurationPropertyDescription result = provider.setTerminalType(Boolean.class, ConfigurationPropertyDescription
                    .builder()).build();

            assertThat(result.getType()).isEqualTo(ConfigurationPropertyType.BOOLEAN);
        }

        @Test
        void intType() {
            ConfigurationPropertyDescription result = provider.setTerminalType(long.class, ConfigurationPropertyDescription
                    .builder()).build();

            assertThat(result.getType()).isEqualTo(ConfigurationPropertyType.INTEGER);
        }

        @Test
        void floatType() {
            ConfigurationPropertyDescription result = provider.setTerminalType(double.class, ConfigurationPropertyDescription
                    .builder()).build();

            assertThat(result.getType()).isEqualTo(ConfigurationPropertyType.FLOAT);
        }

        @Test
        void stringType() {
            ConfigurationPropertyDescription result = provider.setTerminalType(String.class, ConfigurationPropertyDescription
                    .builder()).build();

            assertThat(result.getType()).isEqualTo(ConfigurationPropertyType.STRING);
        }

        @Test
        void urlType() {
            ConfigurationPropertyDescription result = provider.setTerminalType(URL.class, ConfigurationPropertyDescription
                    .builder()).build();

            assertThat(result.getType()).isEqualTo(ConfigurationPropertyType.STRING);
        }

        @Test
        void durationType() {
            ConfigurationPropertyDescription result = provider.setTerminalType(Duration.class, ConfigurationPropertyDescription
                    .builder()).build();

            assertThat(result.getType()).isEqualTo(ConfigurationPropertyType.DURATION);
        }

    }

}
