// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaContext;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.utils.cron.CronExpressionConverter;
import energy.eddie.dataneeds.utils.cron.CronExpressionDeserializer;
import energy.eddie.dataneeds.utils.cron.CronExpressionSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.scheduling.support.CronExpression;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@SuppressWarnings("NullAway")
public abstract class AiidaDataNeed extends TimeframedDataNeed implements AiidaDataNeedInterface {
    @Column(name = "transmission_schedule", nullable = false)
    @Convert(converter = CronExpressionConverter.class)
    @JsonProperty(required = true)
    @JsonSerialize(using = CronExpressionSerializer.class)
    @JsonDeserialize(using = CronExpressionDeserializer.class)
    @Schema(description = "The schedule in cron format, at which the AIIDA instance should send data.")
    private CronExpression transmissionSchedule;

    @JsonProperty
    @Column(name = "acknowledgement_required", nullable = false)
    @Schema(description = "Whether the receiving party should acknowledge the reception of the data.")
    private boolean acknowledgementRequired = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aiida_data_need_permission_commands",
            joinColumns = @JoinColumn(name = "data_need_id"),
            schema = "data_needs")
    @Column(name = "permission_command")
    @Enumerated(EnumType.STRING)
    @JsonProperty
    @Schema(description = "Permission commands the eligible party is explicitly granted to send.")
    private Set<PermissionCommand.Action> allowedPermissionCommands = Set.of();

    @Schema(description = "Define the schema for the outgoing data (Raw, CIM, Saref)")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aiida_data_need_schemas",
            joinColumns = @JoinColumn(name = "data_need_id"),
            schema = "data_needs")
    @Column(name = "schema")
    @JsonProperty(required = true)
    @Enumerated(EnumType.STRING)
    @NotEmpty(message = "must contain at least one schema")
    private Set<AiidaSchema> schemas;

    @Schema(description = "The kinds of asset the data may be retrieved from ('CONNECTION-AGREEMENT-POINT', 'CONTROLLABLE-UNIT', 'DEDICATED-MEASUREMENT-DEVICE', 'SUBMETER'). An empty or missing list allows any asset.")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aiida_data_need_assets",
            joinColumns = @JoinColumn(name = "data_need_id"),
            schema = "data_needs")
    @Column(name = "asset")
    @JsonProperty
    @Enumerated(EnumType.STRING)
    // Mutable, since Hibernate adds the elements to the default value of a new instance when merging
    private Set<AiidaAsset> assets = new HashSet<>();

    @Schema(description = "Define contexts to be provided to AIIDA ('FLEXIBLE-CONNECTION-AGREEMENT')")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aiida_data_need_contexts",
            joinColumns = @JoinColumn(name = "data_need_id"),
            schema = "data_needs")
    @Column(name = "context")
    @JsonProperty
    @Enumerated(EnumType.STRING)
    private Set<AiidaContext> contexts;

    @Override
    public boolean acknowledgementRequired() {
        return acknowledgementRequired;
    }

    @Override
    public Set<PermissionCommand.Action> allowedPermissionCommands() {
        return allowedPermissionCommands;
    }

    @Override
    public Set<AiidaAsset> assets() {
        return assets == null ? Set.of() : assets;
    }

    /**
     * The single {@code asset} field was replaced by the {@code assets} list. Reject it explicitly, since unknown
     * properties are otherwise silently ignored, which would widen the data need to allow any asset.
     */
    @JsonSetter("asset")
    @SuppressWarnings("unused")
    private void rejectLegacyAsset(Object ignored) {
        throw new IllegalArgumentException("The field 'asset' is no longer supported, use the list 'assets' instead");
    }

    @Override
    public Set<AiidaContext> contexts() {
        return contexts;
    }

    @Override
    public UUID dataNeedId() {
        return UUID.fromString(id());
    }

    @Override
    public Set<AiidaSchema> schemas() {
        return schemas;
    }

    @Override
    public CronExpression transmissionSchedule() {
        return transmissionSchedule;
    }

    public abstract Set<AiidaSchema> supportedSchemas();
}
