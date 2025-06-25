package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class NlAccountingPointValidatedEvent extends NlPermissionEvent {
    @Column(columnDefinition = "text")
    private final String state;
    @Column(columnDefinition = "text")
    private final String codeVerifier;
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;
    @Column(name = "house_number")
    private final String houseNumber;
    @Column(name = "postal_code")
    private final String postalCode;

    public NlAccountingPointValidatedEvent(
            String permissionId,
            String state,
            String codeVerifier,
            LocalDate start,
            LocalDate end,
            String houseNumber,
            String postalCode
    ) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.state = state;
        this.codeVerifier = codeVerifier;
        this.start = start;
        this.end = end;
        this.houseNumber = houseNumber;
        this.postalCode = postalCode;
    }

    @SuppressWarnings("NullAway.Init")
    protected NlAccountingPointValidatedEvent() {
        state = null;
        codeVerifier = null;
        start = null;
        end = null;
        houseNumber = null;
        postalCode = null;
    }

    public String state() {
        return state;
    }

    public String codeVerifier() {
        return codeVerifier;
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }

    public String houseNumber() {
        return houseNumber;
    }

    public String postalCode() {
        return postalCode;
    }
}
