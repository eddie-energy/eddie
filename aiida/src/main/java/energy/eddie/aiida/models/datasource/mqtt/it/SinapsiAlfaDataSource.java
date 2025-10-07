package energy.eddie.aiida.models.datasource.mqtt.it;

import energy.eddie.aiida.config.datasource.it.SinapsiAlfaConfiguration;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.errors.SinapsiAlflaEmptyConfigException;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SINAPSI_ALFA)
@SuppressWarnings("NullAway")
public class SinapsiAlfaDataSource extends MqttDataSource {
    @SuppressWarnings("NullAway")
    protected SinapsiAlfaDataSource() {}

    public SinapsiAlfaDataSource(SinapsiAlfaDataSourceDto dto, UUID userId) {
        super(dto, userId);
    }

    public void generateMqttSettings(
            SinapsiAlfaConfiguration config,
            String activationKey
    ) throws SinapsiAlflaEmptyConfigException {
        if (config.mqttUsername().isEmpty() || config.mqttPassword().isEmpty()) {
            throw new SinapsiAlflaEmptyConfigException();
        }

        this.mqttInternalHost = config.mqttHost();
        this.mqttExternalHost = config.mqttHost();
        this.mqttSubscribeTopic = SinapsiAlfaConfiguration.TOPIC_PREFIX
                                  + config.mqttUsername()
                                  + SinapsiAlfaConfiguration.TOPIC_INFIX
                                  + activationKey
                                  + SinapsiAlfaConfiguration.TOPIC_SUFFIX;
        this.mqttUsername = config.mqttUsername();
        this.mqttPassword = config.mqttPassword();
    }

    @Override
    public void setMqttPassword(String password) {
        // ignore, password is fixed
    }
}
