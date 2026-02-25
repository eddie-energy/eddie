// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.aiida.ObisCode;
import org.springframework.scheduling.support.CronExpression;

import java.util.Set;
import java.util.UUID;

public interface AiidaDataNeedInterface {
    /**
     * Returns the kind of asset the data is retrieved from
     *
     * @see AiidaAsset
     */
    AiidaAsset asset();

    /**
     * Returns the data need ID
     */
    UUID dataNeedId();

    /**
     * Returns the set of identifiers for the data that should be shared.
     */
    Set<ObisCode> dataTags();

    /**
     * Returns the schema for the data
     *
     * @see AiidaSchema
     */
    Set<AiidaSchema> schemas();

    /**
     * Returns the schedule in cron format, at which data should be sent.
     */
    CronExpression transmissionSchedule();

    /**
     * Returns whether the receiving party should acknowledge the reception of the data.
     * If true, the receiving party is expected to send an acknowledgment envelope back to the sender after receiving the data.
     * If false, no acknowledgment is expected.
     */
    boolean isAcknowledgementRequired();

    /**
     * Returns the type of the Data Need
     */
    String type();
}
