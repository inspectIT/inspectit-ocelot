package rocks.inspectit.ocelot.instrumentation.http;

import io.opencensus.stats.AggregationData;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.IInspectitContext;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * uses global-propagation-tests.yml
 */
public class HttpOutMetricTest {

    public static final String GOOGLE_HOST = "www.google.com";

    public static final String GOOGLE_SEARCH_URL = "https://www.google.com/search?q=hello";
    public static final String GOOGLE_SEARCH_PATH = "/search";

    public static final String GOOGLE_404_URL = "https://www.google.com/blub";
    public static final String GOOGLE_404_PATH = "/blub";

    @Nested
    class ApacheClient {

        CloseableHttpClient client;

        @BeforeEach
        void setupClient() {
            RequestConfig.Builder requestBuilder = RequestConfig.custom();
            HttpClientBuilder builder = HttpClientBuilder.create();
            builder.setDefaultRequestConfig(requestBuilder.build());
            client = builder.build();

            TestUtils.waitForInstrumentationToComplete();
        }

        @AfterEach
        void destroyClient() throws Exception {
            client.close();
        }

        @Test
        void testGoogleSearch() throws Exception {
            IInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service", "apache_client_test");
            ctx.makeActive();
            client.execute(new HttpGet(GOOGLE_SEARCH_URL));
            ctx.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("service", "apache_client_test");
            tags.put("http_host", GOOGLE_HOST);
            tags.put("http_path", GOOGLE_SEARCH_PATH);
            tags.put("http_status", "200");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/out/responsetime/sum", tags)).getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }

        @Test
        void testGoogle404() throws Exception {
            IInspectitContext ctx = Instances.contextManager.enterNewContext();
            ctx.setData("service", "apache_client_test");
            ctx.makeActive();
            client.execute(new HttpGet(GOOGLE_404_URL));
            ctx.close();

            TestUtils.waitForOpenCensusQueueToBeProcessed();

            Map<String, String> tags = new HashMap<>();
            tags.put("service", "apache_client_test");
            tags.put("http_host", GOOGLE_HOST);
            tags.put("http_path", GOOGLE_404_PATH);
            tags.put("http_status", "404");

            long cnt = ((AggregationData.CountData) TestUtils.getDataForView("http/out/count", tags)).getCount();
            double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("http/out/responsetime/sum", tags)).getSum();

            assertThat(cnt).isEqualTo(1);
            assertThat(respSum).isGreaterThan(0);
        }

    }


}