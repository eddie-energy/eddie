package energy.eddie.aiida.adapters.datasource.fr.transformer.history;

import com.fasterxml.jackson.annotation.JsonIgnore;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3DataField;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3Json;

import java.util.HashMap;
import java.util.Map;

/**
 * Meaning of each value:
 * <p>
 * <i>motdetat</i> Meter status<br>
 * <i>adco</i> Meter’s address<br>
 * <i>optarif</i> Chosen tariff option<br>
 * <i>isousc</i> Subscribed intensity in unit A<br>
 * <i>base</i> Base index option in unit Wh<br>
 * <i>ptec</i> Current Tariff period<br>
 * <i>iinst</i> Instantaneous Intensity (effective current) in unit A<br>
 * <i>imax</i> Maximum intensity called in unit A<br>
 * <i>papp</i> Apparent power in unit VA (S (in VA), rounded up to the nearest ten)<br>
 * <i>hhphc</i> Peak hours Off-peak hours<br>
 * </p>
 */
public record MicroTeleinfoV3HistoryModeJson(
        @JsonIgnore Map<String, MicroTeleinfoV3DataField> energyData) implements MicroTeleinfoV3Json {

    public MicroTeleinfoV3HistoryModeJson {
        energyData = new HashMap<>();
    }

    @Override
    public void putEnergyData(String key, MicroTeleinfoV3DataField value) {
        energyData.put(key, value);
    }
}