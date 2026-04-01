// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.data.needs.MultipleDataNeedCalculationResult.CalculationResult;
import energy.eddie.api.agnostic.data.needs.MultipleDataNeedCalculationResult.InvalidDataNeedCombination;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

@Service
public class PermissionRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private static final String DATA_NEED_ID = "dataNeedIds";
    private final EsPermissionRequestRepository repository;
    private final AccountingPointDataService accountingPointDataService;
    private final PermissionRequestConsumer permissionRequestConsumer;
    private final Outbox outbox;
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final BundleService bundleService;

    @Autowired
    public PermissionRequestService(
            EsPermissionRequestRepository repository,
            AccountingPointDataService accountingPointDataService,
            PermissionRequestConsumer permissionRequestConsumer,
            Outbox outbox,
            DataNeedCalculationService<DataNeed> calculationService,
            BundleService bundleService
    ) {
        this.repository = repository;
        this.accountingPointDataService = accountingPointDataService;
        this.permissionRequestConsumer = permissionRequestConsumer;
        this.outbox = outbox;
        this.calculationService = calculationService;
        this.bundleService = bundleService;
    }

    public Set<String> acceptPermission(Set<String> permissionIds) {
        var ids = new HashSet<String>();
        for (String permissionId : permissionIds) {
            try {
                var permissionRequest = getPermissionRequestById(permissionId);
                LOGGER.atInfo()
                      .addArgument(permissionRequest::permissionId)
                      .log("Got request to accept permission {}");
                accountingPointDataService
                        .fetchAccountingPointDataForPermissionRequest(permissionRequest)
                        .subscribe(accountingPointData ->
                                           permissionRequestConsumer.acceptPermission(permissionRequest,
                                                                                      accountingPointData),
                                   error -> permissionRequestConsumer.consumeError(error, permissionRequest)
                        );
                ids.add(permissionId);
            } catch (Exception e) {
                LOGGER.warn("Could not find permission request {} when accepting it", permissionId, e);
            }
        }
        return ids;
    }

    public Set<String> rejectPermission(Set<String> permissionIds) {
        var ids = new HashSet<String>();
        for (String permissionId : permissionIds) {
            try {
                var permissionRequest = getPermissionRequestById(permissionId);
                LOGGER.atInfo()
                      .addArgument(permissionRequest::permissionId)
                      .log("Got request to reject permission {}");
                outbox.commit(new EsSimpleEvent(permissionId, PermissionProcessStatus.REJECTED));
                ids.add(permissionId);
            } catch (Exception e) {
                LOGGER.warn("Could not find permission request {} when rejecting it", permissionId, e);
            }
        }
        return ids;
    }

    /**
     * Creates multiple new permission requests with the start and end date according to the data needs. It adds one day to the
     * end date automatically to ensure that the end date is inclusive.
     * If only one permission request is going to be created this method forwards all exceptions that might occur during the creation.
     * If there are multiple permission requests to be created, it catches and logs the exception
     *
     * @param requestForCreation basis for the permission request.
     * @return a validated permission request, that was sent to the permission administrator.
     * @throws DataNeedNotFoundException    if the data need does not exist.
     * @throws UnsupportedDataNeedException if the region connector does not support the data need.
     */
    public CreatedPermissionRequest createAndSendPermissionRequest(
            PermissionRequestForCreation requestForCreation
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException, EsValidationException {
        var dataNeedIds = requestForCreation.dataNeedIds();
        LOGGER.debug("Got request to create new permission requests with data need IDs: {}",
                     requestForCreation.dataNeedIds());
        if (dataNeedIds.size() == 1) {
            var dataNeedId = dataNeedIds.iterator().next();
            var calculation = calculationService.calculate(dataNeedId);
            var permissionId = createPermissionRequest(requestForCreation, dataNeedId, calculation, null);
            return new CreatedPermissionRequest(permissionId);
        }
        var calculations = calculationService.calculateAll(dataNeedIds);
        if (calculations instanceof InvalidDataNeedCombination(Set<String> offendingDataNeedIds, String message)) {
            throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID,
                                                   offendingDataNeedIds.iterator().next(),
                                                   message);
        }
        var permissionIds = new ArrayList<String>(dataNeedIds.size());
        var bundleId = UUID.randomUUID();
        var calcs = (CalculationResult) calculations;
        for (var dataNeed : calcs.result().entrySet()) {
            try {
                var permissionId = createPermissionRequest(requestForCreation,
                                                           dataNeed.getKey(),
                                                           dataNeed.getValue(),
                                                           bundleId);
                LOGGER.trace("Created permission request with id {}", permissionId);
                permissionIds.add(permissionId);
            } catch (Exception e) {
                LOGGER.warn("Failed to create permission request data need with ID {}", dataNeed.getKey(), e);
            }
        }
        bundleService.sendBundledAuthorizationRequest(bundleId);
        return new CreatedPermissionRequest(permissionIds);
    }

    public void terminatePermission(String permissionId) throws PermissionNotFoundException {
        var permissionRequest = getPermissionRequestById(permissionId);
        outbox.commit(new EsSimpleEvent(permissionRequest.permissionId(), PermissionProcessStatus.TERMINATED));
    }

    private String createPermissionRequest(
            PermissionRequestForCreation requestForCreation,
            String dataNeedId,
            DataNeedCalculationResult calculation,
            @Nullable UUID bundleId
    ) throws EsValidationException, UnsupportedDataNeedException, DataNeedNotFoundException {
        var permissionId = UUID.randomUUID().toString();
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
        switch (calculation) {
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
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID,
                                                       dataNeedId,
                                                       message);
            }
            case ValidatedHistoricalDataDataNeedResult vhdResult ->
                    handleValidatedHistoricalDataNeed(vhdResult, permissionId, bundleId);
            case AccountingPointDataNeedResult ignored -> handleAccountingPointDataNeed(permissionId, bundleId);
            case CESUJoinRequestDataNeedResult cesuResult ->
                    handleCESUJoinRequestDataNeed(cesuResult, permissionId, bundleId);
            default -> {
                String message = "Data Need with ID %s is not supported!".formatted(dataNeedId);
                outbox.commit(new EsMalformedEvent(permissionId,
                                                   List.of(new AttributeError(DATA_NEED_ID, message))));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
            }
        }
        return permissionId;
    }

    private void handleCESUJoinRequestDataNeed(
            CESUJoinRequestDataNeedResult dataNeed,
            String permissionId,
            @Nullable UUID bundleId
    ) {
        var allowedMeasurementType = allowedMeasurementType(dataNeed.supportedGranularities());
        outbox.commit(new EsValidatedEvent(
                permissionId,
                dataNeed.energyDataTimeframe().start(),
                dataNeed.energyDataTimeframe().end(),
                allowedMeasurementType,
                bundleId
        ));
    }

    private EsPermissionRequest getPermissionRequestById(String permissionId) throws PermissionNotFoundException {
        return repository.findByPermissionId(permissionId)
                         .orElseThrow(() -> new PermissionNotFoundException(permissionId));
    }

    private void handleValidatedHistoricalDataNeed(
            ValidatedHistoricalDataDataNeedResult calculation,
            String permissionId,
            @Nullable UUID bundleId
    ) {
        var allowedMeasurementType = allowedMeasurementType(calculation.granularities());
        var energyDataTimeframe = calculation.energyTimeframe();
        outbox.commit(new EsValidatedEvent(
                permissionId,
                energyDataTimeframe.start(),
                energyDataTimeframe.end(),
                allowedMeasurementType,
                bundleId
        ));
    }

    private void handleAccountingPointDataNeed(String permissionId, @Nullable UUID bundleId) {
        LocalDate today = LocalDate.now(ZONE_ID_SPAIN);
        outbox.commit(new EsValidatedEvent(
                permissionId,
                today,
                today,
                null,
                bundleId
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
