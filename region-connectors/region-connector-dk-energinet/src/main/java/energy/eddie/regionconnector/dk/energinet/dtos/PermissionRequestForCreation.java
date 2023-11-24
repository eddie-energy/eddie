package energy.eddie.regionconnector.dk.energinet.dtos;

import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;

import java.time.ZonedDateTime;

public record PermissionRequestForCreation(
        String connectionId,
        ZonedDateTime start,
        ZonedDateTime end,
        String refreshToken,
        PeriodResolutionEnum periodResolution,
        String meteringPoint,
        String dataNeedId
) {
}
