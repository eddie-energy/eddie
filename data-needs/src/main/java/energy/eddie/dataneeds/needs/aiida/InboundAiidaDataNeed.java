// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "inbound_aiida_data_need", schema = "data_needs")
@DiscriminatorValue(InboundAiidaDataNeed.DISCRIMINATOR_VALUE)
@SuppressWarnings("NullAway")
public class InboundAiidaDataNeed extends AiidaDataNeed {
    public static final String DISCRIMINATOR_VALUE = "inbound-aiida";

    @SuppressWarnings("NullAway.Init")
    public InboundAiidaDataNeed() {
        // Default constructor for JPA
    }
}