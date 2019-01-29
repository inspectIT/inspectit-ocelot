package rocks.inspectit.oce.core.instrumentation.hook;

import net.bytebuddy.asm.Advice;

public class DispatchHookAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Object origin) {
        System.out.println("\tEntering: " + origin);
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Origin Object origin) {
        System.out.println("\tExiting: " + origin);
    }

}
