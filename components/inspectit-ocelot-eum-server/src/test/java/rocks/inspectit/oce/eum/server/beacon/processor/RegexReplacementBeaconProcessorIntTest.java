package rocks.inspectit.oce.eum.server.beacon.processor;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
class RegexReplacementBeaconProcessorIntTest {

    @Autowired
    private RegexReplacementBeaconProcessor processor;

    @Test
    void testRemoveQueryParameters() {
        Beacon in = Beacon.of(ImmutableMap.of(
                "u", "http://localhost:8080/a/b?param1=value1&param2=value2",
                "pgu", "http://otherhost:8081/foo/bar?foo=bar")
        );
        Beacon b = processor.process(in);

        assertThat(b.get("U_NO_QUERY")).isEqualTo("http://localhost:8080/a/b");
        assertThat(b.get("U_PATH")).isEqualTo("/a/b");
        assertThat(b.get("U_HOST")).isEqualTo("localhost");
        assertThat(b.get("U_PORT")).isEqualTo("8080");
        assertThat(b.get("PGU_NO_QUERY")).isEqualTo("http://otherhost:8081/foo/bar");
        assertThat(b.get("PGU_PATH")).isEqualTo("/foo/bar");
        assertThat(b.get("PGU_HOST")).isEqualTo("otherhost");
        assertThat(b.get("PGU_PORT")).isEqualTo("8081");
    }

    @Test
    void testRemoveQueryParametersFromUnwiseUrl() {
        Beacon in = Beacon.of(ImmutableMap.of(
                "u", "http://localhost/a/b?match[]={t=33}&parmam2=value1")
        );
        Beacon b = processor.process(in);
        assertThat(b.get("U_NO_QUERY")).isEqualTo("http://localhost/a/b");
        assertThat(b.get("U_PATH")).isEqualTo("/a/b");
        assertThat(b.get("U_HOST")).isEqualTo("localhost");
        assertThat(b.get("U_PORT")).isNull();
        assertThat(b.get("PGU_NO_QUERY")).isNull();
        assertThat(b.get("PGU_PATH")).isNull();
        assertThat(b.get("PGU_HOST")).isNull();
        assertThat(b.get("PGU_PORT")).isNull();
    }


}