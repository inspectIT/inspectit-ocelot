package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextPropagationUtil;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class BrowserPropagationUtilTest {

    @InjectMocks
    BrowserPropagationUtil browserPropagationUtil;

    final static String key = "Cookie";

    @BeforeEach
    void setUp() {
        browserPropagationUtil.setSessionIdKey(key);
    }

    @Test
    void verifySessionIdKeyExists() {
        Set<String> headers = ContextPropagationUtil.getPropagationHeaderNames();

        assertThat(headers.contains(key)).isTrue();
    }

    @Test
    void verifySessionIdKeyIsUpdated() {
        Set<String> headers = ContextPropagationUtil.getPropagationHeaderNames();
        assertThat(headers.contains(key)).isTrue();

        String newKey = "NewCookie";
        browserPropagationUtil.setSessionIdKey(newKey);

        assertThat(headers.contains(key)).isFalse();
        assertThat(headers.contains(newKey)).isTrue();
    }
}
