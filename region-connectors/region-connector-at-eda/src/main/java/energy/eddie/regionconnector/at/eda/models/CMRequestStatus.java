package energy.eddie.regionconnector.at.eda.models;

import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationMessageType;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Optional;

public record CMRequestStatus(
        NotificationMessageType messageType,
        String conversationId,
        List<Integer> statusCodes,
        String message,
        @Nullable String cmRequestId,
        @Nullable String cmConsentId,
        @Nullable String meteringPoint
) {

    public CMRequestStatus(
            NotificationMessageType messageType,
            String conversationId,
            List<Integer> statusCodes,
            @Nullable String cmRequestId,
            @Nullable String cmConsentId,
            @Nullable String meteringPoint
    ) {
        this(messageType,
             conversationId,
             statusCodes,
             statusCodesToMessage(statusCodes),
             cmRequestId,
             cmConsentId,
             meteringPoint);
    }

    private static String statusCodesToMessage(List<Integer> statusCodes) {
        return statusCodes.stream()
                          .map(ResponseCode::new)
                          .map(ResponseCode::toString)
                          .reduce((a, b) -> a + ", " + b)
                          .orElse("No response codes provided.");
    }

    public CMRequestStatus(NotificationMessageType messageType, String conversationId, String message) {
        this(messageType, conversationId, List.of(), message, null, null, null);
    }

    public Optional<String> getCMConsentId() {
        return Optional.ofNullable(cmConsentId);
    }

    public Optional<String> getMeteringPoint() {
        return Optional.ofNullable(meteringPoint);
    }
}
