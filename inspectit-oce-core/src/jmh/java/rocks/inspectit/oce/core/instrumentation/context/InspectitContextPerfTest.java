package rocks.inspectit.oce.core.instrumentation.context;

import org.openjdk.jmh.annotations.*;
import rocks.inspectit.oce.core.instrumentation.config.model.DataProperties;
import rocks.inspectit.oce.core.tags.CommonTagsManager;
import rocks.inspectit.oce.core.tags.ITagsProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class InspectitContextPerfTest {

    private CommonTagsManager emptyCommonTagManager;

    private CommonTagsManager commonTagsManager;

    private DataProperties dataProperties;

    @Param(value = {"false", "true"})
    private boolean interactWithAppTagContext;

    @Setup
    public void init() {
        emptyCommonTagManager = new CommonTagsManager();
        commonTagsManager = new CommonTagsManager();
        commonTagsManager.register(new ITagsProvider() {
            @Override
            public int getPriority() {
                return 0;
            }

            @Override
            public Map<String, String> getTags() {
                HashMap<String, String> map = new HashMap<>();
                map.put("common-tag-1", "common-tag-1");
                map.put("common-tag-2", "common-tag-2");
                return map;
            }
        });

        dataProperties = DataProperties.builder()
                .upPropagatedWithinJVM("propagate-1")
                .upPropagatedWithinJVM("propagate-2")
                .build();
    }

    @Benchmark
    public void rootOnly() {
        InspectitContext fromCurrent = InspectitContext.createFromCurrent(emptyCommonTagManager, dataProperties, interactWithAppTagContext);

        fromCurrent.makeActive();
        fromCurrent.close();
    }

    @Benchmark
    public void rootOnly_with2CommonTags() {
        InspectitContext fromCurrent = InspectitContext.createFromCurrent(emptyCommonTagManager, dataProperties, interactWithAppTagContext);

        fromCurrent.makeActive();
        fromCurrent.close();
    }

    @Benchmark
    public void rootOnly_with2DataProviderTags() {
        InspectitContext fromCurrent = InspectitContext.createFromCurrent(emptyCommonTagManager, dataProperties, interactWithAppTagContext);
        fromCurrent.setData("data-1", "data-1");
        fromCurrent.setData("data-2", "data-2");
        fromCurrent.makeActive();
        fromCurrent.close();
    }

    @Benchmark
    public void rootPlusOne() {
        InspectitContext parent = InspectitContext.createFromCurrent(commonTagsManager, dataProperties, interactWithAppTagContext);
        parent.makeActive();

        InspectitContext fromCurrent = InspectitContext.createFromCurrent(commonTagsManager, dataProperties, interactWithAppTagContext);
        fromCurrent.makeActive();
        fromCurrent.close();

        parent.close();
    }

    @Benchmark
    public void rootPlusOne_with2UpPropagatedDataProviderTags() {
        InspectitContext parent = InspectitContext.createFromCurrent(commonTagsManager, dataProperties, interactWithAppTagContext);
        parent.makeActive();

        InspectitContext fromCurrent = InspectitContext.createFromCurrent(commonTagsManager, dataProperties, interactWithAppTagContext);
        fromCurrent.setData("propagate-3", "propagate-3");
        fromCurrent.setData("propagate-4", "propagate-4");
        fromCurrent.makeActive();
        fromCurrent.close();

        parent.close();
    }


}
