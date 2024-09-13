package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import energy.eddie.regionconnector.us.green.button.permission.GreenButtonDataSourceInformation;
import energy.eddie.regionconnector.us.green.button.permission.events.UsCreatedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMalformedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsValidatedEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.dtos.PermissionRequestForCreation;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestCreationService.class);
    private static final String UNSUPPORTED_DATA_NEED_MESSAGE = "This Region Connector only supports Validated Historical Data Data Needs for Electricity";
    private static final String UNSUPPORTED_GRANULARITY_MESSAGE = "Required granularity not supported.";
    private static final String DATA_NEED_ID = "dataNeedId";
    private final UsPermissionRequestRepository repository;
    private final GreenButtonConfiguration configuration;
    private final DataNeedsService dataNeedsService;
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final Outbox outbox;

    public PermissionRequestCreationService(
            UsPermissionRequestRepository repository,
            GreenButtonConfiguration configuration,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")  // defined in core
            DataNeedsService dataNeedsService,
            DataNeedCalculationService<DataNeed> calculationService,
            Outbox outbox
    ) {
        this.repository = repository;
        this.configuration = configuration;
        this.dataNeedsService = dataNeedsService;
        this.calculationService = calculationService;
        this.outbox = outbox;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequestForCreation) throws DataNeedNotFoundException, UnsupportedDataNeedException, MissingClientIdException, MissingClientSecretException {
        var permissionId = UUID.randomUUID().toString();
        var dataSourceInformation = new GreenButtonDataSourceInformation(permissionRequestForCreation.companyId(),
                                                                         permissionRequestForCreation.countryCode());

        outbox.commit(new UsCreatedEvent(permissionId,
                                         permissionRequestForCreation.connectionId(),
                                         permissionRequestForCreation.dataNeedId(),
                                         permissionRequestForCreation.jumpOffUrl(),
                                         dataSourceInformation));

        var dataNeedWrapper = dataNeedsService.findById(permissionRequestForCreation.dataNeedId());
        if (dataNeedWrapper.isEmpty()) {
            outbox.commit(new UsMalformedEvent(
                    permissionId,
                    List.of(new AttributeError(DATA_NEED_ID, "Unknown dataNeedId"))
            ));
            throw new DataNeedNotFoundException(permissionRequestForCreation.dataNeedId());
        }

        var dataNeed = dataNeedWrapper.get();
        var calculation = calculationService.calculate(dataNeed);

        if (!calculation.supportsDataNeed()
            || calculation.energyDataTimeframe() == null
            || calculation.permissionTimeframe() == null) {
            outbox.commit(new UsMalformedEvent(permissionId,
                                               List.of(new AttributeError(DATA_NEED_ID,
                                                                          UNSUPPORTED_DATA_NEED_MESSAGE))));
            throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID,
                                                   dataNeed.id(),
                                                   UNSUPPORTED_DATA_NEED_MESSAGE);
        }
        if (calculation.granularities() == null || calculation.granularities().isEmpty()) {
            outbox.commit(new UsMalformedEvent(permissionId,
                                               List.of(new AttributeError(DATA_NEED_ID,
                                                                          UNSUPPORTED_GRANULARITY_MESSAGE))));
            throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID,
                                                   dataNeed.id(),
                                                   UNSUPPORTED_GRANULARITY_MESSAGE);
        }

        var companyId = permissionRequestForCreation.companyId();
        try {
            validateUsConfiguration(companyId);
        } catch (MissingClientIdException | MissingClientSecretException e) {
            outbox.commit(new UsMalformedEvent(permissionId, List.of(new AttributeError("companyId", e.getMessage()))));
            throw e;
        }

        var clientId = configuration.clientIds().get(companyId);

        Scope scope;
        try {
            scope = new Scope.ScopeBuilder().addDataField(Scope.DataField.INTERVALS)
                                            .withHistoricalDataStart(calculation.energyDataTimeframe().start())
                                            .withOngoingDataEnd(calculation.energyDataTimeframe().end())
                                            .withGranularity(calculation.granularities().getFirst())
                                            .build();
        } catch (IllegalStateException e) {
            outbox.commit(new UsMalformedEvent(permissionId, List.of(new AttributeError("scope", e.getMessage()))));
            throw e;
        }

        String scopeString = scope.toString();
        outbox.commit(new UsValidatedEvent(permissionId,
                                           calculation.energyDataTimeframe().start(),
                                           calculation.energyDataTimeframe().end(),
                                           calculation.granularities().getFirst(),
                                           scopeString));

        URI redirectUri = buildRedirectUri(permissionId,
                                           permissionRequestForCreation.jumpOffUrl(),
                                           clientId,
                                           scopeString);
        LOGGER.info("Redirect URI: {}", redirectUri);

        return new CreatedPermissionRequest(permissionId, redirectUri);
    }

    private void validateUsConfiguration(String companyId) throws MissingClientIdException, MissingClientSecretException {
        if (!configuration.clientIds().containsKey(companyId)) {
            throw new MissingClientIdException();
        }

        if (!configuration.clientSecrets().containsKey(companyId)) {
            throw new MissingClientSecretException();
        }
    }

    private URI buildRedirectUri(String permissionId, String jumpOffUrl, String clientId, String scope) {
        return UriComponentsBuilder.fromHttpUrl(jumpOffUrl)
                                   .path("/oauth/authorize")
                                   .queryParam("response_type", UriUtils.encode("code", StandardCharsets.UTF_8))
                                   .queryParam("client_id", UriUtils.encode(clientId, StandardCharsets.UTF_8))
                                   .queryParam("state", UriUtils.encode(permissionId, StandardCharsets.UTF_8))
                                   .queryParam("redirect_uri",
                                               UriUtils.encode(configuration.redirectUri(), StandardCharsets.UTF_8))
                                   .queryParam("scope", UriUtils.encode(scope, StandardCharsets.UTF_8))
                                   .build(true)
                                   .toUri();
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return repository.findByPermissionId(permissionId)
                         .map(request -> new ConnectionStatusMessage(request.connectionId(),
                                                                     request.permissionId(),
                                                                     request.dataNeedId(),
                                                                     request.dataSourceInformation(),
                                                                     request.status()));
    }
}
