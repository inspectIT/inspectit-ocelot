package rocks.inspectit.ocelot.rest.agent;

import lombok.extern.java.Log;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet filter handling the access to the REST interface used by the agent.
 * The filter will verify that all requests processed by this filter do provide an authorization header.
 */
@Log
public class AgentBearerFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        // ###########################################
        // THIS IS JUST A DUMMY IMPLEMENTATION
        // ###########################################
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;
        final String authHeader = request.getHeader("authorization");

        log.info("Requesting URL: " + request.getRequestURL().toString());
        log.info("> Authorization header: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        chain.doFilter(request, response);
    }
}
