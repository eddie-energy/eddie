package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AccountingPointValidatedEventTest {

    @Test
    void constructor_setsAllFieldsAndStatus() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 6, 1);

        AccountingPointValidatedEvent event = new AccountingPointValidatedEvent("pid", start, end);

        assertThat(event.permissionId()).isEqualTo("pid");
        assertThat(event.status()).isEqualTo(PermissionProcessStatus.VALIDATED);
        assertThat(event.start()).isEqualTo(start);
        assertThat(event.end()).isEqualTo(end);
        assertThat(event.eventCreated()).isNotNull();
    }

    @Test
    void protectedNoArgConstructor_initialisesNullsForJpa() {
        AccountingPointValidatedEvent event = new AccountingPointValidatedEvent() {};

        assertThat(event.start()).isNull();
        assertThat(event.end()).isNull();
    }
}