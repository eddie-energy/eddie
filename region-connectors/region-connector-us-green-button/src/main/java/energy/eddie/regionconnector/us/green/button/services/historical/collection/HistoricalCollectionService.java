package energy.eddie.regionconnector.us.green.button.services.historical.collection;

import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.api.Pages;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.HistoricalCollectionResponse;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Meter;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReadingPk;
import energy.eddie.regionconnector.us.green.button.persistence.MeterReadingRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class HistoricalCollectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoricalCollectionService.class);
    private final GreenButtonApi api;
    private final DataNeedMatcher dataNeedMatcher;
    private final MeterReadingRepository meterReadingRepository;

    public HistoricalCollectionService(
            GreenButtonApi api,
            DataNeedMatcher dataNeedMatcher,
            MeterReadingRepository meterReadingRepository
    ) {
        this.api = api;
        this.dataNeedMatcher = dataNeedMatcher;
        this.meterReadingRepository = meterReadingRepository;
    }

    /**
     * Persists meters for permission request if it matches the {@link DataNeedMatcher} criteria.
     *
     * @param permissionRequest the permission request that is used to request meters
     * @return Empty mono
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Flux<MeterReading> persistMetersForPermissionRequest(UsGreenButtonPermissionRequest permissionRequest) {
        return api.fetchMeters(Pages.NO_SLURP, List.of(permissionRequest.authorizationUid()), permissionRequest.dataSourceInformation().meteredDataAdministratorId())
                  .map(dataNeedMatcher::filterMetersNotMeetingDataNeedCriteria)
                  .flatMap(Flux::fromIterable)
                  .mapNotNull(meter -> persistMeter(meter, permissionRequest.permissionId()));
    }

    /**
     * Activates historical collection for all passed MeterReadings.
     * If historical collection could not be triggered for a meter, it is removed from the permission request.
     *
     * @param meters that should be activated for historical collection
     * @param permissionRequest permission request containing the meters
     */
    public Mono<Void> triggerHistoricalDataCollection(List<MeterReading> meters, UsGreenButtonPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        var meterUids = meters.stream().map(MeterReading::meterUid).toList();
        return api.collectHistoricalData(meterUids, permissionRequest.dataSourceInformation().meteredDataAdministratorId())
                  .map(HistoricalCollectionResponse::meters)
                  .doOnSuccess(activatedMeters -> removeNotActivatedMeters(activatedMeters,
                                                                           meterUids,
                                                                           permissionId))
                  .then();
    }

    /**
     * Activates historical collection for all passed MeterReadings.
     * If historical collection could not be triggered for a meter, it is removed from the permission request.
     *
     * @param permissionRequest contains the meters that should be activated for historical collection
     */
    public Mono<Void> triggerHistoricalDataCollection(UsGreenButtonPermissionRequest permissionRequest) {
        return triggerHistoricalDataCollection(permissionRequest.lastMeterReadings(), permissionRequest);
    }


    private void removeNotActivatedMeters(
            List<String> activatedMeters,
            List<String> meterUids,
            String permissionId
    ) {
        var notActivated = new ArrayList<>(meterUids);
        notActivated.removeAll(activatedMeters);
        var notActivatedPks = notActivated.stream()
                                          .map(meterUid -> new MeterReadingPk(
                                                  permissionId,
                                                  meterUid
                                          ))
                                          .toList();
        meterReadingRepository.deleteAllById(notActivatedPks);
    }

    private MeterReading persistMeter(Meter meter, String permissionId) {
        var meterUid = meter.uid();
        LOGGER.info("Adding meter {} to permission request {}", meterUid, permissionId);
        var meterReading = meterReadingRepository.findById(new MeterReadingPk(permissionId, meterUid));
        if (meterReading.isPresent()) {
            LOGGER.info("Meter {} already exists for permission request {}", meterUid, permissionId);
            return meterReading.get();
        }
        return meterReadingRepository.save(new MeterReading(permissionId,
                                                            meterUid,
                                                            null,
                                                            PollingStatus.DATA_NOT_READY));
    }
}
