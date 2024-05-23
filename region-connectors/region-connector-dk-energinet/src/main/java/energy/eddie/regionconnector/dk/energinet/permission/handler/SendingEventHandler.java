package energy.eddie.regionconnector.dk.energinet.permission.handler;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.permission.events.DKValidatedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkAcceptedEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkSimpleEvent;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.UnknownHostException;

@Component
public class SendingEventHandler implements EventHandler<DKValidatedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendingEventHandler.class);
    private final Outbox outbox;
    private final EnerginetCustomerApi customerApi;
    private final DkPermissionRequestRepository repository;

    public SendingEventHandler(
            Outbox outbox,
            EventBus eventBus,
            EnerginetCustomerApi customerApi,
            DkPermissionRequestRepository repository
    ) {
        this.outbox = outbox;
        this.customerApi = customerApi;
        this.repository = repository;
        eventBus.filteredFlux(DKValidatedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(DKValidatedEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        var pr = repository.getByPermissionId(permissionId);
        customerApi.accessToken(pr.refreshToken())
                   .subscribe(
                           accessToken -> commitAccepted(permissionId, accessToken),
                           error -> commitErrorStatus(permissionId, error)
                   );
    }

    private void commitAccepted(String permissionId, String accessToken) {
        LOGGER.info("Got accepted permission request {}", permissionId);
        outbox.commit(new DkSimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        outbox.commit(new DkAcceptedEvent(permissionId, accessToken));
    }

    private void commitErrorStatus(String permissionId, Throwable throwable) {
        switch (throwable) {
            case HttpClientErrorException.TooManyRequests ignored -> {
                LOGGER.info("Too many requests send to api when sending permission request {}",
                            permissionId,
                            throwable);
                outbox.commit(new DkSimpleEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND));
            }
            case WebClientResponseException.Unauthorized ignored -> unauthorized(throwable, permissionId);
            case HttpClientErrorException.Unauthorized ignored -> unauthorized(throwable, permissionId);
            case WebClientRequestException webClientRequestException
                    when webClientRequestException.contains(UnknownHostException.class) -> {
                LOGGER.info("Unknown host exception occurred when sending permission request {}",
                            permissionId,
                            throwable);
                outbox.commit(new DkSimpleEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND));
            }
            case Throwable ignored -> {
                LOGGER.warn("Got unknown error for permission request {}", permissionId, throwable);
                commitInvalidEvent(permissionId);
            }
        }
    }

    private void unauthorized(Throwable throwable, String permissionId) {
        LOGGER.info("Got unauthorized response for permission request {}", permissionId, throwable);
        commitInvalidEvent(permissionId);
    }

    private void commitInvalidEvent(String permissionId) {
        outbox.commit(new DkSimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        outbox.commit(new DkSimpleEvent(permissionId, PermissionProcessStatus.INVALID));
    }
}
