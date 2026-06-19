// SPDX-FileCopyrightText: 2026 The EDDIE Developers
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.cim.transformer;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.adapters.datasource.cim.transformer.ShellyPayloadTranslator;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ShellyPayloadTranslatorTest {

    private static final String SWITCH_PAYLOAD = """
            {
              "src": "shellyplugsg3-28372f2c745c",
              "dst": "aiida/1/events",
              "method": "NotifyStatus",
              "params": {
                "ts": 1773420285,
                "switch:0": {
                  "apower": 73.2,
                  "voltage": 235.0,
                  "current": 0.372,
                  "aenergy": { "total": 2067.124 },
                  "ret_aenergy": { "total": 0.0 }
                }
              }
            }
            """;

    @Test
    void shellyPayloadTranslator_translatesGen3Payload() {
        var mapper = ObjectMapperCreatorUtil.mapper();
        var translator = new ShellyPayloadTranslator(mapper);

        var opt = translator.tryTranslate(SWITCH_PAYLOAD);

        assertTrue(opt.isPresent(), "Translator should accept Shelly payload");

        List<AiidaRecordValue> values = opt.get();

        boolean hasApower = values.stream().anyMatch(v -> v.dataTag() == ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L1 && v.rawTag().endsWith("apower") && v.rawUnitOfMeasurement() == UnitOfMeasurement.WATT);
        boolean hasAenergy = values.stream().anyMatch(v -> v.dataTag() == ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L1 && v.rawTag().endsWith("aenergy.total") && v.rawUnitOfMeasurement() == UnitOfMeasurement.WATT_HOUR);

        assertTrue(hasApower, "Expected apower mapped to OBIS and unit");
        assertTrue(hasAenergy, "Expected aenergy.total mapped to OBIS and unit");
    }

    @Test
    void shellyPayloadTranslator_handlesPrefixedShellyNotification() {
      var mapper = ObjectMapperCreatorUtil.mapper();
      var translator = new ShellyPayloadTranslator(mapper);

      var prefixed = "shelly_notification:163 Status change of switch:0: " + SWITCH_PAYLOAD;

      var opt = translator.tryTranslate(prefixed);

      assertTrue(opt.isPresent(), "Translator should accept prefixed Shelly payload");

      List<AiidaRecordValue> values = opt.get();

      boolean hasApower = values.stream().anyMatch(v -> v.dataTag() == ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L1 && v.rawTag().endsWith("apower") && v.rawUnitOfMeasurement() == UnitOfMeasurement.WATT);

      assertTrue(hasApower, "Expected apower mapped when payload is prefixed");
    }
}
