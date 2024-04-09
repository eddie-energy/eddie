package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompletelyInThePastOrInTheFutureEventValidatorTest {
    @Test
    void notValid_whenFromAndToAreNotCompletelyInThePast() {
        // Given
        var validator = new CompletelyInThePastOrInTheFutureEventValidator();
        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(5);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).plusDays(5);
        var event = new CreatedEvent(
                "pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"),
                start,
                end,
                "mid", AllowedGranularity.PT15M, "cmId", "convId"
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
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = now.minusDays(11);
        LocalDate end = now.minusDays(1);
        var validator = new CompletelyInThePastOrInTheFutureEventValidator();
        var event = new CreatedEvent(
                "pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"),
                start,
                end,
                "mid", AllowedGranularity.PT15M, "cmId", "convId"
        );

        // When
        var validationResult = validator.validate(event);

        // Then
        assertEquals(0, validationResult.size());
    }

    @Test
    void isValid_whenStartAndEndDateInTheFuture() {
        // Given
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = now.plusDays(1);
        LocalDate end = now.plusDays(1);
        var event = new CreatedEvent(
                "pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"),
                start,
                end,
                "mid", AllowedGranularity.PT15M, "cmId", "convId"
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
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        var validator = new CompletelyInThePastOrInTheFutureEventValidator();
        var event = new CreatedEvent(
                "pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"),
                now,
                null,
                "mid", AllowedGranularity.PT15M, "cmId", "convId"
        );

        // When
        var validationResult = validator.validate(event);

        // Then
        assertEquals(0, validationResult.size());
    }

    @Test
    void isValid_whenStartDateIsInTheFutureAndDataToEmpty() {
        // Given
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        var validator = new CompletelyInThePastOrInTheFutureEventValidator();
        var event = new CreatedEvent(
                "pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"),
                now.plusDays(10),
                null,
                "mid", AllowedGranularity.PT15M, "cmId", "convId"
        );

        // When
        var validationResult = validator.validate(event);

        // Then
        assertEquals(0, validationResult.size());
    }

    @Test
    void isNotValid_whenStartDateIsInThePastAndDataToEmpty() {
        // Given
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        var validator = new CompletelyInThePastOrInTheFutureEventValidator();
        var event = new CreatedEvent(
                "pid", "cid", "dnid", new EdaDataSourceInformation("dsoId"),
                now.minusDays(10),
                null,
                "mid", AllowedGranularity.PT15M, "cmId", "convId"
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
