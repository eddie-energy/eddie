package energy.eddie.aiida.datasources.sga;

import energy.eddie.aiida.datasources.MqttDataSourceAdapter;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SmartGatewaysAdapter extends MqttDataSourceAdapter<SmartGatewaysDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartGatewaysAdapter.class);
    private static final String DSMR_TARIFF_LOW = "0001";

    /**
     * Creates the datasource for the Smart Gateways Adapter. It connects to the specified MQTT broker and expects that the
     * adapter publishes its JSON messages on the specified topic. Any OBIS code without a time field will be assigned a
     * Unix timestamp of 0.
     *
     * @param dataSource The entity of the data source.
     */
    public SmartGatewaysAdapter(SmartGatewaysDataSource dataSource) {
        super(dataSource, LOGGER);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        LOGGER.trace("Topic {} new message: {}", topic, message);
        try {
            var adapterMessage = SmartGatewaysAdapterValueDeserializer.deserialize(message.getPayload());
            List<AiidaRecordValue> aiidaRecordValues = new ArrayList<>();

            SmartGatewaysAdapterMessageField powerCurrentlyDelivered = adapterMessage.powerCurrentlyDelivered();
            SmartGatewaysAdapterMessageField powerCurrentlyReturned = adapterMessage.powerCurrentlyReturned();
            SmartGatewaysAdapterMessageField electricityDelivered;
            SmartGatewaysAdapterMessageField electricityReturned;

            if (Objects.equals(adapterMessage.electricityTariff().value(), DSMR_TARIFF_LOW)) {
                electricityDelivered = adapterMessage.electricityDeliveredTariff1();
                electricityReturned = adapterMessage.electricityReturnedTariff1();
            } else {
                electricityDelivered = adapterMessage.electricityDeliveredTariff2();
                electricityReturned = adapterMessage.electricityReturnedTariff2();
            }

            addAiidaRecordValue(aiidaRecordValues, electricityDelivered);
            addAiidaRecordValue(aiidaRecordValues, electricityReturned);
            addAiidaRecordValue(aiidaRecordValues, powerCurrentlyDelivered);
            addAiidaRecordValue(aiidaRecordValues, powerCurrentlyReturned);

            emitAiidaRecord(dataSource.asset(), aiidaRecordValues);
        } catch (Exception e) {
            LOGGER.error("Error while deserializing JSON received from adapter. JSON was {}",
                         new String(message.getPayload(), StandardCharsets.UTF_8),
                         e);
        }
    }

    /**
     * Will always throw {@link UnsupportedOperationException}, as this datasource is not designed to publish data.
     *
     * @param token The delivery token associated with the message.
     * @throws UnsupportedOperationException Always thrown, as this datasource is not designed to publish data.
     */
    @Override
    public void deliveryComplete(IMqttToken token) throws UnsupportedOperationException {
        LOGGER.warn(
                "Got deliveryComplete notification, but {} mustn't publish any MQTT messages but just listen. Token was {}",
                SmartGatewaysAdapter.class.getName(),
                token);
        throw new UnsupportedOperationException("The " + SmartGatewaysAdapter.class.getName() + " mustn't publish any MQTT messages");
    }

    private void addAiidaRecordValue(List<AiidaRecordValue> aiidaRecordValues, SmartGatewaysAdapterMessageField recordValue) {
        aiidaRecordValues.add(new AiidaRecordValue(recordValue.rawTag(),
                                                   recordValue.obisCode(),
                                                   String.valueOf(recordValue.value()),
                                                   recordValue.unitOfMeasurement(),
                                                   String.valueOf(recordValue.value()),
                                                   recordValue.unitOfMeasurement()));
    }
}
