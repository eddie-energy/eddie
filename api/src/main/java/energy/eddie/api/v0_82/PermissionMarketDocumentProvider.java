// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v0_82;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import reactor.core.publisher.Flux;

public interface PermissionMarketDocumentProvider extends AutoCloseable {
    /**
     * Data stream of all PermissionMarketDocument updates created by this region connector. The
     * PermissionMarketDocument will contain the new state of the permission in the process
     *
     * @return PermissionMarketDocument stream that can be consumed only once
     */
    Flux<PermissionEnvelope> getPermissionMarketDocumentStream();
}
