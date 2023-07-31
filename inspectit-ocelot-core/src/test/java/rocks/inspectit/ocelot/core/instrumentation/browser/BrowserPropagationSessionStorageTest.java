package rocks.inspectit.ocelot.core.instrumentation.browser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BrowserPropagationSessionStorageTest {

    private static final String validSessionID = "test=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-4942-453a-9243-7d8422803604";
    private static final String anotherValidSessionID = "test=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-4942-453a-9243-92439b443924";
    private static final String shortSessionID = "test-session";
    private static final String longSessionID = "test1=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-49c42-45b3a-9243-7d8422803604-92439b443924;test2=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-49c42-45b3a-9243-7d8422803604-92439b443924;test3=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-49c42-45b3a-9243-7d8422803604-92439b443924;test4=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-49c42-45b3a-9243-7d8422803604-92439b443924;test5=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-49c42-45b3a-9243-7d8422803604-92439b443924;test6=83311527d6a6de76a60a72a041808a63;b0b2b4cf=ad9fef38-49c42-45b3a-9243-7d8422803604-92439b443924;";

    @Test
    void verifyValidSessionID() {
        BrowserPropagationSessionStorage sessionStorage = BrowserPropagationSessionStorage.getInstance();
        BrowserPropagationDataStorage dataStorage = sessionStorage.getOrCreateDataStorage(validSessionID);

        assertThat(dataStorage).isNotNull();
    }

    @Test
    void verifyInvalidSessionID() {
        BrowserPropagationSessionStorage sessionStorage = BrowserPropagationSessionStorage.getInstance();
        BrowserPropagationDataStorage dataStorage1 = sessionStorage.getOrCreateDataStorage(shortSessionID);
        BrowserPropagationDataStorage dataStorage2 = sessionStorage.getOrCreateDataStorage(longSessionID);

        assertThat(dataStorage1).isNull();
        assertThat(dataStorage2).isNull();
    }

    @Test
    void verifySessionLimit() {
        BrowserPropagationSessionStorage sessionStorage = BrowserPropagationSessionStorage.getInstance();
        sessionStorage.setSessionLimit(1);

        BrowserPropagationDataStorage dataStorage1 = sessionStorage.getOrCreateDataStorage(validSessionID);
        BrowserPropagationDataStorage dataStorage2 = sessionStorage.getOrCreateDataStorage(anotherValidSessionID);

        assertThat(dataStorage1).isNotNull();
        assertThat(dataStorage2).isNull();
    }
}
