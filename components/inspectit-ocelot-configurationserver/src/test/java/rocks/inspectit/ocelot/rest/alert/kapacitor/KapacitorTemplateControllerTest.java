package rocks.inspectit.ocelot.rest.alert.kapacitor;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.Template;
import rocks.inspectit.ocelot.rest.alert.kapacitor.model.TemplateVariable;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class KapacitorTemplateControllerTest extends KapacitorControllerTestBase {

    private KapacitorTemplateController controller;

    @BeforeEach
    void setup() {
        controller = super.createWithMock(KapacitorTemplateController::new);
    }

    @Nested
    class GetAllTemplates {

        @Test
        void templatesListedCorrectly() {
            mockKapacitor.expect(requestTo("/kapacitor/v1/templates"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(getTestJson("templates.json"), MediaType.APPLICATION_JSON));

            List<Template> result = controller.getAllTemplates();

            assertThat(result).containsExactlyInAnyOrder(
                    Template.builder()
                            .id("baseline")
                            .created("2020-07-07T08:41:38.64538964Z")
                            .modified("2020-07-07T08:41:38.64538964Z")
                            .build(),
                    Template.builder()
                            .id("windowed_baseline")
                            .created("2020-07-07T08:41:38.725505818Z")
                            .modified("2020-07-07T08:41:38.725505818Z")
                            .build()
            );
            mockKapacitor.verify();
        }
    }

    @Nested
    class GetTemplate {

        @Test
        void templateWithDescriptionAndTopic() {
            mockKapacitor.expect(requestTo("/kapacitor/v1/templates/baseline"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(getTestJson("templates_baseline.json"), MediaType.APPLICATION_JSON));

            Template result = controller.getTemplate("baseline");

            assertThat(result).isEqualTo(
                    Template.builder()
                            .id("baseline")
                            .created("2020-07-07T08:41:38.64538964Z")
                            .modified("2020-07-07T08:41:38.64538964Z")
                            .description("Bli Bla Blub description")
                            .hasTopicVariable(true)
                            .vars(ImmutableList.<TemplateVariable>builder()
                                    .add(TemplateVariable.builder()
                                            .name("database")
                                            .description("")
                                            .type("string")
                                            .value("test")
                                            .build()
                                    )
                                    .add(TemplateVariable.builder()
                                            .name("inputMeasurement")
                                            .description("The input measurement for which the baseline should be computed")
                                            .type("string")
                                            .build()
                                    )
                                    .add(TemplateVariable.builder()
                                            .name("interval")
                                            .description("")
                                            .type("duration")
                                            .value("15m")
                                            .build()
                                    )
                                    .add(TemplateVariable.builder()
                                            .name("float_val")
                                            .description("")
                                            .type("float")
                                            .value(-42.42)
                                            .build()
                                    )
                                    .add(TemplateVariable.builder()
                                            .name("retention")
                                            .description("")
                                            .type("string")
                                            .build()
                                    )
                                    .build())
                            .build()
            );
            mockKapacitor.verify();
        }

        @Test
        void templateWithoutDescription() {
            mockKapacitor.expect(requestTo("/kapacitor/v1/templates/windowed_baseline"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(getTestJson("templates_windowed_baseline.json"), MediaType.APPLICATION_JSON));

            Template result = controller.getTemplate("windowed_baseline");

            assertThat(result).isEqualTo(
                    Template.builder()
                            .id("windowed_baseline")
                            .created("2020-07-07T08:41:38.725505818Z")
                            .modified("2020-07-07T08:41:38.725505818Z")
                            .description("")
                            .hasTopicVariable(false)
                            .vars(Collections.emptyList())
                            .build()
            );
            mockKapacitor.verify();
        }
    }

}
