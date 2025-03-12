package energy.eddie.aiida.models.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@DiscriminatorColumn(name = "data_source_type", discriminatorType = DiscriminatorType.STRING)
public abstract class DataSource {
    @Id
    @SuppressWarnings({"unused", "NullAway"})
    @JsonProperty
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;
    @Schema(description = "UUID of the user that owns the permission.")
    protected UUID userId;
    @JsonProperty
    @Enumerated(EnumType.STRING)
    protected AiidaAsset asset;
    @JsonProperty
    protected String name;
    @JsonProperty
    protected boolean enabled;
    @JsonProperty
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source_type", insertable = false, updatable = false)
    protected DataSourceType dataSourceType;

    @SuppressWarnings("NullAway")
    protected DataSource() {}

    protected DataSource(DataSourceDto dto, UUID userId) {
        this.id = dto.id();
        this.userId = userId;
        this.asset = AiidaAsset.forValue(dto.asset());
        this.name = dto.name();
        this.enabled = dto.enabled();
        this.dataSourceType = DataSourceType.fromIdentifier(dto.dataSourceType());
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public AiidaAsset asset() {
        return asset;
    }

    public String name() {
        return name;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DataSourceType dataSourceType() {
        return dataSourceType;
    }
}
