package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata;
import energy.eddie.regionconnector.es.datadis.consumer.PermissionRequestConsumer;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.stream.Stream;

public class PermissionRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private static final Period MAX_TIME_IN_THE_PAST = Period.ofMonths(
            -DatadisRegionConnectorMetadata.MAXIMUM_MONTHS_IN_THE_PAST);
    /**
     * The maximum time in the future that a permission request can be created for. 24 months minus one day. Datadis API
     * is exclusive on the end date, so we need to subtract one day here.
     */
    private static final Period MAX_TIME_IN_THE_FUTURE = Period.ofMonths(
            DatadisRegionConnectorMetadata.MAXIMUM_MONTHS_IN_THE_FUTURE).minusDays(1);
    private final EsPermissionRequestRepository repository;
    private final PermissionRequestFactory permissionRequestFactory;
    private final SupplyApiService supplyApiService;
    private final PermissionRequestConsumer permissionRequestConsumer;
    private final DataNeedsService dataNeedsService;

    @Autowired
    public PermissionRequestService(
            EsPermissionRequestRepository repository,
            PermissionRequestFactory permissionRequestFactory,
            SupplyApiService supplyApiService,
            PermissionRequestConsumer permissionRequestConsumer,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Injected from another spring context
            DataNeedsService dataNeedsService
    ) {
        this.repository = repository;
        this.permissionRequestFactory = permissionRequestFactory;
        this.supplyApiService = supplyApiService;
        this.permissionRequestConsumer = permissionRequestConsumer;
        this.dataNeedsService = dataNeedsService;
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return repository.findByPermissionId(permissionId)
                         .map(permissionRequest -> new ConnectionStatusMessage(
                                 permissionRequest.connectionId(),
                                 permissionRequest.permissionId(),
                                 permissionRequest.dataNeedId(),
                                 permissionRequest.dataSourceInformation(),
                                 permissionRequest.status(),
                                 permissionRequest.errorMessage()
                         ));
    }

    public void acceptPermission(String permissionId) throws PermissionNotFoundException {
        var permissionRequest = getPermissionRequestById(permissionId);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Got request to accept permission {}", permissionRequest.permissionId());
        }

        supplyApiService.fetchSupplyForPermissionRequest(permissionRequest)
                        .subscribe(
                                supply -> permissionRequestConsumer.acceptPermission(permissionRequest, supply),
                                e -> permissionRequestConsumer.consumeError(e, permissionRequest)
                        );
    }

    private EsPermissionRequest getPermissionRequestById(String permissionId) throws PermissionNotFoundException {
        return repository.findByPermissionId(permissionId)
                         .map(permissionRequestFactory::create)
                         .orElseThrow(() -> new PermissionNotFoundException(permissionId));
    }

    public void rejectPermission(String permissionId) throws PermissionNotFoundException, StateTransitionException {
        var permissionRequest = getPermissionRequestById(permissionId);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Got request to reject permission {}", permissionRequest.permissionId());
        }
        permissionRequest.reject();
    }

    public Stream<EsPermissionRequest> getAllAcceptedPermissionRequests() {
        return repository.findAllAccepted().map(permissionRequestFactory::create);
    }

    /**
     * Creates a new permission request with the start and end date according to the data needs. It adds one day to the
     * end date automatically to ensure that the end date is inclusive.
     *
     * @param requestForCreation basis for the permission request.
     * @return a validated permission request, that was sent to the permission administrator.
     * @throws StateTransitionException     if something happens during validation or transmission of the permission
     *                                      request.
     * @throws DataNeedNotFoundException    if the data need does not exist.
     * @throws UnsupportedDataNeedException if the region connector does not support the data need.
     */
    public PermissionRequest createAndSendPermissionRequest(
            PermissionRequestForCreation requestForCreation
    ) throws StateTransitionException, DataNeedNotFoundException, UnsupportedDataNeedException {
        LOGGER.info("Got request to create a new permission, request was: {}", requestForCreation);
        var refDate = LocalDate.now(DatadisRegionConnectorMetadata.ZONE_ID_SPAIN);
        var dataNeed = dataNeedsService.findDataNeedAndCalculateStartAndEnd(requestForCreation.dataNeedId(), refDate,
                                                                            MAX_TIME_IN_THE_PAST,
                                                                            MAX_TIME_IN_THE_FUTURE);
        if (!(dataNeed.timeframedDataNeed() instanceof ValidatedHistoricalDataDataNeed vhdDataNeed)) {
            throw new UnsupportedDataNeedException(DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   requestForCreation.dataNeedId(),
                                                   "This region connector only supports ValidatedHistoricalData DataNeeds");
        }
        var granularity = findGranularity(vhdDataNeed.id(), vhdDataNeed.minGranularity(), vhdDataNeed.maxGranularity());
        var request = permissionRequestFactory.create(requestForCreation,
                                                      dataNeed.calculatedStart(),
                                                      dataNeed.calculatedEnd(),
                                                      granularity);
        request.validate();
        request.sendToPermissionAdministrator();
        request.receivedPermissionAdministratorResponse();
        return request;
    }

    private static Granularity findGranularity(
            String dataNeedId, Granularity min,
            Granularity max
    ) throws UnsupportedDataNeedException {
        for (Granularity granularity : DatadisRegionConnectorMetadata.SUPPORTED_GRANULARITIES) {
            if (granularity.minutes() >= min.minutes() && granularity.minutes() <= max.minutes()) {
                return granularity;
            }
        }
        throw new UnsupportedDataNeedException(DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID, dataNeedId,
                                               "Unsupported granularity");
    }

    public void terminatePermission(String permissionId) throws PermissionNotFoundException, StateTransitionException {
        var permissionRequest = getPermissionRequestById(permissionId);
        permissionRequest.terminate();
    }
}
