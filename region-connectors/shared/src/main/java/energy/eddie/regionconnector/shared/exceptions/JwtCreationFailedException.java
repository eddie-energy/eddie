package energy.eddie.regionconnector.shared.exceptions;

import com.nimbusds.jose.JOSEException;

/**
 * Wrapper class to avoid having to add JOSE dependencies to a module just to be able to catch a {@link JOSEException}.
 */
public class JwtCreationFailedException extends Exception {
    public JwtCreationFailedException(JOSEException cause) {
        super("Failed to create JWT", cause);
    }
}
