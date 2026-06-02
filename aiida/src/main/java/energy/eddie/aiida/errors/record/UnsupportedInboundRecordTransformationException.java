// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.record;

import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.api.agnostic.aiida.AiidaSchema;

public class UnsupportedInboundRecordTransformationException extends Exception {
    public UnsupportedInboundRecordTransformationException(
            AiidaSchema sourceSchema,
            InboundMessageFormat targetFormat
    ) {
        super("Inbound record transformation is not supported for source schema %s and target format %s."
                      .formatted(sourceSchema, targetFormat));
    }
}
