package rocks.inspectit.ocelot.config.model.exporters.tags;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@NoArgsConstructor
public class TagsExporterSettings {

    @Valid
    private HttpExporterSettings http;
}
