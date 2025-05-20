package rocks.inspectit.ocelot.core.config.propertysources.http.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.commons.models.info.AgentSystemInformation;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemInfoCollectorTest {

    @Test
    void shouldBeSerializable() {
        String systemInfo = SystemInfoCollector.get().collect();

        assertThat(systemInfo).isNotEmpty();
    }

    @Test
    void shouldBeDeserializable() throws JsonProcessingException {
        ObjectReader objectReader = new ObjectMapper().reader().forType(AgentSystemInformation.class);

        String string = SystemInfoCollector.get().collect();
        AgentSystemInformation systemInfo = objectReader.readValue(string);

        assertThat(systemInfo).isNotNull();
    }
}
