package rocks.inspectit.oce.core.instrumentation.hook;

import lombok.Value;
import rocks.inspectit.oce.bootstrap.context.IInspectitContext;

public interface IHookAction {

    void execute(ExecutionContext context);

    String getName();

    @Value
    class ExecutionContext {

        private Object[] methodArguments;

        private Object thiz;

        private Object returnValue;

        private Throwable thrown;

        private MethodHook hook;

        private IInspectitContext inspectitContext;

    }
}
