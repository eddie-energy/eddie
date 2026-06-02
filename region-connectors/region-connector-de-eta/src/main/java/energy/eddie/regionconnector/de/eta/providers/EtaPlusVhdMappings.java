package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import jakarta.annotation.Nullable;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * Shared wire-string-to-CIM mappings used by both v0.82 and v1.04 intermediate VHD documents.
 *
 * <p>Holds only logic that is identical across CIM versions; version-specific enum lookups stay in
 * the intermediate docs themselves so each one keeps its own type signatures.
 */
public final class EtaPlusVhdMappings {

    private EtaPlusVhdMappings() {}

    /** {@code true} if the wire {@code direction} is {@code "Generation"} (prosumer feed-in). */
    public static boolean isProduction(String direction) {
        return "Generation".equals(direction);
    }

    /** {@code true} if the wire {@code status} is {@code "VALIDATED"} — the only value the
     * /meters/historical endpoint emits per contract. */
    public static boolean isValidatedStatus(String wireStatus) {
        return "VALIDATED".equals(wireStatus);
    }

    /** Returns readings ordered by ascending timestamp. */
    public static List<EtaPlusMeteredData.MeterReading> sortByTimestamp(
            List<EtaPlusMeteredData.MeterReading> readings) {
        return readings.stream()
                .sorted(Comparator.comparing(EtaPlusMeteredData.MeterReading::timestamp))
                .toList();
    }

    /**
     * Returns the {@link EnergyType} that corresponds to the wire {@code unit} string,
     * or {@code null} if the unit is not recognised.
     *
     * <p>Per ETA Plus contract, the unit is constant within a historical-readings response,
     * so checking the first reading's unit is sufficient to determine the energy type.
     */
    @Nullable
    public static EnergyType energyTypeFromUnit(@Nullable String unit) {
        if (unit == null) {
            return null;
        }
        return switch (unit) {
            case "kWh", "KWH", "MWh", "MWH" -> EnergyType.ELECTRICITY;
            case "m³", "m3", "M3" -> EnergyType.NATURAL_GAS;
            default -> null;
        };
    }

    /**
     * Infers the data {@link Granularity} from the interval between the first two entries of an
     * already-sorted reading list, or {@code null} if fewer than two readings are present or the
     * interval does not match any known {@link Granularity} value.
     */
    @Nullable
    public static Granularity inferGranularity(List<EtaPlusMeteredData.MeterReading> sortedReadings) {
        if (sortedReadings.size() < 2) {
            return null;
        }
        Duration interval = Duration.between(
                sortedReadings.get(0).timestamp(),
                sortedReadings.get(1).timestamp());
        for (Granularity g : Granularity.values()) {
            if (g.duration().equals(interval)) {
                return g;
            }
        }
        return null;
    }
}