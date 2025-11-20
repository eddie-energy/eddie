package energy.eddie.regionconnector.aiida.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import energy.eddie.regionconnector.aiida.exceptions.MqttTopicException;
import energy.eddie.regionconnector.aiida.exceptions.PermissionInvalidException;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class MqttMessageCallback implements MqttCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttMessageCallback.class);

    private static final String STATUS_TOPIC_SUFFIX = MqttTopicType.STATUS.baseTopicName();
    private static final String SMART_METER_P1_CIM_SUFFIX =
            AiidaSchema.SMART_METER_P1_CIM.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
    private static final String SMART_METER_P1_RAW_SUFFIX =
            AiidaSchema.SMART_METER_P1_RAW.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());

    private final AiidaPermissionRequestViewRepository permissionRequestViewRepository;
    private final Sinks.Many<AiidaConnectionStatusMessageDto> statusSink;
    private final Sinks.Many<RTDEnvelope> nearRealTimeDataSink;
    private final Sinks.Many<RawDataMessage> rawDataMessageSink;
    private final ObjectMapper objectMapper;

    public MqttMessageCallback(
            AiidaPermissionRequestViewRepository permissionRequestViewRepository,
            Sinks.Many<AiidaConnectionStatusMessageDto> statusSink,
            Sinks.Many<RTDEnvelope> nearRealTimeDataSink,
            Sinks.Many<RawDataMessage> rawDataMessageSink,
            ObjectMapper objectMapper
    ) {
        this.permissionRequestViewRepository = permissionRequestViewRepository;
        this.statusSink = statusSink;
        this.nearRealTimeDataSink = nearRealTimeDataSink;
        this.rawDataMessageSink = rawDataMessageSink;
        this.objectMapper = objectMapper;
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        LOGGER.warn("Disconnected from MQTT broker {}", disconnectResponse);
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        LOGGER.error("Mqtt error occurred", exception);
    }

    @Override
    public void messageArrived(
            String topic,
            MqttMessage message
    ) throws JsonProcessingException, PermissionNotFoundException, PermissionInvalidException, MqttTopicException {
        try {
            if (topic.endsWith(STATUS_TOPIC_SUFFIX)) {
                handleStatusMessage(message);
            } else if (topic.endsWith(SMART_METER_P1_CIM_SUFFIX)) {
                handleSmartMeterP1CimMessage(message);
            } else if (topic.endsWith(SMART_METER_P1_RAW_SUFFIX)) {
                handleSmartMeterP1RawMessage(topic, message);
            } else {
                LOGGER.warn("Received MQTT message on unknown topic {}", topic);
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not process MQTT message on topic {}", topic, e);
            throw e;
        } catch (PermissionNotFoundException | PermissionInvalidException | MqttTopicException e) {
            LOGGER.error("Could not handle MQTT message on topic {}", topic, e);
            throw e;
        }
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        LOGGER.trace("Delivery complete for MqttToken {}", token);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        LOGGER.info("Connected to MQTT broker {}, was because of reconnect: {}", serverURI, reconnect);
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        // Not needed, as no advanced authentication is required
    }

    private void handleStatusMessage(MqttMessage message) throws JsonProcessingException {
        var statusMessage = objectMapper.readValue(message.toString(), AiidaConnectionStatusMessageDto.class);
        var permissionId = statusMessage.permissionId();

        LOGGER.info("Received connection status message for permission {} with status {}",
                    permissionId,
                    statusMessage.status());
        statusSink.tryEmitNext(statusMessage);
    }

    private void handleSmartMeterP1CimMessage(
            MqttMessage message
    ) throws JsonProcessingException, PermissionNotFoundException, PermissionInvalidException {
        var nearRealTimeDataEnvelope = objectMapper.readValue(message.toString(), RTDEnvelope.class);
        var permissionId = nearRealTimeDataEnvelope.getMessageDocumentHeaderMetaInformationPermissionId();
        getAndValidatePermissionRequest(permissionId);

        LOGGER.info("Received near real-time data market document for permission {} and final customer {}",
                    permissionId,
                    nearRealTimeDataEnvelope.getMessageDocumentHeaderMetaInformationFinalCustomerId());
        nearRealTimeDataSink.tryEmitNext(nearRealTimeDataEnvelope);
    }

    private void handleSmartMeterP1RawMessage(
            String topic,
            MqttMessage message
    ) throws PermissionNotFoundException, PermissionInvalidException, MqttTopicException {
        var permissionId = MqttTopic.extractPermissionIdFromTopic(
                topic,
                MqttTopicType.OUTBOUND_DATA,
                AiidaSchema.SMART_METER_P1_RAW
        );
        var permissionRequest = getAndValidatePermissionRequest(permissionId);

        var rawDataMessage = new RawDataMessage(
                permissionRequest,
                new String(message.getPayload(), StandardCharsets.UTF_8)
        );

        LOGGER.info("Received RawDataMessage for permission {} and AIIDA {}",
                    permissionId,
                    permissionRequest.aiidaId());
        rawDataMessageSink.tryEmitNext(rawDataMessage);
    }

    private AiidaPermissionRequest getAndValidatePermissionRequest(
            String permissionId
    ) throws PermissionNotFoundException, PermissionInvalidException {
        var permissionRequest = permissionRequestViewRepository
                .findByPermissionId(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        validatePermissionRequest(permissionRequest);

        return permissionRequest;
    }

    private void validatePermissionRequest(AiidaPermissionRequest permissionRequest) throws PermissionInvalidException {
        validateStatus(permissionRequest);
        validateTimespan(permissionRequest);
    }

    private void validateStatus(AiidaPermissionRequest permissionRequest) throws PermissionInvalidException {
        if (permissionRequest.status() != PermissionProcessStatus.ACCEPTED) {
            throw new PermissionInvalidException(
                    permissionRequest.permissionId(),
                    "Permission status is not ACCEPTED but %s".formatted(permissionRequest.status())
            );
        }
    }

    private void validateTimespan(AiidaPermissionRequest permissionRequest) throws PermissionInvalidException {
        var now = LocalDate.now();

        if (now.isBefore(permissionRequest.start()) || now.isAfter(permissionRequest.end())) {
            throw new PermissionInvalidException(
                    permissionRequest.permissionId(),
                    "Current date is outside of permission timespan (%s - %s)".formatted(permissionRequest.start(),
                                                                                         permissionRequest.end())
            );
        }
    }
}
