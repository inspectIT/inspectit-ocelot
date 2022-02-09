package inspectit.ocelot.config.doc.generator.docobjects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public abstract class BaseDoc {

    private final String name;
    private final String description;

}
