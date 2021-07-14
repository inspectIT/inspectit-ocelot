package rocks.inspectit.ocelot.config.validation;

import org.eclipse.jgit.transport.URIish;
import rocks.inspectit.ocelot.config.model.RemoteConfigurationsSettings;
import rocks.inspectit.ocelot.config.model.RemoteConfigurationsSettings.AuthenticationType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for {@link RemoteConfigurationsSettings}.
 */
public class RemoteConfigurationsValidator implements ConstraintValidator<RemoteConfigurationsConstraint, RemoteConfigurationsSettings> {

    @Override
    public void initialize(RemoteConfigurationsConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(RemoteConfigurationsSettings value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        // no validation in case it is disabled
        if (!value.isEnabled()) {
            return true;
        }

        boolean isValid = true;
        URIish gitUri = value.getGitRepositoryUriAsUriisch();

        // URI must be valid
        if (gitUri == null) {
            isValid = false;
            context.buildConstraintViolationWithTemplate("The URI of the remote Git must be specified and must be valid!")
                    .addPropertyNode("gitRepositoryUri")
                    .addConstraintViolation();
        }

        if (gitUri != null) {
            String scheme = gitUri.getScheme();
            boolean isSsh = scheme == null || scheme.equals("ssh");

            // ssh with password is not supported
            if (isSsh && value.getAuthenticationType() == AuthenticationType.PASSWORD) {
                isValid = false;
                context.buildConstraintViolationWithTemplate("SSH using password authentication is not supported.")
                        .addPropertyNode("authenticationType")
                        .addConstraintViolation();
            }

            // ppk must be used with a SSH connection
            if (!isSsh && value.getAuthenticationType() == AuthenticationType.PPK) {
                isValid = false;
                context.buildConstraintViolationWithTemplate("Authentication method 'PPK' can only be used with an SSH connection.")
                        .addPropertyNode("authenticationType")
                        .addConstraintViolation();
            }
        }

        return isValid;
    }
}
