// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v0_82.outbound;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import reactor.core.publisher.Flux;

/**
 * Sets a stream containing permission market documents, which can be subscribed to, to propagate the permission market documents to the eligible party.
 * A permission market document contains status changes of a permission request.
 */
public interface PermissionMarketDocumentOutboundConnector {
    /**
     * Gets a flux of permission market documents
     *
     * @param permissionMarketDocumentStream the permission market documents
     */
    void setPermissionMarketDocumentStream(Flux<PermissionEnvelope> permissionMarketDocumentStream);
}
