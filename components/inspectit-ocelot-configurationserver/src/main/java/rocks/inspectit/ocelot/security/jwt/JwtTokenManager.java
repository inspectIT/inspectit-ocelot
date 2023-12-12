package rocks.inspectit.ocelot.security.jwt;

import com.google.common.annotations.VisibleForTesting;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * Creates and verifies JWT based access tokens.
 * Tokens contain the name of the user and an expiration date.
 * Tokens are only considered valid if the expiration date has not been exceeded yet.
 */
@Component
@Slf4j
public class JwtTokenManager {

    @Autowired
    @VisibleForTesting
    InspectitServerSettings config;

    @Autowired
    @VisibleForTesting
    List<UserDetailsService> services;

    /**
     * We dynamically generate a secret to sign the tokens with at server start.
     * This means that tokens automatically become invalid as soon as the server restarts.
     */
    private final SecretKey secret = Jwts.SIG.HS256.key().build();

    /**
     * Creates a token containing the specified username.
     * The token expires after the specified Duration via {@link InspectitServerSettings#getTokenLifespan()} from now.
     *
     * @param username the username for which the token is generated
     * @return the generated token
     */
    public String createToken(String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("Username must not be null or empty.");
        }

        Date now = new Date();
        Date expiration = new Date(now.getTime() + config.getTokenLifespan().toMillis());

        return Jwts.builder()
                .subject(username)
                .expiration(expiration)
                .signWith(secret)
                .compact();
    }

    /**
     * Performs authentication using a given token.
     * The token is parsed and the signature is checked for validity.
     * If the token has not expired yet, an {@link Authentication} for the user stored in the token is created and returned.
     * It is also ensured that the specified user still exists and has not been deleted in the meantime.
     * <p>
     * If any of the steps fail (meaning that the token is invalid) an exception is thrown.
     *
     * @param token the token to parse, created via {@link #createToken(String)}
     * @return the {@link Authentication} if the token was valid
     * @throws JwtException              thrown if the token is invalid or has expired
     * @throws UsernameNotFoundException thrown if the user does not exist
     */
    public Authentication authenticateWithToken(String token) throws JwtException, UsernameNotFoundException {
        Claims jwtToken = Jwts.parser().verifyWith(secret)
                .build().parseSignedClaims(token)
                .getPayload();

        String username = jwtToken.getSubject();
        UserDetails userDetails = loadUser(username);

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * Tries to load the user with the given username. All existing {@link UserDetailsService} will be queried until
     * one of them is returning a user. A {@link UsernameNotFoundException} will be thrown if the desired user does
     * not exist in any of the details services.
     *
     * @param username the username of the desired user
     * @return the found {@link UserDetails} object
     * @throws UsernameNotFoundException if the user does not exist
     */
    private UserDetails loadUser(String username) throws UsernameNotFoundException {
        for (UserDetailsService detailsService : services) {
            try {
                return detailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException exception) {
                if (log.isDebugEnabled()) {
                    log.debug("User '{}' could not be loaded using details service {}", username, detailsService.getClass().getSimpleName());
                }
            }
        }
        throw new UsernameNotFoundException(username);
    }
}
