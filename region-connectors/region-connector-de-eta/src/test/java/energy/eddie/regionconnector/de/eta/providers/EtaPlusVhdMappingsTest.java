package energy.eddie.regionconnector.de.eta.providers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EtaPlusVhdMappingsTest {

    @ParameterizedTest
    @CsvSource({
            "kWh,KWH",
            "KWH,KWH",
            "MWh,MWH",
            "MWH,MWH",
            "m³,MTQ",
            "m3,MTQ",
            "M3,MTQ"
    })
    void translateUnit_supportedWireValue_returnsCimCode(String wire, String expected) {
        assertThat(EtaPlusVhdMappings.translateUnit(wire)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Wh", "GJ", "MJ", "unknown", ""})
    void translateUnit_unsupportedWireValue_throws(String wire) {
        assertThatThrownBy(() -> EtaPlusVhdMappings.translateUnit(wire))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void translateUnit_null_throws() {
        assertThatThrownBy(() -> EtaPlusVhdMappings.translateUnit(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

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

    private static EtaPlusMeteredData.MeterReading reading(ZonedDateTime ts, double value) {
        return new EtaPlusMeteredData.MeterReading(ts, value, "kWh", "VALIDATED", "Consumption");
    }
}