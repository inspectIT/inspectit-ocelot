package rocks.inspectit.oce.eum.server.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.events.RegisteredTagsEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.*;

@ExtendWith(MockitoExtension.class)
public class MeasuresAndViewsManagerTest {

    private final Set<String> tags = new HashSet<>(Arrays.asList("first", "second"));

    @InjectMocks
    MeasuresAndViewsManager manager = new MeasuresAndViewsManager();

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Captor
    private ArgumentCaptor<RegisteredTagsEvent> eventArgumentCaptor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    EumServerConfiguration configuration;

    @Nested
    class UpdateMetricDefinitions {

        @BeforeEach
        public void setupMocks() {
            when(configuration.getTags().getExtra()).thenReturn(Collections.singletonMap("first", "value"));
        }

        @Test
        void verifyRegisteredTagsEvent() {
            manager.processRegisteredTags(tags);
            Mockito.verify(applicationEventPublisher).publishEvent(eventArgumentCaptor.capture());
            assertThat(eventArgumentCaptor.getValue().getRegisteredTags()).isEqualTo(tags);
            assertThat(manager.getRegisteredExtraTags()).isEqualTo(Collections.singleton("first"));
        }
    }

}
