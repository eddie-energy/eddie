// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission.dataneed;

import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;

public class AiidaLocalDataNeedFactory {
    private AiidaLocalDataNeedFactory() {}

    public static AiidaLocalDataNeed create(AiidaDataNeed dataNeed) {
        return switch (dataNeed) {
            case InboundAiidaDataNeed inbound -> new InboundAiidaLocalDataNeed(inbound);
            case OutboundAiidaDataNeed outbound -> new OutboundAiidaLocalDataNeed(outbound);
            default -> throw new IllegalArgumentException("Unknown AiidaDataNeed type: " + dataNeed.getClass());
        };
    }
}
