package rocks.inspectit.ocelot.rest.alert.kapacitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.Handler;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.Topic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class KapacitorTopicControllerTest extends KapacitorControllerTestBase {

    private KapacitorTopicController controller;

    @BeforeEach
    void setup() {
        controller = super.createWithMock(KapacitorTopicController::new);
    }

    @Nested
    class GetTopics {

        @Test
        void topicsListedCorrectly() {
            mockKapacitor.expect(requestTo("/kapacitor/v1/alerts/topics"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(getTestJson("topics.json"), MediaType.APPLICATION_JSON));

            List<Topic> topics = controller.getTopics();

            assertThat(topics).containsExactlyInAnyOrder(
                    Topic.builder()
                            .id("system")
                            .level("CRITICAL")
                            .build(),
                    Topic.builder()
                            .id("app")
                            .level("OK")
                            .build()
            );
            mockKapacitor.verify();
        }

    }

    @Nested
    class GetHandlers {

        @Test
        void handlersListedCorrectly() {
            mockKapacitor.expect(requestTo("/kapacitor/v1/alerts/topics/system/handlers"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(getTestJson("topic_system_handlers.json"), MediaType.APPLICATION_JSON));

            List<Handler> handlers = controller.getHandlers("system");

            assertThat(handlers).containsExactlyInAnyOrder(
                    Handler.builder()
                            .id("some_slack")
                            .kind("slack")
                            .options(Collections.singletonMap("channel", "#alerts"))
                            .build(),
                    Handler.builder()
                            .id("empty_smtp")
                            .kind("smtp")
                            .build(),
                    Handler.builder()
                            .id("some_smtp")
                            .kind("smtp")
                            .match("some lambda")
                            .options(Collections.singletonMap("to", Arrays.asList("my.mail@web.com")))
                            .build()
            );
            mockKapacitor.verify();
        }

    }

    @Nested
    class AddHandler {

        @Test
        void addWithMatch() {
            Handler toAdd = Handler.builder()
                    .id("my_handler")
                    .kind("some_kind")
                    .match("whatever")
                    .build();

            mockKapacitor.expect(requestTo("/kapacitor/v1/alerts/topics/system/handlers"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().json(
                            "{" +
                                    "\"id\" : \"my_handler\"," +
                                    "\"kind\" : \"some_kind\"," +
                                    "\"match\" : \"whatever\"" +
                                    "}"
                            , true))
                    .andRespond(withSuccess(getTestJson("topic_system_handlers_some_smtp.json"), MediaType.APPLICATION_JSON));

            Handler result = controller.addHandler("system", toAdd);

            assertThat(result).isNotNull();
            mockKapacitor.verify();
        }

        @Test
        void addWithOptions() {
            Handler toAdd = Handler.builder()
                    .id("my_handler")
                    .kind("some_kind")
                    .options(Collections.singletonMap("hello", Collections.singletonList("world")))
                    .build();

            mockKapacitor.expect(requestTo("/kapacitor/v1/alerts/topics/system/handlers"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().json(
                            "{" +
                                    "\"id\" : \"my_handler\"," +
                                    "\"kind\" : \"some_kind\"," +
                                    "\"options\" : {\"hello\" : [\"world\"]}" +
                                    "}"
                            , true))
                    .andRespond(withSuccess(getTestJson("topic_system_handlers_some_smtp.json"), MediaType.APPLICATION_JSON));

            Handler result = controller.addHandler("system", toAdd);

            assertThat(result).isNotNull();
            mockKapacitor.verify();
        }

    }

    @Nested
    class ReplaceHandler {

        @Test
        void configureOnlyMatch() {
            Handler toUpdate = Handler.builder()
                    .kind("somekind")
                    .match("whatever")
                    .build();

            mockKapacitor.expect(requestTo("/kapacitor/v1/alerts/topics/system/handlers/my_handler"))
                    .andExpect(method(HttpMethod.PUT))
                    .andExpect(content().json(
                            "{" +
                                    "\"id\" : \"my_handler\"," +
                                    "\"kind\" : \"somekind\"," +
                                    "\"match\" : \"whatever\"" +
                                    "}"
                            , true))
                    .andRespond(withSuccess());

            controller.replaceHandler("system", "my_handler", toUpdate);

            mockKapacitor.verify();
        }

        @Test
        void updateOptions() {
            Handler toUpdate = Handler.builder()
                    .kind("somekind")
                    .options(Collections.singletonMap("hello", Collections.singletonList("world")))
                    .build();

            mockKapacitor.expect(requestTo("/kapacitor/v1/alerts/topics/system/handlers/my_handler"))
                    .andExpect(method(HttpMethod.PUT))
                    .andExpect(content().json(
                            "{" +
                                    "\"id\" : \"my_handler\"," +
                                    "\"kind\" : \"somekind\"," +
                                    "\"options\" : {\"hello\" : [\"world\"]}" +
                                    "}"
                            , true))
                    .andRespond(withSuccess());

            controller.replaceHandler("system", "my_handler", toUpdate);

            mockKapacitor.verify();
        }

        @Test
        void updateIdAndKind() {
            Handler toUpdate = Handler.builder()
                    .id("new_id")
                    .kind("new_kind")
                    .build();

            mockKapacitor.expect(requestTo("/kapacitor/v1/alerts/topics/system/handlers/my_handler"))
                    .andExpect(method(HttpMethod.PUT))
                    .andExpect(content().json(
                            "{" +
                                    "\"id\" : \"new_id\" ," +
                                    "\"kind\" : \"new_kind\" " +
                                    "}"
                            , true))
                    .andRespond(withSuccess());

            controller.replaceHandler("system", "my_handler", toUpdate);

            mockKapacitor.verify();
        }

    }

    @Nested
    class RemoveHandler {

        @Test
        void deleteHandler() {
            mockKapacitor.expect(requestTo("/kapacitor/v1/alerts/topics/system/handlers/my_handler"))
                    .andExpect(method(HttpMethod.DELETE))
                    .andRespond(withSuccess());

            controller.removeHandler("system", "my_handler");

            mockKapacitor.verify();
        }
    }

}
