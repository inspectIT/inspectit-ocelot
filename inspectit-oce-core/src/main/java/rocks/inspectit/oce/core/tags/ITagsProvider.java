package rocks.inspectit.oce.core.tags;

import lombok.Getter;

import java.util.Map;

public interface ITagsProvider {

    /**
     * Returns priority of this tag providers, higher int values mean higher priority.
     * <p>
     * For convenience implementations can use {@link Priority#getValue()} to position them self in 3 main priorities (LOW, MEDIUM, HIGH), but are free to specify any int negative or positive value.
     *
     * @return Priority of this tag providers
     */
    int getPriority();

    /**
     * Get tags provided by this metrics providers.
     *
     * @return Get tags provided by this metrics providers.
     */
    Map<String, String> getTags();

    /**
     * Priority helper.
     */
    enum Priority {

        LOW(-100),

        MEDIUM(0),

        HIGH(100);

        @Getter
        private final int value;

        Priority(int value) {
            this.value = value;
        }
    }

}
