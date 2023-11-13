package energy.eddie.regionconnector.aiida.dtos;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record PermissionRequestForCreation(@NotBlank(message = "ConnectionId must not be empty")
                                           String connectionId,
                                           @NotBlank(message = "DataNeedId must not be empty")
                                           String dataNeedId,
                                           Instant startTime,
                                           Instant expirationTime
) {
}
