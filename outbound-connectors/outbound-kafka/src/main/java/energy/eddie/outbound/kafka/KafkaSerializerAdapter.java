// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.kafka;

import energy.eddie.cim.serde.MessageSerde;
import energy.eddie.cim.serde.SerializationException;
import jakarta.annotation.Nullable;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class KafkaSerializerAdapter implements Serializer<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSerializerAdapter.class);
    private final MessageSerde serde;

    KafkaSerializerAdapter(MessageSerde serde) {
        this.serde = serde;
    }

    @Override
    @Nullable
    public byte[] serialize(String topic, Object data) {
        if (data == null) {
            return null;
        }
        return serialize(data);
    }

    // Sonar wants us to return an empty array, but the kafka implementations return null, so will do the same
    @SuppressWarnings("java:S1168")
    @Nullable
    private byte[] serialize(Object payload) {
        try {
            return serde.serialize(payload);
        } catch (SerializationException e) {
            LOGGER.warn("Could not serialize message of type {}", payload.getClass(), e);
            return null;
        }
    }
}
