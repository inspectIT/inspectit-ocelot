package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ConditionalActionSettings;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessorFactory;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConditionalHookActionTest {

    @Mock
    IHookAction actualAction;

    @Mock
    IHookAction.ExecutionContext ctx;

    @Mock
    VariableAccessorFactory variableAccessorFactory;

    @Nested
    class OnlyIfNotNull {

        @Test
        void conditionMet() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfNotNull("my_data");
            when(variableAccessorFactory.getVariableAccessor("my_data")).thenReturn((ctx) -> "something");
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction).execute(same(ctx));
        }

        @Test
        void conditionNotMet() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfNotNull("my_data");
            when(variableAccessorFactory.getVariableAccessor("my_data")).thenReturn((ctx) -> null);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction, never()).execute(any());
        }
    }

    @Nested
    class OnlyIfNull {

        @Test
        void conditionMet() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfNull("my_data");
            when(variableAccessorFactory.getVariableAccessor("my_data")).thenReturn((ctx) -> null);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction).execute(same(ctx));
        }

        @Test
        void conditionNotMet() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfNull("my_data");
            when(variableAccessorFactory.getVariableAccessor("my_data")).thenReturn((ctx) -> "something");
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction, never()).execute(any());
        }
    }

    @Nested
    class OnlyIfTrue {

        @Test
        void conditionValueIsTrue() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfTrue("my_data");
            when(variableAccessorFactory.getVariableAccessor("my_data")).thenReturn((ctx) -> true);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction).execute(same(ctx));
        }

        @Test
        void conditionValueIsFalse() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfTrue("my_data");
            when(variableAccessorFactory.getVariableAccessor("my_data")).thenReturn((ctx) -> false);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction, never()).execute(any());
        }

        @Test
        void conditionValueIsNull() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfTrue("my_data");
            when(variableAccessorFactory.getVariableAccessor("my_data")).thenReturn((ctx) -> null);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction, never()).execute(any());
        }
    }

    @Nested
    class OnlyIfFalse {

        @Test
        void conditionValueIsFalse() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfFalse("my_data");
            when(variableAccessorFactory.getVariableAccessor("my_data")).thenReturn((ctx) -> false);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction).execute(same(ctx));
        }

        @Test
        void conditionValueIsTrue() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfFalse("my_data");
            when(variableAccessorFactory.getVariableAccessor("my_data")).thenReturn((ctx) -> true);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction, never()).execute(any());
        }

        @Test
        void conditionValueIsNull() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfFalse("my_data");
            when(variableAccessorFactory.getVariableAccessor("my_data")).thenReturn((ctx) -> null);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction, never()).execute(any());
        }
    }

    @Nested
    class Conjunction {

        @Test
        void bothConditionsMet() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfTrue("true_check");
            settings.setOnlyIfNotNull("not_null_check");
            doReturn((VariableAccessor) (ctx) -> true).when(variableAccessorFactory).getVariableAccessor("true_check");
            doReturn((VariableAccessor) (ctx) -> "something").when(variableAccessorFactory)
                    .getVariableAccessor("not_null_check");
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction).execute(same(ctx));
        }

        @Test
        void oneConditionNotMet() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfTrue("true_check");
            settings.setOnlyIfNotNull("not_null_check");
            doReturn((VariableAccessor) (ctx) -> false).when(variableAccessorFactory).getVariableAccessor("true_check");
            doReturn((VariableAccessor) (ctx) -> "something").when(variableAccessorFactory)
                    .getVariableAccessor("not_null_check");
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction, never()).execute(any());
        }

        @Test
        void bothConditionsNotMet() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfTrue("true_check");
            settings.setOnlyIfNotNull("not_null_check");
            doReturn((VariableAccessor) (ctx) -> false).when(variableAccessorFactory).getVariableAccessor("true_check");
            doReturn((VariableAccessor) (ctx) -> null).when(variableAccessorFactory)
                    .getVariableAccessor("not_null_check");
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction, variableAccessorFactory);

            conditional.execute(ctx);

            verify(actualAction, never()).execute(any());
        }
    }

}
