package rocks.inspectit.oce.core.instrumentation.config;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.GenericDataProviderSettings;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedGenericDataProviderConfig;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DataProviderResolverUnitTest {

    @InjectMocks
    private DataProviderResolver resolver = new DataProviderResolver();

    @Nested
    public class ResolveDataProviders {

        private static final String PROVIDER_NAME = "My-Provider1";
        private static final String PROVIDER_VALUE_BODY = "return new Integer(42);";

        InstrumentationSettings config;
        GenericDataProviderSettings inputProvider;

        @BeforeEach
        void setup() {
            config = new InstrumentationSettings();
            inputProvider = new GenericDataProviderSettings();
            inputProvider.setValueBody(PROVIDER_VALUE_BODY);
            config.setDataProviders(Maps.newHashMap(PROVIDER_NAME, inputProvider));
        }

        @Test
        void verifyCaseSensitivity() {
            Map<String, ResolvedGenericDataProviderConfig> result = resolver.resolveProviders(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(PROVIDER_NAME);
            assertThat(result.get(PROVIDER_NAME).getName()).isEqualTo(PROVIDER_NAME);
        }

        @Test
        void verifyThizTypeExtracted() {
            inputProvider.getInput().put("thiz", "MyClass");

            Map<String, ResolvedGenericDataProviderConfig> result = resolver.resolveProviders(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(PROVIDER_NAME);

            ResolvedGenericDataProviderConfig rc = result.get(PROVIDER_NAME);
            assertThat(rc.getExpectedThisType()).isEqualTo("MyClass");
            assertThat(rc.getName()).isEqualTo(PROVIDER_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo(PROVIDER_VALUE_BODY);
        }


        @Test
        void verifyMethodArgumentsExtracted() {
            inputProvider.getInput().put("arg1", "MyClass");
            inputProvider.getInput().put("arg3", "MyOtherClass");

            Map<String, ResolvedGenericDataProviderConfig> result = resolver.resolveProviders(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(PROVIDER_NAME);

            ResolvedGenericDataProviderConfig rc = result.get(PROVIDER_NAME);
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(PROVIDER_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).hasSize(2);
            assertThat(rc.getExpectedArgumentTypes()).containsEntry(1, "MyClass");
            assertThat(rc.getExpectedArgumentTypes()).containsEntry(3, "MyOtherClass");
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo(PROVIDER_VALUE_BODY);
        }


        @Test
        void verifyAdditionalArgumentsExtracted() {
            inputProvider.getInput().put("argument", "MyClass");
            inputProvider.getInput().put("x", "MyOtherClass");

            Map<String, ResolvedGenericDataProviderConfig> result = resolver.resolveProviders(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(PROVIDER_NAME);

            ResolvedGenericDataProviderConfig rc = result.get(PROVIDER_NAME);
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(PROVIDER_NAME);
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getAdditionalArgumentTypes()).hasSize(2);
            assertThat(rc.getAdditionalArgumentTypes()).containsEntry("argument", "MyClass");
            assertThat(rc.getAdditionalArgumentTypes()).containsEntry("x", "MyOtherClass");
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo(PROVIDER_VALUE_BODY);
        }

        @Test
        void verifyReturnValueTypeExtracted() {
            inputProvider.getInput().put("returnValue", "MyClass");

            Map<String, ResolvedGenericDataProviderConfig> result = resolver.resolveProviders(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(PROVIDER_NAME);

            ResolvedGenericDataProviderConfig rc = result.get(PROVIDER_NAME);
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(PROVIDER_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedReturnValueType()).isEqualTo("MyClass");
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo(PROVIDER_VALUE_BODY);
        }

        @Test
        void verifyThrownExtracted() {
            inputProvider.getInput().put("thrown", "java.lang.Throwable");

            Map<String, ResolvedGenericDataProviderConfig> result = resolver.resolveProviders(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(PROVIDER_NAME);

            ResolvedGenericDataProviderConfig rc = result.get(PROVIDER_NAME);
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(PROVIDER_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isTrue();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo(PROVIDER_VALUE_BODY);
        }

        @Test
        void verifyImportedPackagedExtracted() {
            inputProvider.getImports().add("my.package");
            inputProvider.getImports().add("my.other.package");

            Map<String, ResolvedGenericDataProviderConfig> result = resolver.resolveProviders(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(PROVIDER_NAME);

            ResolvedGenericDataProviderConfig rc = result.get(PROVIDER_NAME);
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(PROVIDER_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).containsExactly("my.package", "my.other.package");
            assertThat(rc.getValueBody()).isEqualTo(PROVIDER_VALUE_BODY);
        }

        @Test
        void verifyValueUsedIfPresent() {
            inputProvider.setValueBody(null);
            inputProvider.setValue("\"Test\"");

            Map<String, ResolvedGenericDataProviderConfig> result = resolver.resolveProviders(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(PROVIDER_NAME);

            ResolvedGenericDataProviderConfig rc = result.get(PROVIDER_NAME);
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(PROVIDER_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo("return \"Test\";");
        }
    }
}
