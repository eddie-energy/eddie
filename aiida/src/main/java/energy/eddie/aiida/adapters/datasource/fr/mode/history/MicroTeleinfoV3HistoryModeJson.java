package energy.eddie.aiida.adapters.datasource.fr.mode.history;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.adapters.datasource.fr.mode.MicroTeleinfoV3DataField;
import energy.eddie.aiida.adapters.datasource.fr.mode.MicroTeleinfoV3Json;

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
public record MicroTeleinfoV3HistoryModeJson(@JsonProperty("MOTDETAT") MicroTeleinfoV3DataField motdetat,
                                             @JsonProperty("ADCO") MicroTeleinfoV3DataField adco,
                                             @JsonProperty("OPTARIF") MicroTeleinfoV3DataField optarif,
                                             @JsonProperty("ISOUSC") MicroTeleinfoV3DataField isousc,
                                             @JsonProperty("BASE") MicroTeleinfoV3DataField base,
                                             @JsonProperty("PTEC") MicroTeleinfoV3DataField ptec,
                                             @JsonProperty("IINST") MicroTeleinfoV3DataField iinst,
                                             @JsonProperty("IMAX") MicroTeleinfoV3DataField imax,
                                             @JsonProperty("PAPP") MicroTeleinfoV3DataField papp,
                                             @JsonProperty("HHPHC") MicroTeleinfoV3DataField hhphc) implements MicroTeleinfoV3Json {
}