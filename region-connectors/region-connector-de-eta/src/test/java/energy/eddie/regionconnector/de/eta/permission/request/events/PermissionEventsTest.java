package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionEventsTest {

    @Test
    void testCreatedEvent() {
        CreatedEvent event = new CreatedEvent("p1", "dn1", "c1", "m1");
        assertThat(event.permissionId()).isEqualTo("p1");
        assertThat(event.dataNeedId()).isEqualTo("dn1");
        assertThat(event.connectionId()).isEqualTo("c1");
        assertThat(event.meteringPointId()).isEqualTo("m1");
        assertThat(event.status()).isEqualTo(PermissionProcessStatus.CREATED);
        assertThat(event.eventCreated()).isNotNull();
    }

    @Test
    void testMalformedEvent() {
        AttributeError error = new AttributeError("f1", "m1");
        MalformedEvent event = new MalformedEvent("p1", error);
        assertThat(event.permissionId()).isEqualTo("p1");
        assertThat(event.attributeErrors()).containsExactly(error);
        assertThat(event.status()).isEqualTo(PermissionProcessStatus.MALFORMED);

        MalformedEvent event2 = new MalformedEvent("p1", List.of(error));
        assertThat(event2.attributeErrors()).containsExactly(error);
    }

    @Test
    void testValidatedEvent() {
        LocalDate start = LocalDate.now(ZoneId.systemDefault());
        LocalDate end = LocalDate.now(ZoneId.systemDefault()).plusDays(1);
        ValidatedEvent event = new ValidatedEvent("p1", start, end, Granularity.PT15M);
        assertThat(event.permissionId()).isEqualTo("p1");
        assertThat(event.start()).isEqualTo(start);
        assertThat(event.end()).isEqualTo(end);
        assertThat(event.granularity()).isEqualTo(Granularity.PT15M);
        assertThat(event.status()).isEqualTo(PermissionProcessStatus.VALIDATED);
    }

    @Test
    void testSimpleEvent() {
        SimpleEvent event = new SimpleEvent("p1", PermissionProcessStatus.TERMINATED);
        assertThat(event.permissionId()).isEqualTo("p1");
        assertThat(event.status()).isEqualTo(PermissionProcessStatus.TERMINATED);
        assertThat(event.eventCreated()).isNotNull();
    }

    @Test
    void testPersistablePermissionEventNoArgs() {
        // Just for coverage of the protected no-args constructor used by JPA
        PersistablePermissionEvent event = new PersistablePermissionEvent() {};
        assertThat(event.permissionId()).isNull();
        assertThat(event.status()).isNull();
        assertThat(event.eventCreated()).isNull();
    }
}
