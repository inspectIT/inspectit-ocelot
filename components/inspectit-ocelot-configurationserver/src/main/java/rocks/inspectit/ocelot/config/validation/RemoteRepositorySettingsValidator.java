package rocks.inspectit.ocelot.config.validation;

import rocks.inspectit.ocelot.config.model.RemoteRepositorySettings;
import rocks.inspectit.ocelot.config.model.RemoteRepositorySettings.AuthenticationType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for {@link RemoteRepositorySettings}.
 */
public class RemoteRepositorySettingsValidator implements ConstraintValidator<RemoteRepositorySettingsConstraint, RemoteRepositorySettings> {

    @Override
    public void initialize(RemoteRepositorySettingsConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(RemoteRepositorySettings settings, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        boolean isValid = true;

        String scheme = settings.getGitRepositoryUri().getScheme();
        boolean isSsh = scheme == null || scheme.equals("ssh");

        // ssh with password is not supported
        if (isSsh && settings.getAuthenticationType() == AuthenticationType.PASSWORD) {
            isValid = false;
            context.buildConstraintViolationWithTemplate("SSH using password authentication is not supported.")
                    .addPropertyNode("authenticationType")
                    .addConstraintViolation();
        }

        // ppk must be used with a SSH connection
        if (!isSsh && settings.getAuthenticationType() == AuthenticationType.PPK) {
            isValid = false;
            context.buildConstraintViolationWithTemplate("Authentication method 'PPK' can only be used with an SSH connection.")
                    .addPropertyNode("authenticationType")
                    .addConstraintViolation();
        }

        return isValid;
    }
}
