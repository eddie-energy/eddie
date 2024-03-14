package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StartIsBeforeOrEqualEndValidatorTest {
    @Test
    void isValid_whenPermissionRequestEndNull() {
        // Given
        StartIsBeforeOrEqualEndValidator validator = new StartIsBeforeOrEqualEndValidator();
        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                null,
                ZonedDateTime.now(ZoneOffset.UTC),
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
    void isNotValid_whenPermissionRequestEndIsBeforeStart() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = start.minusDays(1);
        StartIsBeforeOrEqualEndValidator validator = new StartIsBeforeOrEqualEndValidator();
        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                null,
                start,
                end,
                "mpid",
                Granularity.PT15M,
                "cmId",
                "concId");
        // When
        var res = validator.validate(created);

        // Then
        assertAll(
                () -> assertEquals(1, res.size()),
                () -> assertEquals("dataFrom", res.get(0).name())
        );
    }

    @Test
    void isValid_whenPermissionRequestStartIsBeforeEnd() {
        // Given
        StartIsBeforeOrEqualEndValidator validator = new StartIsBeforeOrEqualEndValidator();
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = start.plusDays(1);
        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                null,
                start,
                end,
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
    void isValid_whenPermissionRequestStartIsEqualEnd() {
        // Given
        StartIsBeforeOrEqualEndValidator validator = new StartIsBeforeOrEqualEndValidator();
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                null,
                start,
                start,
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