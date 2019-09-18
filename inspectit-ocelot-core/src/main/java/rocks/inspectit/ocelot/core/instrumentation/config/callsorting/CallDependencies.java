package rocks.inspectit.ocelot.core.instrumentation.config.callsorting;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.OrderSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ActionCallConfig;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Extracts the dependencies of a given {@link ActionCallConfig}.
 * These dependencies define the order in which all calls for a given rule are executed.
 */
class CallDependencies {

    /**
     * Reads teh given action call configuration and extracts all dependencies.
     *
     * @param call the call to analyze
     * @return the dependencies of the call.
     */
    public static CallDependencies collectFor(ActionCallConfig call) {
        return new CallDependencies(call);
    }

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

    private CallDependencies(ActionCallConfig call) {
        source = call;
        collectImplicitDependencies();
        //explicit dependencies replace implicit ones
        collectExplicitDependencies();
        //"reads-before-overriden" takes precendence over "reads"
        readsBeforeWritten.forEach(reads::remove);
    }

    /**
     * Collects dependencies which are not explicitly specified through {@link ActionCallSettings#getOrder()}.
     * Implicit dependencies are for example data-inputs used by the action.
     */
    private void collectImplicitDependencies() {
        writes.add(source.getName());
        ActionCallSettings settings = source.getCallSettings();

        addIfNotBlank(settings.getOnlyIfFalse(), reads);
        addIfNotBlank(settings.getOnlyIfTrue(), reads);
        addIfNotBlank(settings.getOnlyIfNotNull(), reads);
        addIfNotBlank(settings.getOnlyIfNull(), reads);

        settings.getDataInput().values().forEach(key -> addIfNotBlank(key, reads));
    }

    private void collectExplicitDependencies() {
        OrderSettings order = source.getCallSettings().getOrder();

        order.getWrites().forEach((data, written) -> addOrRemove(data, written, writes));
        order.getReads().forEach((data, read) -> addOrRemove(data, read, reads));
        order.getReadsBeforeWritten().forEach((data, read) -> addOrRemove(data, read, readsBeforeWritten));
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
