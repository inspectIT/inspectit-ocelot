package rocks.inspectit.ocelot.rest.ui;

import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller providing user specific endpoints like login which is used by the UI.
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @PostMapping("/login")
    @ApiOperation(value = "Login in a user", notes = "This endpoint is used by users to log them in.")
    public LoginResponse login(@RequestBody UserCredentials credentials) {
        // ###########################################
        // THIS IS JUST A DUMMY IMPLEMENTATION
        // ###########################################
        LoginResponse response = new LoginResponse();
        response.setUsername(credentials.getUsername());
        response.setPassword(credentials.getPassword());
        response.setCurrentTime(System.currentTimeMillis());

        return response;
    }

    @Data
    private static class UserCredentials {
        private String username;
        private String password;
    }

    @Data
    private static class LoginResponse {
        private long currentTime;
        private String username;
        private String password;
    }
}
