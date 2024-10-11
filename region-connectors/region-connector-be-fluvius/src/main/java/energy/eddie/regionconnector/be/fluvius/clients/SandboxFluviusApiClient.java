package energy.eddie.regionconnector.be.fluvius.clients;

import energy.eddie.regionconnector.be.fluvius.client.model.CreateMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.FluviusSessionCreateResultResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.GetMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
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

    public SandboxFluviusApiClient(FluviusApi api) {
        LOGGER.info("Mock mandates are enabled, using sandbox fluvius client to mock mandates");
        this.api = api;
    }

    @Override
    public Mono<FluviusSessionCreateResultResponseModelApiDataResponse> shortUrlIdentifier(
            String permissionId,
            Flow flow,
            ZonedDateTime from,
            ZonedDateTime to
    ) {
        return mockMandate(permissionId, from, to)
                .flatMap(res -> api.shortUrlIdentifier(permissionId, flow, from, to));
    }

    @Override
    public Mono<GetMandateResponseModelApiDataResponse> mandateFor(String permissionId) {
        return api.mandateFor(permissionId);
    }

    @Override
    public Mono<CreateMandateResponseModelApiDataResponse> mockMandate(
            String permissionId,
            ZonedDateTime from,
            ZonedDateTime to
    ) {
        return api.mockMandate(permissionId, from, to);
    }
}
