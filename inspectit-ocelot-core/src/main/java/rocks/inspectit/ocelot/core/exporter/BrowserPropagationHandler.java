package rocks.inspectit.ocelot.core.exporter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.instrumentation.browser.BrowserPropagationDataStorage;
import rocks.inspectit.ocelot.core.instrumentation.browser.BrowserPropagationSessionStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST-API to expose browser propagation data. Additionally, data can be overwritten from outside.
 * To access a data storage, a sessionID has to be provided, which references a data storage.
 * <p>
 * The expected data format to receive and export data are EntrySets, for example:
 * [{"key1": "123"}, {"key2": "321"}]
 * <p>
 * <b>Marked for removal</b>, because the agent should not expose such an API to the outside.
 * Instead, use any data within the application requests via the {@link BAGGAGE_HEADER}
 */
@Slf4j
@Deprecated
public class BrowserPropagationHandler implements HttpHandler {

    /**
     * Header, which should be used to store session-Ids
     */
    private final String sessionIdHeader;

    /**
     * List of allowed origins, which are able to access the HTTP-server
     */
    private final List<String> allowedOrigins;

    private final ObjectMapper mapper = new ObjectMapper();

    private final BrowserPropagationSessionStorage sessionStorage = BrowserPropagationSessionStorage.get();

    public BrowserPropagationHandler(String sessionIdHeader, List<String> allowedOrigins) {
        this.sessionIdHeader = sessionIdHeader;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        log.debug("Tags HTTP-server received request");
        String origin = exchange.getRequestHeaders().getFirst("Origin");

        if (isAllowedOrigin(origin)) {
            String method = exchange.getRequestMethod().toLowerCase(Locale.ROOT);
            if (method.equals("options")) {
                handleOptions(exchange);
            } else if (sessionIdExists(exchange)) {
                addCorsHeaders(exchange);

                String sessionID = exchange.getRequestHeaders().getFirst(sessionIdHeader);
                BrowserPropagationDataStorage dataStorage = sessionStorage.getDataStorage(sessionID);
                if(dataStorage != null) {
                    dataStorage.updateTimestamp(System.currentTimeMillis());
                    switch (method) {
                        case "get":
                            handleGet(exchange, dataStorage);
                            break;
                        case "put":
                            handlePut(exchange, dataStorage);
                            break;
                        default:
                            exchange.sendResponseHeaders(405, -1);
                    }
                } else {
                    log.debug("Data storage with session id {} not found", sessionID);
                    exchange.sendResponseHeaders(404, -1);
                }
            } else {
                exchange.sendResponseHeaders(400, -1);
            }
        } else {
            exchange.sendResponseHeaders(403, -1);
        }
        exchange.close();
    }

    private void handleGet(HttpExchange exchange, BrowserPropagationDataStorage dataStorage) throws IOException {
        Map<String, Object> propagationData = dataStorage.readData();
        String res = mapper.writeValueAsString(propagationData.entrySet());
        byte[] bytes = res.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handlePut(HttpExchange exchange, BrowserPropagationDataStorage dataStorage) throws IOException {
        Map<String, String> newPropagationData = getRequestBody(exchange);
        if(newPropagationData != null) {
            dataStorage.writeData(newPropagationData);
            exchange.sendResponseHeaders(200, -1);
        }
        else exchange.sendResponseHeaders(400, -1);
    }

    private void handleOptions(HttpExchange exchange) throws IOException {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        String accessControlRequestMethod = exchange.getRequestHeaders().getFirst("Access-Control-Request-Method");
        String accessControlRequestHeaders = exchange.getRequestHeaders().getFirst("Access-Control-Request-Headers");

        if (accessControlRequestMethod != null && accessControlRequestHeaders != null &&
                accessControlRequestHeaders.equalsIgnoreCase(sessionIdHeader)) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, PUT");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", sessionIdHeader);
            exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
            exchange.getResponseHeaders().add("Access-Control-Max-Age", "3600");

            exchange.sendResponseHeaders(200, -1);
        } else {
            exchange.sendResponseHeaders(403, -1);
        }
    }

    /**
     * Converts the input stream of the exchange to a map
     */
    private Map<String, String> getRequestBody(HttpExchange exchange) {
        try (InputStream is = exchange.getRequestBody()) {
            Set<Map.Entry<String,String>> entrySet = mapper.readValue(is, new TypeReference<Set<Map.Entry<String,String>>>() {});
            return entrySet.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) {
            log.debug("Request to Tags HTTP-server failed", e);
            return null;
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if(origin != null && !origin.isEmpty()) {
            String method = exchange.getRequestMethod().toLowerCase(Locale.ROOT);

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", method);
            exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
        }
    }

    /**
     * If wildcard is used, allow every origin. Alternatively, check if current origin is allowed.
     */
    private boolean isAllowedOrigin(String origin) {
        return allowedOrigins.contains("*") || allowedOrigins.contains(origin);
    }

    /**
     * @return true, if the session-id header was found
     */
    private boolean sessionIdExists(HttpExchange exchange) {
        return null != exchange.getRequestHeaders().getFirst(sessionIdHeader);
    }
}
