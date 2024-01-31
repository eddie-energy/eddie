package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.state.InvalidState;
import energy.eddie.regionconnector.es.datadis.permission.request.state.MalformedState;
import energy.eddie.regionconnector.es.datadis.permission.request.state.UnableToSendState;
import jakarta.annotation.Nullable;

public record DatadisPermissionRequestStatusMessageResolver(PermissionRequestState state) {
    public @Nullable String getStatusMessage() {
        return switch (state) {
            case InvalidState invalidState -> invalidState.reason().getMessage();
            case MalformedState malformedState -> malformedState.reason().getMessage();
            case UnableToSendState unableToSendState -> unableToSendState.reason().getMessage();
            default -> null;
        };
    }
}