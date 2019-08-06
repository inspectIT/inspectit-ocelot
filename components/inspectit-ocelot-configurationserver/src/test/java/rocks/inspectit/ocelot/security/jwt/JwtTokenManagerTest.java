package rocks.inspectit.ocelot.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import javax.xml.crypto.Data;
import java.time.Duration;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenManagerTest {

    @InjectMocks
    private JwtTokenManager manager;

    @Mock
    private UserDetailsService userDetailsService;

    @Nested
    public class CreateToken {

        @Test
        public void validToken() {
            InspectitServerSettings settings = InspectitServerSettings.builder().tokenLifespan(Duration.ofMinutes(1)).build();
            manager.config = settings;

            String result = manager.createToken("username");

            assertThat(result).isNotEmpty();
            String[] splitToken = result.split("\\.");
            String unsignedToken = splitToken[0] + "." + splitToken[1] + ".";
            Claims jwt = (Claims) Jwts.parser().parse(unsignedToken).getBody();
            assertThat(jwt.getSubject()).isEqualTo("username");
            assertThat(jwt.getExpiration()).isBefore(DateUtils.addSeconds(new Date(), 65));
            assertThat(jwt.getExpiration()).isAfter(DateUtils.addSeconds(new Date(), 55));
        }

        @Test
        public void emptyUsername() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.createToken(""))
                    .withMessage("Username must not be null or empty.");
        }

        @Test
        public void nullUsername() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.createToken(null))
                    .withMessage("Username must not be null or empty.");
        }
    }

    @Nested
    public class AuthenticateWithToken {

        @Mock
        UserDetails userDetails;

        @Test
        public void successfulAuthentication() {
            InspectitServerSettings settings = InspectitServerSettings.builder().tokenLifespan(Duration.ofMinutes(1)).build();
            manager.config = settings;
            String token = manager.createToken("username");

            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);

            Authentication result = manager.authenticateWithToken(token);

            assertThat(result).isNotNull();
            assertThat(result.getPrincipal()).isSameAs(userDetails);
            verify(userDetailsService).loadUserByUsername("username");
            verifyNoMoreInteractions(userDetailsService);
        }

        @Test
        public void userNotFound() {
            InspectitServerSettings settings = InspectitServerSettings.builder().tokenLifespan(Duration.ofMinutes(1)).build();
            manager.config = settings;
            String token = manager.createToken("username");

            when(userDetailsService.loadUserByUsername(anyString())).thenThrow(UsernameNotFoundException.class);

            assertThatExceptionOfType(UsernameNotFoundException.class)
                    .isThrownBy(() -> manager.authenticateWithToken(token));

            verify(userDetailsService).loadUserByUsername("username");
            verifyNoMoreInteractions(userDetailsService);
        }
        @Test
        public void invalidJwtToken() {
            InspectitServerSettings settings = InspectitServerSettings.builder().tokenLifespan(Duration.ofMinutes(1)).build();
            manager.config = settings;

            assertThatExceptionOfType(JwtException.class)
                    .isThrownBy(() -> manager.authenticateWithToken("this-is-not-a-token"));

            verifyZeroInteractions(userDetailsService);
        }
    }
}