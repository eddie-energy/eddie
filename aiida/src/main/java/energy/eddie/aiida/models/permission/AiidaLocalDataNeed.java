package energy.eddie.aiida.models.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import energy.eddie.aiida.dtos.PermissionDetailsDto;
import energy.eddie.dataneeds.utils.cron.CronExpressionConverter;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.utils.cron.CronExpressionDeserializer;
import energy.eddie.dataneeds.utils.cron.CronExpressionSerializer;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.springframework.scheduling.support.CronExpression;

import java.util.Set;

/**
 * Stores the locally required information about a data need of a permission.
 */
@Entity
public class AiidaLocalDataNeed {
    @Id
    @Column(nullable = false, name = "permission_id")
    private String permissionId;
    @Column(nullable = false, name = "data_need_id")
    @JsonProperty
    private final String dataNeedId;
    @Column(nullable = false)
    private final String type;
    @Column(nullable = false)
    @JsonProperty
    private final String name;
    @Column(nullable = false)
    @JsonProperty
    private final String purpose;
    @Column(nullable = false, name = "policy_link")
    @JsonProperty
    private final String policyLink;
    @Column(nullable = false, name = "transmission_schedule")
    @Convert(converter = CronExpressionConverter.class)
    @JsonProperty
    @JsonSerialize(using = CronExpressionSerializer.class)
    @JsonDeserialize(using = CronExpressionDeserializer.class)
    private final CronExpression transmissionSchedule;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aiida_local_data_need_data_tags", joinColumns = @JoinColumn(name = "data_need_id", referencedColumnName = "data_need_id"))
    @Column(name = "data_tags")
    @Nullable
    @JsonProperty
    private final Set<String> dataTags;

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway")
    protected AiidaLocalDataNeed() {
        this.dataNeedId = null;
        this.type = null;
        this.name = null;
        this.purpose = null;
        this.policyLink = null;
        this.transmissionSchedule = null;
        this.dataTags = null;
    }

    public AiidaLocalDataNeed(PermissionDetailsDto details) {
        this.permissionId = details.permissionId();
        this.dataNeedId = details.dataNeed().id();
        this.type = details.dataNeed().type();
        this.name = details.dataNeed().name();
        this.purpose = details.dataNeed().purpose();
        this.policyLink = details.dataNeed().policyLink();
        this.transmissionSchedule = details.dataNeed().transmissionSchedule();

        if (details.dataNeed() instanceof GenericAiidaDataNeed genericAiida) {
            this.dataTags = Set.copyOf(genericAiida.dataTags());
        } else {
            this.dataTags = null;
        }
    }

    public String permissionId() {
        return permissionId;
    }

    public String dataNeedId() {
        return dataNeedId;
    }

    public String type() {
        return type;
    }

    public String name() {
        return name;
    }

    public String purpose() {
        return purpose;
    }

    public String policyLink() {
        return policyLink;
    }

    public CronExpression transmissionSchedule() {
        return transmissionSchedule;
    }

    public @Nullable Set<String> dataTags() {
        return dataTags;
    }
}
