// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.models;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This record represents a response code from EDA in the context of "Consent Management". The documentation for the
 * response codes can be found <a href="https://www.ebutilities.at/responsecodes">here</a>
 *
 * @param responseCode The response code
 */
public record ResponseCode(int responseCode) {
    private static final Map<Integer, String> LOOKUP =
            Arrays.stream(KnownResponseCodes.values())
                  .collect(Collectors.toUnmodifiableMap(
                          KnownResponseCodes::getCode,
                          KnownResponseCodes::getMessage
                  ));

    @Override
    @NonNull
    public String toString() {
        return getMessage() + " (response code " + responseCode + ")";
    }

    private String getMessage() {
        return LOOKUP.getOrDefault(responseCode, "Unknown response code");
    }

    /**
     * This enumeration contains all well known response codes with description that might be returned by a DSO in Austria.
     *
     * @see <a href="https://www.ebutilities.at/responsecodes">https://www.ebutilities.at/responsecodes</a>
     */
    public enum KnownResponseCodes {
        METERING_POINT_NOT_FOUND(56, "Metering point not found"),
        METERING_POINT_NOT_SUPPLIED(57, "Metering point not supplied"),
        INVALID_REQUEST(76, "Invalid request data"),
        RECEIVED(99, "Message received"),
        INVALID_PROCESSDATE(82, "Invalid process dates"),
        COMPETING_PROCESSES(86, "Competing processes ongoing"),
        INVALID_ENERGY_DIRECTION(104, "Invalid energy direction"),
        ZP_ALREADY_ASSIGNED(156, "Metering point already assigned"),
        METERING_POINT_NOT_ELIGIBLE_TO_PARTICIPATE(158, "Metering point not eligible to participate"),
        METERING_POINT_INACTIVE(159, "Metering point inactive/not installed at process date"),
        DISTRIBUTION_MODEL_NOT_CORRESPONDING_TO_AGREEMENT(160,
                                                          "Distribution model does not correspond to the agreement"),
        REJECTED(172, "Customer rejected the request"),
        TIMEOUT(173, "Customer did not respond to the request"),
        REQUESTED_DATA_NOT_DELIVERABLE(174, "Requested data not deliverable"),
        ACCEPTED(175, "Customer accepted the request"),
        TERMINATION_SUCCESSFUL(176, "Consent successfully withdrawn"),
        NO_CONSENT_PRESENT(177, "No data sharing available"),
        CONSENT_ALREADY_EXISTS(178, "Consent already exists"),
        CONSENT_REQUEST_ID_ALREADY_EXISTS(179, "ConsentId already exists"),
        CONSENT_ID_EXPIRED(180, "ConsentId expired"),
        COMMUNITY_ID_NOT_FOUND(181, "Community ID not found"),
        CUSTOMER_HAS_OPTED(184, "Customer has opted"),
        METERING_POINT_NOT_IN_AREA_OF_ENERGY_COMMUNITY(185, "Metering point not in area of the energy community"),
        CONSENT_AND_METERINGPOINT_DO_NOT_MATCH(187, "ConsentId and MeteringPointId are not associated"),
        PARTICIPATION_FACTOR_EXCEEDED(188, "Participation rate of 100% would be exceeded"),
        PARTICIPATION_LIMIT_EXCEEDED(196, "Participation limit exceeded"),
        UNSTABLE_COMMUNICATION(204, "No sufficiently stable communication available for metering point"),
        ;

        private final int code;
        private final String message;

        KnownResponseCodes(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public static ResponseCode.@Nullable KnownResponseCodes fromCode(int code) {
            for (KnownResponseCodes value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
            return null;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
