package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompletelyInThePastOrInTheFutureEventValidatorTest {
    @Test
    void notValid_whenFromAndToAreNotCompletelyInThePast() {
        // Given
        var validator = new CompletelyInThePastOrInTheFutureEventValidator();
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(5);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).plusDays(5);
        var event = new CreatedEvent(
                "pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"),
                start,
                end,
                "mid", Granularity.PT15M, "cmId", "convId"
        );

        // When
        var validationResult = validator.validate(event);

        // Then
        assertAll(
                () -> assertEquals(1, validationResult.size()),
                () -> assertEquals("start", validationResult.getFirst().name())
        );
    }

    @Test
    void isValid_whenStartAndEndDateInThePast() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime start = now.minusDays(11);
        ZonedDateTime end = now.minusDays(1);
        var validator = new CompletelyInThePastOrInTheFutureEventValidator();
        var event = new CreatedEvent(
                "pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"),
                start,
                end,
                "mid", Granularity.PT15M, "cmId", "convId"
        );

        // When
        var validationResult = validator.validate(event);

        // Then
        assertEquals(0, validationResult.size());
    }

    @Test
    void isValid_whenStartAndEndDateInTheFuture() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime start = now.plusDays(1);
        ZonedDateTime end = now.plusDays(1);
        var event = new CreatedEvent(
                "pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"),
                start,
                end,
                "mid", Granularity.PT15M, "cmId", "convId"
        );
        var validator = new CompletelyInThePastOrInTheFutureEventValidator();

        // When
        var validationResult = validator.validate(event);

        // Then
        assertEquals(0, validationResult.size());
    }

    @Test
    void isValid_whenStartDateIsNow() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        var validator = new CompletelyInThePastOrInTheFutureEventValidator();
        var event = new CreatedEvent(
                "pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"),
                now,
                null,
                "mid", Granularity.PT15M, "cmId", "convId"
        );

        // When
        var validationResult = validator.validate(event);

        // Then
        assertEquals(0, validationResult.size());
    }

    @Test
    void isValid_whenStartDateIsInTheFutureAndDataToEmpty() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        var validator = new CompletelyInThePastOrInTheFutureEventValidator();
        var event = new CreatedEvent(
                "pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"),
                now.plusDays(10),
                null,
                "mid", Granularity.PT15M, "cmId", "convId"
        );

        // When
        var validationResult = validator.validate(event);

        // Then
        assertEquals(0, validationResult.size());
    }

    @Test
    void isNotValid_whenStartDateIsInThePastAndDataToEmpty() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        var validator = new CompletelyInThePastOrInTheFutureEventValidator();
        var event = new CreatedEvent(
                "pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"),
                now.minusDays(10),
                null,
                "mid", Granularity.PT15M, "cmId", "convId"
        );


        // When
        var validationResult = validator.validate(event);

        // Then
        assertAll(
                () -> assertEquals(1, validationResult.size()),
                () -> assertEquals("start", validationResult.getFirst().name())
        );
    }
}