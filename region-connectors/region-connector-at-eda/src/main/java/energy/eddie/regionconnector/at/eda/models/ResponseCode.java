// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.models;

/**
 * This record represents a response code from EDA in the context of "Consent Management". The documentation for the
 * response codes can be found <a href="https://www.ebutilities.at/responsecodes">here</a>
 *
 * @param responseCode The response code
 * @param message      The message associated with the response code
 */
public record ResponseCode(int responseCode, String message) {


    public ResponseCode(int responseCode) {
        this(responseCode, getMessage(responseCode));
    }

    private static String getMessage(int responseCode) {
        return switch (responseCode) {
            case CmReqOnl.METERING_POINT_NOT_FOUND -> "Metering point not found";
            case CmReqOnl.METERING_POINT_NOT_SUPPLIED -> "Metering point not supplied";
            case CmReqOnl.INVALID_REQUEST -> "Invalid request data";
            case CmRevSP.INVALID_PROCESSDATE -> "Invalid process dates";
            case CmReqOnl.RECEIVED -> "Message received";
            case CmReqOnl.REJECTED -> "Customer rejected the request";
            case CmReqOnl.TIMEOUT -> "Customer did not respond to the request";
            case CmReqOnl.REQUESTED_DATA_NOT_DELIVERABLE -> "Requested data not deliverable";
            case CmReqOnl.ACCEPTED -> "Customer accepted the request";
            case CmRevSP.TERMINATION_SUCCESSFUL -> "Consent successfully withdrawn";
            case CmRevSP.NO_CONSENT_PRESENT -> "No data sharing available";
            case CmReqOnl.CONSENT_ALREADY_EXISTS -> "Consent already exists";
            case CmReqOnl.CONSENT_REQUEST_ID_ALREADY_EXISTS -> "ConsentId already exists";
            case CmRevSP.CONSENT_ID_EXPIRED -> "ConsentId expired";
            case CmRevSP.CONSENT_AND_METERINGPOINT_DO_NOT_MATCH -> "ConsentId and MeteringPointId are not associated";
            default -> "Unknown response code";
        };
    }

    @Override
    public String toString() {
        return message + " (response code " + responseCode + ")";
    }

    public static class CmRevSP {
        public static final int TERMINATION_SUCCESSFUL = 176;
        public static final int NO_CONSENT_PRESENT = 177;
        public static final int CONSENT_ID_EXPIRED = 180;
        public static final int INVALID_PROCESSDATE = 82;
        public static final int CONSENT_AND_METERINGPOINT_DO_NOT_MATCH = 187;

        private CmRevSP() {
        }
    }

    public static class CmReqOnl {
        public static final int RECEIVED = 99;

        public static final int ACCEPTED = 175;
        public static final int TIMEOUT = 173;
        public static final int REJECTED = 172;
        public static final int METERING_POINT_NOT_FOUND = 56;
        public static final int METERING_POINT_NOT_SUPPLIED = 57;
        public static final int INVALID_REQUEST = 76;
        public static final int REQUESTED_DATA_NOT_DELIVERABLE = 174;
        public static final int CONSENT_ALREADY_EXISTS = 178;
        public static final int CONSENT_REQUEST_ID_ALREADY_EXISTS = 179;

        private CmReqOnl() {
        }
    }
}
