package rocks.inspectit.ocelot.security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.security.Security;

public class GrpcSslConfiguration implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // Via BouncyCastle ConfigurationServer is able to read private keys encoded in PCKS#1.
        // This class is applied via application.yml
        Security.addProvider(new BouncyCastleProvider());
    }
}
