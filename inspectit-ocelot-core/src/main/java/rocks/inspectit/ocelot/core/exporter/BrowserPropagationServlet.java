package rocks.inspectit.ocelot.core.exporter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.browser.BrowserPropagationDataStorage;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST-API to expose browser propagation data
 * Additionally, data can be overwritten from outside
 *
 * The expected data format to receive and export data are EntrySets, for example:
 * [{"key1": "123"}, {"key2": "321"}]
 */
@Slf4j
public class BrowserPropagationServlet extends HttpServlet {

    private final ObjectMapper mapper;
    private final BrowserPropagationDataStorage dataStorage;
    private int responseCode;

    public BrowserPropagationServlet() {
        this.mapper = new ObjectMapper();
        this.dataStorage = BrowserPropagationDataStorage.getInstance();
    }

    private void setResponseCode(int code) {
        this.responseCode = code;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("Tags HTTP-server received GET-request");
        this.setResponseCode(HttpServletResponse.SC_OK);
        Map<String, Object> propagationData = this.dataStorage.readData();
        String res = mapper.writeValueAsString(propagationData.entrySet());

        response.setContentType("application/json");
        response.setStatus(this.responseCode);
        response.getWriter().write(res);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("Tags HTTP-server received PUT-request");
        this.setResponseCode(HttpServletResponse.SC_OK);
        Map<String, Object> newPropagationData = this.getRequestBody(request);

        if(!newPropagationData.isEmpty()) this.dataStorage.writeData(newPropagationData);

        response.setStatus(this.responseCode);
    }

    private Map<String, Object> getRequestBody(HttpServletRequest request) {
        try (BufferedReader reader = request.getReader()) {
            Set<Map.Entry<String,Object>> entrySet = mapper.readValue(reader, new TypeReference<Set<Map.Entry<String,Object>>>() {});
            return entrySet.stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) {
            log.info("Request failed");
            this.setResponseCode(HttpServletResponse.SC_BAD_REQUEST);
            return Collections.emptyMap();
        }
    }
}
