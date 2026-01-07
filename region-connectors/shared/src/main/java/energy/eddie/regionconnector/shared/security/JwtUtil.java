package energy.eddie.regionconnector.shared.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.mint.DefaultJWSMinter;
import com.nimbusds.jose.mint.JWSMinter;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class JwtUtil {
    public static final String JWT_PERMISSIONS_CLAIM = "permissions";
    public static final JWSAlgorithm JWS_ALGORITHM = JWSAlgorithm.HS256;
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtil.class);
    private final JWSMinter<SecurityContext> minter;
    private final JWTProcessor<SecurityContext> jwtProcessor;
    private final int timeoutDuration;

    /**
     * @param jwtHmacSecret Base64 encoded secret that should be used as secret key for calculating and validating the
     *                      HMAC signatures.
     */
    public JwtUtil(String jwtHmacSecret, int timeoutDuration) {
        if (timeoutDuration <= 0) {
            throw new IllegalArgumentException("timeoutDuration must be greater than 0");
        }
        byte[] secretBytes = Base64.getDecoder().decode(jwtHmacSecret);

        if (secretBytes.length != 32)
            throw new IllegalArgumentException(
                    "Secret for signing JWTs with HS256 algorithm needs to be exactly 32 bytes long");

        ImmutableSecret<SecurityContext> immutableSecret = new ImmutableSecret<>(secretBytes);
        DefaultJWSMinter<SecurityContext> defaultJWSMinter = new DefaultJWSMinter<>();
        defaultJWSMinter.setJWKSource(immutableSecret);

        var keySelector = new JWSVerificationKeySelector<>(JWS_ALGORITHM, immutableSecret);
        var defaultProcessor = new DefaultJWTProcessor<>();
        defaultProcessor.setJWSKeySelector(keySelector);

        this.minter = defaultJWSMinter;
        this.jwtProcessor = defaultProcessor;
        this.timeoutDuration = timeoutDuration;
    }

    /**
     * Returns a map of the permissions contained in the supplied JWT grouped by region connector ID.
     *
     * @return Map of the permissions grouped by region connector ID. It is never null, but may be empty.
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<String>> getPermissions(@Nullable String jwtString) {
        try {
            if (jwtString != null) {
                JWTClaimsSet existingJwt = jwtProcessor.process(jwtString, null);
                var claims = (Map<String, List<String>>) existingJwt.getClaim(JWT_PERMISSIONS_CLAIM);
                return claims != null ? claims : Collections.emptyMap();
            }
        } catch (Exception exception) {
            // e.g. JWT is not valid, or does not contain the expected values, etc...
            LOGGER.trace("Failed to get existing permissions from JWT {}", jwtString, exception);
        }
        return Collections.emptyMap();
    }

    /**
     * Creates a new signed JWT and adds the supplied {@code permissionId} to the map of permitted permissions. The
     * returned JWT is to be included by the region connector element to make requests that update the permission.
     *
     * @param regionConnectorId ID of the region connector that created the new permission.
     * @param permissionId      ID of the newly created permission.
     * @return Serialized JWT.
     * @throws JwtCreationFailedException If for any reason, the creation of the JWT failed.
     */
    public String createJwt(String regionConnectorId, String permissionId) throws JwtCreationFailedException {
        var permissions = Map.of(regionConnectorId, List.of(permissionId));

        JWSHeader header = new JWSHeader.Builder(JWS_ALGORITHM)
                .type(JOSEObjectType.JWT)
                .build();

        var expirationTime = Instant.now(Clock.systemUTC()).plus(timeoutDuration, ChronoUnit.HOURS);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issueTime(Date.from(Instant.now(Clock.systemUTC())))
                .expirationTime(Date.from(expirationTime))
                .claim(JWT_PERMISSIONS_CLAIM, permissions)
                .build();

        try {
            return minter.mint(header, claimsSet.toPayload(), null).serialize();
        } catch (JOSEException e) {
            throw new JwtCreationFailedException(e);
        }
    }
}
