package energy.eddie.aiida.adapters.datasource.fr.transformer.history;

import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3DataField;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

import java.util.Map;
import java.util.Optional;

import static energy.eddie.aiida.adapters.datasource.fr.transformer.history.HistoryModeEntry.*;

public class MicroTeleinfoV3AdapterHistoryModeMeasurement extends SmartMeterAdapterMeasurement {
    private final HistoryModeEntry historyModeEntry;

    public MicroTeleinfoV3AdapterHistoryModeMeasurement(String entryKey, String rawValue) {
        super(entryKey, rawValue);
        this.historyModeEntry = HistoryModeEntry.fromEntryKey(entryKey);
    }

    @Override
    public ObisCode obisCode() {
        return historyModeEntry.obisCode();
    }

    @Override
    public UnitOfMeasurement rawUnitOfMeasurement() {
        return historyModeEntry.rawUnitOfMeasurement();
    }

    public static Optional<MicroTeleinfoV3AdapterHistoryModeMeasurement> calculateAiidaPositiveActiveEnergyFromHistoryModeData(
            Map<String, MicroTeleinfoV3DataField> historyModeData
    ) {
        var hchc = historyModeData.get(HistoryModeEntry.HCHC.name());
        var hchp = historyModeData.get(HistoryModeEntry.HCHP.name());

        if (hchc != null && hchp != null) {
            var positiveActiveEnergy = Integer.parseInt(hchc.raw()) + Integer.parseInt(hchp.raw());
            return Optional.of(new MicroTeleinfoV3AdapterHistoryModeMeasurement(AIIDA_POSITIVE_ACTIVE_ENERGY.name(),
                                                                                String.valueOf(positiveActiveEnergy))
            );
        }
        return Optional.empty();
    }

    public static Optional<MicroTeleinfoV3AdapterHistoryModeMeasurement> calculateAiidaPositiveActiveInstantaneousPowerFromHistoryModeData(
            Map<String, MicroTeleinfoV3DataField> historyModeData
    ) {
        var iinst = historyModeData.get(IINST.name());

        if (iinst != null) {
            var voltage = 200;
            var positiveActiveInstantaneousPower = voltage * Integer.parseInt(iinst.raw());

            return Optional.of(new MicroTeleinfoV3AdapterHistoryModeMeasurement(
                    AIIDA_POSITIVE_ACTIVE_INSTANTANEOUS_POWER.name(),
                    String.valueOf(positiveActiveInstantaneousPower))
            );
        }
        return Optional.empty();
    }
}
