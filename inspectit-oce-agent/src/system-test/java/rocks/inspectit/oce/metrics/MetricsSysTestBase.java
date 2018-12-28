package rocks.inspectit.oce.metrics;

import org.junit.jupiter.api.BeforeAll;

public class MetricsSysTestBase {

    @BeforeAll
    public static void sleep() throws Exception {
        //TODO check how we can globally wait only once
        Thread.sleep(1000);
    }
}
