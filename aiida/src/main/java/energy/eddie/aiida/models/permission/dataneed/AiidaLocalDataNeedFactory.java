// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission.dataneed;

import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;

public class AiidaLocalDataNeedFactory {
    private AiidaLocalDataNeedFactory() {}

    public static AiidaLocalDataNeed create(AiidaDataNeed dataNeed) {
        return InboundAiidaDataNeed.DISCRIMINATOR_VALUE.equals(dataNeed.type())
                ? new InboundAiidaLocalDataNeed(dataNeed)
                : new OutboundAiidaLocalDataNeed(dataNeed);
    }
}
