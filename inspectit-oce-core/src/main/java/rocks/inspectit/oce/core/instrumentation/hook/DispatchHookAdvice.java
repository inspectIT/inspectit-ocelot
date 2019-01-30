package rocks.inspectit.oce.core.instrumentation.hook;

import net.bytebuddy.asm.Advice;

/**
 * This class provides the hook implementations which are injected into specified classes in order to gather data.
 * <p>
 * IMPORTANT: Note that the implementation is inlined into the target application, thus, it have no access to the classes
 * loaded by the inspectIT classloader! When needed, the classes provided in the bootstrap package have to be used!
 */
public class DispatchHookAdvice {

    /**
     * The content of this method is added before the target method! See {@link Advice} for existing parameter annotations.
     */
    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Object origin) {
        System.out.println("\tEntering: " + origin);
    }

    /**
     * The content of this method is added after the target method! See {@link Advice} for existing parameter annotations.
     */
    @Advice.OnMethodExit
    public static void onExit(@Advice.Origin Object origin) {
        System.out.println("\tExiting: " + origin);
    }

}
