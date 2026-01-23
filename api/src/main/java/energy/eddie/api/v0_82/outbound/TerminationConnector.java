// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v0_82.outbound;

import energy.eddie.api.utils.Pair;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import reactor.core.publisher.Flux;

/**
 * A termination connector receives permission market documents, which are intended to terminate an already accepted permission request.
 */
public interface TerminationConnector {
    /**
     * A flux of termination documents, which are special permission market documents, and an optional region-connector ID as String.
     *
     * @return A pair of an optional region-connector ID and a termination market document.
     */
    Flux<Pair<String, PermissionEnvelope>> getTerminationMessages();
}
