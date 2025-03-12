package energy.eddie.aiida.datasources.fr;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

/**
 * Meaning of each value:
 *
 * @param motdetat Meter status
 * @param adco     Meter’s address
 * @param optarif  Chosen tariff option
 * @param isousc   Subscribed intensity in unit A
 * @param base     Base index option in unit Wh
 * @param ptec     Current Tariff period
 * @param iinst    Instantaneous Intensity (effective current) in unit A
 * @param imax     Maximum intensity called in unit A
 * @param papp     Apparent power in unit VA (S (in VA), rounded up to the nearest ten)
 * @param hhphc    Peak hours Off-peak hours
 */
public record MicroTeleinfoV3AdapterJson(@JsonProperty("MOTDETAT") TeleinfoDataField motdetat,
                                         @JsonProperty("ADCO") TeleinfoDataField adco,
                                         @JsonProperty("OPTARIF") TeleinfoDataField optarif,
                                         @JsonProperty("ISOUSC") TeleinfoDataField isousc,
                                         @JsonProperty("BASE") TeleinfoDataField base,
                                         @JsonProperty("PTEC") TeleinfoDataField ptec,
                                         @JsonProperty("IINST") TeleinfoDataField iinst,
                                         @JsonProperty("IMAX") TeleinfoDataField imax,
                                         @JsonProperty("PAPP") TeleinfoDataField papp,
                                         @JsonProperty("HHPHC") TeleinfoDataField hhphc) {
    public record TeleinfoDataField(String raw, @JsonProperty("value") Object value,
                                    UnitOfMeasurement unitOfMeasurement, ObisCode mappedObisCode) {}
}