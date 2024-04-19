package rocks.inspectit.ocelot.core.metrics.tagGuards;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PersistedTagsReaderWriterTest {
    @TempDir
    File anotherTempDir;
    String tempFileName;
    Map<String, Map<String, Set<String>>> tagValues;

    @BeforeEach
    public void setup() {
        tempFileName = generateTempFilePath();
        tagValues = createTagValues();
    }


    @Test
    void ofWillReturnAValidInstanceIfFilenameIsValid() {

        //GIVEN || WHEN
        PersistedTagsReaderWriter readerWriter = PersistedTagsReaderWriter.of(tempFileName);
        readerWriter.write(tagValues);

        //THEN
        Map<String, Map<String, Set<String>>> loaded = readerWriter.read();

        assertThat(loaded).flatExtracting("measure_1")
                .flatExtracting("tagKey_1")
                .containsExactlyInAnyOrder("value1", "value2", "value3");
    }

    @Test
    void readWillReturnAnEmptyMapIfPathIsEmpty() {

        //GIVEN
        PersistedTagsReaderWriter readerWriter = PersistedTagsReaderWriter.of(StringUtils.EMPTY);

        //WHEN
        final Map<String, Map<String, Set<String>>> result = readerWriter.read();

        //THEN
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void readWillReturnAnEmptyMapIfThereIsNoFileInThePath() {

        //GIVEN
        PersistedTagsReaderWriter readerWriter = PersistedTagsReaderWriter.of("blubb");

        //WHEN
        final Map<String, Map<String, Set<String>>> result = readerWriter.read();

        //THEN
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void writeWillAddTagsIfEverythingIsValid() {

        //GIVEN
        PersistedTagsReaderWriter readerWriter = PersistedTagsReaderWriter.of(tempFileName);

        //WHEN
        readerWriter.write(tagValues);
        final Map<String, Map<String, Set<String>>> result = readerWriter.read();

        //THEN
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    void writeWillReturnAnEmptyMapIfPathIsEmpty() {

        //GIVEN
        PersistedTagsReaderWriter readerWriter = PersistedTagsReaderWriter.of(StringUtils.EMPTY);

        //WHEN
        readerWriter.write(tagValues);
        final Map<String, Map<String, Set<String>>> result = readerWriter.read();

        //THEN
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void writeWillReturnAnEmptyMapIfPathIsWrong() {

        //GIVEN
        PersistedTagsReaderWriter readerWriter = PersistedTagsReaderWriter.of(tempFileName);

        //WHEN
        readerWriter.write(tagValues);
        PersistedTagsReaderWriter readerWriterWrongPath = PersistedTagsReaderWriter.of("blubb");
        final Map<String, Map<String, Set<String>>> result = readerWriterWrongPath.read();

        //THEN
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void wirteWillReturnAnEmptyMapIfTagMapIsNull() {

        //GIVEN
        PersistedTagsReaderWriter readerWriter = PersistedTagsReaderWriter.of(tempFileName);

        //WHEN
        readerWriter.write(null);
        final Map<String, Map<String, Set<String>>> result = readerWriter.read();

        //THEN
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void writeWillReturnAnEmptyIf() {

        //GIVEN
        PersistedTagsReaderWriter readerWriter = PersistedTagsReaderWriter.of(tempFileName);

        //WHEN
        readerWriter.write(new HashMap<>());
        final Map<String, Map<String, Set<String>>> result = readerWriter.read();

        //THEN
        Assertions.assertTrue(result.isEmpty());
    }


    @Test
    public void testReadWriteTagsFromDisk() {
        PersistedTagsReaderWriter readerWriter = PersistedTagsReaderWriter.of(tempFileName);
        readerWriter.write(tagValues);


        Map<String, Map<String, Set<String>>> loaded = readerWriter.read();

        assertThat(loaded).flatExtracting("measure_1")
                .flatExtracting("tagKey_1")
                .containsExactlyInAnyOrder("value1", "value2", "value3");

    }

    private String generateTempFilePath() {
        File inspectTempFile = new File(anotherTempDir, "inspectit.txt");
        return inspectTempFile.getPath();
    }

    private Map<String, Map<String, Set<String>>> createTagValues() {
        Set<String> tagValue = new HashSet<>();
        tagValue.add("value1");
        tagValue.add("value2");
        tagValue.add("value3");

        Map<String, Set<String>> tagKeys2Values = Maps.newHashMap();
        tagKeys2Values.put("tagKey_1", tagValue);

        Map<String, Map<String, Set<String>>> measure2TagKeys = Maps.newHashMap();
        measure2TagKeys.put("measure_1", tagKeys2Values);

        return measure2TagKeys;
    }
}
