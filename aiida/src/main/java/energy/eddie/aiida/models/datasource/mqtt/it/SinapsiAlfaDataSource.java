package energy.eddie.aiida.models.datasource.mqtt.it;

import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SINAPSI_ALFA)
@SuppressWarnings("NullAway")
public class SinapsiAlfaDataSource extends MqttDataSource {
    private static final String HOST = "tcp://hstbrk.sghiot.com:1883";
    private static final String TOPIC_SUFFIX = "/";
    private static final String TOPIC_INFIX = "/iomtsgdata/";
    private static final String DUMMY_USERNAME = "oetzi"; // TODO: provided by the user
    private static final String DUMMY_PASSWORD = "oetzi123"; // TODO: provided by the user
    private static final String DUMMY_ACTIVATION_KEY = "my-activiation-key"; // TODO: provided by the user

    @SuppressWarnings("NullAway")
    protected SinapsiAlfaDataSource() {}

    public SinapsiAlfaDataSource(DataSourceDto dto, UUID userId, DataSourceMqttDto dataSourceMqttDto) {
        super(dto, userId, dataSourceMqttDto);

        this.mqttInternalHost = HOST;
        this.mqttExternalHost = HOST;
        this.mqttUsername = DUMMY_USERNAME;
        this.mqttPassword = DUMMY_PASSWORD;
    }

    @Override
    protected void updateMqttSubscribeTopic() {
        this.mqttSubscribeTopic = TOPIC_SUFFIX + DUMMY_USERNAME + TOPIC_INFIX + DUMMY_ACTIVATION_KEY;
    }

    @Override
    public void setMqttPassword(String password) {
        // ignore, password is fixed
    }
}
