package energy.eddie.regionconnector.de.eta.providers;

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
}