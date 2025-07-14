package rocks.inspectit.ocelot.core.instrumentation.context.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        PropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(validSessionID);

        assertThat(dataStorage).isNotNull();
    }

    @Test
    void verifyInvalidSessionID() {
        PropagationDataStorage dataStorage1 = sessionStorage.getOrCreateDataStorage(shortSessionID);
        PropagationDataStorage dataStorage2 = sessionStorage.getOrCreateDataStorage(longSessionID);

        assertThat(dataStorage1).isNull();
        assertThat(dataStorage2).isNull();
    }

    @Test
    void verifySessionLimit() {
        updateProperties(props -> {
            props.setProperty("inspectit.instrumentation.sessions.session-limit", 1);
        });

        PropagationDataStorage dataStorage1 = sessionStorage.getOrCreateDataStorage(validSessionID);
        PropagationDataStorage dataStorage2 = sessionStorage.getOrCreateDataStorage(anotherValidSessionID);

        assertThat(dataStorage1).isNotNull();
        assertThat(dataStorage2).isNull();
    }

    @Test
    void verifySessionCleanedUpAfterMarkedForRemoval() throws InterruptedException {
        updateProperties(props -> {
            props.setProperty("inspectit.instrumentation.sessions.time-to-live", Duration.ofMillis(50));
            props.setProperty("inspectit.instrumentation.data.test-key.session-storage", true);
        });

        Map<String, String> data = new HashMap<>();
        data.put("test-key", "test-value");

        PropagationDataStorage dataStorage1 = sessionStorage.getOrCreateDataStorage(validSessionID);
        PropagationDataStorage dataStorage2 = sessionStorage.getOrCreateDataStorage(anotherValidSessionID);

        Thread.sleep(100);
        // Mark storages for removal
        sessionStorage.cleanUpStorages();

        assertThat(sessionStorage.getDataStorage(validSessionID)).isNotNull();
        assertThat(sessionStorage.getDataStorage(anotherValidSessionID)).isNotNull();

        // Refresh storage 2
        dataStorage2.writeData(data);
        // Remove storage 1
        sessionStorage.cleanUpStorages();

        assertThat(sessionStorage.getDataStorage(validSessionID)).isNull();
        assertThat(sessionStorage.getDataStorage(anotherValidSessionID)).isNotNull();

        Thread.sleep(100);
        // Mark storage 2 for removal
        sessionStorage.cleanUpStorages();

        assertThat(sessionStorage.getDataStorage(validSessionID)).isNull();
        assertThat(sessionStorage.getDataStorage(anotherValidSessionID)).isNotNull();

        // Remove storage 2
        sessionStorage.cleanUpStorages();

        assertThat(sessionStorage.getDataStorage(validSessionID)).isNull();
        assertThat(sessionStorage.getDataStorage(anotherValidSessionID)).isNull();
    }
}
