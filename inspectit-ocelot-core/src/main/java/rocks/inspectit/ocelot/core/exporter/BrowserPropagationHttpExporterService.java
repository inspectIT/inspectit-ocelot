package rocks.inspectit.ocelot.core.exporter;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.tags.HttpExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.servlet.http.HttpServlet;
import java.net.InetSocketAddress;

/**
 * Tags HTTP-Server to "export" data-tags to browsers
 * The server contains two endpoints:
 *  1. GET: To query propagation data
 *  2. PUT: To overwrite propagation data
 */
@Component
@Slf4j
public class BrowserPropagationHttpExporterService extends DynamicallyActivatableService {
    private Server server;

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
        HttpServlet httpServlet = new BrowserPropagationServlet();

        server = new Server(new InetSocketAddress(host, port));
        String contextPath = "";
        ServletContextHandler contextHandler = new ServletContextHandler(server, contextPath);
        contextHandler.addServlet(new ServletHolder(httpServlet), path);
        server.setStopAtShutdown(true);

        try {
            server.start();
            log.info("Starting Tags HTTP-Server on {}:{}{} ", host, port, path);
        } catch (Exception e) {
            log.error("Starting of Tags HTTP-Server failed");
            return false;
        }
        return true;
    }

    @Override
    protected boolean doDisable() {
        if(server != null) {
            try {
                server.stop();
                log.info("Stopping Tags HTTP-Server");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }
}
