package energy.eddie.regionconnector.at.eda.utils;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.MeteringIntervall;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;

public class MeteringIntervalUtil {
    private MeteringIntervalUtil() {
    }

    public static Granularity toGranularity(MeteringIntervall meteringInterval) throws InvalidMappingException {
        return switch (meteringInterval) {
            case D -> Granularity.P1D;
            case H -> Granularity.PT1H;
            case QH -> Granularity.PT15M;
            default ->
                    throw new InvalidMappingException("Unexpected MeteringInterval value: '" + meteringInterval + "'");
        };
    }
}