// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.examples.exampleapp.kafka.serdes;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class PermissionMarketDocumentSerde implements Serde<PermissionEnvelope> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionMarketDocumentSerde.class);
    private final ObjectMapper mapper;

    public PermissionMarketDocumentSerde(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Serializer<PermissionEnvelope> serializer() {
        throw new UnsupportedOperationException("This Serde doesn't support serialization!");
    }

    @Override
    public Deserializer<PermissionEnvelope> deserializer() {
        return (topic, data) -> {
            try {
                return mapper.readValue(data, PermissionEnvelope.class);
            } catch (JacksonException e) {
                LOGGER.error("Error while deserializing ConnectionStatusMessage.", e);
                return null;
            }
        };
    }
}
