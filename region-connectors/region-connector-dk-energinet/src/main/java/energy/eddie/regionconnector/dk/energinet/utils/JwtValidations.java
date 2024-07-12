package energy.eddie.regionconnector.dk.energinet.utils;

import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;

public final class JwtValidations {
    private JwtValidations() {}

    /**
     * Validates a JWT based on its expiration date. If the JWT is not at least valid until {@code date} returns false,
     * otherwise true
     *
     * @param jwt  the JWT
     * @param date the minimum end date of the JWT
     * @return if the JWT is valid until {@code date}
     */
    public static boolean isValidUntil(String jwt, LocalDate date) {
        var other = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        try {
            SignedJWT signedJwt = SignedJWT.parse(jwt);
            var expirationDate = signedJwt.getJWTClaimsSet().getExpirationTime().toInstant();
            return !expirationDate.isBefore(other);
        } catch (ParseException e) {
            return false;
        }
    }
}
