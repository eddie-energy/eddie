package energy.eddie.aiida.application.information;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "aiida_application_information")
@SuppressWarnings("NullAway")
public class ApplicationInformation {
    @Id
    @Column(name = "aiida_id")
    private UUID aiidaId;

    @CreatedDate
    @SuppressWarnings("unused")
    private Instant createdAt;

    public ApplicationInformation() {
        this(UUID.randomUUID(), Instant.now());
    }

    public ApplicationInformation(UUID aiidaId, Instant createdAt) {
        this.aiidaId = aiidaId;
        this.createdAt = createdAt;
    }

    public UUID aiidaId() {
        return aiidaId;
    }
}
