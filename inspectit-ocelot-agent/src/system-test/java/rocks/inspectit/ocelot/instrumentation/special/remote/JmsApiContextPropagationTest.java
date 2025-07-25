package rocks.inspectit.ocelot.instrumentation.special.remote;

import io.opencensus.common.Scope;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.utils.TestUtils;

import javax.jms.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * uses global-propagation-tests.yml
 */
public class JmsApiContextPropagationTest {

    private static final String BROKER_URL = "vm://localhost?broker.persistent=false";

    private static final String QUEUE_NAME = "testQueue";

    private BrokerService broker;

    private Connection connection;

    private Session session;

    private MessageProducer producer;

    private MessageConsumer consumer;

    @BeforeEach
    void setUp() throws Exception {
        broker = new BrokerService();
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.start();

        ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
        connection = factory.createConnection();
        connection.start();

        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination queue = session.createQueue(QUEUE_NAME);
        producer = session.createProducer(queue);
        consumer = session.createConsumer(queue);

        TestUtils.waitForClassInstrumentations(
                Arrays.asList(
                        Class.forName("javax.jms.MessageProducer"),
                        Class.forName("javax.jms.MessageListener")
                ),
                true, 30, TimeUnit.SECONDS);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
        broker.stop();
    }

    @Nested
    class DownPropagation {

        @Test
        void shouldWriteDownPropagatedData() throws Exception {
            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("down_propagated"), TagValue.create("myvalue"))
                    .buildScoped()
            ) {
                TextMessage message = session.createTextMessage("test");
                producer.send(message);
            }

            Message received = consumer.receive(2000);

            String baggage = received.getStringProperty("Baggage");
            assertThat(baggage).contains("down_propagated=myvalue");
        }

        @Test
        void shouldReadDownPropagatedData() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            List<Object> propagationData = new LinkedList<>();
            // Use asynchronous message listener to read down propagated data
            consumer.setMessageListener(message -> {
                InternalInspectitContext myCtx = Instances.contextManager.enterNewContext();
                myCtx.makeActive();
                propagationData.add(myCtx.getData("down_propagated"));
                myCtx.close();
                latch.countDown();
            });

            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("down_propagated"), TagValue.create("myvalue"))
                    .buildScoped()) {

                TextMessage message = session.createTextMessage("test");
                producer.send(message);
            }

            Assertions.assertThat(propagationData).contains("myvalue");
        }
    }
}
