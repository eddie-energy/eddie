// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
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
     * Returns the set of identifiers for the data that should be shared by the AIIDA instance.
     */
    Set<ObisCode> dataTags();

    /**
     * Returns the schema for the outgoing data
     *
     * @see AiidaSchema
     */
    Set<AiidaSchema> schemas();

    /**
     * Returns the schedule in cron format, at which the AIIDA instance should send data.
     */
    CronExpression transmissionSchedule();

    /**
     * Returns the type of the Data Need
     */
    String type();
}
