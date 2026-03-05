// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.mqtt.inbound.InboundDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttAccessControlEntry;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.MqttUser;
import energy.eddie.aiida.models.datasource.mqtt.SecretGenerator;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@SecondaryTable(name = InboundDataSource.TABLE_NAME)
@DiscriminatorValue(DataSourceType.Identifiers.INBOUND)
public class InboundDataSource extends MqttDataSource {
    protected static final String TABLE_NAME = "data_source_mqtt_inbound";

    @Column(name = "access_code", table = TABLE_NAME)
    @Schema(description = "The access code to retrieve the inbound data.")
    @JsonProperty
    protected String accessCode;

    @OneToOne(mappedBy = "dataSource")
    protected Permission permission;

    @Transient
    @JsonIgnore
    private MqttStreamingConfig config;

    @SuppressWarnings("NullAway")
    protected InboundDataSource() {}

    public InboundDataSource(InboundDataSourceDto dto, UUID userId, Permission permission) {
        this(dto, userId, permission, SecretGenerator.generate());
    }

    public InboundDataSource(InboundDataSourceDto dto, UUID userId, Permission permission, String accessCode) {
        super(dto, userId);
        this.permission = permission;

        this.config = Objects.requireNonNull(permission.mqttStreamingConfig());

        this.internalHost = config.serverUri();
        this.externalHost = config.serverUri();

        this.accessCode = accessCode;
    }

    public String accessCode() {
        return accessCode;
    }

    public Permission permission() {
        return permission;
    }

    @Nullable
    public String acknowledgementTopic() {
        return config != null ? config.acknowledgementTopic() : null;
    }

    @Override
    protected void createMqttUser() {
        this.user = new MqttUser(config.username().toString(), config.password());
    }

    @Override
    protected void createAccessControlEntry() {
        this.accessControlEntry = new MqttAccessControlEntry(config.username().toString(), config.dataTopic());
    }

    public static class Builder {
        private final InboundDataSourceDto dataSourceDto;
        private final UUID userId;
        private final Permission permission;

        @SuppressWarnings("NullAway")
        public Builder(Permission permission) {
            this.userId = Objects.requireNonNull(permission.userId());

            this.permission = Objects.requireNonNull(permission);

            var dataNeed = Objects.requireNonNull(permission.dataNeed());
            this.dataSourceDto = new InboundDataSourceDto(dataNeed.asset(), permission.id());
        }

        public InboundDataSource build() {
            return new InboundDataSource(dataSourceDto, userId, permission);
        }
    }
}
