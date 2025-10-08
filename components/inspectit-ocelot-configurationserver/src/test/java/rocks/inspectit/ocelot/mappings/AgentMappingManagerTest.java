package rocks.inspectit.ocelot.mappings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.events.AgentMappingsSourceBranchChangedEvent;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.file.accessor.git.RevisionAccess;
import rocks.inspectit.ocelot.file.accessor.workingdirectory.WorkingDirectoryAccessor;
import rocks.inspectit.ocelot.file.versioning.Branch;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentMappingManagerTest {

    @InjectMocks
    AgentMappingManager manager;

    @Mock
    AgentMappingSerializer serializer;

    @Mock
    FileManager fileManager;

    @Mock
    WorkingDirectoryAccessor writeAccessor;

    @Mock
    RevisionAccess readAccessor;

    @Mock
    ApplicationEventPublisher publisher;

    @BeforeEach
    void init() throws IOException {
        lenient().doReturn(writeAccessor).when(fileManager).getWorkingDirectory();
        lenient().doReturn(readAccessor).when(fileManager).getWorkspaceRevision();
        when(readAccessor.agentMappingsExist()).thenReturn(true);

        InspectitServerSettings settings = InspectitServerSettings.builder().initialAgentMappingsSourceBranch("workspace").build();
        serializer = Mockito.spy(new AgentMappingSerializer(settings, fileManager, publisher));
        serializer.postConstruct();

        manager = new AgentMappingManager(serializer, fileManager);

        verify(serializer).postConstruct();
        verify(serializer).readAgentMappings(readAccessor);
    }

    @Nested
    class GetAgentMappings {

        @Test
        void successfullyLoadAgentMappings() throws IOException {
            String mappingYaml = "- name: \"my-mapping\"\n" +
                    "  sources:\n" +
                    "  - \"/configs\"\n" +
                    "  attributes:\n" +
                    "    region: \"eu-west\"";
            doReturn(Optional.of(mappingYaml)).when(readAccessor).readAgentMappings();

            // reset agentMappings, to trigger readAgentMappings() from readAccessor
            serializer.writeAgentMappings(null);
            List<AgentMapping> agentMappings = manager.getAgentMappings();

            assertThat(agentMappings).hasSize(1);
            assertThat(agentMappings.get(0))
                    .extracting(AgentMapping::name).isEqualTo("my-mapping");
        }

        @Test
        void agentMappingsAreBroken() {
            List<AgentMapping> agentMappings = manager.getAgentMappings();

            assertThat(agentMappings).isEmpty();
        }
    }

    @Nested
    class SetAgentMappings {

        @Test
        @SuppressWarnings("unchecked")
        void successfullySetMappings() throws IOException {
            AgentMapping mapping = AgentMapping.builder()
                    .name("my-mapping")
                    .attribute("attributeA", "valueA")
                    .source("sourceA")
                    .build();

            manager.setAgentMappings(Collections.singletonList(mapping));

            ArgumentCaptor<String> writtenMapping = ArgumentCaptor.forClass(String.class);
            verify(writeAccessor).writeAgentMappings(writtenMapping.capture());

            assertThat(manager.getAgentMappings()).containsExactly(mapping);
        }

        @Test
        void writingMappingsFileFails() throws IOException {
            AgentMapping mapping = AgentMapping.builder().name("mapping").build();
            doThrow(IOException.class).when(serializer).writeAgentMappings(any());

            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> manager.setAgentMappings(Collections.singletonList(mapping)));

            assertThat(manager.getAgentMappings()).isEmpty();
        }

        @Test
        void setNull() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.setAgentMappings(null))
                    .withMessage("The agent mappings should not be null");

            verifyNoMoreInteractions(serializer);
        }
    }

    @Nested
    class GetAgentMapping {

        @Test
        void getAgentMapping() {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            doReturn(Arrays.asList(mappingA, mappingB)).when(serializer).readCachedAgentMappings();

            Optional<AgentMapping> result = manager.getAgentMapping("second");

            assertThat(result).isNotEmpty();
            assertThat(result).contains(mappingB);
            verifyNoMoreInteractions(writeAccessor);
        }

        @Test
        void noMappingFound() {
            AgentMapping mapping = AgentMapping.builder().name("first").build();

            doReturn(Arrays.asList(mapping)).when(serializer).readCachedAgentMappings();

            Optional<AgentMapping> result = manager.getAgentMapping("not-existing");

            assertThat(result).isEmpty();
            verifyNoMoreInteractions(writeAccessor);
        }

        @Test
        void getNullName() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.getAgentMapping(null))
                    .withMessage("The mapping name should not be empty or null");

            verifyNoMoreInteractions(serializer, writeAccessor);
        }

        @Test
        void getEmptyName() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.getAgentMapping(""))
                    .withMessage("The mapping name should not be empty or null");

            verifyNoMoreInteractions(serializer, writeAccessor);
        }
    }

    @Nested
    class DeleteAgentMapping {

        @Test
        @SuppressWarnings("unchecked")
        void successfullyDeleteMapping() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            doReturn(Arrays.asList(mappingA, mappingB)).when(serializer).readCachedAgentMappings();

            boolean result = manager.deleteAgentMapping("first");

            ArgumentCaptor<List<AgentMapping>> writtenMapping = ArgumentCaptor.forClass(List.class);
            verify(serializer).writeAgentMappings(writtenMapping.capture());

            assertThat(result).isTrue();
            assertThat(writtenMapping.getValue()).containsExactly(mappingB);
            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }

        @Test
        void mappingDoesNotExist() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            doReturn(Arrays.asList(mappingA, mappingB)).when(serializer).readCachedAgentMappings();

            boolean result = manager.deleteAgentMapping("not-existing");

            assertThat(result).isFalse();
            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }

        @Test
        void lastMappingCannotBeDeleted() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("last").build();

            doReturn(Collections.singletonList(mappingA)).when(serializer).readCachedAgentMappings();

            boolean result = manager.deleteAgentMapping("last");

            assertThat(result).isFalse();
            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }

        @Test
        void writingFileFails() throws IOException {
            doThrow(IOException.class).when(serializer).writeAgentMappings(any());
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            doReturn(Arrays.asList(mappingA, mappingB)).when(serializer).readCachedAgentMappings();

            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> manager.deleteAgentMapping("first"));

            verify(serializer, times(1)).writeAgentMappings(any());
            verify(serializer).readCachedAgentMappings();
        }

        @Test
        void deleteNullMapping() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.deleteAgentMapping(null))
                    .withMessage("The mapping name should not be empty or null");

            verifyNoMoreInteractions(serializer);
        }
    }

    @Nested
    class AddAgentMapping {

        @Test
        @SuppressWarnings("unchecked")
        void addingAgentMapping() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();

            doReturn(Collections.emptyList()).when(serializer).readCachedAgentMappings();

            manager.addAgentMapping(mappingA);

            ArgumentCaptor<List<AgentMapping>> writtenMapping = ArgumentCaptor.forClass(List.class);
            verify(serializer).writeAgentMappings(writtenMapping.capture());

            assertThat(writtenMapping.getValue()).containsExactly(mappingA);
            verify(serializer).writeAgentMappings(any());
            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }

        @Test
        void addingAgentMappingToExisting() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            AtomicReference<List<AgentMapping>> mappingsHolder = new AtomicReference<>(Collections.emptyList());
            doAnswer((rq) -> mappingsHolder.get()).when(serializer).readCachedAgentMappings();
            doAnswer((rq) -> {
                mappingsHolder.set(rq.getArgument(0));
                return null;
            }).when(serializer).writeAgentMappings(anyList());

            manager.addAgentMapping(mappingA);
            manager.addAgentMapping(mappingB);

            assertThat(mappingsHolder.get()).containsExactly(mappingB, mappingA);
            verify(serializer, times(2)).writeAgentMappings(any());
            verify(serializer, times(2)).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }

        @Test
        void addNullMapping() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.addAgentMapping(null))
                    .withMessage("The agent mapping should not be null");

            verifyNoMoreInteractions(serializer);
        }

        @Test
        void addingAgentMappingWithNullName() {
            AgentMapping mappingA = AgentMapping.builder().name(null).build();

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.addAgentMapping(mappingA))
                    .withMessage("The agent mapping's name should not be null or empty");

            verifyNoMoreInteractions(serializer);
        }

        @Test
        void addingAgentMappingWithEmptyName() {
            AgentMapping mappingA = AgentMapping.builder().name("").build();

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> manager.addAgentMapping(mappingA))
                    .withMessage("The agent mapping's name should not be null or empty");

            verifyNoMoreInteractions(serializer);
        }

        @Test
        void updateMapping() throws IOException {
            AtomicReference<List<AgentMapping>> mappingsHolder = new AtomicReference<>(Collections.emptyList());
            doAnswer((rq) -> mappingsHolder.get()).when(serializer).readCachedAgentMappings();
            doAnswer((rq) -> {
                mappingsHolder.set(rq.getArgument(0));
                return null;
            }).when(serializer).writeAgentMappings(anyList());

            AgentMapping mappingA = AgentMapping.builder().name("mapping").build();

            manager.addAgentMapping(mappingA);

            assertThat(manager.getAgentMappings()).containsExactly(mappingA);
            AgentMapping storedMapping = manager.getAgentMapping("mapping").get();
            assertThat(storedMapping.sources()).isEmpty();

            mappingA = AgentMapping.builder().name("mapping").source("/newSource").build();
            manager.addAgentMapping(mappingA);

            assertThat(manager.getAgentMappings()).containsExactly(mappingA);
            storedMapping = manager.getAgentMapping("mapping").get();
            assertThat(storedMapping.sources()).contains("/newSource");
        }
    }

    @Nested
    class AddAgentMappingBefore {

        @Test
        @SuppressWarnings("unchecked")
        void successfullyAddMappingBefore() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            doReturn(Arrays.asList(mappingA, mappingB)).when(serializer).readCachedAgentMappings();

            manager.addAgentMappingBefore(mappingC, "second");

            ArgumentCaptor<List<AgentMapping>> writtenMapping = ArgumentCaptor.forClass(List.class);
            verify(serializer).writeAgentMappings(writtenMapping.capture());

            assertThat(writtenMapping.getValue()).containsExactly(mappingA, mappingC, mappingB);
            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }

        @Test
        @SuppressWarnings("unchecked")
        void addBeforeFirst() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            doReturn(Arrays.asList(mappingA, mappingB)).when(serializer).readCachedAgentMappings();

            manager.addAgentMappingBefore(mappingC, "first");

            ArgumentCaptor<List<AgentMapping>> writtenMapping = ArgumentCaptor.forClass(List.class);
            verify(serializer).writeAgentMappings(writtenMapping.capture());
            assertThat(writtenMapping.getValue()).containsExactly(mappingC, mappingA, mappingB);

            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }

        @Test
        @SuppressWarnings("unchecked")
        void addBeforeItself() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            doReturn(Arrays.asList(mappingA, mappingB, mappingC)).when(serializer)
                    .readCachedAgentMappings();

            manager.addAgentMappingBefore(mappingB, "second");

            ArgumentCaptor<List<AgentMapping>> writtenMapping = ArgumentCaptor.forClass(List.class);
            verify(serializer).writeAgentMappings(writtenMapping.capture());
            assertThat(writtenMapping.getValue()).containsExactly(mappingA, mappingB, mappingC);

            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }

        @Test
        @SuppressWarnings("unchecked")
        void moveExistingMapping() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            doReturn(Arrays.asList(mappingA, mappingB, mappingC)).when(serializer)
                    .readCachedAgentMappings();

            manager.addAgentMappingBefore(mappingC, "first");

            ArgumentCaptor<List<AgentMapping>> writtenMapping = ArgumentCaptor.forClass(List.class);
            verify(serializer).writeAgentMappings(writtenMapping.capture());
            assertThat(writtenMapping.getValue()).containsExactly(mappingC, mappingA, mappingB);

            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);

        }

        @Test
        void targetNotExists() {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            doReturn(Collections.singletonList(mappingA)).when(serializer).readCachedAgentMappings();

            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> manager.addAgentMappingBefore(mappingB, "not-existing"))
                    .withMessage("The agent mapping has not been added because the mapping 'not-existing' does not exists, thus, cannot be added before it");

            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }
    }

    @Nested
    class AddAgentMappingAfter {

        @Test
        @SuppressWarnings("unchecked")
        void successfullyAddMappingAfter() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            doReturn(Arrays.asList(mappingA, mappingB)).when(serializer).readCachedAgentMappings();

            manager.addAgentMappingAfter(mappingC, "second");

            ArgumentCaptor<List<AgentMapping>> writtenMapping = ArgumentCaptor.forClass(List.class);
            verify(serializer).writeAgentMappings(writtenMapping.capture());
            assertThat(writtenMapping.getValue()).containsExactly(mappingA, mappingB, mappingC);

            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }

        @Test
        @SuppressWarnings("unchecked")
        void addAfterItself() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            doReturn(Arrays.asList(mappingA, mappingB, mappingC)).when(serializer)
                    .readCachedAgentMappings();

            manager.addAgentMappingAfter(mappingB, "second");

            ArgumentCaptor<List<AgentMapping>> writtenMapping = ArgumentCaptor.forClass(List.class);
            verify(serializer).writeAgentMappings(writtenMapping.capture());
            assertThat(writtenMapping.getValue()).containsExactly(mappingA, mappingB, mappingC);

            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }

        @Test
        @SuppressWarnings("unchecked")
        void moveExistingMapping() throws IOException {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();
            AgentMapping mappingC = AgentMapping.builder().name("third").build();

            doReturn(Arrays.asList(mappingA, mappingB, mappingC)).when(serializer)
                    .readCachedAgentMappings();

            manager.addAgentMappingAfter(mappingC, "first");

            ArgumentCaptor<List<AgentMapping>> writtenMapping = ArgumentCaptor.forClass(List.class);
            verify(serializer).writeAgentMappings(writtenMapping.capture());
            assertThat(writtenMapping.getValue()).containsExactly(mappingA, mappingC, mappingB);

            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }

        @Test
        void targetNotExists() {
            AgentMapping mappingA = AgentMapping.builder().name("first").build();
            AgentMapping mappingB = AgentMapping.builder().name("second").build();

            doReturn(Arrays.asList(mappingA)).when(serializer).readCachedAgentMappings();

            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> manager.addAgentMappingAfter(mappingB, "not-existing"))
                    .withMessage("The agent mapping has not been added because the mapping 'not-existing' does not exists, thus, cannot be added after it");

            verify(serializer).readCachedAgentMappings();
            verifyNoMoreInteractions(serializer);
        }
    }

    @Nested
    class SetAgentMappingsSourceBranch {

        @Test
        void verifySourceBranchHasChanged() {
            when(fileManager.getLiveRevision()).thenReturn(readAccessor);
            when(readAccessor.agentMappingsExist()).thenReturn(true);

            Branch oldBranch = manager.getSourceBranch();
            Branch newBranch = manager.setSourceBranch("LIVE");

            verify(publisher, times(1)).publishEvent(any(AgentMappingsSourceBranchChangedEvent.class));
            assertThat(oldBranch.equals(newBranch)).isFalse();
        }

        @Test
        void verifyThrowsExceptions()  {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> manager.setSourceBranch(null))
                    .withMessage("The set source branch cannot be null");
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> manager.setSourceBranch("unknown"))
                    .withMessage("Unhandled branch: unknown");
        }
    }
}
