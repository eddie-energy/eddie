// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import energy.eddie.api.agnostic.aiida.ObisCode;

import java.util.List;

public class AiidaRecordValidator {
    private AiidaRecordValidator() {
        // Util Class
    }

    public static List<String> checkInvalidDataTags(AiidaRecord rec) {
        return rec.aiidaRecordValues()
                  .stream()
                  .filter(aiidaRecordValue -> aiidaRecordValue.dataTag() == ObisCode.UNKNOWN)
                  .map(AiidaRecordValue::rawTag)
                  .toList();
    }
}
