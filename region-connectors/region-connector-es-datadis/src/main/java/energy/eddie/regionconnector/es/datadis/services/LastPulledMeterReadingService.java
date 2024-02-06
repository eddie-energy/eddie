package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;

@Service
public class LastPulledMeterReadingService {

    @SuppressWarnings("java:S1118") // sonar complains that it should be a utility class
    public LastPulledMeterReadingService(Flux<IdentifiableMeteringData> meteringDataFlux) {
        meteringDataFlux.subscribe(LastPulledMeterReadingService::updateLastPulledMeterReading);
    }

    private static void updateLastPulledMeterReading(IdentifiableMeteringData identifiableMeteringData) {
        var permissionRequest = identifiableMeteringData.permissionRequest();
        var lastMeterReading = identifiableMeteringData.meteringData().getLast();
        ZonedDateTime meteringDataDate = ZonedDateTime.of(lastMeterReading.date(), lastMeterReading.time(), ZONE_ID_SPAIN);

        permissionRequest.lastPulledMeterReading().ifPresentOrElse(
                lastPulledMeterReading -> {
                    if (meteringDataDate.isAfter(lastPulledMeterReading)) {
                        permissionRequest.setLastPulledMeterReading(meteringDataDate);
                    }
                },
                () -> permissionRequest.setLastPulledMeterReading(meteringDataDate)
        );
    }
}