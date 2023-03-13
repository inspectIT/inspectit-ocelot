package rocks.inspectit.ocelot.core.exporter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocolSubset;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Test class for {@link rocks.inspectit.ocelot.config.model.exporters.TransportProtocolSubsetValidator}
 */
@ExtendWith(MockitoExtension.class)
public class TransportProtocolSubsetValidatorTest {

    private static final Logger logger = LoggerFactory.getLogger(TransportProtocolSubsetValidatorTest.class);

    private Validator validator;

    @BeforeEach
    void beforeEach() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void testSupportedTransportProtocol() {
        DummyExporterSettings settings = new DummyExporterSettings();
        settings.setProtocol(TransportProtocol.GRPC);
        Set<ConstraintViolation<DummyExporterSettings>> violations = validator.validate(settings);
        assertThat(violations.isEmpty()).isTrue();
    }

    @Test
    void testUnsupportedTransportProtocol() {
        DummyExporterSettings settings = new DummyExporterSettings();
        settings.setProtocol(TransportProtocol.HTTP_PROTOBUF);
        Set<ConstraintViolation<DummyExporterSettings>> violations = validator.validate(settings);
        assertThat(violations.isEmpty()).isFalse();
        assertThat(violations.stream().anyMatch(vio -> vio.getMessage().contains("'protocol'"))).isTrue();
        for (ConstraintViolation<DummyExporterSettings> violation : violations) {
            System.out.println(violation.getMessage());
        }
    }

    private class DummyExporterSettings {

        @TransportProtocolSubset(anyOf = {TransportProtocol.GRPC, TransportProtocol.HTTP_THRIFT})
        private TransportProtocol protocol;

        public void setProtocol(TransportProtocol protocol) {
            this.protocol = protocol;
        }
    }

}


