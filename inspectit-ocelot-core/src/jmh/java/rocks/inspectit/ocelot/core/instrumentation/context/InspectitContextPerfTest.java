package rocks.inspectit.ocelot.core.instrumentation.context;

import org.openjdk.jmh.annotations.*;
import rocks.inspectit.ocelot.core.instrumentation.config.model.DataProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class InspectitContextPerfTest {

    private Map<String, String> commonTags;

    private DataProperties dataProperties;

    @Param(value = {"false", "true"})
    private boolean interactWithAppTagContext;

    @Setup
    public void init() {
        commonTags = new HashMap<>();
        commonTags.put("common-tag-1", "common-tag-1");
        commonTags.put("common-tag-2", "common-tag-2");

        dataProperties = DataProperties.builder()
                .upPropagatedWithinJVM("propagate-1")
                .upPropagatedWithinJVM("propagate-2")
                .build();
    }

    @Benchmark
    public void rootOnly() {
        InspectitContext fromCurrent = InspectitContext.createFromCurrent(Collections.emptyMap(), dataProperties, interactWithAppTagContext);

        fromCurrent.makeActive();
        fromCurrent.close();
    }

    @Benchmark
    public void rootOnly_with2CommonTags() {
        InspectitContext fromCurrent = InspectitContext.createFromCurrent(Collections.emptyMap(), dataProperties, interactWithAppTagContext);

        fromCurrent.makeActive();
        fromCurrent.close();
    }

    @Benchmark
    public void rootOnly_with2Tags() {
        InspectitContext fromCurrent = InspectitContext.createFromCurrent(Collections.emptyMap(), dataProperties, interactWithAppTagContext);
        fromCurrent.setData("data-1", "data-1");
        fromCurrent.setData("data-2", "data-2");
        fromCurrent.makeActive();
        fromCurrent.close();
    }

    @Benchmark
    public void rootPlusOne() {
        InspectitContext parent = InspectitContext.createFromCurrent(commonTags, dataProperties, interactWithAppTagContext);
        parent.makeActive();

        InspectitContext fromCurrent = InspectitContext.createFromCurrent(commonTags, dataProperties, interactWithAppTagContext);
        fromCurrent.makeActive();
        fromCurrent.close();

        parent.close();
    }

    @Benchmark
    public void rootPlusOne_with2UpPropagatedTags() {
        InspectitContext parent = InspectitContext.createFromCurrent(commonTags, dataProperties, interactWithAppTagContext);
        parent.makeActive();

        InspectitContext fromCurrent = InspectitContext.createFromCurrent(commonTags, dataProperties, interactWithAppTagContext);
        fromCurrent.setData("propagate-3", "propagate-3");
        fromCurrent.setData("propagate-4", "propagate-4");
        fromCurrent.makeActive();
        fromCurrent.close();

        parent.close();
    }


}
