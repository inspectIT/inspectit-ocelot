package rocks.inspectit.ocelot.agentcommunication;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.IntegrationTestBase;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class GrpcSslConfigurationTest extends IntegrationTestBase {

    public static final String ROOT_DIRECTORY = "/private_key_tests/";

    @Test
    void successfullyCreatesGrpcSslContextWithPCKS1FormattedPrivateKey() throws IOException {
        try (InputStream certificate = getResourceAsStream("certificate.cert");
             InputStream privateKey = getResourceAsStream("private_pcks1.key")) {
            SslContext sslContext = GrpcSslContexts.forServer(certificate, privateKey, null).build();
            assertThat(sslContext).isNotNull();
        }
    }

    private InputStream getResourceAsStream (String relativePathName) {
        return this.getClass().getResourceAsStream(ROOT_DIRECTORY + relativePathName);
    }
}
