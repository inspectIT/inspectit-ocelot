package rocks.inspectit.ocelot.instrumentation.tracing;

import io.opencensus.trace.AttributeValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.sql.DriverManager;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class JigsawTest extends TraceTestBase {

    @BeforeAll
    static void setup() throws ClassNotFoundException {
        TestUtils.waitForClassInstrumentation(DriverManager.class, 15, TimeUnit.SECONDS);
    }

    @Test
    void verifySqlActionInjected() {
        DriverManager.getDrivers();
        assertSpansExported(spans -> {
            assertThat(spans).anySatisfy((span) -> {
                assertThat(span.getName()).isEqualTo("DriverManager.getDrivers");
                assertThat(span.getAttributes().getAttributeMap().get("test"))
                        .isEqualTo(AttributeValue.stringAttributeValue("success"));
            });
        });
    }
}
