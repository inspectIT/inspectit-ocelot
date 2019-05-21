package rocks.inspectit.ocelot.rest.ui;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ConfigurationControllerTest {

    @InjectMocks
    ConfigurationController controller;

    @Nested
    public class FetchConfigurations {

        @Test
        public void valid() {
            Collection<String> result = controller.fetchConfigurations();

            assertThat(result).isNotEmpty();
        }
    }
}