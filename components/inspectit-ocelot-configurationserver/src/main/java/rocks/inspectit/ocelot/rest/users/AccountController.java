package rocks.inspectit.ocelot.rest.users;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.error.exceptions.NotSupportedWithLdapException;
import rocks.inspectit.ocelot.rest.AbstractBaseController;
import rocks.inspectit.ocelot.rest.ErrorInfo;
import rocks.inspectit.ocelot.security.config.UserRoleConfiguration;
import rocks.inspectit.ocelot.security.jwt.JwtTokenManager;
import rocks.inspectit.ocelot.user.User;
import rocks.inspectit.ocelot.user.UserPermissions;
import rocks.inspectit.ocelot.user.UserService;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rest controller allowing users to manage their own account.
 */
@RestController
@Slf4j
public class AccountController extends AbstractBaseController {

    /**
     * Error object returned when the password field of a change request is blank.
     */
    private static final ErrorInfo NO_PASSWORD_ERROR = ErrorInfo.builder()
            .error(ErrorInfo.Type.NO_PASSWORD)
            .message("Password must not be empty")
            .build();

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenManager tokenManager;

    @Operation(summary = "Create an access token", description = "Creates a fresh access token for the user making this request." +
            " Instead of using User and Password based HTTP authentication, the user can then user the header 'Authorization: Bearer <TOKEN>' for authentication." +
            "The token expires after the time specified by ${inspectit.token-lifespan}, which by default is 60 minutes." +
            "In case of a server restart, all previously issued tokens become invalid.")
    @ApiResponse(responseCode = "200", description = "The access token", content = @Content(mediaType = "text/plain", examples = @ExampleObject("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTU2MTYyODE1NH0.KelDW1OXg9xlMjSiblwZqui7sya4Crq833b-98p8UZ4"))    )
    @GetMapping({"account/token", "account/token/"})
    public String acquireNewAccessToken(Authentication auth) {
        Optional<User> userOptional = userService.getUserByName(auth.getName());

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setLastLoginTime(System.currentTimeMillis());
            userService.addOrUpdateUser(user);
        }
        return tokenManager.createToken(auth.getName());
    }

    @Operation(summary = "Change Password", description = "Changes the password of the logged in user." +
            " This endpoint does not work with token-based authentication, only HTTP basic auth is allowed.")
    @PutMapping({"account/password", "account/password/"})
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest newPassword, Authentication auth) {
        if (ObjectUtils.isEmpty(newPassword.getPassword())) {
            return ResponseEntity.badRequest().body(NO_PASSWORD_ERROR);
        }
        User user = userService.getUserByName(auth.getName()).get();
        if (user.isLdapUser()) {
            throw new NotSupportedWithLdapException();
        }
        User updatedUser = user
                .toBuilder()
                .password(newPassword.getPassword())
                .build();
        userService.addOrUpdateUser(updatedUser);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get the user's permissions", description = "Queries the permissions of the currently logged-in user.")
    @ApiResponse(responseCode = "200", description = "The permissions of the user")
    @GetMapping({"account/permissions", "account/permissions/"})
    public UserPermissions getPermissions(Authentication auth) {
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return UserPermissions.builder()
                .write(roles.contains(UserRoleConfiguration.WRITE_ACCESS_ROLE))
                .promote(roles.contains(UserRoleConfiguration.PROMOTE_ACCESS_ROLE))
                .admin(roles.contains(UserRoleConfiguration.ADMIN_ACCESS_ROLE))
                .build();
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
