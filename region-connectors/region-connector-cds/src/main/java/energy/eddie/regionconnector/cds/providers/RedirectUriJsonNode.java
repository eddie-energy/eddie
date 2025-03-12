package energy.eddie.regionconnector.cds.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import jakarta.annotation.Nullable;

import java.util.function.Function;

class RedirectUriJsonNode implements Function<CdsPermissionRequest, JsonNode> {
    private final ObjectMapper objectMapper;

    RedirectUriJsonNode(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @Nullable
    public JsonNode apply(CdsPermissionRequest permissionRequest) {
        var redirectUri = permissionRequest.redirectUri();
        if (permissionRequest.status() != PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR || redirectUri.isEmpty()) {
            return null;
        }
        return objectMapper.createObjectNode()
                           .set("redirectUri", objectMapper.valueToTree(redirectUri.get()));
    }
}
