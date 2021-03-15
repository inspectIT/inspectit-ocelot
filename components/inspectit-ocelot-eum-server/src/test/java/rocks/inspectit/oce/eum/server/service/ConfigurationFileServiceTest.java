package rocks.inspectit.oce.eum.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConfigurationFileServiceTest {

    private ConfigurationFileService configurationFileService = null;

    @BeforeEach
    public void setup() {
        configurationFileService = new ConfigurationFileService();
    }

    @Nested
    public class Init {

        @Test
        public void locationSet() throws IOException {
            ConfigurationFileService configurationFileServiceSpy = Mockito.spy(configurationFileService);
            System.setProperty("spring.config.location", "file:my/test/path");

            configurationFileServiceSpy.init();

            assertThat(configurationFileServiceSpy.filePath).isEqualTo("my/test/path");
        }

        @Test
        public void noLocationSetNoFilePresent() throws IOException {
            ConfigurationFileService configurationFileServiceSpy = Mockito.spy(configurationFileService);
            File mockFile = mock(File.class);
            when(mockFile.createNewFile()).thenReturn(true);
            doReturn(mockFile).when(configurationFileServiceSpy).getFileObject();
            doReturn("test-config").when(configurationFileServiceSpy).getDefaultConfig();
            doNothing().when(configurationFileServiceSpy).saveFile(any());

            configurationFileServiceSpy.init();

            assertThat(configurationFileServiceSpy.filePath).isEqualTo(ConfigurationFileService.DEFAULT_FILE_PATH);
            verify(configurationFileServiceSpy).saveFile("test-config");
            verify(configurationFileServiceSpy).getFileObject();
            verify(configurationFileServiceSpy).init();
            verifyNoMoreInteractions(configurationFileServiceSpy);
        }

        @Test
        public void noLocationSetFilePresent() throws IOException {
            ConfigurationFileService configurationFileServiceSpy = Mockito.spy(configurationFileService);
            File mockFile = mock(File.class);
            when(mockFile.createNewFile()).thenReturn(false);
            doReturn(mockFile).when(configurationFileServiceSpy).getFileObject();

            configurationFileServiceSpy.init();

            assertThat(configurationFileServiceSpy.filePath).isEqualTo(ConfigurationFileService.DEFAULT_FILE_PATH);
            verify(configurationFileServiceSpy).getFileObject();
            verify(configurationFileServiceSpy).init();
            verifyNoMoreInteractions(configurationFileServiceSpy);
        }

    }

    @Nested
    public class GetDefaultConfig {

        @Test
        public void returnsFile() throws IOException {
            ConfigurationFileService configurationFileServiceSpy = Mockito.spy(configurationFileService);
            BufferedReader mockReader = mock(BufferedReader.class);
            when(mockReader.readLine()).thenReturn("line1").thenReturn("line2").thenReturn(null);
            when(configurationFileServiceSpy.getDefaultFileReader()).thenReturn(mockReader);

            String testConfig = configurationFileServiceSpy.getDefaultConfig();

            verify(mockReader).close();
            assertThat(testConfig).isEqualTo("line1\nline2\n");
        }

        @Test
        public void closesAfterException() throws IOException {
            ConfigurationFileService configurationFileServiceSpy = Mockito.spy(configurationFileService);
            BufferedReader mockReader = mock(BufferedReader.class);
            when(mockReader.readLine()).thenThrow(new FileNotFoundException("Something went wrong!"));
            when(configurationFileServiceSpy.getDefaultFileReader()).thenReturn(mockReader);

            String testConfig = configurationFileServiceSpy.getDefaultConfig();

            verify(mockReader).close();
        }
    }

}
