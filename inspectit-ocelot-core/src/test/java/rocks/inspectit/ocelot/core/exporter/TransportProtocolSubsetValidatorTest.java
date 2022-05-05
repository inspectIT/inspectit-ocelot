package rocks.inspectit.ocelot.config.validation;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocolSubset;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocolSubsetValidator;

import javax.validation.ConstraintValidator;
import javax.validation.Validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Test class for {@link rocks.inspectit.ocelot.config.model.exporters.TransportProtocolSubsetValidator}
 */
@ExtendWith(MockitoExtension.class)
public class TransportProtocolSubsetValidatorTest {

    private static ConstraintValidator validator;

    private static final Logger logger = LoggerFactory.getLogger(TransportProtocolSubsetValidatorTest.class);

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    HibernateConstraintValidatorContext ctx;

    @BeforeEach
    void initMock() {
        doReturn(ctx).when(ctx).unwrap(any());

    }

    @BeforeAll
    private static void beforeAll() {
        // construct validator
        validator = new TransportProtocolSubsetValidator();

        Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void testSupportedTransportProtocol() {
        DummyExporterSettings settings = new DummyExporterSettings();
        settings.setProtocol(TransportProtocol.GRPC);
        assertThat(validator.isValid(new Object() {

            @TransportProtocolSubset(anyOf = {TransportProtocol.GRPC})
            private TransportProtocol transportProtocol = TransportProtocol.HTTP_PROTOBUF;
        }, ctx)).isTrue();
    }

    private class DummyExporterSettings {

        @TransportProtocolSubset(anyOf = {TransportProtocol.GRPC})
        private TransportProtocol protocol;

        public void setProtocol(TransportProtocol protocol) {
            this.protocol = protocol;
        }

        public TransportProtocol getProtocol() {
            return protocol;
        }
    }

}


