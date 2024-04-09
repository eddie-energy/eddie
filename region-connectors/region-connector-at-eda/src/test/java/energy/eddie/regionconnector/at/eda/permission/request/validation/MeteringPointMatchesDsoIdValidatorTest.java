package energy.eddie.regionconnector.at.eda.permission.request.validation;

import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;

import static energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint.DSO_ID_LENGTH;
import static org.junit.jupiter.api.Assertions.*;

class MeteringPointMatchesDsoIdValidatorTest {
    @ParameterizedTest
    @EmptySource
    @NullSource
    void testValidationWithEmptyMeteringPointId_returnsEmptyErrorList(String meteringPointId) {
        // Given
        var validator = new MeteringPointMatchesDsoIdValidator();
        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                new EdaDataSourceInformation("123"),
                null,
                null,
                meteringPointId,
                AllowedGranularity.PT15M,
                "cmId",
                "concId");
        // When
        var res = validator.validate(created);

        // Then
        assertTrue(res.isEmpty());
    }

    @Test
    void testValidationWithMatchingMeteringPointId_returnsEmptyErrorList() {
        var validator = new MeteringPointMatchesDsoIdValidator();
        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                new EdaDataSourceInformation("0".repeat(DSO_ID_LENGTH)),
                null,
                null,
                "0".repeat(DSO_ID_LENGTH) + "123456789",
                AllowedGranularity.PT15M,
                "cmId",
                "concId");
        // When
        var res = validator.validate(created);

        // Then
        assertTrue(res.isEmpty());
    }

    @Test
    void testValidationWithNotMatchingMeteringPointId_returnsErrorList() {
        var validator = new MeteringPointMatchesDsoIdValidator();
        var created = new CreatedEvent(
                "pid",
                "cid",
                "dnid",
                new EdaDataSourceInformation("0".repeat(DSO_ID_LENGTH)),
                null,
                null,
                "1".repeat(DSO_ID_LENGTH) + "123456789",
                AllowedGranularity.PT15M,
                "cmId",
                "concId");
        // When
        var res = validator.validate(created);

        // Then
        assertAll(
                () -> assertEquals(1, res.size()),
                () -> assertEquals("meteringPointId", res.getFirst().name())
        );
    }
}
