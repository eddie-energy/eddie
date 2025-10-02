package energy.eddie.aiida.dtos.datasource.mqtt.it;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;

@SuppressWarnings({"NullAway.Init"})
public class SinapsiAlfaDataSourceDto extends DataSourceDto {
    @JsonProperty
    protected String activationKey;

    public String activationKey() {
        return activationKey;
    }
}
