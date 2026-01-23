// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.retransmission;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import reactor.core.publisher.Mono;

/**
 * Used to poll validated historical data.
 * @param <T> The type of permission request that is used
 */
@FunctionalInterface
public interface PollingFunction<T extends PermissionRequest> {
    /**
     * Used to poll data for a permission request.
     * The timeframe is specified by the retransmission request.
     * Should only work for permission requests that are either accepted or fulfilled.
     * The data need of the permission request has to be a {@link energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed}.
     *
     * @param permissionRequest The permission request for which data should be polled.
     * @param retransmissionRequest Specifies the timeframe of the data.
     * @return If the polling was successful.
     */
    Mono<RetransmissionResult> poll(T permissionRequest, RetransmissionRequest retransmissionRequest);

}
