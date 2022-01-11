package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.sdk.metrics.export.MetricProducer;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CustomMetricReaderFactory implements MetricReaderFactory {

    private Map<String, MetricReaderFactory> readerFactories = new ConcurrentHashMap<>();

    @Override
    public MetricReader apply(MetricProducer producer) {
        return null;
    }

    public boolean registerMetricReaderFactory(String registerName, MetricReaderFactory factory) {
        // TODO: support immediate replacement? or do we need to unregister it before?
        if (null != readerFactories.put(registerName, factory)) {
            log.error("A MetricReaderFactory with the name '{}' was already registered.", registerName);
            return false;
        } else {
            log.info("Successfully registered the '{}' MetricReaderFactory ({})", registerName, factory.getClass()
                    .getName());
            return true;
        }
    }

    public boolean unregisterMetricReaderFactory(String registerName) {
        MetricReaderFactory metricReaderFactory = readerFactories.remove(registerName);
        if (null == metricReaderFactory) {
            log.error("Could not unregister '{}' as no such MetricReaderFactory was registered", registerName);
            return false;
        } else {
            log.info("Successfully unregistered the '{}' MetricReaderFactory ({}}", registerName, metricReaderFactory.getClass()
                    .getName());
            return true;
        }
    }
}
