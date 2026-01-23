// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.fr.transformer;

import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.Optional;

public class MicroTeleinfoV3DataFieldDeserializer extends StdDeserializer<MicroTeleinfoV3DataField> {
    public MicroTeleinfoV3DataFieldDeserializer() {
        super(MicroTeleinfoV3DataField.class);
    }

    @Override
    public MicroTeleinfoV3DataField deserialize(JsonParser jp, DeserializationContext context) {
        JsonNode node = jp.readValueAsTree();

        var raw = node.get("raw").asString();
        var valueNode = node.get("value");
        var value = valueNode.asString();

        var unit = determineUnit(jp.currentName());
        var obisCode = determineObisCode(jp.currentName());
        var timestamp = timestampIfAvailable(jp.currentName(), node);

        return new MicroTeleinfoV3DataField(raw, value, unit, obisCode, timestamp);
    }

    private Optional<MicroTeleinfoV3Timestamp> timestampIfAvailable(String fieldName, JsonNode node) {
        return switch (fieldName) {
            case "DATE", "SMAXSN", "SMAXSN1", "SMAXSN2", "SMAXSN3", "SMAXSN-1", "SMAXSN1-1", "SMAXSN2-1", "SMAXSN3-1",
                 "SMAXIN", "SMAXIN-1", "CCASN", "CCASN-1", "CCAIN", "CCAIN-1", "UMOY1", "UMOY2", "UMOY3", "DPM1",
                 "FPM1", "DPM2", "FPM2", "DPM3", "FPM3" -> {
                var timestampNode = node.get("timestamp");
                yield Optional.of(new MicroTeleinfoV3Timestamp(timestampNode.get("dst").asString(),
                                                               timestampNode.get("date").asString()));
            }
            default -> Optional.empty();
        };
    }

    private UnitOfMeasurement determineUnit(String fieldName) {
        return switch (fieldName) {
            case "ISOUSC", "IINST", "IMAX", "IRMS1", "IRMS2", "IRMS3" -> UnitOfMeasurement.AMPERE;
            case "ADSC", "VTIC", "DATE", "NGTF", "LTARF", "STGE", "DPM1", "FPM1", "DPM2", "FPM2", "DPM3", "FPM3",
                 "MSG1", "MSG2", "PRM", "RELAIS", "NTARF", "NJOURF", "NJOURF+1", "PJOURF+1", "PPOINTE" ->
                    UnitOfMeasurement.NONE;
            case "URMS1", "URMS2", "URMS3", "UMOY1", "UMOY2", "UMOY3" -> UnitOfMeasurement.VOLT;
            case "PAPP", "SINSTS", "SINSTS1", "SINSTS2", "SINSTS3", "SMAXSN", "SMAXSN1", "SMAXSN2", "SMAXSN3",
                 "SMAXSN-1", "SMAXSN1-1", "SMAXSN2-1", "SMAXSN3-1", "SINSTI", "SMAXIN", "SMAXIN-1" ->
                    UnitOfMeasurement.VOLT_AMPERE;
            case "PREF", "PCOUP" -> UnitOfMeasurement.KILO_VOLT_AMPERE;
            case "ERQ1", "ERQ2", "ERQ3", "ERQ4" -> UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR;
            case "CCASN", "CCASN-1", "CCAIN", "CCAIN-1" -> UnitOfMeasurement.WATT;
            case "BASE", "EAST", "EASF01", "EASF02", "EASF03", "EASF04", "EASF05", "EASF06", "EASF07", "EASF08",
                 "EASF09", "EASF10", "EASD01", "EASD02", "EASD03", "EASD04", "EAIT" -> UnitOfMeasurement.WATT_HOUR;
            default -> UnitOfMeasurement.UNKNOWN;
        };
    }

    private ObisCode determineObisCode(String fieldName) {
        return switch (fieldName) {
            case "BASE", "EAST" -> ObisCode.POSITIVE_ACTIVE_ENERGY;
            case "PAPP", "SINSTS" -> ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER;
            case "PRM" -> ObisCode.DEVICE_ID_1;
            default -> ObisCode.UNKNOWN;
        };
    }
}