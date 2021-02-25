package rocks.inspectit.ocelot.config.model.instrumentation.scope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.InternalSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.SpecialSensorSettings;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.validation.Violation;
import rocks.inspectit.ocelot.config.validation.ViolationBuilder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;


public class InstrumentationScopeSettingsTest {

    InspectitConfig root;

    InstrumentationSettings instr;

    MetricsSettings metrics;

    InstrumentationScopeSettings scope;

    @BeforeEach
    void setupDefaultSettings() {
        instr = new InstrumentationSettings();
        instr.setSpecial(new SpecialSensorSettings());
        instr.setInternal(new InternalSettings());

        scope = new InstrumentationScopeSettings();
        metrics = new MetricsSettings();

        root = new InspectitConfig();
        root.setInstrumentation(instr);
        root.setMetrics(metrics);
    }

    @Nested
    class PerformValidation {

        @Test
        void testNonExistingScope() {
            String scopeName = "scope-a";

            Map<String, Boolean> excludeOptions = new HashMap<>();
            excludeOptions.put("scope-b", true);

            InstrumentationScopeSettings scopeSettings = new InstrumentationScopeSettings();
            scopeSettings.setExclude(excludeOptions);

            Map<String, InstrumentationScopeSettings> scopes = new HashMap<>();
            scopes.put(scopeName, scopeSettings);

            instr.setScopes(scopes);

            List<Violation> violations = new ArrayList<>();
            instr.performValidation(root, new ViolationBuilder(violations));

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage())
                    .containsIgnoringCase("scope")
                    .containsIgnoringCase("not")
                    .containsIgnoringCase("defined");
        }

        @Test
        void testDirectCyclicalDependence() {
            String scopeNameA = "scope-a";
            String scopeNameB = "scope-b";

            Map<String, Boolean> excludeOptionsA = new HashMap<>();
            excludeOptionsA.put("scope-b", true);
            Map<String, Boolean> excludeOptionsB = new HashMap<>();
            excludeOptionsB.put("scope-a", true);

            InstrumentationScopeSettings scopeSettingsA = new InstrumentationScopeSettings();
            scopeSettingsA.setExclude(excludeOptionsA);
            InstrumentationScopeSettings scopeSettingsB = new InstrumentationScopeSettings();
            scopeSettingsB.setExclude(excludeOptionsB);

            Map<String, InstrumentationScopeSettings> scopes = new HashMap<>();
            scopes.put(scopeNameA, scopeSettingsA);
            scopes.put(scopeNameB, scopeSettingsB);

            instr.setScopes(scopes);


            List<Violation> violations = new ArrayList<>();
            instr.performValidation(root, new ViolationBuilder(violations));

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage())
                    .containsIgnoringCase("scope")
                    .containsIgnoringCase("cyclical")
                    .containsIgnoringCase("dependence");
            assertThat(violations.get(0).getParameters())
                    .containsValue("scope-b")
                    .containsValue("scope-a -> scope-b -> scope-a");
        }

        @Test
        void testIndirectCyclicalDependence() {
            String scopeNameA = "scope-a";
            String scopeNameB = "scope-b";
            String scopeNameC = "scope-c";

            Map<String, Boolean> excludeOptionsA = new HashMap<>();
            excludeOptionsA.put("scope-b", true);
            Map<String, Boolean> excludeOptionsB = new HashMap<>();
            excludeOptionsB.put("scope-c", true);
            Map<String, Boolean> excludeOptionsC = new HashMap<>();
            excludeOptionsC.put("scope-a", true);

            InstrumentationScopeSettings scopeSettingsA = new InstrumentationScopeSettings();
            scopeSettingsA.setExclude(excludeOptionsA);
            InstrumentationScopeSettings scopeSettingsB = new InstrumentationScopeSettings();
            scopeSettingsB.setExclude(excludeOptionsB);
            InstrumentationScopeSettings scopeSettingsC = new InstrumentationScopeSettings();
            scopeSettingsC.setExclude(excludeOptionsC);

            Map<String, InstrumentationScopeSettings> scopes = new HashMap<>();
            scopes.put(scopeNameA, scopeSettingsA);
            scopes.put(scopeNameB, scopeSettingsB);
            scopes.put(scopeNameC, scopeSettingsC);

            instr.setScopes(scopes);


            List<Violation> violations = new ArrayList<>();
            instr.performValidation(root, new ViolationBuilder(violations));

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).getMessage())
                    .containsIgnoringCase("scope")
                    .containsIgnoringCase("cyclical")
                    .containsIgnoringCase("dependence");
            assertThat(violations.get(0).getParameters())
                    .containsValue("scope-c")
                    .containsValue("scope-a -> scope-b -> scope-c -> scope-a");
        }

        @Test
        void testMultipleExcludes() {
            String scopeNameA = "scope-a";
            String scopeNameB = "scope-b";
            String scopeNameC = "scope-c";

            Map<String, Boolean> excludeOptionsA = new HashMap<>();
            excludeOptionsA.put("scope-b", true);
            excludeOptionsA.put("scope-c", true);
            Map<String, Boolean> excludeOptionsB = new HashMap<>();
            excludeOptionsB.put("scope-c", true);

            InstrumentationScopeSettings scopeSettingsA = new InstrumentationScopeSettings();
            scopeSettingsA.setExclude(excludeOptionsA);
            InstrumentationScopeSettings scopeSettingsB = new InstrumentationScopeSettings();
            scopeSettingsB.setExclude(excludeOptionsB);
            InstrumentationScopeSettings scopeSettingsC = new InstrumentationScopeSettings();

            Map<String, InstrumentationScopeSettings> scopes = new HashMap<>();
            scopes.put(scopeNameA, scopeSettingsA);
            scopes.put(scopeNameB, scopeSettingsB);
            scopes.put(scopeNameC, scopeSettingsC);

            instr.setScopes(scopes);


            List<Violation> violations = new ArrayList<>();
            instr.performValidation(root, new ViolationBuilder(violations));

            assertThat(violations).hasSize(0);
        }

        @Test
        void testInactiveExcludes() {
            String scopeNameA = "scope-a";
            String scopeNameB = "scope-b";
            String scopeNameC = "scope-c";

            Map<String, Boolean> excludeOptionsA = new HashMap<>();
            excludeOptionsA.put("scope-b", true);
            Map<String, Boolean> excludeOptionsB = new HashMap<>();
            excludeOptionsB.put("scope-c", true);
            Map<String, Boolean> excludeOptionsC = new HashMap<>();
            excludeOptionsC.put("scope-a", false);

            InstrumentationScopeSettings scopeSettingsA = new InstrumentationScopeSettings();
            scopeSettingsA.setExclude(excludeOptionsA);
            InstrumentationScopeSettings scopeSettingsB = new InstrumentationScopeSettings();
            scopeSettingsB.setExclude(excludeOptionsB);
            InstrumentationScopeSettings scopeSettingsC = new InstrumentationScopeSettings();
            scopeSettingsC.setExclude(excludeOptionsC);

            Map<String, InstrumentationScopeSettings> scopes = new HashMap<>();
            scopes.put(scopeNameA, scopeSettingsA);
            scopes.put(scopeNameB, scopeSettingsB);
            scopes.put(scopeNameC, scopeSettingsC);

            instr.setScopes(scopes);


            List<Violation> violations = new ArrayList<>();
            instr.performValidation(root, new ViolationBuilder(violations));

            assertThat(violations).hasSize(0);
        }

    }

}

