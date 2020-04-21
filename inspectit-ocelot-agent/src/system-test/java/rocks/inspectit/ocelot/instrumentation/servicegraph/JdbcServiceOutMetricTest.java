package rocks.inspectit.ocelot.instrumentation.servicegraph;

import io.opencensus.stats.AggregationData;
import org.h2.jdbc.JdbcPreparedStatement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * uses global-propagation-tests.yml
 */
public class JdbcServiceOutMetricTest {

    static Connection conn;
    static PreparedStatement preparedSelect;
    static final String SELECT_SQL = "SELECT * FROM PERSON";

    static final String DB_URL_WITHOUT_JDBC = "h2:mem:test";
    static final String DB_URL = "jdbc:" + DB_URL_WITHOUT_JDBC;

    @BeforeAll
    static void setupInMemoryDB() throws Exception {
        Class.forName("org.h2.Driver");
        conn = DriverManager.getConnection(DB_URL + ";DB_CLOSE_DELAY=-1", "", "");
        conn.setAutoCommit(true);

        Statement setup = conn.createStatement();
        setup.execute("CREATE TABLE PERSON(id int primary key, name varchar(255))");
        setup.close();

        preparedSelect = conn.prepareStatement(SELECT_SQL);
    }

    @AfterAll
    static void cleanup() throws Exception {
        conn.close();
    }

    @Test
    void checkPreparedStatementCounted() throws Exception {
        String service = "jdbc_prep";
        TestUtils.waitForClassInstrumentations(Arrays.asList(JdbcPreparedStatement.class), 10, TimeUnit.SECONDS);

        InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
        ctx.setData("service", service);
        ctx.makeActive();

        preparedSelect.execute();


        TestUtils.waitForOpenCensusQueueToBeProcessed();

        Map<String, String> tags = new HashMap<>();
        tags.put("protocol", "jdbc");
        tags.put("service", service);
        tags.put("target_external", DB_URL_WITHOUT_JDBC);

        long cnt = ((AggregationData.CountData) TestUtils.getDataForView("service/out/count", tags)).getCount();
        double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("service/out/responsetime/sum", tags)).getSum();

        assertThat(cnt).isEqualTo(1);
        assertThat(respSum).isGreaterThan(0);

        ctx.close();
    }


    @Test
    void checkDirectStatementCounted() throws Exception {
        String service = "jdbc_non_prep";
        TestUtils.waitForClassInstrumentations(Arrays.asList(JdbcPreparedStatement.class), 10, TimeUnit.SECONDS);

        InternalInspectitContext ctx = Instances.contextManager.enterNewContext();
        ctx.setData("service", service);
        ctx.makeActive();

        conn.createStatement().execute(SELECT_SQL);


        TestUtils.waitForOpenCensusQueueToBeProcessed();

        Map<String, String> tags = new HashMap<>();
        tags.put("protocol", "jdbc");
        tags.put("service", service);
        tags.put("target_external", DB_URL_WITHOUT_JDBC);

        long cnt = ((AggregationData.CountData) TestUtils.getDataForView("service/out/count", tags)).getCount();
        double respSum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("service/out/responsetime/sum", tags)).getSum();

        assertThat(cnt).isEqualTo(1);
        assertThat(respSum).isGreaterThan(0);

        ctx.close();
    }


}