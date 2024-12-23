package energy.eddie.regionconnector.be.fluvius.sandbox;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.model.CreateMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.FluviusSessionCreateResultResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.GetMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.clients.DataServiceType;
import energy.eddie.regionconnector.be.fluvius.clients.FluviusApi;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import jakarta.annotation.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

@Component
@ConditionalOnProperty(name = "region-connector.be.fluvius.mock-mandates", havingValue = "true")
@Priority(value = 1)
public class SandboxFluviusApiClient implements FluviusApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(SandboxFluviusApiClient.class);
    private final FluviusApi api;
    private final BePermissionRequestRepository repository;
    private final DataNeedsService dataNeedsService;

    public SandboxFluviusApiClient(
            FluviusApi api,
            BePermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
        LOGGER.info("Mock mandates are enabled, using sandbox fluvius client to mock mandates");
        this.api = api;
    }

    @Override
    public Mono<FluviusSessionCreateResultResponseModelApiDataResponse> shortUrlIdentifier(
            String permissionId,
            Flow flow,
            ZonedDateTime from,
            ZonedDateTime to,
            Granularity granularity
    ) {
        var pr = repository.getByPermissionId(permissionId);
        var dataNeed = dataNeedsService.getById(pr.dataNeedId());
        var ean = new MockEan(dataNeed, pr, granularity);
        LOGGER.info("Using ean {} for sandbox environment for permission request {}", ean, permissionId);
        return mockMandate(permissionId, from, to, ean.toString())
                .flatMap(res -> api.shortUrlIdentifier(permissionId, flow, from, to, granularity));
    }

    @Override
    public Mono<GetMandateResponseModelApiDataResponse> mandateFor(String permissionId) {
        return api.mandateFor(permissionId);
    }

    @Override
    public Mono<CreateMandateResponseModelApiDataResponse> mockMandate(
            String permissionId,
            ZonedDateTime from,
            ZonedDateTime to,
            String ean
    ) {
        return api.mockMandate(permissionId, from, to, ean);
    }

    @Override
    public Mono<GetEnergyResponseModelApiDataResponse> energy(
            String permissionId,
            String eanNumber,
            DataServiceType dataServiceType,
            ZonedDateTime from,
            ZonedDateTime to
    ) {
        return api.energy(permissionId, eanNumber, dataServiceType, from, to);
    }
}
