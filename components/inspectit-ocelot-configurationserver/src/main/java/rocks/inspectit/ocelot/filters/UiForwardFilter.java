package rocks.inspectit.ocelot.filters;

import lombok.extern.java.Log;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Filter for forwarding all requests which URI is on a folder on the /ui/ endpoint to be forwarded to its 'index.html' file.
 * Example: /ui/example/ => /ui/example/index.html
 */
@Component
@Log
@RequestMapping("/")
public class UiForwardFilter implements Filter {

    @GetMapping
    public ModelAndView redirectRoot(Model model) {
        RedirectView redirectView = new RedirectView("/ui/");
        redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return new ModelAndView(redirectView);
    }

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
