// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.auth.EtaAuthService;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.AuthenticationException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.DeserializationException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusBadRequestException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusForbiddenException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusNotFoundException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusServerException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusClientExceptions.EtaPlusTimeoutException;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusOperationExceptions.RateLimitException;
import energy.eddie.regionconnector.de.eta.permission.credentials.DePermissionCredentials;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionCredentialsRepository;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.providers.AccountingPointDataStream;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusAccountingPointData;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Handles {@link AcceptedEvent}s for permission requests whose data need is
 * {@link AccountingPointDataNeed}. Performs a single-shot fetch against the
 * accounting-point endpoint, publishes the payload to {@link AccountingPointDataStream},
 * and commits the corresponding terminal event.
 *
 * <p>On 401 the handler attempts one in-flight refresh of the customer's access token
 * using the refresh token persisted on the {@link AcceptedEvent}; the refreshed token
 * is held only for the duration of the retry and is not persisted.
 */
@Component
public class AccountingPointDataHandler implements EventHandler<AcceptedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPointDataHandler.class);

    private final DePermissionRequestRepository repository;
    private final DataNeedsService dataNeedsService;
    private final EtaPlusApiClient apiClient;
    private final EtaAuthService authService;
    private final AccountingPointDataStream stream;
    private final Outbox outbox;
    private final DePermissionCredentialsRepository credentialsRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AccountingPointDataHandler(
            EventBus eventBus,
            DePermissionRequestRepository repository,
            DataNeedsService dataNeedsService,
            EtaPlusApiClient apiClient,
            EtaAuthService authService,
            AccountingPointDataStream stream,
            Outbox outbox,
            DePermissionCredentialsRepository credentialsRepository
    ) {
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
        this.apiClient = apiClient;
        this.authService = authService;
        this.stream = stream;
        this.outbox = outbox;
        this.credentialsRepository = credentialsRepository;
        eventBus.filteredFlux(AcceptedEvent.class).subscribe(this::accept);
    }

    @Override
    public void accept(AcceptedEvent event) {
        Optional<DePermissionRequest> optionalPr = repository.findByPermissionId(event.permissionId());
        if (optionalPr.isEmpty()) {
            LOGGER.warn("Permission request not found for id: {}", event.permissionId());
            return;
        }
        DePermissionRequest pr = optionalPr.get();

        DataNeed dataNeed = dataNeedsService.getById(pr.dataNeedId());
        if (!(dataNeed instanceof AccountingPointDataNeed)) {
            return;
        }

        DePermissionCredentials creds = credentialsRepository.findByPermissionId(pr.permissionId())
                .orElse(null);
        if (creds == null) {
            LOGGER.warn("No credentials found for permission {}", pr.permissionId());
            commitSafely(pr.permissionId(), PermissionProcessStatus.UNABLE_TO_SEND);
            return;
        }

        apiClient.fetchAccountingPointData(pr, creds.accessToken())
                 .onErrorResume(AuthenticationException.class, ex -> recoverWithRefreshToken(pr, creds.refreshToken(), ex))
                 .subscribe(
                         data -> {
                             stream.publish(pr, data);
                             commitSafely(pr.permissionId(), PermissionProcessStatus.FULFILLED);
                         },
                         error -> handleError(pr.permissionId(), error)
                 );
    }

    private Mono<EtaPlusAccountingPointData> recoverWithRefreshToken(
            DePermissionRequest pr,
            @Nullable String refreshToken,
            AuthenticationException originalError
    ) {
        if (refreshToken == null) {
            LOGGER.warn("401 fetching accounting point data for permission {} — no refresh token available",
                    pr.permissionId());
            return Mono.error(originalError);
        }

        return authService.refresh(refreshToken)
                .flatMap(refreshResponse -> {
                    if (!refreshResponse.success() || refreshResponse.getAccessToken() == null) {
                        LOGGER.warn("Refresh of access token failed for permission {}", pr.permissionId());
                        return Mono.error(originalError);
                    }
                    return apiClient.fetchAccountingPointData(pr, refreshResponse.getAccessToken())
                            .onErrorResume(AuthenticationException.class, retryError -> {
                                LOGGER.warn(
                                        "401 fetching accounting point data for permission {} after successful token refresh",
                                        pr.permissionId(), retryError);
                                return Mono.error(retryError);
                            });
                });
    }

    private void handleError(String permissionId, Throwable error) {
        PermissionProcessStatus status = switch (error) {
            case EtaPlusForbiddenException e -> {
                LOGGER.warn(
                        "Forbidden fetching accounting point data for permission {} — verify backend scopes for this metering point",
                        permissionId, e);
                yield PermissionProcessStatus.UNFULFILLABLE;
            }
            case EtaPlusNotFoundException e -> {
                LOGGER.warn("Metering point not found for permission {} — marking UNFULFILLABLE", permissionId, e);
                yield PermissionProcessStatus.UNFULFILLABLE;
            }
            case EtaPlusBadRequestException e -> {
                LOGGER.warn("Bad request fetching accounting point data for permission {} — programmer error",
                        permissionId, e);
                yield PermissionProcessStatus.UNFULFILLABLE;
            }
            case AuthenticationException e -> {
                LOGGER.warn(
                        "Authentication failed fetching accounting point data for permission {} — marking UNABLE_TO_SEND",
                        permissionId, e);
                yield PermissionProcessStatus.UNABLE_TO_SEND;
            }
            case RateLimitException e -> markTransient(permissionId, e);
            case EtaPlusServerException e -> markTransient(permissionId, e);
            case EtaPlusTimeoutException e -> markTransient(permissionId, e);
            case DeserializationException e -> markTransient(permissionId, e);
            default -> {
                LOGGER.warn("Unexpected error fetching accounting point data for permission {} — marking UNABLE_TO_SEND",
                        permissionId, error);
                yield PermissionProcessStatus.UNABLE_TO_SEND;
            }
        };
        commitSafely(permissionId, status);
    }

    private PermissionProcessStatus markTransient(String permissionId, Throwable error) {
        LOGGER.warn("Transient error fetching accounting point data for permission {} — marking UNABLE_TO_SEND",
                permissionId, error);
        return PermissionProcessStatus.UNABLE_TO_SEND;
    }

    private void commitSafely(String permissionId, PermissionProcessStatus status) {
        try {
            outbox.commit(new SimpleEvent(permissionId, status));
        } catch (Exception ex) {
            LOGGER.error("Failed to persist {} event for permission {}", status, permissionId, ex);
        }
    }
}