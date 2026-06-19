// SPDX-FileCopyrightText: 2026 The EDDIE Developers
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.cim.transformer;

import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.aiida.adapters.datasource.shelly.transformer.ShellyJson;
import energy.eddie.aiida.adapters.datasource.shelly.transformer.ShellyMeasurement;
import energy.eddie.aiida.models.record.AiidaRecordValue;

import java.util.List;

public class ShellyToAiidaTransformer {
    public static List<AiidaRecordValue> transform(ShellyJson json) {
        return json.params().em()
                   .entrySet()
                   .stream()
                   .flatMap(componentEntry -> componentEntry.getValue()
                                                           .entrySet()
                                                           .stream()
                                                           .map(entry -> new ShellyMeasurement(
                                                                   componentEntry.getKey(),
                                                                   entry.getKey(),
                                                                   String.valueOf(entry.getValue())
                                                           )))
                   .map(SmartMeterAdapterMeasurement::toAiidaRecordValue)
                   .toList();
    }
}
