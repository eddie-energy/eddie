// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

/**
 * CPRequestResult, the enum represents the ResponseCodes described in the <a href="https://www.ebutilities.at/prozesse/304">CR_REQ_PT</a> process
 *
 * @param messageId The ID of the message.
 * @param result The result type.
 */
public record CPRequestResult(
        String messageId,
        Result result
) {
    public enum Result {
        ACCEPTED,
        METERING_POINT_NOT_FOUND,
        PROCESS_DATE_INVALID,
        NO_DATA_AVAILABLE,
        METERING_POINT_NOT_ASSIGNED,
        UNKNOWN_RESPONSE_CODE_ERROR,
        PONTON_ERROR; // Error in the Ponton system like transmission failure, misconfiguration

        public static Result fromResponseCode(int responseCode) {
            return switch (responseCode) {
                case 55 -> Result.METERING_POINT_NOT_ASSIGNED;
                case 56 -> Result.METERING_POINT_NOT_FOUND;
                case 70 -> Result.ACCEPTED;
                case 82 -> Result.PROCESS_DATE_INVALID;
                case 94 -> Result.NO_DATA_AVAILABLE;
                default -> Result.UNKNOWN_RESPONSE_CODE_ERROR;
            };
        }
    }
}
