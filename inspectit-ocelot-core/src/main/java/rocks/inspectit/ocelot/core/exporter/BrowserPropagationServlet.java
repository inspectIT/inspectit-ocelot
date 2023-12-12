package rocks.inspectit.ocelot.core.exporter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.browser.BrowserPropagationDataStorage;
import rocks.inspectit.ocelot.core.instrumentation.browser.BrowserPropagationSessionStorage;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST-API to expose browser propagation data
 * Additionally, data can be overwritten from outside
 * To access a data storage, a sessionID has to be provided, which references a data storage
 * <p>
 * The expected data format to receive and export data are EntrySets, for example:
 * [{"key1": "123"}, {"key2": "321"}]
 */
@Slf4j
public class BrowserPropagationServlet extends HttpServlet {

    /**
     * Header, which should be used to store the session-Ids
     */
    private final String sessionIdHeader;

    /**
     * List of allowed Origins, which are able to access the HTTP-server
     */
    private final List<String> allowedOrigins;
    private final ObjectMapper mapper;
    private final BrowserPropagationSessionStorage sessionStorage;

    public BrowserPropagationServlet(String sessionIdHeader, List<String> allowedOrigins) {
        this.sessionIdHeader = sessionIdHeader;
        this.allowedOrigins = allowedOrigins;
        this.mapper = new ObjectMapper();
        this.sessionStorage = BrowserPropagationSessionStorage.getInstance();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.debug("Tags HTTP-server received GET-request");
        String origin = request.getHeader("Origin");

        //If wildcard is used, allow every origin
        //Alternatively, check if current origin is allowed
        if(allowedOrigins.contains("*") || allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "GET");
            response.setHeader("Access-Control-Allow-Credentials", "true");

            String sessionID = request.getHeader(sessionIdHeader);
            if(sessionID == null) {
                log.warn("Request to Tags HTTP-server misses session ID");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            else {
                BrowserPropagationDataStorage dataStorage = sessionStorage.getDataStorage(sessionID);

                if(dataStorage == null) {
                    log.warn("Data storage with session id " + sessionID + " not found");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                else {
                    dataStorage.updateTimestamp(System.currentTimeMillis());
                    Map<String, Object> propagationData = dataStorage.readData();
                    String res = mapper.writeValueAsString(propagationData.entrySet());
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(res);
                }
            }
        }
        else response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) {
        log.debug("Tags HTTP-server received PUT-request");
        String origin = request.getHeader("Origin");

        //If wildcard is used, allow every origin
        //Alternatively, check if current origin is allowed
        if(allowedOrigins.contains("*") || allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "PUT");
            response.setHeader("Access-Control-Allow-Credentials", "true");

            String sessionID = request.getHeader(sessionIdHeader);
            if(sessionID == null) {
                log.warn("Request to Tags HTTP-server misses session ID");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            else {
                BrowserPropagationDataStorage dataStorage = sessionStorage.getDataStorage(sessionID);

                if(dataStorage == null) {
                    log.warn("Data storage with session id " + sessionID + " not found");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                else {
                    dataStorage.updateTimestamp(System.currentTimeMillis());
                    Map<String, String> newPropagationData = getRequestBody(request);
                    if(newPropagationData != null) {
                        dataStorage.writeData(newPropagationData);
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                    else response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }
        else response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        log.debug("Tags HTTP-server received OPTIONS-request");
        String origin = request.getHeader("Origin");
        String accessControlRequestMethod = request.getHeader("Access-Control-Request-Method");
        String accessControlRequestHeaders = request.getHeader("Access-Control-Request-Headers");

        if (
                origin != null &&
                accessControlRequestMethod != null &&
                accessControlRequestHeaders != null &&
                (allowedOrigins.contains("*") || allowedOrigins.contains(origin)) &&
                accessControlRequestHeaders.equalsIgnoreCase(sessionIdHeader)
        ) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "GET, PUT");
            response.setHeader("Access-Control-Allow-Headers", sessionIdHeader);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setStatus(HttpServletResponse.SC_OK);
        }
        else response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    private Map<String, String> getRequestBody(HttpServletRequest request) {
        try (BufferedReader reader = request.getReader()) {
            Set<Map.Entry<String,String>> entrySet = mapper.readValue(reader, new TypeReference<Set<Map.Entry<String,String>>>() {});
            return entrySet.stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) {
            log.warn("Request to Tags HTTP-server failed", e);
            return null;
        }
    }
}
