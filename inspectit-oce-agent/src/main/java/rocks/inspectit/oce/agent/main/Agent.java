package rocks.inspectit.oce.agent.main;

import java.lang.instrument.Instrumentation;

public class Agent {

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent Main called!");
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent Pre-Main called!");
    }
}