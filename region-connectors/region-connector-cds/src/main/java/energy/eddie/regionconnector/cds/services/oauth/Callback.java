package energy.eddie.regionconnector.cds.services.oauth;

import jakarta.annotation.Nullable;

import java.util.Optional;

public record Callback(@Nullable String code, @Nullable String error, String state) {
    public Optional<String> getCode() {
        return Optional.ofNullable(code);
    }
    public Optional<String> getError() {
        return Optional.ofNullable(error);
    }
}
