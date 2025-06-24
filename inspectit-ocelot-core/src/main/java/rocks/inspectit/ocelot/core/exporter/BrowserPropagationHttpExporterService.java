package rocks.inspectit.ocelot.core.exporter;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.tags.HttpExporterSettings;
import rocks.inspectit.ocelot.core.instrumentation.context.propagation.PropagationSessionStorage;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tags HTTP-Server to "export" data-tags to browsers
 * The server contains two endpoints:
 *  1. GET: To query propagation data
 *  2. PUT: To overwrite propagation data
 * <p>
 * <b>Marked for removal</b>, because the agent should not expose such an API to the outside.
 * Instead, use any data within the application requests via the {@link BAGGAGE_HEADER}
 */
@Slf4j
@Component
@EnableScheduling
@Deprecated
public class BrowserPropagationHttpExporterService extends DynamicallyActivatableService {

    @Autowired
    private PropagationSessionStorage sessionStorage;

    private HttpServer server;

    private HttpHandler httpHandler;

    /**
     * Delay to rerun the scheduled method after the method finished in milliseconds
     */
    private static final int FIXED_DELAY = 10000;

    /**
     * Time to live for browser propagation data in seconds
     */
    private int timeToLive = 300;

    public BrowserPropagationHttpExporterService() {
        super("exporters.tags.http");
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        return !configuration.getExporters()
                .getTags()
                .getHttp()
                .getEnabled()
                .isDisabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        HttpExporterSettings settings = configuration.getExporters().getTags().getHttp();

        String host = settings.getHost();
        int port = settings.getPort();
        String path = settings.getPath();
        int threadLimit = settings.getThreadLimit();
        timeToLive = settings.getTimeToLive();

        int sessionLimit = settings.getSessionLimit();
        sessionStorage.setSessionLimit(sessionLimit);
        sessionStorage.setExporterActive(true);

        String sessionIdHeader = settings.getSessionIdHeader();
        List<String> allowedOrigins = settings.getAllowedOrigins();
        httpHandler = new BrowserPropagationHandler(sessionStorage, sessionIdHeader, allowedOrigins);

        try {
            return startServer(host, port, path, httpHandler, threadLimit);
        } catch (Exception e) {
            log.error("Starting of Tags HTTP-Server failed", e);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        if(server != null) {
            try {
                log.info("Stopping Tags HTTP-Server - All sessions will be removed");
                server.stop(0);
                sessionStorage.clearDataStorages();
                sessionStorage.setExporterActive(false);
            } catch (Exception e) {
                log.error("Error disabling Tags HTTP-Server", e);
            }
        }
        return true;
    }

    protected boolean startServer(String host, int port, String path, HttpHandler handler, int threadLimit) throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext(path, handler);
        ExecutorService executor = Executors.newFixedThreadPool(threadLimit);
        server.setExecutor(executor);

        log.warn("It is not recommended to use the Tags HTTP-Server. Instead read or write data via baggage headers");
        log.info("Starting Tags HTTP-Server on {}:{}{} ", host, port, path);
        server.start();
        return true;
    }

    /**
     * Updates the session storage.
     * Browser propagation data is cached for a specific amount of time (timeToLive).
     * If the time expires, clean up the storage.
     */
    @Scheduled(fixedDelay = FIXED_DELAY)
    public void updateSessionStorage() {
        if(httpHandler == null) return;
        sessionStorage.cleanUpData(timeToLive);
    }
}
