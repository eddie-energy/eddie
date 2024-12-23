package energy.eddie.regionconnector.be.fluvius.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import org.junit.jupiter.api.Test;

import java.time.Period;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnergyTypePredicateTest {
    @Test
    void testReturnsTrue_forAccountingPointDataNeed() {
        // Given
        var dataNeed = new AccountingPointDataNeed();
        var predicate = new EnergyTypePredicate();

        // When
        var res = predicate.test(dataNeed);

        // Then
        assertTrue(res);
    }

    @Test
    void testReturnsTrue_forValidatedHistoricalDataDataNeedForElectricity() {
        // Given
        var dataNeed = getValidatedHistoricalDataDataNeed(EnergyType.ELECTRICITY);
        var predicate = new EnergyTypePredicate();

        // When
        var res = predicate.test(dataNeed);

        // Then
        assertTrue(res);
    }

    @Test
    void testReturnsFalse_forValidatedHistoricalDataDataNeedForUnsupportedEnergyType() {
        // Given
        var dataNeed = getValidatedHistoricalDataDataNeed(EnergyType.HYDROGEN);
        var predicate = new EnergyTypePredicate();

        // When
        var res = predicate.test(dataNeed);

        // Then
        assertFalse(res);
    }

    private static ValidatedHistoricalDataDataNeed getValidatedHistoricalDataDataNeed(EnergyType energyType) {
        return new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(Period.ofYears(-1), Period.ofYears(1), null),
                energyType,
                Granularity.PT5M,
                Granularity.P1Y
        );
    }
}