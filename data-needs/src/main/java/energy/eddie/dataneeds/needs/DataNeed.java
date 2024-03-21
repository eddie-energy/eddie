package energy.eddie.dataneeds.needs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.SmartMeterAiidaDataNeed;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

@Entity
@Table(schema = "data_needs")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AccountingPointDataNeed.class, name = AccountingPointDataNeed.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = ValidatedHistoricalDataDataNeed.class, name = ValidatedHistoricalDataDataNeed.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = SmartMeterAiidaDataNeed.class, name = SmartMeterAiidaDataNeed.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = GenericAiidaDataNeed.class, name = GenericAiidaDataNeed.DISCRIMINATOR_VALUE)
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DataNeed {
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @Column(name = "data_need_id")
    @JsonProperty(required = true)
    private String id;
    @Column(name = "name", nullable = false)
    @JsonProperty(required = true)
    @NotBlank(message = "must not be blank")
    private String name;
    @Schema(description = "Description of the data need for internal use. It is not displayed to the user.")
    @Column(name = "description", nullable = false)
    @JsonProperty(required = true)
    @NotBlank(message = "must not be blank")
    private String description;
    @Schema(description = "Purpose and description of this data need that will be displayed to the user in the popup. Supports markdown formatting.")
    @Column(name = "purpose", nullable = false)
    @JsonProperty(required = true)
    @NotBlank(message = "must not be blank")
    private String purpose;
    @Schema(description = "URL to a document describing the data policy.", example = "https://example.com/your-terms")
    @Column(name = "policy_link", nullable = false)
    @JsonProperty(required = true)
    @NotBlank(message = "must not be blank")
    @URL(message = "must be a valid URL")
    private String policyLink;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "created_at")
    @CreationTimestamp
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant createdAt;

    @SuppressWarnings("NullAway.Init")
    protected DataNeed() {
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String purpose() {
        return purpose;
    }

    public String policyLink() {
        return policyLink;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the timestamp that this data need was created at. This timestamp is only available if data needs are
     * stored in the database, when reading from a config file it will be null.
     *
     * @return Timestamp of creation.
     */
    public Instant createdAt() {
        return createdAt;
    }
}
