package rocks.inspectit.oce.core.instrumentation.sensors;

import net.bytebuddy.asm.Advice;

public class DispatchSensorAdvice {

    @Advice.OnMethodEnter
    public static long onEnter(@Advice.Origin Object origin) {
        System.out.println("\tEntering: " + origin);
        return System.nanoTime();
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Origin Object origin, @Advice.Enter long startTime) {
        System.out.println("\tExiting: " + origin);
        System.out.println("\t   Took: " + (System.nanoTime() - startTime) + "ns");
    }

}
