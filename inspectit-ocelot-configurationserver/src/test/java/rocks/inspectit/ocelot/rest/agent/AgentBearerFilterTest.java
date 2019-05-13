package rocks.inspectit.ocelot.rest.agent;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentBearerFilterTest {

    @InjectMocks
    AgentBearerFilter filter;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain chain;

    @Nested
    public class DoFilter {

        @Test
        public void validRequest() throws IOException, ServletException {
            when(request.getRequestURL()).thenReturn(new StringBuffer("http-url"));
            when(request.getHeader(eq("authorization"))).thenReturn("Bearer SECRET_TOKEN");

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verifyNoMoreInteractions(chain);
            verifyZeroInteractions(response);
        }

        @Test
        public void authorizationMissing() throws IOException, ServletException {
            when(request.getRequestURL()).thenReturn(new StringBuffer("http-url"));

            filter.doFilter(request, response, chain);

            verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
            verifyNoMoreInteractions(response);
            verifyZeroInteractions(chain);
        }

    }

}