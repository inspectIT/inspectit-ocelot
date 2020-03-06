package rocks.inspectit.ocelot.core.instrumentation.context;

import org.openjdk.jmh.annotations.*;
import rocks.inspectit.ocelot.config.model.instrumentation.data.PropagationMode;
import rocks.inspectit.ocelot.core.instrumentation.config.model.propagation.PropagationMetaData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class InspectitContextPerfTest {

    private Map<String, String> commonTags;

    private PropagationMetaData dataProperties;

    @Param(value = {"false", "true"})
    private boolean interactWithAppTagContext;

    @Setup
    public void init() {
        commonTags = new HashMap<>();
        commonTags.put("common-tag-1", "common-tag-1");
        commonTags.put("common-tag-2", "common-tag-2");

        dataProperties = PropagationMetaData.builder()
                .setUpPropagation("propagate-1", PropagationMode.JVM_LOCAL)
                .setUpPropagation("propagate-2", PropagationMode.JVM_LOCAL)
                .build();
    }

    @Benchmark
    public void rootOnly() {
        InspectitContextImpl fromCurrent = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), dataProperties, interactWithAppTagContext);

        fromCurrent.makeActive();
        fromCurrent.close();
    }

    @Benchmark
    public void rootOnly_with2CommonTags() {
        InspectitContextImpl fromCurrent = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), dataProperties, interactWithAppTagContext);

        fromCurrent.makeActive();
        fromCurrent.close();
    }

    @Benchmark
    public void rootOnly_with2Tags() {
        InspectitContextImpl fromCurrent = InspectitContextImpl.createFromCurrent(Collections.emptyMap(), dataProperties, interactWithAppTagContext);
        fromCurrent.setData("data-1", "data-1");
        fromCurrent.setData("data-2", "data-2");
        fromCurrent.makeActive();
        fromCurrent.close();
    }

    @Benchmark
    public void rootPlusOne() {
        InspectitContextImpl parent = InspectitContextImpl.createFromCurrent(commonTags, dataProperties, interactWithAppTagContext);
        parent.makeActive();

        InspectitContextImpl fromCurrent = InspectitContextImpl.createFromCurrent(commonTags, dataProperties, interactWithAppTagContext);
        fromCurrent.makeActive();
        fromCurrent.close();

        parent.close();
    }

    @Benchmark
    public void rootPlusOne_with2UpPropagatedTags() {
        InspectitContextImpl parent = InspectitContextImpl.createFromCurrent(commonTags, dataProperties, interactWithAppTagContext);
        parent.makeActive();

        InspectitContextImpl fromCurrent = InspectitContextImpl.createFromCurrent(commonTags, dataProperties, interactWithAppTagContext);
        fromCurrent.setData("propagate-3", "propagate-3");
        fromCurrent.setData("propagate-4", "propagate-4");
        fromCurrent.makeActive();
        fromCurrent.close();

        parent.close();
    }


}
