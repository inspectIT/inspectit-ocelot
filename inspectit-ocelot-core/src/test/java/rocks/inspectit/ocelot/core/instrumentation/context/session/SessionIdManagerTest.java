package rocks.inspectit.ocelot.core.instrumentation.context.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextPropagation;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class SessionIdManagerTest {

    @InjectMocks
    SessionIdManager sessionIdManager;

    final static String key = "Session-Id";

    @BeforeEach
    void setUp() {
        sessionIdManager.setSessionIdHeader(key);
    }

    @Test
    void verifySessionIdKeyExists() {
        Set<String> headers = ContextPropagation.get().getPropagationHeaderNames();

        assertThat(headers.contains(key)).isTrue();
    }

    @Test
    void verifySessionIdKeyIsUpdated() {
        Set<String> headers = ContextPropagation.get().getPropagationHeaderNames();
        assertThat(headers.contains(key)).isTrue();

        String newKey = "NewCookie";
        sessionIdManager.setSessionIdHeader(newKey);

        assertThat(headers.contains(key)).isFalse();
        assertThat(headers.contains(newKey)).isTrue();
    }
}
