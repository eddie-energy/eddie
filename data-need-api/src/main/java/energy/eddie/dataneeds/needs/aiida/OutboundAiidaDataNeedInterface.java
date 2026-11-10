// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.ObisCode;

import java.util.Set;

public interface OutboundAiidaDataNeedInterface extends AiidaDataNeedInterface {
    /**
     * Returns the set of identifiers for the data that should be shared.
     */
    Set<ObisCode> dataTags();

    /**
     * Returns the kinds of asset the data may be retrieved from. An empty set allows any asset.
     *
     * @see AiidaAsset
     */
    Set<AiidaAsset> assets();
}
