package rocks.inspectit.oce.target;

import org.openjdk.jmh.infra.Blackhole;

public class MethodHookTarget {

    public void methodNoAction() {
        Blackhole.consumeCPU(1);
    }

    public void methodResponseTime() {
        Blackhole.consumeCPU(1);
    }

    public void methodNotInstrumented() {
        Blackhole.consumeCPU(1);
    }

}
