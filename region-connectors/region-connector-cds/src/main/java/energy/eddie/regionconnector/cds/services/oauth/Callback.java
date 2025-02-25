package energy.eddie.regionconnector.cds.services.oauth;

import jakarta.annotation.Nullable;

public record Callback(@Nullable String code, @Nullable String error, String state) {
}
