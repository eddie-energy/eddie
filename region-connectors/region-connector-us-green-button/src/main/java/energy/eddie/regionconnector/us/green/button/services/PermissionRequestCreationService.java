package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingApiTokenException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingCredentialsException;
import energy.eddie.regionconnector.us.green.button.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.us.green.button.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.us.green.button.permission.GreenButtonDataSourceInformation;
import energy.eddie.regionconnector.us.green.button.permission.events.UsCreatedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMalformedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsValidatedEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.helper.Scope;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.us.green.button.GreenButtonRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Service
@SuppressWarnings("NullAway")
public class PermissionRequestCreationService {
    public static final String SCOPE = "scope";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestCreationService.class);
    private static final String DATA_NEED_ID = "dataNeedId";
    private final UsPermissionRequestRepository repository;
    private final GreenButtonConfiguration configuration;
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final Outbox outbox;

    public PermissionRequestCreationService(
            UsPermissionRequestRepository repository,
            GreenButtonConfiguration configuration,
            DataNeedCalculationService<DataNeed> calculationService,
            Outbox outbox
    ) {
        this.repository = repository;
        this.configuration = configuration;
        this.calculationService = calculationService;
        this.outbox = outbox;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequestForCreation) throws DataNeedNotFoundException, UnsupportedDataNeedException, MissingCredentialsException {
        var permissionId = UUID.randomUUID().toString();
        var dataSourceInformation = new GreenButtonDataSourceInformation(permissionRequestForCreation.companyId(),
                                                                         permissionRequestForCreation.countryCode());
        var dataNeedId = permissionRequestForCreation.dataNeedId();
        outbox.commit(new UsCreatedEvent(permissionId,
                                         permissionRequestForCreation.connectionId(),
                                         dataNeedId,
                                         permissionRequestForCreation.jumpOffUrl(),
                                         dataSourceInformation));

        var companyId = permissionRequestForCreation.companyId();
        String clientId;
        try {
            clientId = validateUsConfigurationAndGetClientId(companyId);
        } catch (MissingClientIdException | MissingClientSecretException | MissingApiTokenException e) {
            outbox.commit(new UsMalformedEvent(permissionId, List.of(new AttributeError("companyId", e.getMessage()))));
            throw e;
        }
        var calculation = calculationService.calculate(dataNeedId);
        var redirectUri = switch (calculation) {
            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new UsMalformedEvent(
                        permissionId,
                        List.of(new AttributeError(DATA_NEED_ID, "Unknown dataNeedId"))
                ));
                throw new DataNeedNotFoundException(dataNeedId);
            }
            case DataNeedNotSupportedResult(String message) -> {
                outbox.commit(new UsMalformedEvent(permissionId,
                                                   List.of(new AttributeError(DATA_NEED_ID, message))));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
            }
            case AccountingPointDataNeedResult apResult ->
                    handlePermissionRequest(permissionRequestForCreation, apResult, permissionId, clientId);

            case ValidatedHistoricalDataDataNeedResult vhdResult ->
                    handlePermissionRequest(permissionRequestForCreation, vhdResult, permissionId, clientId);
        };

        LOGGER.info("Permission request {} validated", permissionId);
        return new CreatedPermissionRequest(permissionId, redirectUri);
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return repository.findByPermissionId(permissionId)
                         .map(request -> new ConnectionStatusMessage(request.connectionId(),
                                                                     request.permissionId(),
                                                                     request.dataNeedId(),
                                                                     request.dataSourceInformation(),
                                                                     request.status()));
    }

    public Optional<String> findDataNeedIdByPermissionId(String permissionId) {
        return repository.findByPermissionId(permissionId)
                         .map(PermissionRequest::dataNeedId);
    }

    private URI handlePermissionRequest(
            PermissionRequestForCreation permissionRequest,
            AccountingPointDataNeedResult apDataNeed,
            String permissionId,
            String clientId
    ) {
        var start = apDataNeed.permissionTimeframe().start();
        var end = apDataNeed.permissionTimeframe().end();
        Scope scope;
        try {
            scope = new Scope.ScopeBuilder()
                    .addDataField(Scope.DataField.ACCOUNT_DETAILS)
                    .addDataField(Scope.DataField.INTERVALS)
                    .addDataField(Scope.DataField.BILLS)
                    .withHistoricalDataStart(start)
                    .withOngoingDataEnd(end)
                    .build();
        } catch (IllegalStateException e) {
            outbox.commit(new UsMalformedEvent(permissionId, List.of(new AttributeError(SCOPE, e.getMessage()))));
            throw e;
        }

        var scopeString = scope.toString();
        outbox.commit(new UsValidatedEvent(permissionId, start, end, scopeString));

        return buildRedirectUri(permissionId,
                                permissionRequest.jumpOffUrl(),
                                clientId,
                                scopeString);
    }

    private URI handlePermissionRequest(
            PermissionRequestForCreation permissionRequest,
            ValidatedHistoricalDataDataNeedResult dataNeed,
            String permissionId,
            String clientId
    ) {
        Scope scope;
        try {
            scope = new Scope.ScopeBuilder().addDataField(Scope.DataField.INTERVALS)
                                            .withHistoricalDataStart(dataNeed.energyTimeframe().start())
                                            .withOngoingDataEnd(dataNeed.energyTimeframe().end())
                                            .withGranularity(dataNeed.granularities().getFirst())
                                            .build();
        } catch (IllegalStateException e) {
            outbox.commit(new UsMalformedEvent(permissionId, List.of(new AttributeError(SCOPE, e.getMessage()))));
            throw e;
        }

        String scopeString = scope.toString();
        outbox.commit(new UsValidatedEvent(permissionId,
                                           dataNeed.energyTimeframe().start(),
                                           dataNeed.energyTimeframe().end(),
                                           dataNeed.granularities().getFirst(),
                                           scopeString));

        return buildRedirectUri(permissionId,
                                permissionRequest.jumpOffUrl(),
                                clientId,
                                scopeString);
    }

    private String validateUsConfigurationAndGetClientId(String companyId) throws MissingClientIdException, MissingClientSecretException, MissingApiTokenException {
        configuration.getClientSecretOrThrow(companyId);
        configuration.throwOnMissingToken(companyId);
        return configuration.getClientIdOrThrow(companyId);
    }

    private URI buildRedirectUri(String permissionId, String jumpOffUrl, String clientId, String scope) {
        return UriComponentsBuilder.fromUri(URI.create(jumpOffUrl))
                                   .path("/oauth/authorize")
                                   .queryParam("response_type", UriUtils.encode("code", StandardCharsets.UTF_8))
                                   .queryParam("client_id", UriUtils.encode(clientId, StandardCharsets.UTF_8))
                                   .queryParam("state", UriUtils.encode(permissionId, StandardCharsets.UTF_8))
                                   .queryParam("redirect_uri",
                                               UriUtils.encode(configuration.redirectUri(), StandardCharsets.UTF_8))
                                   .queryParam(SCOPE, UriUtils.encode(scope, StandardCharsets.UTF_8))
                                   .build(true)
                                   .toUri();
    }
}
