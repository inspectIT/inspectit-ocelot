package rocks.inspectit.ocelot.error;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import rocks.inspectit.ocelot.error.exceptions.NotSupportedWithLdapException;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static MockMvc mockMvc;

    @Controller
    public static class ExceptionController {

        @RequestMapping("/exception")
        public void exception() throws Exception {
            throw new Exception("custom-message");
        }

        @RequestMapping("/notSupportedWithLdapException")
        public void notSupportedWithLdapException() {
            throw new NotSupportedWithLdapException();
        }
    }

    @BeforeAll
    public static void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void handleException() throws Exception {
        mockMvc.perform(get("/exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.message").value("Unexpected error"))
                .andExpect(jsonPath("$.debugMessage").value("custom-message"));
    }

    @Test
    public void handleNotSupportedWithLdapException() throws Exception {
        mockMvc.perform(get("/notSupportedWithLdapException"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.message").value("Endpoint is not supported in the current configuration."))
                .andExpect(jsonPath("$.debugMessage").value("Endpoint is not supported when LDAP authentication is used."));
    }
}