package energy.eddie.aiida.adapters.datasource.fr.transformer;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.Map;

public interface MicroTeleinfoV3Json {
    @JsonAnySetter
    void putEnergyData(String key, MicroTeleinfoV3DataField value);

    Map<String, MicroTeleinfoV3DataField> energyData();
}
