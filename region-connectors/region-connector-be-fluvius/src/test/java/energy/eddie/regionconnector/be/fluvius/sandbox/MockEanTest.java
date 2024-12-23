package energy.eddie.regionconnector.be.fluvius.sandbox;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MockEanTest {
    @Test
    void testToString_returnsCorrectEAN_forElectricityDataNeedAndPT15MGranularity() {
        // Given
        var dataNeed = getValidatedHistoricalDataDataNeed(EnergyType.ELECTRICITY);
        var pr = createPermissionRequest(Granularity.PT15M);
        var ean = new MockEan(dataNeed, pr, pr.granularity());

        // When
        var res = ean.toString();

        // Then
        assertEquals("541440110000000001", res);
    }

    @Test
    void testToString_returnsCorrectEAN_forGasDataNeedAndP1DGranularity() {
        // Given
        var dataNeed = getValidatedHistoricalDataDataNeed(EnergyType.NATURAL_GAS);
        var pr = createPermissionRequest(Granularity.P1D);
        var ean = new MockEan(dataNeed, pr, pr.granularity());

        // When
        var res = ean.toString();

        // Then
        assertEquals("541441100000000001", res);
    }

    @Test
    void testToString_returnsCorrectEAN_forAccountingPointDataNeed() {
        // Given
        var dataNeed = new AccountingPointDataNeed();
        var pr = createPermissionRequest(Granularity.P1D);
        var ean = new MockEan(dataNeed, pr, pr.granularity());

        // When
        var res = ean.toString();

        // Then
        assertEquals("541440110000000001", res);
    }

    @Test
    void testToString_throws_forUnsupportedGranularity() {
        // Given
        var dataNeed = getValidatedHistoricalDataDataNeed(EnergyType.NATURAL_GAS);
        var pr = createPermissionRequest(Granularity.PT5M);
        var ean = new MockEan(dataNeed, pr, pr.granularity());

        // When & Then
        assertThrows(IllegalArgumentException.class, ean::toString);
    }

    @Test
    void testToString_throws_forUnsupportedEnergyType() {
        // Given
        var dataNeed = getValidatedHistoricalDataDataNeed(EnergyType.HYDROGEN);
        var pr = createPermissionRequest(Granularity.PT15M);
        var ean = new MockEan(dataNeed, pr, pr.granularity());

        // When & Then
        assertThrows(IllegalArgumentException.class, ean::toString);
    }

    private static FluviusPermissionRequest createPermissionRequest(Granularity granularity) {
        var today = LocalDate.now(ZoneOffset.UTC);
        return new FluviusPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.VALIDATED,
                granularity,
                today,
                today,
                ZonedDateTime.now(ZoneOffset.UTC),
                Flow.B2C
        );
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