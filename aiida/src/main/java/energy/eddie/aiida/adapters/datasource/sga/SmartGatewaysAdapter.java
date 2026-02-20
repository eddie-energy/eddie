// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.sga;

import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysTopic;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import jakarta.annotation.Nullable;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SmartGatewaysAdapter extends MqttDataSourceAdapter<SmartGatewaysDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartGatewaysAdapter.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    final Map<SmartGatewaysTopic, String> batchBuffer = new EnumMap<>(SmartGatewaysTopic.class);
    private final String topicPrefix;
    private final List<SmartGatewaysTopic> expectedTopics;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    @Nullable
    private ScheduledFuture<?> timeoutFuture = null;

    /**
     * Creates the datasource for the Smart Gateways Adapter. It connects to the specified MQTT broker and expects that the
     * adapter publishes its JSON messages on the specified topic. Any OBIS code without a time field will be assigned a
     * Unix timestamp of 0.
     *
     * @param dataSource The entity of the data source.
     */
    public SmartGatewaysAdapter(SmartGatewaysDataSource dataSource, MqttConfiguration mqttConfiguration) {
        super(dataSource, LOGGER, mqttConfiguration);
        this.topicPrefix = topicPrefixOf(dataSource.topic());
        this.expectedTopics = Arrays.stream(SmartGatewaysTopic.values())
                                    .filter(SmartGatewaysTopic::isExpected)
                                    .toList();
    }

    @Override
    public synchronized void messageArrived(String topic, MqttMessage message) {
        LOGGER.trace("Topic {} new message: {}", topic, message);
        try {
            var sgaTopic = SmartGatewaysTopic.from(topic, topicPrefix);

            if (sgaTopic.isExpected()) {
                batchBuffer.put(sgaTopic, new String(message.getPayload(), StandardCharsets.UTF_8));

                if (batchBuffer.size() == 1 && (timeoutFuture == null || timeoutFuture.isDone())) {
                    timeoutFuture = scheduler.schedule(() -> {
                        LOGGER.warn("Batch timeout reached. Processing incomplete batch with {} entries.",
                                    batchBuffer.size());
                        synchronized (SmartGatewaysAdapter.this) {
                            processBatch(batchBuffer);
                            batchBuffer.clear();
                        }
                    }, TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                }

                if (batchBuffer.keySet().containsAll(expectedTopics)) {
                    if (timeoutFuture != null) {
                        timeoutFuture.cancel(false);
                    }
                    processBatch(batchBuffer);
                    batchBuffer.clear();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while processing message from topic {}: {}", topic, e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        super.close();
        scheduler.shutdown();
    }

    private void addAiidaRecordValue(
            List<AiidaRecordValue> aiidaRecordValues,
            SmartGatewaysAdapterMessageField recordValue
    ) {
        aiidaRecordValues.add(new AiidaRecordValue(recordValue.rawTag(),
                                                   recordValue.obisCode(),
                                                   String.valueOf(recordValue.value()),
                                                   recordValue.unitOfMeasurement(),
                                                   String.valueOf(recordValue.value()),
                                                   recordValue.unitOfMeasurement()));
    }

    private void processBatch(Map<SmartGatewaysTopic, String> batch) {
        var adapterMessage = SmartGatewaysAdapterValueDeserializer.deserialize(batch);
        List<AiidaRecordValue> aiidaRecordValues = new ArrayList<>();

        var powerCurrentlyDelivered = adapterMessage.powerCurrentlyDelivered();
        var powerCurrentlyReturned = adapterMessage.powerCurrentlyReturned();
        var electricityDelivered = adapterMessage.electricityDelivered();
        var electricityReturned = adapterMessage.electricityReturned();

        addAiidaRecordValue(aiidaRecordValues, electricityDelivered);
        addAiidaRecordValue(aiidaRecordValues, electricityReturned);
        addAiidaRecordValue(aiidaRecordValues, powerCurrentlyDelivered);
        addAiidaRecordValue(aiidaRecordValues, powerCurrentlyReturned);

        emitAiidaRecord(aiidaRecordValues);
        batchBuffer.clear();
    }

    private String topicPrefixOf(String mqttSubscribeTopic) {
        return Arrays.stream(mqttSubscribeTopic.split("/")).findFirst().orElse("");
    }
}
