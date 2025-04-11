package energy.eddie.regionconnector.be.fluvius.client.model;

import java.util.List;
import java.util.function.Function;

public class Meter<T> {
    private final T rawMeter;
    private final Function<T, String> meterIdExtractor;
    private final List<Readings<?>> readingSources;

    public Meter(T rawMeter, Function<T, String> meterIdExtractor, List<Readings<?>> readingSources) {
        this.rawMeter = rawMeter;
        this.meterIdExtractor = meterIdExtractor;
        this.readingSources = readingSources;
    }

    public static Meter<ElectricityMeterResponseModel> from(ElectricityMeterResponseModel electricityMeter) {
        return new Meter<>(
                electricityMeter,
                ElectricityMeterResponseModel::getMeterID,
                List.of(
                        new Readings<>(electricityMeter.getQuarterHourlyEnergy(),
                                       EQuarterHourlyEnergyItemResponseModel::getTimestampEnd),
                        new Readings<>(electricityMeter.getDailyEnergy(),
                                       EDailyEnergyItemResponseModel::getTimestampEnd)
                )
        );
    }

    public static Meter<GasMeterResponseModel> from(GasMeterResponseModel gasMeter) {
        return new Meter<>(
                gasMeter,
                GasMeterResponseModel::getMeterID,
                List.of(
                        new Readings<>(gasMeter.getHourlyEnergy(), GHourlyEnergyItemResponseModel::getTimestampEnd),
                        new Readings<>(gasMeter.getDailyEnergy(), GDailyEnergyItemResponseModel::getTimestampEnd)
                )
        );
    }

    public String getMeterId() {
        return meterIdExtractor.apply(rawMeter);
    }

    // Sonar doesn't like wildcard returns, but in this case, Readings has mixed generics, so there is no workaround
    @SuppressWarnings("java:S1452")
    public List<Readings<?>> getReadingSources() {
        return readingSources;
    }
}
