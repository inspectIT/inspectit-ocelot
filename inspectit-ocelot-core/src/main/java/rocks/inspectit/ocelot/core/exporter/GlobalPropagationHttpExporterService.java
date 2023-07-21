package rocks.inspectit.ocelot.core.exporter;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import javax.servlet.http.HttpServlet;
import java.net.InetSocketAddress;

@Component
@Slf4j
public class GlobalPropagationHttpExporterService extends DynamicallyActivatableService {
    private Server server;

    public GlobalPropagationHttpExporterService() {
        super("exporters.tags.http", "tags.enabled");
    }

    public void start() {
        if(this.server == null) return;
        try {
            this.server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        return true;
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        String host = "0.0.0.0";
        int port = 9001;
        String path = "/inspectit";
        HttpServlet httpServlet = new GlobalPropagationServlet();

        server = new Server(new InetSocketAddress(host, port));
        ServletContextHandler contextHandler = new ServletContextHandler(server, "");
        contextHandler.addServlet(new ServletHolder(httpServlet), path);
        server.setStopAtShutdown(true);
        this.start();

        return true;
    }

    @Override
    protected boolean doDisable() {
        if(server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }
}
