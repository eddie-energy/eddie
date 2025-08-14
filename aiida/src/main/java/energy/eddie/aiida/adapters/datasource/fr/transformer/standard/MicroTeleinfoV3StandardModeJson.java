package energy.eddie.aiida.adapters.datasource.fr.transformer.standard;

import com.fasterxml.jackson.annotation.JsonIgnore;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3DataField;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3Json;

import java.util.HashMap;
import java.util.Map;

public record MicroTeleinfoV3StandardModeJson(
        @JsonIgnore Map<String, MicroTeleinfoV3DataField> energyData) implements MicroTeleinfoV3Json {

    public MicroTeleinfoV3StandardModeJson {
        energyData = new HashMap<>();
    }

    @Override
    public void putEnergyData(String key, MicroTeleinfoV3DataField value) {
        this.energyData.put(key, value);
    }
}