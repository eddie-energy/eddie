package energy.eddie.regionconnector.nl.mijn.aansluiting.dtos;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PermissionRequestForCreation(
        @NotBlank(message = "must not be blank")
        String connectionId,
        @NotBlank(message = "must not be blank")
        String dataNeedId,
        @NotBlank(message = "must not be blank")
        String verificationCode,
        @Nullable
        @Pattern(regexp = "^\\d{4}[A-Z]{2}$")
        String postalCode
) {

    public PermissionRequestForCreation(String connectionId, String dataNeedId, String verificationCode) {
        this(connectionId, dataNeedId, verificationCode, null);
    }
}
