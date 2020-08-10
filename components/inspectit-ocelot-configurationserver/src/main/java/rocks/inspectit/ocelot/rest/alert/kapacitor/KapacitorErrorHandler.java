package rocks.inspectit.ocelot.rest.alert.kapacitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import rocks.inspectit.ocelot.rest.alert.kapacitor.exceptions.KapacitorServerException;

import java.io.IOException;

@Slf4j
public class KapacitorErrorHandler extends DefaultResponseErrorHandler {

    @Override
    protected void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
        String message = "Unknown Kapacitor Error";
        try {
            ObjectMapper mapper = new ObjectMapper();
            message = mapper.readTree(response.getBody()).get("error").asText(message);
        } catch (Exception e) {
            log.debug("Failed to decode Kapacitor message", e);
        }
        throw new KapacitorServerException(message, statusCode);
    }
}
