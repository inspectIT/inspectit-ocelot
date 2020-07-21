package rocks.inspectit.ocelot.rest.alert.kapacitor;

import org.apache.commons.io.IOUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class KapacitorControllerTestBase {

    protected MockRestServiceServer mockKapacitor;

    protected <T extends KapacitorBaseController> T createWithMock(Function<InspectitServerSettings, T> constructor) {
        T controller = constructor.apply(new InspectitServerSettings());
        controller.kapacitorRestTemplate = new RestTemplate();
        mockKapacitor = MockRestServiceServer.bindTo(controller.kapacitorRestTemplate).build();
        return controller;
    }

    public String getTestJson(String name) {
        try {
            return IOUtils.toString(getClass().getResourceAsStream(name), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
