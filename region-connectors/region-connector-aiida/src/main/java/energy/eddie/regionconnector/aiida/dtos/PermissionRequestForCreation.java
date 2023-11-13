package energy.eddie.regionconnector.aiida.dtos;

import energy.eddie.regionconnector.aiida.web.validation.StartTimeIsBeforeExpirationTime;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

@StartTimeIsBeforeExpirationTime()
public record PermissionRequestForCreation(@NotBlank(message = "ConnectionId must not be empty")
                                           String connectionId,
                                           @NotBlank(message = "DataNeedId must not be empty")
                                           String dataNeedId,
                                           // StartTimeIsBeforeExpirationTimeValidator checks for null values
                                           Instant startTime,
                                           Instant expirationTime
) {
}
