package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.providers.cim.EtaToCimMapper;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.cim.v104.DeValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.regionconnector.shared.services.CommonPollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class PollingService implements CommonPollingService<DePermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);

    private final DataNeedsService dataNeedsService;
    private final EtaPlusApiClient etaPlusApiClient;
    private final EtaToCimMapper mapper;
    private final DeValidatedHistoricalDataMarketDocumentProvider publisher;
    private final DePermissionRequestRepository repository;

    public PollingService(DataNeedsService dataNeedsService,
                          EtaPlusApiClient etaPlusApiClient,
                          EtaToCimMapper mapper,
                          DeValidatedHistoricalDataMarketDocumentProvider publisher,
                          DePermissionRequestRepository repository) {
        this.dataNeedsService = dataNeedsService;
        this.etaPlusApiClient = etaPlusApiClient;
        this.mapper = mapper;
        this.publisher = publisher;
        this.repository = repository;
    }

    @Override
    public void pollTimeSeriesData(DePermissionRequest permissionRequest) {
        if (isInactive(permissionRequest)) {
            return;
        }

        String permissionId = permissionRequest.permissionId();

        LocalDate fetchFromDate = permissionRequest.latestMeterReadingEndDate()
                .map(date -> date.plusDays(1))
                .orElse(permissionRequest.start());

        LocalDate fetchToDate = LocalDate.now(ZoneOffset.UTC).minusDays(1);

        if (fetchFromDate.isAfter(fetchToDate)) {
            LOGGER.debug("Skipping permission {}: Data is up to date.", permissionId);
            return;
        }

        LOGGER.info("Polling data for permission {} from {} to {}", permissionId, fetchFromDate, fetchToDate);

        try {
            List<EtaPlusMeteredData.MeterReading> readings = etaPlusApiClient
                    .streamMeteredData(
                            permissionRequest.meteringPointId(),
                            fetchFromDate,
                            fetchToDate.plusDays(1)
                    )
                    .collectList()
                    .block();

            if (readings == null || readings.isEmpty()) {
                LOGGER.info("No data returned for permission {}", permissionId);
                return;
            }

            Optional<VHDEnvelope> envelopeOpt = mapper.mapToEnvelope(permissionRequest, readings);

            if (envelopeOpt.isPresent()) {
                publisher.emitDocument(envelopeOpt.get());

                updateWatermark(permissionRequest, fetchToDate);

                LOGGER.info("Successfully processed {} readings for permission {}", readings.size(), permissionId);
            }

        } catch (Exception e) {
            LOGGER.error("Failed polling for permission {}", permissionId, e);
        }
    }

    private void updateWatermark(DePermissionRequest request, LocalDate newEndDate) {
        // Break builder chain to handle conditional fields (Fixes NullAway error)
        var builder = DePermissionRequest.builder()
                .permissionId(request.permissionId())
                .connectionId(request.connectionId())
                .meteringPointId(request.meteringPointId())
                .start(request.start())
                .end(request.end())
                .granularity(request.granularity())
                .energyType(request.energyType())
                .status(request.status())
                .created(request.created())
                .dataNeedId(request.dataNeedId())
                .latestReading(newEndDate.atStartOfDay(ZoneOffset.UTC));

        request.message().ifPresent(builder::message);
        request.cause().ifPresent(builder::cause);

        repository.save(builder.build());
        LOGGER.debug("Watermark updated for {} to {}", request.permissionId(), newEndDate);
    }

    @Override
    public boolean isActiveAndNeedsToBeFetched(DePermissionRequest permissionRequest) {
        if (isInactive(permissionRequest)) return false;
        if (permissionRequest.status() != PermissionProcessStatus.ACCEPTED) return false;

        Object dataNeed = dataNeedsService.getById(permissionRequest.dataNeedId());
        return dataNeed instanceof ValidatedHistoricalDataDataNeed;
    }

    private static boolean isInactive(DePermissionRequest permissionRequest) {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        return !permissionRequest.start().isBefore(now);
    }
}