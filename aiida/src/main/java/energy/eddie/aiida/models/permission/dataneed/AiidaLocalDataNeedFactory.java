// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission.dataneed;

import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;

public class AiidaLocalDataNeedFactory {
    private AiidaLocalDataNeedFactory() {}

    public static AiidaLocalDataNeed create(AiidaDataNeed dataNeed) {
        return dataNeed.type().equals(InboundAiidaDataNeed.DISCRIMINATOR_VALUE)
                ? new InboundAiidaLocalDataNeed(dataNeed)
                : new OutboundAiidaLocalDataNeed(dataNeed);
    }
}
