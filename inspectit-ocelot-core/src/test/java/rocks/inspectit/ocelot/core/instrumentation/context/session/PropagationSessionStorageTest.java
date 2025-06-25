package rocks.inspectit.ocelot.core.instrumentation.context.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState.ENABLED;

@DirtiesContext
public class PropagationSessionStorageTest extends SpringTestBase {

    @Autowired
    PropagationSessionStorage sessionStorage;

    private static final String validSessionID = "test=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-4942-453a-9243-7d8422803604";

    private static final String anotherValidSessionID = "test=83311527d6a";

    private static final String shortSessionID = "test-session";

    private static final String longSessionID = "test1=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-49c42-45b3a-9243-7d8422803604-92439b443924;test2=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-49c42-45b3a-9243-7d8422803604-92439b443924;test3=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-49c42-45b3a-9243-7d8422803604-92439b443924;test4=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-49c42-45b3a-9243-7d8422803604-92439b443924;test5=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-49c42-45b3a-9243-7d8422803604-92439b443924;test6=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-49c42-45b3a-9243-7d8422803604-92439b443924;";

    @BeforeEach
    void beforeEach() {
        sessionStorage.clearDataStorages();
    }

    @Test
    void verifyValidSessionID() {
        PropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(validSessionID, null);

        assertThat(dataStorage).isNotNull();
    }

    @Test
    void verifyInvalidSessionID() {
        PropagationDataStorage dataStorage1 = sessionStorage.getOrCreateDataStorage(shortSessionID, null);
        PropagationDataStorage dataStorage2 = sessionStorage.getOrCreateDataStorage(longSessionID, null);

        assertThat(dataStorage1).isNull();
        assertThat(dataStorage2).isNull();
    }

    @Test
    void verifySessionLimit() {
        updateProperties(props -> {
            props.setProperty("inspectit.instrumentation.sessions.session-limit", 1);
        });

        PropagationDataStorage dataStorage1 = sessionStorage.getOrCreateDataStorage(validSessionID, null);
        PropagationDataStorage dataStorage2 = sessionStorage.getOrCreateDataStorage(anotherValidSessionID, null);

        assertThat(dataStorage1).isNotNull();
        assertThat(dataStorage2).isNull();
    }
}
