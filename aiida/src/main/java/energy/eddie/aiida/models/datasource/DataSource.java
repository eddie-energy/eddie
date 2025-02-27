package energy.eddie.aiida.models.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "source_type", discriminatorType = DiscriminatorType.STRING)
public abstract class DataSource {
    @Id
    @SuppressWarnings({"unused", "NullAway"})
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Schema(description = "UUID of the user that owns the permission.")
    private UUID userId;
    @JsonProperty
    @Enumerated(EnumType.STRING)
    private DataSourceType dataSourceType;
    @JsonProperty
    @Enumerated(EnumType.STRING)
    private AiidaAsset asset;
    @JsonProperty
    private String name;
    @JsonProperty
    private boolean enabled;
    @JsonProperty
    @Column(name = "source_type", insertable = false, updatable = false)
    private String sourceType;

    @SuppressWarnings("NullAway")
    protected DataSource() {
    }

    @SuppressWarnings("NullAway")
    protected DataSource(String name, boolean enabled, UUID userId, AiidaAsset asset, DataSourceType dataSourceType) {
        this.name = name;
        this.enabled = enabled;
        this.userId = userId;
        this.asset = asset;
        this.dataSourceType = dataSourceType;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public AiidaAsset getAsset() {
        return asset;
    }

    public void setAsset(AiidaAsset asset) {
        this.asset = asset;
    }

    public DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSourceType() {
        return sourceType;
    }
}
