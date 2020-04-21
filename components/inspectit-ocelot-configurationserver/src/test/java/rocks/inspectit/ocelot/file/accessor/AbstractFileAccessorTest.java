package rocks.inspectit.ocelot.file.accessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.file.FileInfo;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AbstractFileAccessorTest {

    private AbstractFileAccessor accessor = new AbstractFileAccessor() {
        @Override
        protected Optional<byte[]> readFile(String path) {
            return Optional.empty();
        }

        @Override
        protected List<FileInfo> listFiles(String path) {
            return null;
        }
    };

    @BeforeEach
    public void beforeEach() {

    }

    @Nested
    class X {

        @Test
        public void t() {
            Optional<String> result = accessor.readConfigurationFile("../../file.yml");
        }

    }

}