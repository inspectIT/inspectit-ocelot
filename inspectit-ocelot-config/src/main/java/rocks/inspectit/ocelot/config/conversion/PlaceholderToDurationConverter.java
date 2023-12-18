package rocks.inspectit.ocelot.config.conversion;

import org.springframework.boot.convert.DurationStyle;
import org.springframework.core.convert.converter.Converter;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This converter should prevent a ConversionFailedException, if the 'ConfigParser' tries to convert a YAML String containing
 * a placeholder to a property, which does not exist in the same YAML String.
 * Normally, this would not create an exception, because these YAML string would be merged beforehand.
 * However, if each YAML String has to be parsed individually, a ConversionFailedException will be thrown, because the
 * placeholder cannot be resolved, if the referenced value exists in another YAML.
 * <p>
 * This converter converts placeholders to a dummy value to prevent an exception.
 */
public class PlaceholderToDurationConverter implements Converter<String, Duration> {

    private final Pattern placeholderPattern = Pattern.compile("\\$\\{([^}]+)}");
    private final Duration DUMMY_DURATION = Duration.ofHours(1);

    @Override
    public Duration convert(String source) {
        Matcher matcher = placeholderPattern.matcher(source);

        if (matcher.find()) return DUMMY_DURATION;
        else return DurationStyle.detectAndParse(source);
    }
}
