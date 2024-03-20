package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NotOlderThanValidatorTest {

    @Test
    void notValid_whenDateIsTooOld() {
        // Given
        var validator = new NotOlderThanValidator(ChronoUnit.DAYS, 10);

        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                null,
                LocalDate.now(ZoneOffset.UTC).minusDays(15),
                null,
                "mpid",
                Granularity.PT15M,
                "cmId",
                "concId");

        // When
        var res = validator.validate(created);

        // Then
        assertAll(
                () -> assertEquals(1, res.size()),
                () -> assertEquals("dataFrom", res.getFirst().name())
        );
    }

    @Test
    void valid_whenDateIsNotTooOld() {
        // Given
        var validator = new NotOlderThanValidator(ChronoUnit.DAYS, 10);

        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                null,
                LocalDate.now(ZoneOffset.UTC).minusDays(5),
                null,
                "mpid",
                Granularity.PT15M,
                "cmId",
                "concId");

        // When
        var res = validator.validate(created);

        // Then
        assertEquals(0, res.size());
    }

    @Test
    void valid_whenStartIsInTheFuture() {
        // Given
        var validator = new NotOlderThanValidator(ChronoUnit.DAYS, 10);

        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                null,
                LocalDate.now(ZoneOffset.UTC).plusDays(15),
                null,
                "mpid",
                Granularity.PT15M,
                "cmId",
                "concId");

        // When
        var res = validator.validate(created);

        // Then
        assertEquals(0, res.size());
    }

    @Test
    void valid_whenDateIsOnTheSameDay() {
        // Given
        var validator = new NotOlderThanValidator(ChronoUnit.DAYS, 0);

        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                null,
                LocalDate.now(ZoneOffset.UTC),
                null,
                "mpid",
                Granularity.PT15M,
                "cmId",
                "concId");

        // When
        var res = validator.validate(created);

        // Then
        assertEquals(0, res.size());
    }
}
