package energy.eddie.regionconnector.be.fluvius.clients;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.be.fluvius.client.model.CreateMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.FluviusSessionCreateResultResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.GetMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

public interface FluviusApi {
    @SuppressWarnings("NullAway")
        // False positive
    Mono<FluviusSessionCreateResultResponseModelApiDataResponse> shortUrlIdentifier(
            String permissionId,
            Flow flow,
            ZonedDateTime from,
            ZonedDateTime to,
            Granularity granularity
    );

    Mono<GetMandateResponseModelApiDataResponse> mandateFor(String permissionId);

    Mono<CreateMandateResponseModelApiDataResponse> mockMandate(
            String permissionId,
            ZonedDateTime from,
            ZonedDateTime to,
            String ean
    );

    Mono<GetEnergyResponseModelApiDataResponse> energy(
            String permissionId,
            String eanNumber,
            DataServiceType dataServiceType,
            ZonedDateTime from,
            ZonedDateTime to
    );
}
