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
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * Persists meters for permission requests that match the {@link DataNeedMatcher} criteria.
     *
     * @param permissionRequests the permission requests that are used to request meters
     * @return Empty mono
     */
    public Flux<MeterReading> persistMetersForPermissionRequests(List<UsGreenButtonPermissionRequest> permissionRequests) {
        var authIndex = buildAuthIndex(permissionRequests);
        var slurp = permissionRequests.size() > GreenButtonApi.MAX_METER_RESULTS ? Pages.SLURP : Pages.NO_SLURP;
        return api.fetchInactiveMeters(slurp, new ArrayList<>(authIndex.keySet()))
                  .map(dataNeedMatcher::filterMetersNotMeetingDataNeedCriteria)
                  .flatMap(Flux::fromIterable)
                  .mapNotNull(meter -> persistMeter(meter, authIndex));
    }

    /**
     * Activates historical collection for all passed MeterReadings.
     * If historical collection could not be triggered for a meter, it is removed from the permission request.
     *
     * @param meters that should be activated for historical collection.
     */
    public Mono<Void> triggerHistoricalDataCollection(List<MeterReading> meters) {
        var meterUidToPermissionIdReverseIndex = meters.stream()
                                                       .collect(Collectors.toMap(
                                                               MeterReading::meterUid,
                                                               MeterReading::permissionId
                                                       ));
        var meterUids = new ArrayList<>(meterUidToPermissionIdReverseIndex.keySet());
        return api.collectHistoricalData(meterUids)
                  .map(HistoricalCollectionResponse::meters)
                  .doOnSuccess(activatedMeters -> removeNotActivatedMeters(activatedMeters,
                                                                           meterUids,
                                                                           meterUidToPermissionIdReverseIndex))
                  .then();
    }

    @SuppressWarnings("NullAway") // reverse index always contains the permission ID
    private void removeNotActivatedMeters(
            List<String> activatedMeters,
            List<String> meterUids,
            Map<String, String> meterUidToPermissionIdReverseIndex
    ) {
        var notActivated = new ArrayList<>(meterUids);
        notActivated.removeAll(activatedMeters);
        var notActivatedPks = notActivated.stream()
                                          .map(meterUid -> new MeterReadingPk(
                                                  meterUidToPermissionIdReverseIndex.get(meterUid),
                                                  meterUid
                                          ))
                                          .toList();
        meterReadingRepository.deleteAllById(notActivatedPks);
    }


    private static Map<String, String> buildAuthIndex(List<UsGreenButtonPermissionRequest> permissionRequests) {
        return permissionRequests
                .stream()
                .collect(Collectors.toMap(UsGreenButtonPermissionRequest::authorizationUid,
                                          UsGreenButtonPermissionRequest::permissionId));
    }

    @Nullable
    private MeterReading persistMeter(Meter meter, Map<String, String> authIndex) {
        if (!authIndex.containsKey(meter.authorizationUid())) {
            LOGGER.warn("Got unknown metering point {}", meter.uid());
            return null;
        }
        var permissionId = authIndex.get(meter.authorizationUid());
        LOGGER.info("Adding meter {} to permission request {}", meter.uid(), permissionId);
        return meterReadingRepository.save(new MeterReading(permissionId,
                                                            meter.uid(),
                                                            null,
                                                            PollingStatus.DATA_NOT_READY));
    }
}
