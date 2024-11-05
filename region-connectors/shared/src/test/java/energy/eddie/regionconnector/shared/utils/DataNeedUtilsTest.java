package energy.eddie.regionconnector.shared.utils;

import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataNeedUtilsTest {

    @Test
    void convertDataNeedClassesToString() {

        // Given
        List<Class<? extends DataNeed>> classList = List.of(ValidatedHistoricalDataDataNeed.class, AiidaDataNeed.class);

        // When
        var converted = DataNeedUtils.convertDataNeedClassesToString(classList);

        // Expected
        var expected = List.of("ValidatedHistoricalDataDataNeed", "AiidaDataNeed");

        // Then
        assertIterableEquals(expected, converted);
    }
}