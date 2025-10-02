package energy.eddie.aiida.dtos.datasource.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;

@SuppressWarnings({"NullAway.Init"})
public abstract class MqttDataSourceDto extends DataSourceDto {
    @JsonProperty
    protected String internalHost;
    @JsonProperty
    protected String externalHost;
    @JsonProperty
    protected String subscribeTopic;
    @JsonProperty
    protected String username;
    @JsonProperty
    protected String password;

    public String internalHost() {
        return internalHost;
    }

    public String externalHost() {
        return externalHost;
    }

    public String subscribeTopic() {
        return subscribeTopic;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }
}
