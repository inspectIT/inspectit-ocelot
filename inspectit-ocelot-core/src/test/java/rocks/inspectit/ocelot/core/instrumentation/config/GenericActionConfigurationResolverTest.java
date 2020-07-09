package rocks.inspectit.ocelot.core.instrumentation.config;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericActionConfigurationResolverTest {

    @InjectMocks
    private GenericActionConfigurationResolver resolver = new GenericActionConfigurationResolver();

    @Nested
    public class ResolveActions {

        private static final String ACTION_NAME = "My-Action1";

        private static final String ACTION_VALUE_BODY = "return new Integer(42);";

        InstrumentationSettings config;

        GenericActionSettings inputAction;

        @BeforeEach
        void setup() {
            config = new InstrumentationSettings();
            inputAction = new GenericActionSettings();
            inputAction.setValueBody(ACTION_VALUE_BODY);
            config.setActions(Maps.newHashMap(ACTION_NAME, inputAction));
        }

        @Test
        void verifyNamePreserved() {
            Map<String, GenericActionConfig> result = resolver.resolveActions(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(ACTION_NAME);
            assertThat(result.get(ACTION_NAME).getName()).isEqualTo(ACTION_NAME);
        }

        @Test
        void verifyThisTypeExtracted() {
            inputAction.getInput().put(GenericActionSettings.THIS_VARIABLE, "MyClass");

            Map<String, GenericActionConfig> result = resolver.resolveActions(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(ACTION_NAME);

            GenericActionConfig rc = result.get(ACTION_NAME);
            assertThat(rc.isVoid()).isFalse();
            assertThat(rc.getExpectedThisType()).isEqualTo("MyClass");
            assertThat(rc.getName()).isEqualTo(ACTION_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo(ACTION_VALUE_BODY);
        }

        @Test
        void verifyMethodArgumentsExtracted() {
            inputAction.getInput().put(GenericActionSettings.ARG_VARIABLE_PREFIX + 1, "MyClass");
            inputAction.getInput().put(GenericActionSettings.ARG_VARIABLE_PREFIX + 3, "MyOtherClass");

            Map<String, GenericActionConfig> result = resolver.resolveActions(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(ACTION_NAME);

            GenericActionConfig rc = result.get(ACTION_NAME);
            assertThat(rc.isVoid()).isFalse();
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(ACTION_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).hasSize(2);
            assertThat(rc.getExpectedArgumentTypes()).containsEntry(1, "MyClass");
            assertThat(rc.getExpectedArgumentTypes()).containsEntry(3, "MyOtherClass");
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo(ACTION_VALUE_BODY);
        }

        @Test
        void verifyAdditionalArgumentsExtracted() {
            inputAction.getInput().put("argument", "MyClass");
            inputAction.getInput().put("x", "MyOtherClass");

            Map<String, GenericActionConfig> result = resolver.resolveActions(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(ACTION_NAME);

            GenericActionConfig rc = result.get(ACTION_NAME);
            assertThat(rc.isVoid()).isFalse();
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(ACTION_NAME);
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getAdditionalArgumentTypes()).hasSize(2);
            assertThat(rc.getAdditionalArgumentTypes()).containsEntry("argument", "MyClass");
            assertThat(rc.getAdditionalArgumentTypes()).containsEntry("x", "MyOtherClass");
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo(ACTION_VALUE_BODY);
        }

        @Test
        void verifyReturnValueTypeExtracted() {
            inputAction.getInput().put(GenericActionSettings.RETURN_VALUE_VARIABLE, "MyClass");

            Map<String, GenericActionConfig> result = resolver.resolveActions(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(ACTION_NAME);

            GenericActionConfig rc = result.get(ACTION_NAME);
            assertThat(rc.isVoid()).isFalse();
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(ACTION_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedReturnValueType()).isEqualTo("MyClass");
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo(ACTION_VALUE_BODY);
        }

        @Test
        void verifyThrownExtracted() {
            inputAction.getInput().put(GenericActionSettings.THROWN_VARIABLE, "java.lang.Throwable");

            Map<String, GenericActionConfig> result = resolver.resolveActions(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(ACTION_NAME);

            GenericActionConfig rc = result.get(ACTION_NAME);
            assertThat(rc.isVoid()).isFalse();
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(ACTION_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isTrue();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo(ACTION_VALUE_BODY);
        }

        @Test
        void verifyImportedPackagedExtracted() {
            inputAction.getImports().add("my.package");
            inputAction.getImports().add("my.other.package");

            Map<String, GenericActionConfig> result = resolver.resolveActions(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(ACTION_NAME);

            GenericActionConfig rc = result.get(ACTION_NAME);
            assertThat(rc.isVoid()).isFalse();
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(ACTION_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).containsExactly("my.package", "my.other.package");
            assertThat(rc.getValueBody()).isEqualTo(ACTION_VALUE_BODY);
        }

        @Test
        void verifyValueUsedIfPresentForNonVoid() {
            inputAction.setValueBody(null);
            inputAction.setValue("\"Test\"");

            Map<String, GenericActionConfig> result = resolver.resolveActions(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(ACTION_NAME);

            GenericActionConfig rc = result.get(ACTION_NAME);
            assertThat(rc.isVoid()).isFalse();
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(ACTION_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo("return \"Test\";");
        }

        @Test
        void verifyValueUsedIfPresentForVoid() {
            inputAction.setValueBody(null);
            inputAction.setIsVoid(true);
            inputAction.setValue("\"Test\"");

            Map<String, GenericActionConfig> result = resolver.resolveActions(config);

            assertThat(result).hasSize(1);
            assertThat(result).containsKey(ACTION_NAME);

            GenericActionConfig rc = result.get(ACTION_NAME);
            assertThat(rc.isVoid()).isTrue();
            assertThat(rc.getExpectedThisType()).isNull();
            assertThat(rc.getName()).isEqualTo(ACTION_NAME);
            assertThat(rc.getAdditionalArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedArgumentTypes()).isEmpty();
            assertThat(rc.getExpectedReturnValueType()).isNull();
            assertThat(rc.isUsesThrown()).isFalse();
            assertThat(rc.getImportedPackages()).isEmpty();
            assertThat(rc.getValueBody()).isEqualTo("\"Test\";");
        }
    }
}
