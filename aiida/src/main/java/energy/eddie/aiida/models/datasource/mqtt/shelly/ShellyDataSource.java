// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.shelly;

import energy.eddie.aiida.dtos.datasource.mqtt.shelly.ShellyDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttAccessControlEntry;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SHELLY)
public class ShellyDataSource extends MqttDataSource {
    private static final String TOPIC_SUFFIX = "/#";

    @SuppressWarnings("NullAway")
    protected ShellyDataSource() {}

    public ShellyDataSource(ShellyDataSourceDto dto, UUID userId) {
        super(dto, userId);
    }

    @Override
    protected void createAccessControlEntry() {
        var topic = TOPIC_PREFIX + id + TOPIC_SUFFIX;
        accessControlEntry = new MqttAccessControlEntry(id, topic);
    }

    @Override
    protected String topicFormattedForUi() {
        return accessControlEntry.topic().replace(TOPIC_SUFFIX, "");
    }
}
