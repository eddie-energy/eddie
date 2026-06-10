// SPDX-FileCopyrightText: 2026 The ETA+ Developers <bilal.sakhawat@etaplus.energy>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionCredentialsRepository;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.retransmission.PollingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Polls validated historical data for retransmission requests from the ETA Plus API.
 *
 * <p>Invoked by {@link energy.eddie.regionconnector.shared.retransmission.CommonRetransmissionService}
 * only after the request has been validated (permission accepted/fulfilled, validated-historical-data
 * need, and a window inside the permission range and in the past). The fetched data is emitted to the
 * outbound connectors via {@link ValidatedHistoricalDataStream#publishRetransmission}, which does not
 * alter permission state.
 */
@Service
public class EtaRetransmissionPollingFunction implements PollingFunction<DePermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtaRetransmissionPollingFunction.class);

    private final EtaPlusApiClient apiClient;
    private final ValidatedHistoricalDataStream stream;
    private final DePermissionCredentialsRepository credentialsRepository;

    public EtaRetransmissionPollingFunction(
            EtaPlusApiClient apiClient,
            ValidatedHistoricalDataStream stream,
            DePermissionCredentialsRepository credentialsRepository
    ) {
        this.apiClient = apiClient;
        this.stream = stream;
        this.credentialsRepository = credentialsRepository;
    }

    @Override
    public Mono<RetransmissionResult> poll(
            DePermissionRequest permissionRequest,
            RetransmissionRequest retransmissionRequest
    ) {
        String permissionId = permissionRequest.permissionId();
        return credentialsRepository.findByPermissionId(permissionId)
                .map(credentials -> fetchAndPublish(permissionRequest, retransmissionRequest, credentials.accessToken()))
                .orElseGet(() -> {
                    LOGGER.atWarn()
                          .addArgument(permissionId)
                          .log("Cannot retransmit data for permission {}: no credentials available");
                    return Mono.just(new Failure(permissionId, now(), "No credentials available for permission " + permissionId));
                });
    }

    private Mono<RetransmissionResult> fetchAndPublish(
            DePermissionRequest permissionRequest,
            RetransmissionRequest retransmissionRequest,
            String accessToken
    ) {
        String permissionId = permissionRequest.permissionId();
        LOGGER.atInfo()
              .addArgument(permissionId)
              .addArgument(retransmissionRequest::from)
              .addArgument(retransmissionRequest::to)
              .log("Polling validated historical data for retransmission of permission {} from {} to {}");

        return apiClient.fetchMeteredData(
                        permissionRequest, accessToken, retransmissionRequest.from(), retransmissionRequest.to())
                .filter(EtaRetransmissionPollingFunction::hasReadings)
                .doOnNext(data -> stream.publishRetransmission(permissionRequest, data))
                .map(data -> new Success(permissionId, now()))
                .cast(RetransmissionResult.class)
                .defaultIfEmpty(new DataNotAvailable(permissionId, now()))
                .onErrorResume(error -> {
                    LOGGER.atWarn()
                          .addArgument(permissionId)
                          .setCause(error)
                          .log("Retransmission polling failed for permission {}");
                    return Mono.just(new Failure(permissionId, now(), error.getMessage()));
                });
    }

    private static boolean hasReadings(EtaPlusMeteredData data) {
        List<EtaPlusMeteredData.MeterReading> readings = data.readings();
        return readings != null && !readings.isEmpty();
    }

    private static ZonedDateTime now() {
        return ZonedDateTime.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
    }
}