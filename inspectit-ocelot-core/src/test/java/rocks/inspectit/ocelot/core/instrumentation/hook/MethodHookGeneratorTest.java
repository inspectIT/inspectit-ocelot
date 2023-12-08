package rocks.inspectit.ocelot.core.instrumentation.hook;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.config.model.selfmonitoring.ActionTracingMode;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextManager;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.model.MetricAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.span.EndSpanAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.span.SetSpanStatusAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.span.WriteSpanAttributesAction;
import rocks.inspectit.ocelot.core.privacy.obfuscation.ObfuscationManager;
import rocks.inspectit.ocelot.core.selfmonitoring.ActionScopeFactory;
import rocks.inspectit.ocelot.core.testutils.Dummy;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MethodHookGeneratorTest {

    @InjectMocks
    MethodHookGenerator generator;

    @Mock
    ContextManager contextManager;

    @Mock
    ActionScopeFactory actionScopeFactory;

    @Mock
    VariableAccessorFactory variableAccessorFactory;

    @Mock
    ObfuscationManager obfuscation;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InspectitEnvironment environment;

    @BeforeEach
    public void setupEnvironment() {
        lenient().when(environment.getCurrentConfig().getSelfMonitoring().getActionTracing()).thenReturn(ActionTracingMode.ALL_WITH_DEFAULT);
    }

    @Nested
    class BuildHook {

        private final TypeDescription dummyType = TypeDescription.ForLoadedType.of(Dummy.class);

        @Test
        void verifyContextManagerProvided() {
            MethodDescription method = dummyType.getDeclaredMethods().stream()
                    .filter(md -> md.getName().equals("doSomething"))
                    .findFirst().get();
            MethodHookConfiguration config = MethodHookConfiguration.builder().build();

            MethodHook result = generator.buildHook(Dummy.class, method, config);

            Assertions.assertThat(result.getInspectitContextManager()).isSameAs(contextManager);
        }

        @Test
        void verifyConstructorNameAndParameterTypesCorrect() {
            MethodDescription constructor = dummyType.getDeclaredMethods().stream()
                    .filter(MethodDescription::isConstructor)
                    .filter(md -> md.getParameters().size() == 2)
                    .findFirst().get();
            MethodHookConfiguration config = MethodHookConfiguration.builder().build();

            MethodHook result = generator.buildHook(Dummy.class, constructor, config);

            assertThat(result.getMethodInformation().getName()).isEqualTo("<init>");
            assertThat(result.getMethodInformation().getParameterTypes()).containsExactly(String.class, int.class);
        }

        @Test
        void verifyMethodNameAndParameterTypesCorrect() {
            MethodDescription method = dummyType.getDeclaredMethods().stream()
                    .filter(md -> md.getName().equals("doSomething"))
                    .findFirst().get();
            MethodHookConfiguration config = MethodHookConfiguration.builder().build();

            MethodHook result = generator.buildHook(Dummy.class, method, config);

            assertThat(result.getMethodInformation().getName()).isEqualTo("doSomething");
            assertThat(result.getMethodInformation().getParameterTypes()).containsExactly(long.class, String.class);
        }

        @Test
        void verifyDeclaringClassCorrect() {
            MethodDescription method = dummyType.getDeclaredMethods().stream()
                    .filter(md -> md.getName().equals("doSomething"))
                    .findFirst().get();
            MethodHookConfiguration config = MethodHookConfiguration.builder().build();

            MethodHook result = generator.buildHook(Dummy.class, method, config);

            assertThat(result.getMethodInformation().getDeclaringClass()).isSameAs(Dummy.class);
        }
    }

    @Nested
    class BuildMetricAccessor {

        @Test
        public void valueOnly() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(variableAccessorFactory.getConstantAccessor(1D)).thenReturn(mockAccessor);
            MetricRecordingSettings settings = MetricRecordingSettings.builder().metric("name").value("1.0").build();

            MetricAccessor accessor = generator.buildMetricAccessor(settings);

            assertThat(accessor.getVariableAccessor()).isSameAs(mockAccessor);
            assertThat(accessor.getConstantTags()).isEmpty();
            assertThat(accessor.getDataTagAccessors()).isEmpty();
        }

        @Test
        public void dataValueOnly() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(variableAccessorFactory.getVariableAccessor("data-key")).thenReturn(mockAccessor);
            MetricRecordingSettings settings = MetricRecordingSettings.builder().metric("name").value("data-key").build();

            MetricAccessor accessor = generator.buildMetricAccessor(settings);

            assertThat(accessor.getVariableAccessor()).isSameAs(mockAccessor);
            assertThat(accessor.getConstantTags()).isEmpty();
            assertThat(accessor.getDataTagAccessors()).isEmpty();
        }

        @Test
        public void valueAndConstantTag() {
            VariableAccessor mockAccessor = mock(VariableAccessor.class);
            when(variableAccessorFactory.getConstantAccessor(1D)).thenReturn(mockAccessor);
            MetricRecordingSettings settings = MetricRecordingSettings.builder().metric("name").value("1.0")
                    .constantTags(Collections.singletonMap("tag-key", "tag-key")).build();

            MetricAccessor accessor = generator.buildMetricAccessor(settings);

            assertThat(accessor.getVariableAccessor()).isSameAs(mockAccessor);
            assertThat(accessor.getConstantTags()).containsOnly(entry("tag-key", "tag-key"));
            assertThat(accessor.getDataTagAccessors()).isEmpty();
        }

        @Test
        public void valueAndDataTag() {
            VariableAccessor mockAccessorA = mock(VariableAccessor.class);
            doReturn(mockAccessorA).when(variableAccessorFactory).getConstantAccessor(1D);
            VariableAccessor mockAccessorB = mock(VariableAccessor.class);
            doReturn(mockAccessorB).when(variableAccessorFactory).getVariableAccessor("tag-value");
            MetricRecordingSettings settings = MetricRecordingSettings.builder().metric("name").value("1.0")
                    .dataTags(Collections.singletonMap("tag-key", "tag-value")).build();

            MetricAccessor accessor = generator.buildMetricAccessor(settings);

            assertThat(accessor.getVariableAccessor()).isSameAs(mockAccessorA);
            assertThat(accessor.getConstantTags()).isEmpty();
            assertThat(accessor.getDataTagAccessors()).containsOnly(entry("tag-key", mockAccessorB));
        }
    }

    @Nested
    class BuildTracingExitActions {

        @Test
        public void verifyNoActionsGeneratedIfNoSpanStartedOrContinued() {
            RuleTracingSettings settings = RuleTracingSettings.builder()
                    .startSpan(false)
                    .continueSpan(null)
                    .errorStatus("foo")
                    .attributes(ImmutableMap.of("attKey", "attValue"))
                    .endSpan(true)
                    .build();

            MethodHookConfiguration config = MethodHookConfiguration.builder().tracing(settings).build();
            List<IHookAction> actions = generator.buildTracingExitActions(config);

            assertThat(actions).isEmpty();
        }

        @Test
        public void verifyActionsGeneratedIfSpanStarted() {
            RuleTracingSettings settings = RuleTracingSettings.builder()
                    .startSpan(true)
                    .continueSpan(null)
                    .errorStatus("foo")
                    .attributes(ImmutableMap.of("attKey", "attValue"))
                    .endSpan(true)
                    .build();

            MethodHookConfiguration config = MethodHookConfiguration.builder().tracing(settings).build();
            List<IHookAction> actions = generator.buildTracingExitActions(config);

            assertThat(actions)
                    .hasSize(3)
                    .anySatisfy((action) -> assertThat(action).isInstanceOf(SetSpanStatusAction.class))
                    .anySatisfy((action) -> assertThat(action).isInstanceOf(WriteSpanAttributesAction.class))
                    .anySatisfy((action) -> assertThat(action).isInstanceOf(EndSpanAction.class));
        }

        @Test
        public void verifyActionsGeneratedIfSpanContinued() {
            RuleTracingSettings settings = RuleTracingSettings.builder()
                    .startSpan(false)
                    .continueSpan("my span")
                    .errorStatus("foo")
                    .attributes(ImmutableMap.of("attKey", "attValue"))
                    .endSpan(true)
                    .build();

            MethodHookConfiguration config = MethodHookConfiguration.builder().tracing(settings).build();
            List<IHookAction> actions = generator.buildTracingExitActions(config);

            assertThat(actions)
                    .hasSize(3)
                    .anySatisfy((action) -> assertThat(action).isInstanceOf(SetSpanStatusAction.class))
                    .anySatisfy((action) -> assertThat(action).isInstanceOf(WriteSpanAttributesAction.class))
                    .anySatisfy((action) -> assertThat(action).isInstanceOf(EndSpanAction.class));
        }

    }

    @Nested
    class collectMetricTagsInTracing {

        private RuleTracingSettings createTracingSettings(Map<String, String> attributes) {
            return RuleTracingSettings.builder()
                    .startSpan(true)
                    .continueSpan(null)
                    .attributes(attributes)
                    .endSpan(true)
                    .build();
        }

        private MetricRecordingSettings createMetricSettings(Map<String,String> dataTags, Map<String,String> constantTags) {
            return MetricRecordingSettings.builder().metric("name").value("1.0")
                    .dataTags(dataTags).constantTags(constantTags).build();
        }

        @Test
        public void verifyTagsHaveBeenAdded() {
            when(environment.getCurrentConfig().getTracing().isAddMetricTags()).thenReturn(true);
            VariableAccessor tracingAccessor = (context) -> "tracing-text";
            VariableAccessor dataAccessor = (context) -> "data-text";
            VariableAccessor constantAccessor = (context) -> "constant-text";
            when(variableAccessorFactory.getVariableAccessor("tracing-value")).thenReturn(tracingAccessor);
            when(variableAccessorFactory.getVariableAccessor("data-value")).thenReturn(dataAccessor);
            when(variableAccessorFactory.getConstantAccessor(any())).thenReturn(constantAccessor);
            Map<String, VariableAccessor> expectedResult = new HashMap<>();
            expectedResult.put("tracing-key", tracingAccessor);
            expectedResult.put("data-tag", dataAccessor);
            expectedResult.put("constant-tag", constantAccessor);

            Map<String,String> attributes = ImmutableMap.of("tracing-key", "tracing-value");
            RuleTracingSettings tracing = createTracingSettings(attributes);

            Map<String, String> dataTags = ImmutableMap.of("data-tag", "data-value");
            Map<String, String> constantTags = ImmutableMap.of("constant-tag", "constant-value");
            MetricRecordingSettings metric = createMetricSettings(dataTags, constantTags);
            Multiset<MetricRecordingSettings> metrics = HashMultiset.create();
            metrics.add(metric);

            MethodHookConfiguration config = MethodHookConfiguration.builder().tracing(tracing).metrics(metrics).build();
            List<IHookAction> actions = generator.buildTracingExitActions(config);

            Optional<IHookAction> maybeAction = actions.stream().filter(action -> action instanceof WriteSpanAttributesAction).findFirst();
            assertThat(maybeAction.isPresent()).isTrue();
            WriteSpanAttributesAction attributeAction = (WriteSpanAttributesAction) maybeAction.get();

            Map<String,VariableAccessor> accessors = attributeAction.getAttributeAccessors();
            assertThat(accessors.size()).isEqualTo(expectedResult.size());
            assertThat(accessors).containsAllEntriesOf(expectedResult);
        }

        @Test
        public void verifyNoTagsHaveBeenAdded() {
            when(environment.getCurrentConfig().getTracing().isAddMetricTags()).thenReturn(false);
            VariableAccessor tracingAccessor = (context) -> "tracing-text";
            when(variableAccessorFactory.getVariableAccessor("tracing-value")).thenReturn(tracingAccessor);
            Map<String, VariableAccessor> expectedResult = new HashMap<>();
            expectedResult.put("tracing-key", tracingAccessor);

            Map<String,String> attributes = ImmutableMap.of("tracing-key", "tracing-value");
            RuleTracingSettings tracing = createTracingSettings(attributes);

            Map<String, String> dataTags = ImmutableMap.of("data-tag", "data-value");
            Map<String, String> constantTags = ImmutableMap.of("constant-tag", "constant-value");
            MetricRecordingSettings metric = createMetricSettings(dataTags, constantTags);
            Multiset<MetricRecordingSettings> metrics = HashMultiset.create();
            metrics.add(metric);

            MethodHookConfiguration config = MethodHookConfiguration.builder().tracing(tracing).metrics(metrics).build();
            List<IHookAction> actions = generator.buildTracingExitActions(config);

            Optional<IHookAction> maybeAction = actions.stream().filter(action -> action instanceof WriteSpanAttributesAction).findFirst();
            assertThat(maybeAction.isPresent()).isTrue();
            WriteSpanAttributesAction attributeAction = (WriteSpanAttributesAction) maybeAction.get();

            Map<String,VariableAccessor> accessors = attributeAction.getAttributeAccessors();
            assertThat(accessors.size()).isEqualTo(expectedResult.size());
            assertThat(accessors).containsAllEntriesOf(expectedResult);
        }

        @Test
        void verifyTracingOverwritesMetrics() {
            when(environment.getCurrentConfig().getTracing().isAddMetricTags()).thenReturn(true);
            VariableAccessor tracingAccessor = (context) -> "tracing-text";
            when(variableAccessorFactory.getVariableAccessor("tracing-value")).thenReturn(tracingAccessor);
            Map<String, VariableAccessor> expectedResult = new HashMap<>();
            expectedResult.put("one-key", tracingAccessor);

            Map<String,String> attributes = ImmutableMap.of("one-key", "tracing-value");
            RuleTracingSettings tracing = createTracingSettings(attributes);

            Map<String, String> dataTags = ImmutableMap.of("one-key", "data-value");
            Map<String, String> constantTags = ImmutableMap.of("one-key", "constant-value");
            MetricRecordingSettings metric = createMetricSettings(dataTags, constantTags);
            Multiset<MetricRecordingSettings> metrics = HashMultiset.create();
            metrics.add(metric);

            MethodHookConfiguration config = MethodHookConfiguration.builder().tracing(tracing).metrics(metrics).build();
            List<IHookAction> actions = generator.buildTracingExitActions(config);

            Optional<IHookAction> maybeAction = actions.stream().filter(action -> action instanceof WriteSpanAttributesAction).findFirst();
            assertThat(maybeAction.isPresent()).isTrue();
            WriteSpanAttributesAction attributeAction = (WriteSpanAttributesAction) maybeAction.get();

            Map<String,VariableAccessor> accessors = attributeAction.getAttributeAccessors();
            assertThat(accessors.size()).isEqualTo(expectedResult.size());
            assertThat(accessors).containsAllEntriesOf(expectedResult);
        }

        @Test
        void verifyDataTagsOverwriteConstantTags() {
            when(environment.getCurrentConfig().getTracing().isAddMetricTags()).thenReturn(true);
            VariableAccessor dataAccessor = (context) -> "data-text";
            when(variableAccessorFactory.getVariableAccessor("data-value")).thenReturn(dataAccessor);
            Map<String, VariableAccessor> expectedResult = new HashMap<>();
            expectedResult.put("one-key", dataAccessor);

            Map<String,String> attributes = Collections.emptyMap();
            RuleTracingSettings tracing = createTracingSettings(attributes);

            Map<String, String> dataTags = ImmutableMap.of("one-key", "data-value");
            Map<String, String> constantTags = ImmutableMap.of("one-key", "constant-value");
            MetricRecordingSettings metric = createMetricSettings(dataTags, constantTags);
            Multiset<MetricRecordingSettings> metrics = HashMultiset.create();
            metrics.add(metric);

            MethodHookConfiguration config = MethodHookConfiguration.builder().tracing(tracing).metrics(metrics).build();
            List<IHookAction> actions = generator.buildTracingExitActions(config);

            Optional<IHookAction> maybeAction = actions.stream().filter(action -> action instanceof WriteSpanAttributesAction).findFirst();
            assertThat(maybeAction.isPresent()).isTrue();
            WriteSpanAttributesAction attributeAction = (WriteSpanAttributesAction) maybeAction.get();

            Map<String,VariableAccessor> accessors = attributeAction.getAttributeAccessors();
            assertThat(accessors.size()).isEqualTo(expectedResult.size());
            assertThat(accessors).containsAllEntriesOf(expectedResult);
        }

        @Test
        void verifyOnlyDataTags() {
            when(environment.getCurrentConfig().getTracing().isAddMetricTags()).thenReturn(true);
            VariableAccessor dataAccessor = (context) -> "data-text";
            when(variableAccessorFactory.getVariableAccessor("data-value")).thenReturn(dataAccessor);
            Map<String, VariableAccessor> expectedResult = new HashMap<>();
            expectedResult.put("data-tag", dataAccessor);

            Map<String,String> attributes = Collections.emptyMap();
            RuleTracingSettings tracing = createTracingSettings(attributes);

            Map<String, String> dataTags = ImmutableMap.of("data-tag", "data-value");
            Map<String, String> constantTags = Collections.emptyMap();
            MetricRecordingSettings metric = createMetricSettings(dataTags, constantTags);
            Multiset<MetricRecordingSettings> metrics = HashMultiset.create();
            metrics.add(metric);

            MethodHookConfiguration config = MethodHookConfiguration.builder().tracing(tracing).metrics(metrics).build();
            List<IHookAction> actions = generator.buildTracingExitActions(config);

            Optional<IHookAction> maybeAction = actions.stream().filter(action -> action instanceof WriteSpanAttributesAction).findFirst();
            assertThat(maybeAction.isPresent()).isTrue();
            WriteSpanAttributesAction attributeAction = (WriteSpanAttributesAction) maybeAction.get();

            Map<String,VariableAccessor> accessors = attributeAction.getAttributeAccessors();
            assertThat(accessors.size()).isEqualTo(expectedResult.size());
            assertThat(accessors).containsAllEntriesOf(expectedResult);
        }

        @Test
        void verifyOnlyConstantTags() {
            when(environment.getCurrentConfig().getTracing().isAddMetricTags()).thenReturn(true);
            VariableAccessor constantAccessor = (context) -> "constant-text";
            when(variableAccessorFactory.getConstantAccessor(any())).thenReturn(constantAccessor);
            Map<String, VariableAccessor> expectedResult = new HashMap<>();
            expectedResult.put("constant-tag", constantAccessor);

            Map<String,String> attributes = Collections.emptyMap();
            RuleTracingSettings tracing = createTracingSettings(attributes);

            Map<String, String> dataTags = Collections.emptyMap();
            Map<String, String> constantTags = ImmutableMap.of("constant-tag", "constant-value");
            MetricRecordingSettings metric = createMetricSettings(dataTags, constantTags);
            Multiset<MetricRecordingSettings> metrics = HashMultiset.create();
            metrics.add(metric);

            MethodHookConfiguration config = MethodHookConfiguration.builder().tracing(tracing).metrics(metrics).build();
            List<IHookAction> actions = generator.buildTracingExitActions(config);

            Optional<IHookAction> maybeAction = actions.stream().filter(action -> action instanceof WriteSpanAttributesAction).findFirst();
            assertThat(maybeAction.isPresent()).isTrue();
            WriteSpanAttributesAction attributeAction = (WriteSpanAttributesAction) maybeAction.get();

            Map<String,VariableAccessor> accessors = attributeAction.getAttributeAccessors();
            assertThat(accessors.size()).isEqualTo(expectedResult.size());
            assertThat(accessors).containsAllEntriesOf(expectedResult);
        }

        @Test
        void verifyTagsOfMultipleMetrics() {
            when(environment.getCurrentConfig().getTracing().isAddMetricTags()).thenReturn(true);
            VariableAccessor tracingAccessor = (context) -> "tracing-text";
            VariableAccessor dataAccessor1 = (context) -> "data-text-1";
            VariableAccessor dataAccessor2 = (context) -> "data-text-2";
            when(variableAccessorFactory.getVariableAccessor("tracing-value")).thenReturn(tracingAccessor);
            when(variableAccessorFactory.getVariableAccessor("data-value-1")).thenReturn(dataAccessor1);
            when(variableAccessorFactory.getVariableAccessor("data-value-2")).thenReturn(dataAccessor2);
            Map<String, VariableAccessor> expectedResult = new HashMap<>();
            expectedResult.put("tracing-key", tracingAccessor);
            expectedResult.put("data-tag-1", dataAccessor1);
            expectedResult.put("data-tag-2", dataAccessor2);

            Map<String,String> attributes = ImmutableMap.of("tracing-key", "tracing-value");
            RuleTracingSettings tracing = createTracingSettings(attributes);

            Map<String, String> dataTags1 = ImmutableMap.of("data-tag-1", "data-value-1");
            Map<String, String> dataTags2 = ImmutableMap.of("data-tag-2", "data-value-2");
            Map<String, String> constantTags = Collections.emptyMap();
            MetricRecordingSettings metric1 = createMetricSettings(dataTags1, constantTags);
            MetricRecordingSettings metric2 = createMetricSettings(dataTags2, constantTags);
            Multiset<MetricRecordingSettings> metrics = HashMultiset.create();
            metrics.add(metric1);
            metrics.add(metric2);

            MethodHookConfiguration config = MethodHookConfiguration.builder().tracing(tracing).metrics(metrics).build();
            List<IHookAction> actions = generator.buildTracingExitActions(config);

            Optional<IHookAction> maybeAction = actions.stream().filter(action -> action instanceof WriteSpanAttributesAction).findFirst();
            assertThat(maybeAction.isPresent()).isTrue();
            WriteSpanAttributesAction attributeAction = (WriteSpanAttributesAction) maybeAction.get();

            Map<String,VariableAccessor> accessors = attributeAction.getAttributeAccessors();
            assertThat(accessors.size()).isEqualTo(expectedResult.size());
            assertThat(accessors).containsAllEntriesOf(expectedResult);
        }
    }
}
