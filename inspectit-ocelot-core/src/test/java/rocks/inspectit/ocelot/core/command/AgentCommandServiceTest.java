package rocks.inspectit.ocelot.core.command;

import com.google.common.io.Resources;
import io.grpc.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgentCommandServiceTest {

    @InjectMocks
    private AgentCommandService service;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InspectitConfig configuration;

    @Nested
    public class DoEnable {

        @Test
        public void successfullyEnabled() throws InterruptedException {
            lenient().when(configuration.getAgentCommands().getHost()).thenReturn("inspectit.rocks");
            lenient().when(configuration.getAgentCommands().getPort()).thenReturn(9657);

            boolean result = service.doEnable(configuration);

            assertThat(result).isTrue();
            assertThat(service.getClient()).isNotNull();
        }

        @AfterEach
        public void closeGrpcConnection() {
            service.doDisable();
        }
    }

    @Nested
    public class DoDisable {

        @Test
        public void notEnabled() {
            boolean result = service.doDisable();

            assertThat(result).isTrue();
            assertThat(service.getClient()).isNull();
        }

        @Test
        public void isEnabled() throws MalformedURLException, InterruptedException {
            when(configuration.getAgentCommands().getHost()).thenReturn("inspectitrocks");
            when(configuration.getAgentCommands().getPort()).thenReturn(9657);

            service.doEnable(configuration);
            TimeUnit.SECONDS.sleep(5);
            assertThat(service.getClient()).isNotNull();

            boolean result = service.doDisable();
            TimeUnit.SECONDS.sleep(5);

            assertThat(result).isTrue();
            assertThat(service.getClient()).isNull();
        }
    }

    @Nested
    public class GetCommandUrl {

        @Test
        public void validCommandUrl() throws Exception {
            when(configuration.getAgentCommands().getHost()).thenReturn("example.org");

            String result = service.getCommandHost(configuration);

            assertThat(result).isEqualTo("example.org");
        }

        @Test
        public void deriveUrlWithoutConfigUrl() {
            when(configuration.getAgentCommands().isDeriveHostFromHttpConfigUrl()).thenReturn(true);
            when(configuration.getConfig().getHttp().getUrl()).thenReturn(null);

            assertThat(configuration.getConfig().getHttp().getUrl()).isNull();
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> service.getCommandHost(configuration))
                    .withMessage("The URL cannot be derived from the HTTP configuration URL because it is null.");
        }

        @Test
        public void deriveUrl() throws Exception {
            when(configuration.getConfig()
                    .getHttp()
                    .getUrl()).thenReturn(new URL("http://example.org:8090/api/endpoint"));
            when(configuration.getAgentCommands().isDeriveHostFromHttpConfigUrl()).thenReturn(true);
            String result = service.getCommandHost(configuration);

            assertThat(result).isEqualTo("example.org");
        }

        @Test
        public void verifyPrioritization() throws Exception {
            lenient().when(configuration.getAgentCommands().getHost()).thenReturn("inspectit.rocks");
            when(configuration.getConfig().getHttp().getUrl()).thenReturn(new URL("http://example.org/api/endpoint"));
            when(configuration.getAgentCommands().isDeriveHostFromHttpConfigUrl()).thenReturn(true);
            String result = service.getCommandHost(configuration);

            assertThat(result).isEqualTo("example.org");
        }
    }

    @Nested
    public class GetChannel {

        @Mock
        private AgentCommandSettings settings;

        private final String HOST = "inspectit.rocks";

        private final int PORT = 9657;

        @Test
        public void onlyOneOfCertificatesSet() throws IOException {
            when(settings.isUseTls()).thenReturn(true);
            when(settings.getClientCertChainFilePath()).thenReturn(null);
            when(settings.getClientPrivateKeyFilePath()).thenReturn("testpath");

            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> service.getChannel(settings, HOST, PORT))
                    .withMessage(String.format("Only one of clientCertChainFilePath='%s' and clientPrivateKeyFilePath='%s' is set, but either both need to be set or neither.", null, "testpath"));
        }

        /**
         * As of now only tests whether the method runs without errors, because you can not compare the result and
         * expected properly with equals, and you can not get fields to compare either.
         *
         * @throws URISyntaxException
         * @throws IOException
         */
        @Test
        public void withTlsAndTrustCertCollection() throws URISyntaxException, IOException {
            URL pemResource = Resources.getResource("certificates/client.pem");
            File pemPath = Paths.get(pemResource.toURI()).toFile();
            URL keyResource = Resources.getResource("certificates/client.key");
            File keyPath = Paths.get(keyResource.toURI()).toFile();
            URL caResource = Resources.getResource("certificates/ca.pem");
            File caPath = Paths.get(caResource.toURI()).toFile();

            when(settings.isUseTls()).thenReturn(true);
            when(settings.getClientCertChainFilePath()).thenReturn(pemPath.getAbsolutePath());
            when(settings.getClientPrivateKeyFilePath()).thenReturn(keyPath.getAbsolutePath());
            when(settings.getTrustCertCollectionFilePath()).thenReturn(caPath.getAbsolutePath());

            ManagedChannel result = (ManagedChannel) service.getChannel(settings, HOST, PORT);

            ChannelCredentials creds = TlsChannelCredentials.newBuilder()
                    .keyManager(new File(pemPath.getAbsolutePath()), new File(keyPath.getAbsolutePath()))
                    .trustManager(new File(caPath.getAbsolutePath()))
                    .build();
            Channel expected = Grpc.newChannelBuilderForAddress(HOST, PORT, creds).build();

            // assertThat(result).isEqualTo(expected);
        }

        /**
         * As of now only tests whether the method runs without errors, because you can not compare the result and
         * expected properly with equals, and you can not get fields to compare either.
         *
         * @throws URISyntaxException
         * @throws IOException
         */
        @Test
        public void withTlsWithoutTrustCertCollection() throws URISyntaxException, IOException {
            URL pemResource = Resources.getResource("certificates/client.pem");
            File pemPath = Paths.get(pemResource.toURI()).toFile();
            URL keyResource = Resources.getResource("certificates/client.key");
            File keyPath = Paths.get(keyResource.toURI()).toFile();

            when(settings.isUseTls()).thenReturn(true);
            when(settings.getClientCertChainFilePath()).thenReturn(pemPath.getAbsolutePath());
            when(settings.getClientPrivateKeyFilePath()).thenReturn(keyPath.getAbsolutePath());

            Channel result = service.getChannel(settings, HOST, PORT);

            ChannelCredentials creds = TlsChannelCredentials.newBuilder()
                    .keyManager(new File(pemPath.getAbsolutePath()), new File(keyPath.getAbsolutePath()))
                    .build();
            Channel expected = Grpc.newChannelBuilderForAddress(HOST, PORT, creds).build();

            // assertThat(result).isEqualTo(expected);
        }

        /**
         * As of now only tests whether the method runs without errors, because you can not compare the result and
         * expected properly with equals, and you can not get fields to compare either.
         *
         * @throws URISyntaxException
         * @throws IOException
         */
        @Test
        public void withoutTls() throws IOException {

            when(settings.isUseTls()).thenReturn(false);

            Channel result = service.getChannel(settings, HOST, PORT);

            Channel expected = ManagedChannelBuilder.forAddress(HOST, PORT).usePlaintext().build();

            // assertThat(result).isEqualTo(expected);
        }

    }
}
