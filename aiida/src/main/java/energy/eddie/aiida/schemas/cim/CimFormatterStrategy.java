// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas.cim;

import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;

import java.util.UUID;

/**
 * Strategy interface to create a version-specific CIM RTD Envelope
 *
 * @param <T> The version-specific RTDEnvelope class.
 */
public interface CimFormatterStrategy<T> {
    T toRealTimeDataEnvelope(
            UUID aiidaId,
            AiidaRecord aiidaRecord,
            Permission permission
    ) throws CimSchemaFormatterException;
}
