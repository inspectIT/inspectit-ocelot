package rocks.inspectit.ocelot.core.instrumentation.hook;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IMethodHook;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * This class provides the hook implementations which are injected into specified classes in order to gather data.
 *
 * <p>
 * IMPORTANT: Note that the implementation is inlined into the target application, thus, it have no access to the classes
 * loaded by the inspectIT classloader! When needed, the classes provided in the bootstrap package have to be used!
 */
public class DispatchHookAdvices {

    public static <T> DynamicType.Builder<T> adviceOn(DynamicType.Builder<T> builder, ElementMatcher<? super MethodDescription> methods) {
        // @formatter:off
        builder = builder.visit(Advice.to(NonStaticMethodAdvice.class)
                .on(not(isStatic()).and(not(isConstructor())).and(methods)));

        builder = builder.visit(Advice.to(StaticMethodAdvice.class).on(isStatic().and(methods)));

        builder = builder.visit(Advice.to(ConstructorAdvice.class).on(isConstructor().and(methods)));
        // @formatter:on
        return builder;
    }

    private static class NonStaticMethodAdvice {

        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Origin Class<?> declaringClass, @Advice.Origin("#m#s") String signature, @Advice.AllArguments Object[] args, @Advice.This Object thiz, @Advice.Local("hook") IMethodHook hook, @Advice.Local("context") InternalInspectitContext context) {
            hook = Instances.hookManager.getHook(declaringClass, signature);
            context = hook.onEnter(args, thiz);
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onExit(@Advice.AllArguments Object[] args, @Advice.This Object thiz, @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnValue, @Advice.Thrown Throwable thrown, @Advice.Local("hook") IMethodHook hook, @Advice.Local("context") InternalInspectitContext context) {
            hook.onExit(args, thiz, returnValue, thrown, context);
        }
    }

    private static class ConstructorAdvice {

        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Origin Class<?> declaringClass, @Advice.Origin("#m#s") String signature, @Advice.AllArguments Object[] args, @Advice.Local("hook") IMethodHook hook, @Advice.Local("context") InternalInspectitContext context) {
            hook = Instances.hookManager.getHook(declaringClass, signature);
            context = hook.onEnter(args, null);
        }

        @Advice.OnMethodExit
        public static void onExit(@Advice.AllArguments Object[] args, @Advice.This Object thiz, @Advice.Local("hook") IMethodHook hook, @Advice.Local("context") InternalInspectitContext context) {
            hook.onExit(args, thiz, null, null, context);
        }
    }

    private static class StaticMethodAdvice {

        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Origin Class<?> declaringClass, @Advice.Origin("#m#s") String signature, @Advice.AllArguments Object[] args, @Advice.Local("hook") IMethodHook hook, @Advice.Local("context") InternalInspectitContext context) {
            hook = Instances.hookManager.getHook(declaringClass, signature);
            context = hook.onEnter(args, null);
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onExit(@Advice.AllArguments Object[] args, @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnValue, @Advice.Thrown Throwable thrown, @Advice.Local("hook") IMethodHook hook, @Advice.Local("context") InternalInspectitContext context) {
            hook.onExit(args, null, returnValue, thrown, context);
        }
    }

}
