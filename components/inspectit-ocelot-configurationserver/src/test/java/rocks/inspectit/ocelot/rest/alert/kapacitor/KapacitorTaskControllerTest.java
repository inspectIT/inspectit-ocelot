package rocks.inspectit.ocelot.rest.alert.kapacitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.Task;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.TemplateVariable;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class KapacitorTaskControllerTest extends KapacitorControllerTestBase {

    private KapacitorTaskController controller;

    @BeforeEach
    void setup() {
        controller = super.createWithMock(KapacitorTaskController::new);
    }

    @Nested
    class GetAllTasks {

        @Test
        void tasksListedCorrectly() {
            mockKapacitor.expect(requestTo("/kapacitor/v1/tasks"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(getTestJson("tasks.json"), MediaType.APPLICATION_JSON));

            List<Task> result = controller.getAllTasks();

            assertThat(result.size()).isEqualTo(2);
            assertThat(result).anySatisfy(task -> {
                assertThat(task.getId()).isEqualTo("var_types");
                assertThat(task.getCreated()).isEqualTo("2020-07-22T08:21:47.324303054Z");
                assertThat(task.getLastEnabled()).isEqualTo("2020-07-22T08:21:47.324303055Z");
                assertThat(task.getModified()).isEqualTo("2020-07-22T08:21:47.324303056Z");
                assertThat(task.getError()).isEmpty();
                assertThat(task.getExecuting()).isTrue();
                assertThat(task.getStatus()).isEqualTo("enabled");
                assertThat(task.getTemplate()).isEqualTo("my_template");
                assertThat(task.getDescription()).isNull();
                assertThat(task.getTopic()).isNull();
                assertThat(task.getVars()).containsExactlyInAnyOrder(TemplateVariable.builder()
                        .name("some_string")
                        .type("string")
                        .value("test")
                        .description("")
                        .build(), TemplateVariable.builder()
                        .name("some_float")
                        .type("float")
                        .value(4242.0)
                        .description("")
                        .build(), TemplateVariable.builder()
                        .name("some_int")
                        .type("int")
                        .value(42.0)
                        .description("")
                        .build(), TemplateVariable.builder()
                        .name("some_dur")
                        .type("duration")
                        .value("2h")
                        .description("")
                        .build());

            });
            assertThat(result).anySatisfy(task -> {
                assertThat(task.getId()).isEqualTo("special_vars");
                assertThat(task.getCreated()).isEqualTo("2020-07-22T08:21:47.324303054Z");
                assertThat(task.getLastEnabled()).isEqualTo("2020-07-22T08:21:47.324303055Z");
                assertThat(task.getModified()).isEqualTo("2020-07-22T08:21:47.324303056Z");
                assertThat(task.getError()).isEqualTo("Some error");
                assertThat(task.getExecuting()).isFalse();
                assertThat(task.getStatus()).isEqualTo("disabled");
                assertThat(task.getTemplate()).isEqualTo("some_template");
                assertThat(task.getDescription()).isEqualTo("My task description");
                assertThat(task.getTopic()).isEqualTo("my_topic");
                assertThat(task.getVars()).containsExactlyInAnyOrder(TemplateVariable.builder()
                        .name("some_string")
                        .type("string")
                        .value("test")
                        .description("")
                        .build());

            });

            mockKapacitor.verify();
        }
    }

    @Nested
    class GetTasks {

        @Test
        void conflictingTemplateDefinitions() {
            mockKapacitor.expect(requestTo("/kapacitor/v1/tasks/conflicting_template"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(getTestJson("tasks_conflicting_template.json"), MediaType.APPLICATION_JSON));

            Task task = controller.getTask("conflicting_template");

            assertThat(task.getId()).isEqualTo("conflicting_template");
            assertThat(task.getCreated()).isEqualTo("2020-07-22T08:21:47.324303054Z");
            assertThat(task.getLastEnabled()).isEqualTo("2020-07-22T08:21:47.324303055Z");
            assertThat(task.getModified()).isEqualTo("2020-07-22T08:21:47.324303056Z");
            assertThat(task.getError()).isEqualTo("Some error");
            assertThat(task.getExecuting()).isFalse();
            assertThat(task.getStatus()).isEqualTo("disabled");
            assertThat(task.getTemplate()).isEqualTo("other_template");
            assertThat(task.getDescription()).isEqualTo("My task description");
            assertThat(task.getTopic()).isEqualTo("my_topic");
            assertThat(task.getVars()).containsExactlyInAnyOrder(TemplateVariable.builder()
                    .name("some_string")
                    .type("string")
                    .value("test")
                    .description("")
                    .build());

            mockKapacitor.verify();
        }
    }

    @Nested
    class AddTasks {

        @Test
        void addWithTemplate() {
            Task toAdd = Task.builder().id("my_task").template("blub").build();

            mockKapacitor.expect(requestTo("/kapacitor/v1/tasks"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().json("{" + "\"id\" : \"my_task\"," + "\"template-id\" : \"blub\"," + "\"vars\" : { \"inspectit_template_reference\" : {\"type\" : \"string\", \"value\" : \"blub\"} }" + "}", true))
                    .andRespond(withSuccess(getTestJson("tasks_conflicting_template.json"), MediaType.APPLICATION_JSON));

            Task result = controller.addTask(toAdd);

            assertThat(result).isNotNull();

            mockKapacitor.verify();
        }

        @Test
        void addWithDescriptionAndVariable() {
            Task toAdd = Task.builder()
                    .id("my_task")
                    .description("blub")
                    .vars(Collections.singletonList(TemplateVariable.builder()
                            .name("my_dur")
                            .type("duration")
                            .value("7s")
                            .build()))
                    .build();

            mockKapacitor.expect(requestTo("/kapacitor/v1/tasks"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().json("{" + "\"id\" : \"my_task\"," + "\"vars\" : { " + "\"inspectit_template_description\" : {\"type\" : \"string\", \"value\" : \"blub\"}, " + "\"my_dur\" : {\"type\" : \"duration\", \"value\" : 7000000000} " + "}" + "}", true))
                    .andRespond(withSuccess(getTestJson("tasks_conflicting_template.json"), MediaType.APPLICATION_JSON));

            Task result = controller.addTask(toAdd);

            assertThat(result).isNotNull();

            mockKapacitor.verify();
        }

        @Test
        void addWithTopicAndStatus() {
            Task toAdd = Task.builder().id("my_task").topic("blub").status("disabled").build();

            mockKapacitor.expect(requestTo("/kapacitor/v1/tasks"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().json("{" + "\"id\" : \"my_task\"," + "\"status\" : \"disabled\"," + "\"vars\" : { " + "\"topic\" : {\"type\" : \"string\", \"value\" : \"blub\"} " + "}" + "}", true))
                    .andRespond(withSuccess(getTestJson("tasks_conflicting_template.json"), MediaType.APPLICATION_JSON));

            Task result = controller.addTask(toAdd);

            assertThat(result).isNotNull();
            mockKapacitor.verify();
        }
    }

    @Nested
    class UpdateTasks {

        @Test
        void changeId() {
            Task toAdd = Task.builder().id("new_id").build();

            mockKapacitor.expect(requestTo("/kapacitor/v1/tasks/my_task"))
                    .andExpect(method(HttpMethod.PATCH))
                    .andExpect(content().json("{" + "\"id\" : \"new_id\"" + "}", true))
                    .andRespond(withSuccess(getTestJson("tasks_conflicting_template.json"), MediaType.APPLICATION_JSON));

            mockKapacitor.expect(requestTo("/kapacitor/v1/templates/"))
                    .andExpect(content().json("{}"))
                    .andExpect(method(HttpMethod.PATCH))
                    .andRespond(withSuccess(getTestJson("tasks_conflicting_template.json"), MediaType.APPLICATION_JSON));

            Task result = controller.updateTask("my_task", toAdd);

            assertThat(result).isNotNull();
            mockKapacitor.verify();
        }

        @Test
        void changeTemplate() {
            Task toAdd = Task.builder().template("blub").build();

            mockKapacitor.expect(requestTo("/kapacitor/v1/tasks/my_task"))
                    .andExpect(method(HttpMethod.PATCH))
                    .andExpect(content().json("{" + "\"template-id\" : \"blub\"," + "\"vars\" : { \"inspectit_template_reference\" : {\"type\" : \"string\", \"value\" : \"blub\"} }" + "}", true))
                    .andRespond(withSuccess(getTestJson("tasks_conflicting_template.json"), MediaType.APPLICATION_JSON));

            mockKapacitor.expect(requestTo("/kapacitor/v1/templates/blub"))
                    .andExpect(content().json("{" + "\"id\" : \"blub\"}"))
                    .andExpect(method(HttpMethod.PATCH))
                    .andRespond(withSuccess(getTestJson("tasks_conflicting_template.json"), MediaType.APPLICATION_JSON));

            Task result = controller.updateTask("my_task", toAdd);

            assertThat(result).isNotNull();
            mockKapacitor.verify();
        }

        @Test
        void changeDescriptionAndVariable() {
            Task toAdd = Task.builder()
                    .description("blub")
                    .vars(Collections.singletonList(TemplateVariable.builder()
                            .name("my_dur")
                            .type("duration")
                            .value("7s")
                            .build()))
                    .build();

            mockKapacitor.expect(requestTo("/kapacitor/v1/tasks/my_task"))
                    .andExpect(method(HttpMethod.PATCH))
                    .andExpect(content().json("{" + "\"vars\" : { " + "\"inspectit_template_description\" : {\"type\" : \"string\", \"value\" : \"blub\"}, " + "\"my_dur\" : {\"type\" : \"duration\", \"value\" : 7000000000} " + "}" + "}", true))
                    .andRespond(withSuccess(getTestJson("tasks_conflicting_template.json"), MediaType.APPLICATION_JSON));

            mockKapacitor.expect(requestTo("/kapacitor/v1/templates/"))
                    .andExpect(content().json("{}"))
                    .andExpect(method(HttpMethod.PATCH))
                    .andRespond(withSuccess(getTestJson("tasks_conflicting_template.json"), MediaType.APPLICATION_JSON));

            Task result = controller.updateTask("my_task", toAdd);

            assertThat(result).isNotNull();
            mockKapacitor.verify();
        }

        @Test
        void changeTopicAndStatus() {
            Task toAdd = Task.builder().topic("blub").status("disabled").build();

            mockKapacitor.expect(requestTo("/kapacitor/v1/tasks/my_task"))
                    .andExpect(method(HttpMethod.PATCH))
                    .andExpect(content().json("{" + "\"status\" : \"disabled\"," + "\"vars\" : { " + "\"topic\" : {\"type\" : \"string\", \"value\" : \"blub\"} " + "}" + "}", true))
                    .andRespond(withSuccess(getTestJson("tasks_conflicting_template.json"), MediaType.APPLICATION_JSON));

            mockKapacitor.expect(requestTo("/kapacitor/v1/templates/"))
                    .andExpect(content().json("{}"))
                    .andExpect(method(HttpMethod.PATCH))
                    .andRespond(withSuccess(getTestJson("tasks_conflicting_template.json"), MediaType.APPLICATION_JSON));

            Task result = controller.updateTask("my_task", toAdd);

            assertThat(result).isNotNull();
            mockKapacitor.verify();
        }
    }

    @Nested
    class RemoveTask {

        @Test
        void deleteHandler() {
            mockKapacitor.expect(requestTo("/kapacitor/v1/tasks/my_task"))
                    .andExpect(method(HttpMethod.DELETE))
                    .andRespond(withSuccess());

            controller.removeTask("my_task");
            mockKapacitor.verify();
        }
    }

}
