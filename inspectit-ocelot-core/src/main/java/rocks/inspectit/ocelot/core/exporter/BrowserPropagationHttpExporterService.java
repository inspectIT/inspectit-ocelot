package rocks.inspectit.ocelot.core.exporter;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.tags.HttpExporterSettings;
import rocks.inspectit.ocelot.core.instrumentation.browser.BrowserPropagationSessionStorage;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.servlet.http.HttpServlet;
import java.net.InetSocketAddress;

/**
 * Tags HTTP-Server to "export" data-tags to browsers
 * The server contains two endpoints:
 *  1. GET: To query propagation data
 *  2. PUT: To overwrite propagation data
 */

@Slf4j
@Component
@EnableScheduling
public class BrowserPropagationHttpExporterService extends DynamicallyActivatableService {
    private Server server;
    private BrowserPropagationSessionStorage sessionStorage;
    private BrowserPropagationServlet httpServlet;

    /**
     * Stores a reference of the InspectITConfig to enable runtime updates of the session limit
     */
    private InspectitConfig inspectitConfig;

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
        int sessionLimit = settings.getSessionLimit();
        timeToLive = settings.getTimeToLive();
        sessionStorage = BrowserPropagationSessionStorage.getInstance();
        sessionStorage.setSessionLimit(sessionLimit);
        httpServlet = new BrowserPropagationServlet();
        inspectitConfig = configuration;

        return startServer(host, port, path, httpServlet);
    }

    @Override
    protected boolean doDisable() {
        if(server != null) {
            try {
                log.info("Stopping Tags HTTP-Server");
                server.stop();
                sessionStorage.clearDataStorages();
            } catch (Exception e) {
                log.error("Error disabling Tags HTTP-Server", e);
            }
        }
        return true;
    }

    protected boolean startServer(String host, int port, String path, HttpServlet servlet) {
        server = new Server(new InetSocketAddress(host, port));
        String contextPath = "";
        ServletContextHandler contextHandler = new ServletContextHandler(server, contextPath);
        contextHandler.addServlet(new ServletHolder(servlet), path);
        server.setStopAtShutdown(true);

        try {
            log.info("Starting Tags HTTP-Server on {}:{}{} ", host, port, path);
            server.start();
        } catch (Exception e) {
            log.warn("Starting of Tags HTTP-Server failed");
            return false;
        }
        return true;
    }

    /**
     * Updates the session storage:
     * 1.   Browser propagation data is cached for a specific amount of time (timeToLive)
     *      If the time expires, clean up the storage
     * 2.   Update the session limit
     *      Note that this will not delete any active sessions, if the new session limit is exceeded
     */
    @Scheduled(fixedDelay = FIXED_DELAY)
    public void updateSessionStorage() {
        if(httpServlet == null) return;
        sessionStorage.cleanUpData(timeToLive);

        if(inspectitConfig == null) return;
        int sessionLimit = inspectitConfig.getExporters().getTags().getHttp().getSessionLimit();
        sessionStorage.setSessionLimit(sessionLimit);
    }
}
