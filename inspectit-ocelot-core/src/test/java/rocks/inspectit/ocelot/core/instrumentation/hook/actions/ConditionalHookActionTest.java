package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.config.model.instrumentation.actions.ConditionalActionSettings;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConditionalHookActionTest {

    @Mock
    IHookAction actualAction;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IHookAction.ExecutionContext ctx;

    @Nested
    class OnlyIfNotNull {

        @Test
        void conditionMet() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfNotNull("my_data");
            when(ctx.getInspectitContext().getData(eq("my_data"))).thenReturn("something");
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

            conditional.execute(ctx);

            verify(actualAction).execute(same(ctx));
        }

        @Test
        void conditionNotMet() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfNotNull("my_data");
            when(ctx.getInspectitContext().getData(eq("my_data"))).thenReturn(null);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

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
            when(ctx.getInspectitContext().getData(eq("my_data"))).thenReturn(null);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

            conditional.execute(ctx);

            verify(actualAction).execute(same(ctx));
        }

        @Test
        void conditionNotMet() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfNull("my_data");
            when(ctx.getInspectitContext().getData(eq("my_data"))).thenReturn("something");
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

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
            when(ctx.getInspectitContext().getData(eq("my_data"))).thenReturn(true);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

            conditional.execute(ctx);

            verify(actualAction).execute(same(ctx));
        }

        @Test
        void conditionValueIsFalse() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfTrue("my_data");
            when(ctx.getInspectitContext().getData(eq("my_data"))).thenReturn(false);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

            conditional.execute(ctx);

            verify(actualAction, never()).execute(any());
        }

        @Test
        void conditionValueIsNull() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfTrue("my_data");
            when(ctx.getInspectitContext().getData(eq("my_data"))).thenReturn(null);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

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
            when(ctx.getInspectitContext().getData(eq("my_data"))).thenReturn(false);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

            conditional.execute(ctx);

            verify(actualAction).execute(same(ctx));
        }

        @Test
        void conditionValueIsTrue() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfFalse("my_data");
            when(ctx.getInspectitContext().getData(eq("my_data"))).thenReturn(true);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

            conditional.execute(ctx);

            verify(actualAction, never()).execute(any());
        }

        @Test
        void conditionValueIsNull() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfFalse("my_data");
            when(ctx.getInspectitContext().getData(eq("my_data"))).thenReturn(null);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

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
            lenient().when(ctx.getInspectitContext().getData(eq("true_check"))).thenReturn(true);
            lenient().when(ctx.getInspectitContext().getData(eq("not_null_check"))).thenReturn("something");
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

            conditional.execute(ctx);

            verify(actualAction).execute(same(ctx));
        }


        @Test
        void oneConditionNotMet() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfTrue("true_check");
            settings.setOnlyIfNotNull("not_null_check");
            lenient().when(ctx.getInspectitContext().getData(eq("true_check"))).thenReturn(false);
            lenient().when(ctx.getInspectitContext().getData(eq("not_null_check"))).thenReturn("something");
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

            conditional.execute(ctx);

            verify(actualAction, never()).execute(any());
        }


        @Test
        void bothConditionsNotMet() {
            ConditionalActionSettings settings = new ConditionalActionSettings();
            settings.setOnlyIfTrue("true_check");
            settings.setOnlyIfNotNull("not_null_check");
            lenient().when(ctx.getInspectitContext().getData(eq("true_check"))).thenReturn(false);
            lenient().when(ctx.getInspectitContext().getData(eq("not_null_check"))).thenReturn(null);
            IHookAction conditional = ConditionalHookAction.wrapWithConditionChecks(settings, actualAction);

            conditional.execute(ctx);

            verify(actualAction, never()).execute(any());
        }
    }

}
