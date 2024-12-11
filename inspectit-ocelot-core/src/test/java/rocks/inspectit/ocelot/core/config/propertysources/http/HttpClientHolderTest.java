package rocks.inspectit.ocelot.core.config.propertysources.http;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.config.HttpConfigSettings;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpClientHolderTest {

    @Test
    void shouldNotCreateNewHttpClientWhenConfigIsSame() throws IOException {
        HttpClientHolder holder = new HttpClientHolder();
        HttpConfigSettings settings = new HttpConfigSettings();

        settings.setConnectionTimeout(Duration.ofSeconds(30));
        CloseableHttpClient client = holder.getHttpClient(settings);
        CloseableHttpClient anotherClient = holder.getHttpClient(settings);

        assertThat(client).isSameAs(anotherClient);
    }

    @Test
    void shouldCreateNewHttpClientWhenConnectionTimeoutChanges() throws IOException {
        HttpClientHolder holder = new HttpClientHolder();
        HttpConfigSettings settings = new HttpConfigSettings();

        settings.setConnectionTimeout(Duration.ofSeconds(30));
        CloseableHttpClient client = holder.getHttpClient(settings);

        settings.setConnectionTimeout(Duration.ofSeconds(10));
        CloseableHttpClient updatedClient = holder.getHttpClient(settings);

        assertThat(updatedClient).isNotSameAs(client);
    }

    @Test
    void shouldCreateNewHttpClientWhenConnectionRequestTimeoutChanges() throws IOException {
        HttpClientHolder holder = new HttpClientHolder();
        HttpConfigSettings settings = new HttpConfigSettings();

        settings.setConnectionRequestTimeout(Duration.ofSeconds(30));
        CloseableHttpClient client = holder.getHttpClient(settings);

        settings.setConnectionRequestTimeout(Duration.ofSeconds(10));
        CloseableHttpClient updatedClient = holder.getHttpClient(settings);

        assertThat(updatedClient).isNotSameAs(client);
    }

    @Test
    void shouldCreateNewHttpClientWhenSocketTimeoutChanges() throws IOException {
        HttpClientHolder holder = new HttpClientHolder();
        HttpConfigSettings settings = new HttpConfigSettings();

        settings.setSocketTimeout(Duration.ofSeconds(30));
        CloseableHttpClient client = holder.getHttpClient(settings);

        settings.setSocketTimeout(Duration.ofSeconds(10));
        CloseableHttpClient updatedClient = holder.getHttpClient(settings);

        assertThat(updatedClient).isNotSameAs(client);
    }
}
