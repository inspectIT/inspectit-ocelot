package rocks.inspectit.oce.eum.server.configuration.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.eum.server.utils.DefaultTags;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EumTagsSettingsTest {

    @Nested
    public class IsGlobalTagMissing {

        @Test
        public void emptySettings() {
            EumTagsSettings settings = new EumTagsSettings();

            boolean result = settings.isGlobalTagMissing();

            assertThat(result).isFalse();
        }

        @Test
        public void tagMissing() {
            EumTagsSettings settings = new EumTagsSettings();
            settings.getDefineAsGlobal().add("missing-tag");

            boolean result = settings.isGlobalTagMissing();

            assertThat(result).isTrue();
        }

        @Test
        public void hasBeaconTag() {
            EumTagsSettings settings = new EumTagsSettings();
            settings.getDefineAsGlobal().add("beacon-tag");
            settings.getBeacon().put("beacon-tag", "beacon-field");

            boolean result = settings.isGlobalTagMissing();

            assertThat(result).isFalse();
        }

        @Test
        public void hasDefaultTag() {
            EumTagsSettings settings = new EumTagsSettings();
            settings.getDefineAsGlobal().add(DefaultTags.COUNTRY_CODE.toString());

            boolean result = settings.isGlobalTagMissing();

            assertThat(result).isFalse();
        }

        @Test
        public void hasExtraTag() {
            EumTagsSettings settings = new EumTagsSettings();
            settings.getDefineAsGlobal().add("extra-tag");
            settings.getExtra().put("extra-tag", "constant-value");

            boolean result = settings.isGlobalTagMissing();

            assertThat(result).isFalse();
        }
    }

    @Nested
    public class IsCheckUniquenessOfTags {

        @Test
        public void emptySettings() {
            EumTagsSettings settings = new EumTagsSettings();

            boolean result = settings.isCheckUniquenessOfTags();

            assertThat(result).isTrue();
        }

        @Test
        public void noDuplicateTags() {
            EumTagsSettings settings = new EumTagsSettings();
            settings.getExtra().put("tag_a", "");
            settings.getBeacon().put("tag_b", "");

            boolean result = settings.isCheckUniquenessOfTags();

            assertThat(result).isTrue();
        }

        @Test
        public void hasDuplicateTags() {
            EumTagsSettings settings = new EumTagsSettings();
            settings.getExtra().put("tag_a", "");
            settings.getBeacon().put("tag_a", "");

            boolean result = settings.isCheckUniquenessOfTags();

            assertThat(result).isFalse();
        }
    }

    @Nested
    public class IsCheckIPsRangesDoNotOverlap {

        @Test
        public void emptyCustomMapping(){
            EumTagsSettings settings = new EumTagsSettings();

            boolean result = settings.isCheckIpRangesDoNotOverlap();

            assertThat(result).isTrue();
        }

        @Test
        public void ipsAreEqual(){
            EumTagsSettings settings = new EumTagsSettings();
            settings.getCustomIPMapping().put("GER", Arrays.asList(new String[]{"127.127.127.127"}));
            settings.getCustomIPMapping().put("FR", Arrays.asList(new String[]{"127.127.127.127"}));

            boolean result = settings.isCheckIpRangesDoNotOverlap();

            assertThat(result).isFalse();
        }

        @Test
        public void ipsAreNotEqual(){
            EumTagsSettings settings = new EumTagsSettings();
            settings.getCustomIPMapping().put("GER", Arrays.asList(new String[]{"127.127.127.127"}));
            settings.getCustomIPMapping().put("FR", Arrays.asList(new String[]{"127.127.127.128"}));

            boolean result = settings.isCheckIpRangesDoNotOverlap();

            assertThat(result).isTrue();
        }

        @Test
        public void cidrsAreEqual(){
            EumTagsSettings settings = new EumTagsSettings();
            settings.getCustomIPMapping().put("GER", Arrays.asList(new String[]{"10.0.0.0/16"}));
            settings.getCustomIPMapping().put("FR", Arrays.asList(new String[]{"10.0.0.0/16"}));

            boolean result = settings.isCheckIpRangesDoNotOverlap();

            assertThat(result).isFalse();
        }

        @Test
        public void cidrsOverlap(){
            EumTagsSettings settings = new EumTagsSettings();
            settings.getCustomIPMapping().put("GER", Arrays.asList(new String[]{"10.0.0.0/16"}));
            settings.getCustomIPMapping().put("FR", Arrays.asList(new String[]{"10.0.0.0/17"}));

            boolean result = settings.isCheckIpRangesDoNotOverlap();

            assertThat(result).isFalse();
        }

        @Test
        public void cidrsDoNotOverlap(){
            EumTagsSettings settings = new EumTagsSettings();
            settings.getCustomIPMapping().put("GER", Arrays.asList(new String[]{"10.0.0.0/16"}));
            settings.getCustomIPMapping().put("FR", Arrays.asList(new String[]{"10.1.0.0/16"}));

            boolean result = settings.isCheckIpRangesDoNotOverlap();

            assertThat(result).isTrue();
        }

        @Test
        public void cidrContainsIp(){
            EumTagsSettings settings = new EumTagsSettings();
            settings.getCustomIPMapping().put("GER", Arrays.asList(new String[]{"10.0.0.0/16"}));
            settings.getCustomIPMapping().put("FR", Arrays.asList(new String[]{"10.0.0.1"}));

            boolean result = settings.isCheckIpRangesDoNotOverlap();

            assertThat(result).isFalse();
        }

        @Test
        public void cidrDoesNotContainIp(){
            EumTagsSettings settings = new EumTagsSettings();
            settings.getCustomIPMapping().put("GER", Arrays.asList(new String[]{"10.0.0.0/16"}));
            settings.getCustomIPMapping().put("FR", Arrays.asList(new String[]{"11.0.0.1"}));

            boolean result = settings.isCheckIpRangesDoNotOverlap();

            assertThat(result).isTrue();
        }

        @Test
        public void cidrsOfSameLabelAreOverlapping(){
            EumTagsSettings settings = new EumTagsSettings();
            settings.getCustomIPMapping().put("GER", Arrays.asList(new String[]{"10.0.0.0/16", "10.0.0.0/17"}));
            settings.getCustomIPMapping().put("FR", Arrays.asList(new String[]{"11.0.0.1"}));

            boolean result = settings.isCheckIpRangesDoNotOverlap();

            assertThat(result).isTrue();
        }

        @Test
        public void cidrAndIpOfSameLabelAreOverlapping(){
            EumTagsSettings settings = new EumTagsSettings();
            settings.getCustomIPMapping().put("GER", Arrays.asList(new String[]{"10.0.0.0/16", "10.0.0.1"}));
            settings.getCustomIPMapping().put("FR", Arrays.asList(new String[]{"11.0.0.1"}));

            boolean result = settings.isCheckIpRangesDoNotOverlap();

            assertThat(result).isTrue();
        }
    }
}