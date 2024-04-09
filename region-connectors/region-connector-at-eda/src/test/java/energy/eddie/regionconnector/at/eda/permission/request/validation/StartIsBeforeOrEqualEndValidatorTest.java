package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;

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
                LocalDate.now(ZoneOffset.UTC),
                null,
                "mpid",
                AllowedGranularity.PT15M,
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
        LocalDate start = LocalDate.now(ZoneOffset.UTC);
        LocalDate end = start.minusDays(1);
        StartIsBeforeOrEqualEndValidator validator = new StartIsBeforeOrEqualEndValidator();
        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                null,
                start,
                end,
                "mpid",
                AllowedGranularity.PT15M,
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
    void isValid_whenPermissionRequestStartIsBeforeEnd() {
        // Given
        StartIsBeforeOrEqualEndValidator validator = new StartIsBeforeOrEqualEndValidator();
        LocalDate start = LocalDate.now(ZoneOffset.UTC);
        LocalDate end = start.plusDays(1);
        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                null,
                start,
                end,
                "mpid",
                AllowedGranularity.PT15M,
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
        LocalDate start = LocalDate.now(ZoneOffset.UTC);
        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                null,
                start,
                start,
                "mpid",
                AllowedGranularity.PT15M,
                "cmId",
                "concId");
        // When
        var res = validator.validate(created);

        // Then
        assertEquals(0, res.size());
    }
}
