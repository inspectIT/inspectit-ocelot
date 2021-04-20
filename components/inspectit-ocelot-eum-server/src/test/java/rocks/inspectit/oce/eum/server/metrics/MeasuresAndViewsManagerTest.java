package rocks.inspectit.oce.eum.server.metrics;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.events.RegisteredTagsEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

import java.util.*;

@ExtendWith(MockitoExtension.class)
public class MeasuresAndViewsManagerTest {

    @InjectMocks
    private MeasuresAndViewsManager manager = new MeasuresAndViewsManager();

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Captor
    private ArgumentCaptor<RegisteredTagsEvent> eventArgumentCaptor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EumServerConfiguration configuration;

    @Nested
    class ProcessRegisteredTags {

        @Test
        void registerNoTag() {
            when(configuration.getTags().getExtra()).thenReturn(Collections.emptyMap());

            manager.processRegisteredTags(Collections.emptySet());

            verify(applicationEventPublisher).publishEvent(eventArgumentCaptor.capture());
            assertThat(eventArgumentCaptor.getValue().getRegisteredTags()).isEmpty();
            assertThat(manager.registeredExtraTags).isEmpty();
        }

        @Test
        void registerSingleTag() {
            when(configuration.getTags().getExtra()).thenReturn(Collections.singletonMap("first", "value"));

            manager.processRegisteredTags(Collections.singleton("first"));

            verify(applicationEventPublisher).publishEvent(eventArgumentCaptor.capture());
            assertThat(eventArgumentCaptor.getValue().getRegisteredTags()).containsExactly("first");
            assertThat(manager.registeredExtraTags).containsExactly("first");
        }

        @Test
        void registerMultipleTags() {
            Map<String, String> tagMap = ImmutableMap.of("first", "value", "second", "value");
            when(configuration.getTags().getExtra()).thenReturn(tagMap);

            manager.processRegisteredTags(Sets.newHashSet("first", "second"));

            verify(applicationEventPublisher).publishEvent(eventArgumentCaptor.capture());
            assertThat(eventArgumentCaptor.getValue().getRegisteredTags()).containsExactlyInAnyOrder("first", "second");
            assertThat(manager.registeredExtraTags).containsExactlyInAnyOrder("first", "second");
        }

        @Test
        void registerTagsMultipleTimes() {
            Map<String, String> tagMap = ImmutableMap.of("first", "value", "second", "value");
            when(configuration.getTags().getExtra()).thenReturn(tagMap);

            // first execution
            manager.processRegisteredTags(Collections.singleton("first"));

            assertThat(manager.registeredExtraTags).containsExactly("first");

            // second execution
            manager.processRegisteredTags(Collections.singleton("second"));

            assertThat(manager.registeredExtraTags).containsExactlyInAnyOrder("first", "second");
            verify(applicationEventPublisher, times(2)).publishEvent(eventArgumentCaptor.capture());
            RegisteredTagsEvent eventOne = eventArgumentCaptor.getAllValues().get(0);
            RegisteredTagsEvent eventTwo = eventArgumentCaptor.getAllValues().get(1);
            assertThat(eventOne.getRegisteredTags()).containsExactlyInAnyOrder("first");
            assertThat(eventTwo.getRegisteredTags()).containsExactlyInAnyOrder("first", "second");
        }
    }

}
