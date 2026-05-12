// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusOperationExceptions.RateLimitException;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.CommonPollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;

/**
 * Polls validated historical data from the ETA Plus API for active permission requests.
 * Invoked by {@link energy.eddie.regionconnector.shared.services.CommonFutureDataService}
 * on a cron schedule for each ACCEPTED permission request whose data need is validated
 * historical data.
 */
@Service
public class PollingService implements CommonPollingService<DePermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);

    private final EtaPlusApiClient apiClient;
    private final ValidatedHistoricalDataStream stream;
    private final Outbox outbox;

    public PollingService(EtaPlusApiClient apiClient, ValidatedHistoricalDataStream stream, Outbox outbox) {
        this.apiClient = apiClient;
        this.stream = stream;
        this.outbox = outbox;
    }

    @Override
    public void pollTimeSeriesData(DePermissionRequest permissionRequest) {
        permissionRequest.accessToken().ifPresentOrElse(
                token -> fetchAndPublish(permissionRequest, token),
                () -> LOGGER.atWarn()
                            .addArgument(permissionRequest::permissionId)
                            .log("Skipping poll for permission {}: no access token on permission request")
        );
    }

    @Override
    public boolean isActiveAndNeedsToBeFetched(DePermissionRequest permissionRequest) {
        LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
        if (permissionRequest.start().isAfter(today)) {
            return false;
        }
        return permissionRequest.latestMeterReadingEndDate()
                                .map(today::isAfter)
                                .orElse(true);
    }

    private void fetchAndPublish(DePermissionRequest pr, String accessToken) {
        LOGGER.atInfo()
              .addArgument(pr::permissionId)
              .log("Polling validated historical data for permission request {}");

        apiClient.fetchMeteredData(pr, accessToken)
                 .subscribe(
                         data -> stream.publish(pr, data),
                         error -> handleError(pr.permissionId(), error)
                 );
    }

    private void handleError(String permissionId, Throwable error) {
        if (error instanceof RateLimitException) {
            LOGGER.atWarn()
                  .addArgument(permissionId)
                  .log("Rate limit exceeded while polling permission {}, marking as UNABLE_TO_SEND for retry");
            commitSafely(permissionId, PermissionProcessStatus.UNABLE_TO_SEND);
        } else if (error instanceof WebClientResponseException.Forbidden) {
            LOGGER.atWarn()
                  .addArgument(permissionId)
                  .log("Permission {} appears to be revoked while polling, emitting REVOKED");
            commitSafely(permissionId, PermissionProcessStatus.REVOKED);
        } else {
            LOGGER.atError()
                  .addArgument(permissionId)
                  .setCause(error)
                  .log("Error polling validated historical data for permission {}");
            commitSafely(permissionId, PermissionProcessStatus.UNABLE_TO_SEND);
        }
    }

    private void commitSafely(String permissionId, PermissionProcessStatus status) {
        try {
            outbox.commit(new SimpleEvent(permissionId, status));
        } catch (Exception ex) {
            LOGGER.atError()
                  .addArgument(status)
                  .addArgument(permissionId)
                  .setCause(ex)
                  .log("Failed to persist {} event for permission {}");
        }
    }
}