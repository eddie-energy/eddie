// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata;
import energy.eddie.regionconnector.es.datadis.consumer.PermissionRequestConsumer;
import energy.eddie.regionconnector.es.datadis.dtos.AllowedGranularity;
import energy.eddie.regionconnector.es.datadis.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.exceptions.EsValidationException;
import energy.eddie.regionconnector.es.datadis.permission.events.EsCreatedEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsMalformedEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsValidatedEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.validation.IdentifierValidator;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

@Service
public class PermissionRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private static final String DATA_NEED_ID = "dataNeedId";
    private final EsPermissionRequestRepository repository;
    private final AccountingPointDataService accountingPointDataService;
    private final PermissionRequestConsumer permissionRequestConsumer;
    private final Outbox outbox;
    private final DataNeedCalculationService<DataNeed> calculationService;

    @Autowired
    public PermissionRequestService(
            EsPermissionRequestRepository repository,
            AccountingPointDataService accountingPointDataService,
            PermissionRequestConsumer permissionRequestConsumer,
            Outbox outbox,
            DataNeedCalculationService<DataNeed> calculationService
    ) {
        this.repository = repository;
        this.accountingPointDataService = accountingPointDataService;
        this.permissionRequestConsumer = permissionRequestConsumer;
        this.outbox = outbox;
        this.calculationService = calculationService;
    }

    public void acceptPermission(String permissionId) throws PermissionNotFoundException {
        var permissionRequest = getPermissionRequestById(permissionId);
        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .log("Got request to accept permission {}");
        accountingPointDataService
                .fetchAccountingPointDataForPermissionRequest(permissionRequest)
                .doOnError(error -> permissionRequestConsumer.consumeError(error, permissionRequest))
                .onErrorComplete()
                .subscribe(accountingPointData ->
                                   permissionRequestConsumer.acceptPermission(permissionRequest, accountingPointData)
                );
    }

    public void rejectPermission(String permissionId) throws PermissionNotFoundException {
        var permissionRequest = getPermissionRequestById(permissionId);
        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .log("Got request to reject permission {}");
        outbox.commit(new EsSimpleEvent(permissionId, PermissionProcessStatus.REJECTED));
    }

    /**
     * Creates a new permission request with the start and end date according to the data needs. It adds one day to the
     * end date automatically to ensure that the end date is inclusive.
     *
     * @param requestForCreation basis for the permission request.
     * @return a validated permission request, that was sent to the permission administrator.
     * @throws DataNeedNotFoundException    if the data need does not exist.
     * @throws UnsupportedDataNeedException if the region connector does not support the data need.
     */
    public CreatedPermissionRequest createAndSendPermissionRequest(
            PermissionRequestForCreation requestForCreation
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException, EsValidationException {
        LOGGER.info("Got request to create a new permission, request was: {}", requestForCreation);
        var permissionId = UUID.randomUUID().toString();
        var dataNeedId = requestForCreation.dataNeedId();
        outbox.commit(new EsCreatedEvent(permissionId,
                                         requestForCreation.connectionId(),
                                         dataNeedId,
                                         requestForCreation.nif(),
                                         requestForCreation.meteringPointId()));
        var isValid = new IdentifierValidator().isValidIdentifier(requestForCreation.nif());
        if (!isValid) {
            var error = new AttributeError("nif", "Invalid NIF");
            outbox.commit(new EsMalformedEvent(permissionId, List.of(error)));
            throw new EsValidationException(error);
        }
        var calculation = calculationService.calculate(dataNeedId);
        switch (calculation) {
            case AiidaDataNeedResult ignored -> {
                String message = "AiidaDataDataNeedResult not supported!";
                outbox.commit(new EsMalformedEvent(permissionId, List.of(new AttributeError(DATA_NEED_ID, message))));
                throw new UnsupportedDataNeedException(DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                       dataNeedId,
                                                       message);
            }
            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new EsMalformedEvent(
                        permissionId,
                        List.of(new AttributeError(DATA_NEED_ID, "DataNeed not found"))
                ));
                throw new DataNeedNotFoundException(dataNeedId);
            }
            case DataNeedNotSupportedResult(String message) -> {
                outbox.commit(new EsMalformedEvent(
                        permissionId,
                        List.of(new AttributeError(DATA_NEED_ID, message))
                ));
                throw new UnsupportedDataNeedException(DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                       dataNeedId,
                                                       message);
            }
            case ValidatedHistoricalDataDataNeedResult vhdResult ->
                    handleValidatedHistoricalDataNeed(vhdResult, permissionId);
            case AccountingPointDataNeedResult ignored -> handleAccountingPointDataNeed(permissionId);
        }
        return new CreatedPermissionRequest(permissionId);
    }

    public void terminatePermission(String permissionId) throws PermissionNotFoundException {
        var permissionRequest = getPermissionRequestById(permissionId);
        outbox.commit(new EsSimpleEvent(permissionRequest.permissionId(), PermissionProcessStatus.TERMINATED));
    }

    private EsPermissionRequest getPermissionRequestById(String permissionId) throws PermissionNotFoundException {
        return repository.findByPermissionId(permissionId)
                         .orElseThrow(() -> new PermissionNotFoundException(permissionId));
    }

    private void handleValidatedHistoricalDataNeed(
            ValidatedHistoricalDataDataNeedResult calculation,
            String permissionId
    ) {
        var allowedMeasurementType = allowedMeasurementType(calculation.granularities());
        var energyDataTimeframe = calculation.energyTimeframe();
        outbox.commit(new EsValidatedEvent(
                permissionId,
                energyDataTimeframe.start(),
                energyDataTimeframe.end(),
                allowedMeasurementType
        ));
    }

    private void handleAccountingPointDataNeed(String permissionId) {
        LocalDate today = LocalDate.now(ZONE_ID_SPAIN);
        outbox.commit(new EsValidatedEvent(
                permissionId,
                today,
                today,
                null
        ));
    }

    private static AllowedGranularity allowedMeasurementType(List<Granularity> granularities) {
        boolean hourly = false;
        boolean quarterHourly = false;
        if (granularities.contains(Granularity.PT1H)) {
            hourly = true;
        }
        if (granularities.contains(Granularity.PT15M)) {
            quarterHourly = true;
        }
        if (hourly && quarterHourly) {
            return AllowedGranularity.PT15M_OR_PT1H;
        }
        if (hourly) {
            return AllowedGranularity.PT1H;
        }
        return AllowedGranularity.PT15M;
    }
}
