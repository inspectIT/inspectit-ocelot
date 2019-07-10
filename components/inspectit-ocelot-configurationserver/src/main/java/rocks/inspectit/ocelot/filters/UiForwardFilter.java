package rocks.inspectit.ocelot.filters;

import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Filter for forwarding all requests which URI is on a folder on the /ui/ endpoint to be forwarded to its 'index.html' file.
 * Example: /ui/example/ => /ui/example/index.html
 */
@Component
@Log
public class UiForwardFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String requestURI = ((HttpServletRequest) request).getRequestURI();

        if (requestURI.startsWith("/ui/") && requestURI.endsWith("/")) {
            String forwardURI = requestURI + "index.html";
            log.info("Forwarding '" + requestURI + "' to '" + forwardURI + "'");
            request.getRequestDispatcher(forwardURI).forward(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }

}
