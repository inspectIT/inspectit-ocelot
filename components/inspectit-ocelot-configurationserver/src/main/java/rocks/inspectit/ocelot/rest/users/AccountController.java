package rocks.inspectit.ocelot.rest.users;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rocks.inspectit.ocelot.authentication.JwtTokenManager;
import rocks.inspectit.ocelot.rest.AbstractBaseController;

import java.io.IOException;

/**
 * Rest controller allowing users to manage their own account.
 */
@RestController
public class AccountController extends AbstractBaseController {

    @Autowired
    JwtTokenManager tokenManager;

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
}
