// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.client.customer.data;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.exceptions.NoTokenException;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Component
public class CustomerDataClientErrorHandler implements Predicate<Throwable> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerDataClientErrorHandler.class);
    private final Outbox outbox;

    public CustomerDataClientErrorHandler(Outbox outbox) {this.outbox = outbox;}


    @Override
    public boolean test(Throwable e) {
        return e instanceof WebClientResponseException.Unauthorized || e instanceof NoTokenException;
    }

    public Consumer<Throwable> thenRevoke(CdsPermissionRequest permissionRequest) {
        return t -> revoke(permissionRequest, t);
    }

    public void revoke(CdsPermissionRequest permissionRequest, Throwable e) {
        var permissionId = permissionRequest.permissionId();
        LOGGER.atInfo()
              .addArgument(permissionId)
              .log("Revoking permission request {} because of exception while requesting validated historical data", e);
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
    }
}
