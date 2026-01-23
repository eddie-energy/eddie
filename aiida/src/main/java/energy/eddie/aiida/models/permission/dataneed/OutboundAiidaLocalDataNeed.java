// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission.dataneed;

import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(OutboundAiidaDataNeed.DISCRIMINATOR_VALUE)
@SuppressWarnings("NullAway")
public class OutboundAiidaLocalDataNeed extends AiidaLocalDataNeed {
    @SuppressWarnings("NullAway.Init")
    protected OutboundAiidaLocalDataNeed() {
    }

    public OutboundAiidaLocalDataNeed(AiidaDataNeed dataNeed) {
        super(dataNeed);
    }
}