package energy.eddie.aiida.adapters.datasource.cim;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.cim.CimDataSource;
import energy.eddie.aiida.schemas.cim.v1_04.utils.CimUtil;
import energy.eddie.cim.v1_04.rtd.TimeSeries;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CimAdapter extends MqttDataSourceAdapter<CimDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimAdapter.class);
    private final ObjectMapper mapper;

    public CimAdapter(CimDataSource dataSource, ObjectMapper mapper, MqttConfiguration mqttConfiguration) {
        super(dataSource, LOGGER, mqttConfiguration);
        this.mapper = mapper;
    }


    @Override
    public void messageArrived(String topic, MqttMessage message) {
        LOGGER.trace("Topic {} new message: {}", topic, message);

        try {
            var cimTimeSeries = mapper.readValue(message.getPayload(), TimeSeries.class);

            emitAiidaRecord(dataSource.asset(), CimUtil.timeSeriesToAiidaRecordValues(cimTimeSeries));
        } catch (IOException e) {
            LOGGER.error("Error while deserializing JSON received from adapter. JSON was {}",
                         new String(message.getPayload(), StandardCharsets.UTF_8),
                         e);
        }
    }
}
