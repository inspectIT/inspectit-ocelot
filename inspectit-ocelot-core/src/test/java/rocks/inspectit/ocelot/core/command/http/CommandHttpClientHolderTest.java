package rocks.inspectit.ocelot.core.command.http;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.command.AgentCommandSettings;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandHttpClientHolderTest {

    @Nested
    class Discovery {

        @Test
        void shouldNotCreateNewHttpClientWhenConfigIsSame() throws IOException {
            CommandHttpClientHolder holder = new CommandHttpClientHolder();
            AgentCommandSettings settings = new AgentCommandSettings();

            settings.setConnectionTimeout(Duration.ofSeconds(30));
            CloseableHttpClient client = holder.getDiscoveryHttpClient(settings);
            CloseableHttpClient anotherClient = holder.getDiscoveryHttpClient(settings);

            assertThat(client).isSameAs(anotherClient);
        }

        @Test
        void shouldCreateNewHttpClientWhenConnectionTimeoutChanges() throws IOException {
            CommandHttpClientHolder holder = new CommandHttpClientHolder();
            AgentCommandSettings settings = new AgentCommandSettings();

            settings.setConnectionTimeout(Duration.ofSeconds(30));
            CloseableHttpClient client = holder.getDiscoveryHttpClient(settings);

            settings.setConnectionTimeout(Duration.ofSeconds(10));
            CloseableHttpClient updatedClient = holder.getDiscoveryHttpClient(settings);

            assertThat(updatedClient).isNotSameAs(client);
        }

        @Test
        void shouldCreateNewHttpClientWhenConnectionRequestTimeoutChanges() throws IOException {
            CommandHttpClientHolder holder = new CommandHttpClientHolder();
            AgentCommandSettings settings = new AgentCommandSettings();

            settings.setConnectionRequestTimeout(Duration.ofSeconds(30));
            CloseableHttpClient client = holder.getDiscoveryHttpClient(settings);

            settings.setConnectionRequestTimeout(Duration.ofSeconds(10));
            CloseableHttpClient updatedClient = holder.getDiscoveryHttpClient(settings);

            assertThat(updatedClient).isNotSameAs(client);
        }

        @Test
        void shouldCreateNewHttpClientWhenSocketTimeoutChanges() throws IOException {
            CommandHttpClientHolder holder = new CommandHttpClientHolder();
            AgentCommandSettings settings = new AgentCommandSettings();

            settings.setSocketTimeout(Duration.ofSeconds(30));
            CloseableHttpClient client = holder.getDiscoveryHttpClient(settings);

            settings.setSocketTimeout(Duration.ofSeconds(10));
            CloseableHttpClient updatedClient = holder.getDiscoveryHttpClient(settings);

            assertThat(updatedClient).isNotSameAs(client);
        }

        @Test
        void shouldCreateNewHttpClientWhenTTLChanges() throws IOException {
            CommandHttpClientHolder holder = new CommandHttpClientHolder();
            AgentCommandSettings settings = new AgentCommandSettings();

            settings.setTimeToLive(Duration.ofSeconds(30));
            CloseableHttpClient client = holder.getDiscoveryHttpClient(settings);

            settings.setTimeToLive(Duration.ofSeconds(10));
            CloseableHttpClient updatedClient = holder.getDiscoveryHttpClient(settings);

            assertThat(updatedClient).isNotSameAs(client);
        }
    }

    @Nested
    class Live {

        @Test
        void shouldNotCreateNewHttpClientWhenConfigIsSame() throws IOException {
            CommandHttpClientHolder holder = new CommandHttpClientHolder();
            AgentCommandSettings settings = new AgentCommandSettings();

            settings.setLiveConnectionTimeout(Duration.ofSeconds(30));
            CloseableHttpClient client = holder.getLiveHttpClient(settings);
            CloseableHttpClient anotherClient = holder.getLiveHttpClient(settings);

            assertThat(client).isSameAs(anotherClient);
        }

        @Test
        void shouldCreateNewHttpClientWhenConnectionTimeoutChanges() throws IOException {
            CommandHttpClientHolder holder = new CommandHttpClientHolder();
            AgentCommandSettings settings = new AgentCommandSettings();

            settings.setLiveConnectionTimeout(Duration.ofSeconds(30));
            CloseableHttpClient client = holder.getLiveHttpClient(settings);

            settings.setLiveConnectionTimeout(Duration.ofSeconds(10));
            CloseableHttpClient updatedClient = holder.getLiveHttpClient(settings);

            assertThat(updatedClient).isNotSameAs(client);
        }

        @Test
        void shouldCreateNewHttpClientWhenConnectionRequestTimeoutChanges() throws IOException {
            CommandHttpClientHolder holder = new CommandHttpClientHolder();
            AgentCommandSettings settings = new AgentCommandSettings();

            settings.setLiveConnectionRequestTimeout(Duration.ofSeconds(30));
            CloseableHttpClient client = holder.getLiveHttpClient(settings);

            settings.setLiveConnectionRequestTimeout(Duration.ofSeconds(10));
            CloseableHttpClient updatedClient = holder.getLiveHttpClient(settings);

            assertThat(updatedClient).isNotSameAs(client);
        }

        @Test
        void shouldCreateNewHttpClientWhenSocketTimeoutChanges() throws IOException {
            CommandHttpClientHolder holder = new CommandHttpClientHolder();
            AgentCommandSettings settings = new AgentCommandSettings();

            settings.setLiveSocketTimeout(Duration.ofSeconds(30));
            CloseableHttpClient client = holder.getLiveHttpClient(settings);

            settings.setLiveSocketTimeout(Duration.ofSeconds(10));
            CloseableHttpClient updatedClient = holder.getLiveHttpClient(settings);

            assertThat(updatedClient).isNotSameAs(client);
        }

        @Test
        void shouldCreateNewHttpClientWhenTTLChanges() throws IOException {
            CommandHttpClientHolder holder = new CommandHttpClientHolder();
            AgentCommandSettings settings = new AgentCommandSettings();

            settings.setTimeToLive(Duration.ofSeconds(30));
            CloseableHttpClient client = holder.getLiveHttpClient(settings);

            settings.setTimeToLive(Duration.ofSeconds(10));
            CloseableHttpClient updatedClient = holder.getLiveHttpClient(settings);

            assertThat(updatedClient).isNotSameAs(client);
        }
    }
}
