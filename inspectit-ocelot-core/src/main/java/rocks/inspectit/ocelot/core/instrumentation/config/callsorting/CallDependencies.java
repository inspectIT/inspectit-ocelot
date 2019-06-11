package rocks.inspectit.ocelot.core.instrumentation.config.callsorting;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ActionCallConfig;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Extracts the dependencies of a given {@link ActionCallConfig}.
 * These dependencies define the order in which all calls for a given rule are executed.
 */
class CallDependencies {

    @Getter
    private ActionCallConfig source;

    /**
     * The data keys read by the given action call.
     */
    @Getter
    private Set<String> reads = new HashSet<>();

    /**
     * The data keys written by the given action call.
     */
    @Getter
    private Set<String> writes = new HashSet<>();

    /**
     * The data keys read by the given action call.
     * The difference to {@link #reads} is that these data-keys are read before any action writes them!
     * As a result, e.g. when used in the "entry" section this allows to read down-propagated data.
     */
    @Getter
    private Set<String> readsBeforeWritten = new HashSet<>();

    /**
     * Reads teh given action call configuration and extracts all dependencies.
     *
     * @param call the call to analyze
     * @return the dependencies of the call.
     */
    static CallDependencies collectFor(ActionCallConfig call) {
        CallDependencies result = new CallDependencies();
        result.source = call;

        collectImplicitDependencies(call, result);
        //explicit dependencies replace the implicit ones from above
        collectExplicitDependencies(call, result);

        //"reads-before-overriden" takes precendence over "reads"
        result.readsBeforeWritten.forEach(result.reads::remove);

        return result;
    }

    private static void collectImplicitDependencies(ActionCallConfig call, CallDependencies result) {
        result.writes.add(call.getName());
        ActionCallSettings settings = call.getCallSettings();

        addIfNotBlank(settings.getOnlyIfFalse(), result.reads);
        addIfNotBlank(settings.getOnlyIfTrue(), result.reads);
        addIfNotBlank(settings.getOnlyIfNotNull(), result.reads);
        addIfNotBlank(settings.getOnlyIfNull(), result.reads);

        settings.getDataInput().values().forEach(key -> addIfNotBlank(key, result.reads));
    }

    private static void collectExplicitDependencies(ActionCallConfig call, CallDependencies result) {
        ActionCallSettings settings = call.getCallSettings();

        settings.getWrites().forEach((data, written) -> addOrRemove(data, written, result.writes));
        settings.getReads().forEach((data, read) -> addOrRemove(data, read, result.reads));
        settings.getReadsBeforeWritten().forEach((data, read) -> addOrRemove(data, read, result.readsBeforeWritten));
    }


    private static void addOrRemove(String value, boolean shouldBeContained, Collection<? super String> sink) {
        if (shouldBeContained) {
            sink.add(value);
        } else {
            sink.remove(value);
        }
    }

    private static void addIfNotBlank(String value, Collection<? super String> sink) {
        if (!StringUtils.isEmpty(value)) {
            sink.add(value);
        }
    }

}
