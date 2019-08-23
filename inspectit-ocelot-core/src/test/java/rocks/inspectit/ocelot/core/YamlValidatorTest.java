package rocks.inspectit.ocelot.core;


import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"inspectit.config.file-based.frequency=50ms", "inspectit.ICHGEHENICHT.file-based.frequency=50ms"})
public class ValidatorTest extends SpringTestBase {

    @Test
    public void methode(){
        assertThat(true).isTrue();
        System.out.println("HALLOO!!");
    }


}
