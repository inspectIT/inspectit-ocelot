package rocks.inspectit.ocelot.rest.users;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.authentication.JwtTokenManager;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.rest.ErrorInfo;
import rocks.inspectit.ocelot.user.LocalUserDetailsService;
import rocks.inspectit.ocelot.user.User;

import java.io.IOException;

/**
 * Rest controller allowing users to manage their own account.
 */
@RestController
public class AccountController extends AbstractBaseController {

    /**
     * Error object returned when the password field of a change request is blank.
     */
    private static final ErrorInfo NO_PASSWORD_ERROR = ErrorInfo.builder()
            .error(ErrorInfo.Type.NO_PASSWORD)
            .message("Password must not be empty")
            .build();

    @Autowired
    private LocalUserDetailsService userDetailsService;

    @Autowired
    private JwtTokenManager tokenManager;

    @ApiOperation(value = "Create an access token", notes = "Creates a fresh access token for the user making this request." +
            " Instead of using User and Password based HTTP authentication, the user can then user the header 'Authorization: Bearer <TOKEN>' for authentication." +
            "The token expires after the time specified by ${inspectit.token-lifespan}, which by default is 60 minutes." +
            "In case of a server restart, all previously issued tokens become invalid.")
    @ApiResponse(code = 200, message = "The access token", examples =
    @Example(value = @ExampleProperty(value = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTU2MTYyODE1NH0.KelDW1OXg9xlMjSiblwZqui7sya4Crq833b-98p8UZ4", mediaType = "text/plain")))
    @GetMapping("account/token")
    public String acuireNewAccessToken(Authentication user) throws IOException {
        return tokenManager.createToken(user.getName());
    }

    @ApiOperation(value = "Change Password", notes = "Changes the password of the logged in user." +
            " This endpoint does not work with token-based authentication, only HTTP basic auth is allowed.")
    @PutMapping("account/password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest newPassword, Authentication user) {
        if (StringUtils.isEmpty(newPassword.getPassword())) {
            return ResponseEntity.badRequest().body(NO_PASSWORD_ERROR);
        }
        User updatedUser = userDetailsService.getUserByName(user.getName())
                .get()
                .toBuilder()
                .password(newPassword.getPassword())
                .build();
        userDetailsService.addOrUpdateUser(updatedUser);

        return ResponseEntity.ok().build();
    }

    /**
     * The payload for a password change, passed to {@link #changePassword(PasswordChangeRequest, Authentication)}.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordChangeRequest {
        private String password;
    }
}
