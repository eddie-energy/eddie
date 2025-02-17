package energy.eddie.regionconnector.cds.dtos;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequestForCreation(@NotBlank long cdsId, @NotBlank String dataNeedId, @NotBlank String connectionId) {
}
