package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EtaPlusVhdMappingsTest {

    @ParameterizedTest
    @CsvSource({
            "Generation,true",
            "Consumption,false",
            "GENERATION,false",
            "unknown,false",
            "'',false"
    })
    void isProduction_recognisesGenerationOnly(String direction, boolean expected) {
        assertThat(EtaPlusVhdMappings.isProduction(direction)).isEqualTo(expected);
    }

    @Test
    void isProduction_null_returnsFalse() {
        assertThat(EtaPlusVhdMappings.isProduction(null)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "VALIDATED,true",
            "validated,false",
            "ESTIMATED,false",
            "MEASURED,false",
            "'',false"
    })
    void isValidatedStatus_recognisesExactValidatedOnly(String wireStatus, boolean expected) {
        assertThat(EtaPlusVhdMappings.isValidatedStatus(wireStatus)).isEqualTo(expected);
    }

    @Test
    void isValidatedStatus_null_returnsFalse() {
        assertThat(EtaPlusVhdMappings.isValidatedStatus(null)).isFalse();
    }

    @Test
    void sortByTimestamp_returnsAscendingByTimestamp() {
        var t0 = ZonedDateTime.of(2026, 4, 30, 22, 0, 0, 0, ZoneOffset.UTC);
        var unsorted = List.of(
                reading(t0.plusHours(2), 3.0),
                reading(t0, 1.0),
                reading(t0.plusHours(1), 2.0)
        );

        var sorted = EtaPlusVhdMappings.sortByTimestamp(unsorted);

        assertThat(sorted).extracting(EtaPlusMeteredData.MeterReading::value)
                .containsExactly(1.0, 2.0, 3.0);
    }

    @Test
    void sortByTimestamp_alreadySortedListIsReturnedInOrder() {
        var t0 = ZonedDateTime.of(2026, 4, 30, 22, 0, 0, 0, ZoneOffset.UTC);
        var sortedInput = List.of(reading(t0, 1.0), reading(t0.plusHours(1), 2.0));

        assertThat(EtaPlusVhdMappings.sortByTimestamp(sortedInput))
                .extracting(EtaPlusMeteredData.MeterReading::value)
                .containsExactly(1.0, 2.0);
    }

    // ---- energyTypeFromUnit ----

    @ParameterizedTest
    @CsvSource({
            "kWh,ELECTRICITY",
            "KWH,ELECTRICITY",
            "MWh,ELECTRICITY",
            "MWH,ELECTRICITY",
            "m³,NATURAL_GAS",
            "m3,NATURAL_GAS",
            "M3,NATURAL_GAS",
    })
    void energyTypeFromUnit_mapsKnownUnitsCorrectly(String unit, EnergyType expected) {
        assertThat(EtaPlusVhdMappings.energyTypeFromUnit(unit)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"BTU", "therm", "GJ", "ft3"})
    void energyTypeFromUnit_unknownUnit_returnsNull(String unit) {
        assertThat(EtaPlusVhdMappings.energyTypeFromUnit(unit)).isNull();
    }

    @Test
    void energyTypeFromUnit_null_returnsNull() {
        assertThat(EtaPlusVhdMappings.energyTypeFromUnit(null)).isNull();
    }

    // ---- inferGranularity ----

    @Test
    void inferGranularity_singleReading_returnsNull() {
        var t0 = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        assertThat(EtaPlusVhdMappings.inferGranularity(List.of(reading(t0, 1.0)))).isNull();
    }

    @Test
    void inferGranularity_emptyList_returnsNull() {
        assertThat(EtaPlusVhdMappings.inferGranularity(List.of())).isNull();
    }

    @Test
    void inferGranularity_15minInterval_returnsPT15M() {
        var t0 = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var readings = List.of(reading(t0, 1.0), reading(t0.plusMinutes(15), 2.0));
        assertThat(EtaPlusVhdMappings.inferGranularity(readings)).isEqualTo(Granularity.PT15M);
    }

    @Test
    void inferGranularity_60minInterval_returnsPT1H() {
        var t0 = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var readings = List.of(reading(t0, 1.0), reading(t0.plusHours(1), 2.0));
        assertThat(EtaPlusVhdMappings.inferGranularity(readings)).isEqualTo(Granularity.PT1H);
    }

    @Test
    void inferGranularity_24hourInterval_returnsP1D() {
        var t0 = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var readings = List.of(reading(t0, 1.0), reading(t0.plusDays(1), 2.0));
        assertThat(EtaPlusVhdMappings.inferGranularity(readings)).isEqualTo(Granularity.P1D);
    }

    @Test
    void inferGranularity_unrecognisedInterval_returnsNull() {
        var t0 = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        // 7-minute interval does not correspond to any Granularity enum value
        var readings = List.of(reading(t0, 1.0), reading(t0.plusMinutes(7), 2.0));
        assertThat(EtaPlusVhdMappings.inferGranularity(readings)).isNull();
    }

    private static EtaPlusMeteredData.MeterReading reading(ZonedDateTime ts, double value) {
        return new EtaPlusMeteredData.MeterReading(ts, value, "kWh", "VALIDATED", "Consumption");
    }
}