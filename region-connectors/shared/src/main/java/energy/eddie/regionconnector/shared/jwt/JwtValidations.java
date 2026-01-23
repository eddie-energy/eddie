// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.jwt;

import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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
        var zonedDateTime = date.atStartOfDay(ZoneId.systemDefault());
        return isValidUntil(jwt, zonedDateTime);
    }

    /**
     * Validates a JWT based on its expiration date. If the JWT is not at least valid until {@code zonedDateTime} returns false,
     * otherwise true
     *
     * @param jwt           the JWT
     * @param zonedDateTime the minimum end datetime of the JWT
     * @return if the JWT is valid until {@code zonedDateTime}
     */
    public static boolean isValidUntil(String jwt, ZonedDateTime zonedDateTime) {
        var instant = zonedDateTime.toInstant();
        try {
            SignedJWT signedJwt = SignedJWT.parse(jwt);
            var expirationDate = signedJwt.getJWTClaimsSet().getExpirationTime().toInstant();
            return !expirationDate.isBefore(instant);
        } catch (ParseException e) {
            return false;
        }
    }
}
