// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.retransmission;

import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import reactor.core.publisher.Mono;

public interface RegionConnectorRetransmissionService {

    /**
     * Request retransmission of data
     *
     * @param retransmissionRequest the request specifying the data to be retransmitted
     * @return a {@link Mono} that emits a {@link RetransmissionResult} with the result of the retransmission
     */
    Mono<RetransmissionResult> requestRetransmission(RetransmissionRequest retransmissionRequest);
}
