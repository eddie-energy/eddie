// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.cim;

import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.models.datasource.mqtt.cim.CimDataSource;
import energy.eddie.aiida.schemas.rtd.cim.v1_12.CimStrategy;
import energy.eddie.cim.v1_12.rtd.TimeSeries;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import energy.eddie.aiida.adapters.datasource.cim.transformer.ShellyToAiidaTransformer;
import energy.eddie.aiida.adapters.datasource.cim.transformer.PayloadToAiidaTranslator;

public class CimAdapter extends MqttDataSourceAdapter<CimDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimAdapter.class);
    private final ObjectMapper mapper;
    private final CimStrategy cimStrategy;
    private final List<PayloadToAiidaTranslator> translators;

    public CimAdapter(CimDataSource dataSource,
                      ObjectMapper mapper,
                      MqttConfiguration mqttConfiguration,
                      List<PayloadToAiidaTranslator> translators) {
        super(dataSource, LOGGER, mqttConfiguration);
        this.mapper = mapper;
        this.cimStrategy = new CimStrategy();
        this.translators = List.copyOf(translators);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        LOGGER.trace("Topic {} new message: {}", topic, message);

        try {
            var cimTimeSeries = mapper.readValue(message.getPayload(), TimeSeries.class);

            emitAiidaRecord(cimStrategy.timeSeriesToAiidaRecordValues(cimTimeSeries));
        } catch (JacksonException e) {
            // If the payload is not a CIM TimeSeries, try translator chain (e.g., Shelly JSON -> AIIDA)
            var payload = new String(message.getPayload(), StandardCharsets.UTF_8).trim();

            var translated = translators.stream()
                                        .map(t -> t.tryTranslate(payload))
                                        .flatMap(Optional::stream)
                                        .findFirst();

            if (translated.isPresent()) {
                emitAiidaRecord(translated.get());
            } else {
                LOGGER.error("Error while deserializing JSON received from adapter. JSON was {}",
                             payload,
                             e);
            }
        } catch (CimSchemaFormatterException e) {
            LOGGER.error("Error while serializing the AIIDA record.", e);
        }
    }
}
