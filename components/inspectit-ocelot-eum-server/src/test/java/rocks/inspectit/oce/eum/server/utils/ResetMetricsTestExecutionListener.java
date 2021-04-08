package rocks.inspectit.oce.eum.server.utils;

import io.prometheus.client.CollectorRegistry;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Test listener for clearing the metrics registry. This is necessary so that the Prometheus exporter can start successfully.
 * This is needed in case Spring is starting multuple ApplicationContexts (e.g. {@link org.springframework.test.annotation.DirtiesContext} is used).
 * See: https://github.com/prometheus/client_java/issues/279
 */
public class ResetMetricsTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestClass(TestContext testContext) {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Override
    public void afterTestClass(TestContext testContext) {
        CollectorRegistry.defaultRegistry.clear();
    }
}
