package rocks.inspectit.ocelot.instrumentation.tracing;

import io.opentelemetry.api.common.AttributeKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.sql.DriverManager;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class JigsawTest extends TraceTestBase {

    @BeforeAll
    static void setup() throws ClassNotFoundException {
        TestUtils.waitForClassInstrumentation(DriverManager.class, true, 15, TimeUnit.SECONDS);
    }

    @Test
    void verifySqlActionInjected() {
        DriverManager.getDrivers();
        assertSpansExported(spans -> {
            assertThat(spans).anySatisfy((span) -> {
                assertThat(span.getName()).isEqualTo("DriverManager.getDrivers");
                assertThat(span.getAttributes().asMap().get(AttributeKey.stringKey("test"))).isEqualTo("success");
            });
        });
    }
}
