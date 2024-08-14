package energy.eddie.regionconnector.fi.fingrid.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EventReason(
        @JsonProperty("EventReasonCode") String eventReasonCode,
        @JsonProperty("EventReasonText") String eventReasonText
) {
    /**
     * Reason Code when empty response is returned. Could mean that there is no data in the specified timeframe, that
     * there is no permission for this account, a number of validation errors, or that the requested resolution is too
     * small.
     */
    public static final String EMPTY_RESPONSE_REASON = "RC-MDM-RMV-100";
}
