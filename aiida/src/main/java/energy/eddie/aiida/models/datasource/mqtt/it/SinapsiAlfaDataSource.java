// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.it;

import com.fasterxml.jackson.annotation.JsonIgnore;
import energy.eddie.aiida.config.datasource.it.SinapsiAlfaConfiguration;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.errors.datasource.mqtt.it.SinapsiAlfaEmptyConfigException;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttAccessControlEntry;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.mqtt.MqttConnectionEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SINAPSI_ALFA)
public class SinapsiAlfaDataSource extends MqttDataSource {
    @Transient
    @JsonIgnore
    private String activationKey;

    @Transient
    @JsonIgnore
    private SinapsiAlfaConfiguration config;

    @SuppressWarnings("NullAway")
    protected SinapsiAlfaDataSource() {}

    @SuppressWarnings("NullAway")
    public SinapsiAlfaDataSource(SinapsiAlfaDataSourceDto dto, UUID userId) {
        super(dto, userId);
    }

    public void configure(
            SinapsiAlfaConfiguration config,
            String activationKey
    ) throws SinapsiAlfaEmptyConfigException {
        if (config.mqttUsername().isEmpty() || config.mqttPassword().isEmpty()) {
            throw new SinapsiAlfaEmptyConfigException();
        }

        this.mqttConnection = new MqttConnectionEntity(config.mqttHost(), config.mqttHost());
        this.activationKey = activationKey;
        this.config = config;
    }

    @Override
    public void updatePassword(BCryptPasswordEncoder encoder, String plaintextPassword) {
        // Ignore, as the password is set in the constructor
    }

    @Override
    protected void createMqttUser() {
        this.mqttConnection.createMqttUser(config.mqttUsername(), config.mqttPassword());
    }

    @Override
    protected void createAccessControlEntry() {
        var username = config.mqttUsername();
        var topic = SinapsiAlfaConfiguration.TOPIC_PREFIX
                    + username
                    + SinapsiAlfaConfiguration.TOPIC_INFIX
                    + activationKey
                    + SinapsiAlfaConfiguration.TOPIC_SUFFIX;

        this.accessControlEntry = new MqttAccessControlEntry(username, topic);
    }
}
