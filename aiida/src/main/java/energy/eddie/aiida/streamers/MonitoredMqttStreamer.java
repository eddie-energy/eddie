package energy.eddie.aiida.streamers;

import io.micrometer.core.instrument.*;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class MonitoredMqttStreamer implements MqttCallback {
    private static final Logger log = LoggerFactory.getLogger(MonitoredMqttStreamer.class);

    private final MqttClient client;
    private final MeterRegistry meterRegistry;
    private final String serviceName;

    private final Timer messageLatencyTimer;
    private final Counter errorCounter;
    private final Counter timeoutCounter;
    private final Counter messageCounter;
    private final DistributionSummary deliveryIntervalSummary;

    private final Gauge connectionStatusGauge;

    private volatile boolean isConnected = false;

    private static final AtomicLong lastDeliveryTimestamp = new AtomicLong(0);
    private static volatile boolean isGaugeRegistered = false;

    private long lastSentTimestamp = -1;

    public MonitoredMqttStreamer(String brokerUrl, String clientId, String serviceName, MeterRegistry meterRegistry) throws MqttException {
        this.meterRegistry = meterRegistry;
        this.serviceName = serviceName;
        this.client = new MqttClient(brokerUrl, clientId);
        this.client.setCallback(this);

        Tags tags = Tags.of("service", serviceName);

        this.messageLatencyTimer = Timer.builder("mqtt.message.latency")
                                        .tags(tags)
                                        .register(meterRegistry);

        this.errorCounter = Counter.builder("mqtt.errors")
                                   .tags(tags)
                                   .register(meterRegistry);

        this.timeoutCounter = Counter.builder("mqtt.timeouts")
                                     .tags(tags)
                                     .register(meterRegistry);

        this.messageCounter = Counter.builder("mqtt.messages")
                                     .tags(tags)
                                     .register(meterRegistry);

        this.deliveryIntervalSummary = DistributionSummary.builder("mqtt.delivery.interval.seconds")
                                                          .baseUnit("seconds")
                                                          .tags(tags)
                                                          .publishPercentiles(0.5, 0.95, 0.99)
                                                          .register(meterRegistry);

        this.connectionStatusGauge = Gauge.builder("mqtt.connection.status", this, streamer -> streamer.isConnected ? 1 : 0)
                                          .description("MQTT broker connection status: 1 = connected, 0 = disconnected")
                                          .tag("service", serviceName)
                                          .register(meterRegistry);

        registerGaugeOnce(tags);

        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        this.client.connect(options);
    }

    private synchronized void registerGaugeOnce(Tags tags) {
        if (meterRegistry.find("mqtt_data_delivery_interval_seconds_max").tags(tags).gauge() == null) {
            Gauge.builder("mqtt_data_delivery_interval_seconds_max", lastDeliveryTimestamp::get)
                 .description("Last MQTT message delivery interval in seconds (as seen by publisher)")
                 .tags(tags)
                 .register(meterRegistry);
            isGaugeRegistered = true;
        }
    }

    public void publishMessage(String topic, String payload) throws MqttException {
        log.info("Publishing message to topic: {}", topic);
        long currentTime = System.nanoTime();

        if (lastSentTimestamp > 0) {
            double intervalSeconds = (currentTime - lastSentTimestamp) / 1_000_000_000.0;
            deliveryIntervalSummary.record(intervalSeconds);
            lastDeliveryTimestamp.set((long) intervalSeconds);
        }
        lastSentTimestamp = currentTime;

        long startTime = System.nanoTime();
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            client.publish(topic, message);

            messageLatencyTimer.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            messageCounter.increment();
        } catch (MqttException e) {
            errorCounter.increment();
            throw e;
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("Connected to MQTT broker at {}", serverURI);
        isConnected = true;
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        log.warn("Disconnected from MQTT broker: {}", disconnectResponse.getReasonString());
        isConnected = false;
        errorCounter.increment();
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        log.error("MQTT error occurred: {}", exception.getMessage());
        errorCounter.increment();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        log.debug("Message arrived on topic {}: {}", topic, new String(message.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        if (!token.isComplete()) {
            timeoutCounter.increment();
        }
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        log.debug("MQTT auth packet arrived: reason code = {}", reasonCode);
    }
}
