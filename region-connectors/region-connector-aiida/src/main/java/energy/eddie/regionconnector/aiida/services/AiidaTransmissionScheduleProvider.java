package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataNeedsService;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82.TransmissionScheduleProvider;
import jakarta.annotation.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public record AiidaTransmissionScheduleProvider(DataNeedsService dataNeedsService)
        implements TransmissionScheduleProvider<AiidaPermissionRequestInterface> {

    @Nullable
    @Override
    public String findTransmissionSchedule(AiidaPermissionRequestInterface pr) {
        return dataNeedsService
                .getDataNeed(pr.dataNeedId())
                .map(DataNeed::transmissionInterval)
                .map(ti -> Duration.of(ti, ChronoUnit.SECONDS).toString())
                .orElse(null);
    }
}
