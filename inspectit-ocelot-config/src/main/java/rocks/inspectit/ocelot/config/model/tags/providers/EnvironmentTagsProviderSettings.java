package rocks.inspectit.ocelot.config.model.tags.providers;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EnvironmentTagsProviderSettings {

    /**
     * If providers is enabled.
     */
    private boolean enabled;

    /**
     * If true tries to resolve the host name using {@link java.net.InetAddress}.
     */
    private boolean resolveHostName;

    /**
     * If true tries to resolve the host arch using {@code System.getProperty("os.arch")}.
     */
    private boolean resolveHostArch;

    /**
     * If true tries to resolve the host ip using {@link java.net.InetAddress}.
     */
    private boolean resolveHostIp;

}
