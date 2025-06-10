package rocks.inspectit.ocelot.core.exporter;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.AgentProperties;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.tags.HttpExporterSettings;
import rocks.inspectit.ocelot.core.instrumentation.browser.BrowserPropagationSessionStorage;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;
import rocks.inspectit.ocelot.core.utils.CoreUtils;


import javax.servlet.http.HttpServlet;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;

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

    private Tomcat tomcat;

    private BrowserPropagationSessionStorage sessionStorage;

    private BrowserPropagationServlet httpServlet;

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
        timeToLive = settings.getTimeToLive();

        int sessionLimit = settings.getSessionLimit();
        sessionStorage = BrowserPropagationSessionStorage.get();
        sessionStorage.setSessionLimit(sessionLimit);
        sessionStorage.setExporterActive(true);

        String sessionIdHeader = settings.getSessionIdHeader();
        List<String> allowedOrigins = settings.getAllowedOrigins();
        httpServlet = new BrowserPropagationServlet(sessionIdHeader, allowedOrigins);

        return startServer(host, port, path, httpServlet);
    }

    @Override
    protected boolean doDisable() {
        if(tomcat != null) {
            try {
                log.info("Stopping Tags HTTP-Server - All sessions will be removed");
                tomcat.stop();
                sessionStorage.clearDataStorages();
                sessionStorage.setExporterActive(false);
            } catch (Exception e) {
                log.error("Error disabling Tags HTTP-Server", e);
            }
        }
        return true;
    }

    protected boolean startServer(String host, int port, String path, HttpServlet servlet) {
        // TODO properly configure the server
        tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.setHostname(host);
        String appBase = ".";
        tomcat.getHost().setAppBase(appBase);


        File baseDir = new File(CoreUtils.getTempDir());
        tomcat.setBaseDir(baseDir.getAbsolutePath());


        Context context = tomcat.addContext("", baseDir.getAbsolutePath());
        Tomcat.addServlet(context, "BrowserPropagationServlet", servlet);
        context.addServletMappingDecoded(path, "BrowserPropagationServlet");

        try {
            log.warn("It is not recommended to use the Tags HTTP-Server. Instead read or write data via baggage headers");
            log.info("Starting Tags HTTP-Server on {}:{}{} ", host, port, path);
            tomcat.start();
        } catch (Exception e) {
            log.error("Starting of Tags HTTP-Server failed", e);
            return false;
        }
        return true;
    }

    /**
     * Updates the session storage.
     * Browser propagation data is cached for a specific amount of time (timeToLive).
     * If the time expires, clean up the storage.
     */
    @Scheduled(fixedDelay = FIXED_DELAY)
    public void updateSessionStorage() {
        if(httpServlet == null) return;
        sessionStorage.cleanUpData(timeToLive);
    }
}
