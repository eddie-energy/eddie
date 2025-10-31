// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas;

import energy.eddie.aiida.errors.formatter.SchemaFormatterException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;

public interface SchemaFormatter {
    AiidaSchema supportedSchema();

    byte[] format(
            AiidaRecord aiidaRecord,
            Permission permission
    ) throws SchemaFormatterException;
}
