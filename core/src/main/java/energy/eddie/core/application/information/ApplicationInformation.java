package energy.eddie.core.application.information;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "core", name = "eddie_application_information")
@SuppressWarnings("NullAway")
public class ApplicationInformation {
    @Id
    @Column(name = "eddie_id")
    @JsonProperty
    private UUID eddieId;

    @CreatedDate
    @SuppressWarnings("unused")
    private Instant createdAt;

    public ApplicationInformation() {
        this(UUID.randomUUID(), Instant.now());
    }

    public ApplicationInformation(UUID eddieId, Instant createdAt) {
        this.eddieId = eddieId;
        this.createdAt = createdAt;
    }

    public UUID eddieId() {
        return eddieId;
    }
}
