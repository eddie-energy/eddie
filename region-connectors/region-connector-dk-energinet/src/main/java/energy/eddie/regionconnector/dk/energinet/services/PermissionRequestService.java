package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.SendToPermissionAdministratorException;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPoints;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.Optional;
import java.util.UUID;

@Service
public class PermissionRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private final DkEnerginetCustomerPermissionRequestRepository repository;
    private final PermissionRequestFactory requestFactory;
    private final EnerginetCustomerApi energinetCustomerApi;
    private final TaskExecutor fetchRecordsExecutor;
    private final Sinks.Many<ConsumptionRecord> consumptionRecordSink;

    public PermissionRequestService(
            DkEnerginetCustomerPermissionRequestRepository repository,
            PermissionRequestFactory requestFactory,
            EnerginetCustomerApi energinetCustomerApi,
            TaskExecutor fetchRecordsExecutor,
            Sinks.Many<ConsumptionRecord> consumptionRecordSink) {
        this.repository = repository;
        this.requestFactory = requestFactory;
        this.energinetCustomerApi = energinetCustomerApi;
        this.fetchRecordsExecutor = fetchRecordsExecutor;
        this.consumptionRecordSink = consumptionRecordSink;
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return repository.findByPermissionId(permissionId).map(request ->
                new ConnectionStatusMessage(
                        request.connectionId(),
                        request.permissionId(),
                        request.dataNeedId(),
                        request.regionalInformation(),
                        request.state().status())
        );
    }

    /**
     * Creates a new {@link PermissionRequest}, validates it and sends it to the permission administrator.
     *
     * @param requestForCreation Dto that contains the necessary information for this permission request.
     * @return The created PermissionRequest
     * @throws ValidationException                    If the {@code requestForCreation} is not valid.
     * @throws SendToPermissionAdministratorException When an error occurs while sending the request to the PA.
     *                                                If {@link SendToPermissionAdministratorException#userFault()} is true, the customer provided an invalid refresh token.
     */
    public PermissionRequest createAndSendPermissionRequest(PermissionRequestForCreation requestForCreation) throws StateTransitionException {
        var permissionRequest = requestFactory.create(requestForCreation);
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();
        // if sendToPA doesn't fail, we have a valid refreshToken and can start polling the records in the background
        permissionRequest.receivedPermissionAdministratorResponse();

        fetchRecordsExecutor.execute(() -> {
            // TODO refactor this and use non-blocking api client
            energinetCustomerApi.setRefreshToken(permissionRequest.refreshToken());
            energinetCustomerApi.setUserCorrelationId(UUID.fromString(permissionRequest.permissionId()));
            MeteringPoints meteringPoints = new MeteringPoints();
            meteringPoints.addMeteringPointItem(permissionRequest.meteringPoint());
            MeteringPointsRequest meteringPointsRequest = new MeteringPointsRequest().meteringPoints(meteringPoints);
            try {
                permissionRequest.accept();
                energinetCustomerApi.apiToken();
            } catch (FeignException e) {
                LOGGER.error("Something went wrong while fetching token from Energinet:", e);
            } catch (StateTransitionException e) {
                LOGGER.error("Error while transitioning a state", e);
            }

            try {
                var consumptionRecord = energinetCustomerApi.getTimeSeries(
                        permissionRequest.start(),
                        permissionRequest.end(),
                        permissionRequest.periodResolution(),
                        meteringPointsRequest
                );

                consumptionRecord.setConnectionId(permissionRequest.connectionId());
                consumptionRecord.setPermissionId(permissionRequest.permissionId());
                consumptionRecord.setDataNeedId(permissionRequest.dataNeedId());
                consumptionRecordSink.tryEmitNext(consumptionRecord);
            } catch (FeignException e) {
                LOGGER.error("Something went wrong while fetching data from Energinet:", e);
            }
        });

        return permissionRequest;
    }
}