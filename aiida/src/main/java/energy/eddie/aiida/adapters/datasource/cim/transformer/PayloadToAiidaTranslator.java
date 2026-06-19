// SPDX-FileCopyrightText: 2026 The EDDIE Developers
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.cim.transformer;

import energy.eddie.aiida.models.record.AiidaRecordValue;

import java.util.List;
import java.util.Optional;

/**
 * Adapter point for translating arbitrary MQTT payloads to AIIDA record values.
 * Implementations should return an empty Optional if they cannot translate the payload.
 */
public interface PayloadToAiidaTranslator {
    Optional<List<AiidaRecordValue>> tryTranslate(String payload);
}
