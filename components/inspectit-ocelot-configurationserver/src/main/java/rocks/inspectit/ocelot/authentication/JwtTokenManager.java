package rocks.inspectit.ocelot.authentication;

import com.google.common.annotations.VisibleForTesting;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.user.LocalUserDetailsService;

import java.security.Key;
import java.util.Date;

/**
 * Creates and verifies JWT based access tokens.
 * Tokens contain the name of the user and an expiration date.
 * Tokens are only considered valid if the expiration date has not been exceeded yet.
 */
@Component
public class JwtTokenManager {

    @VisibleForTesting
    @Autowired
    InspectitServerSettings config;

    /**
     * We dynamically generate a secret to sign the tokens with at server start.
     * This means that tokens automatically become invalid as soon as the server restarts.
     */
    private Key secret = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    @Autowired
    private LocalUserDetailsService userDetailsService;

    /**
     * Creates a token containing the specified username.
     * The token expires after the specified Duration via {@link InspectitServerSettings#getTokenLifespan()} from now.
     *
     * @param username the username for which the token is generated
     * @return the generated token
     */
    public String createToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + config.getTokenLifespan().toMillis());
        return Jwts.builder()
                .setSubject(username)
                .setExpiration(expiration)
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
        Claims parsedAndValidatedToken = Jwts.parser().setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();

        String username = parsedAndValidatedToken.getSubject();
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

}
