package energy.eddie.regionconnector.aiida.services;

import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public record AiidaTransmissionScheduleProvider(DataNeedsService dataNeedsService)
        implements TransmissionScheduleProvider<AiidaPermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaTransmissionScheduleProvider.class);

    @Nullable
    @Override
    public String findTransmissionSchedule(AiidaPermissionRequest pr) {
        return dataNeedsService
                .findById(pr.dataNeedId())
                .map(dataNeed -> {
                    if (dataNeed instanceof AiidaDataNeed aiidaDataNeed)
                        return Duration.ofSeconds(aiidaDataNeed.transmissionInterval()).toString();

                    LOGGER.warn(
                            "Finding transmission schedule for non-AIIDA data need with ID {} is not possible. Permission ID was {}",
                            dataNeed.id(),
                            pr.permissionId());
                    return null;
                })
                .orElse(null);
    }
}
