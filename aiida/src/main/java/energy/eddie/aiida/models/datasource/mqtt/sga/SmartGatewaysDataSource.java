// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.sga;

import energy.eddie.aiida.dtos.datasource.mqtt.sga.SmartGatewaysDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttAccessControlEntry;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SMART_GATEWAYS_ADAPTER)
public class SmartGatewaysDataSource extends MqttDataSource {
    private static final int MAX_CHARACTERS_FOR_SGA_TOPIC_PREFIX = 10;
    private static final int MAX_CHARACTERS_FOR_SECRET = MAX_CHARACTERS_FOR_SGA_TOPIC_PREFIX - TOPIC_PREFIX.length();
    private static final String TOPIC_SUFFIX = "/dsmr/reading/+";

    @SuppressWarnings("NullAway")
    protected SmartGatewaysDataSource() {}

    public SmartGatewaysDataSource(SmartGatewaysDataSourceDto dto, UUID userId) {
        super(dto, userId);
    }

    @Override
    protected void createAccessControlEntry() {
        var topic = TOPIC_PREFIX + id.toString().substring(0, MAX_CHARACTERS_FOR_SECRET) + TOPIC_SUFFIX;
        accessControlEntry = new MqttAccessControlEntry(id, topic);
    }

    @Override
    protected String topicFormattedForUi() {
        return accessControlEntry.topic().replace(TOPIC_SUFFIX, "");
    }
}
