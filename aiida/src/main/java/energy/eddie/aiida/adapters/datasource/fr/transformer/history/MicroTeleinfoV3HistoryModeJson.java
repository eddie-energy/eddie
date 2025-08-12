package energy.eddie.aiida.adapters.datasource.fr.transformer.history;

import com.fasterxml.jackson.annotation.JsonIgnore;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3DataField;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3Json;

import java.util.HashMap;
import java.util.Map;

/**
 * Linky meter teleinformation fields.
 * <p>
 * Each entry includes:
 * <ul>
 *   <li><b>Label</b> – name used in the teleinfo frame</li>
 *   <li><b>Length</b> – number of characters</li>
 *   <li><b>Unit</b> – measurement unit, if applicable</li>
 *   <li><b>Description</b> – meaning or usage</li>
 * </ul>
 * </p>
 *
 * <h3>Common to Single-phase and Three-phase</h3>
 * <ul>
 *   <li><i>ADCO</i> (12, ADS) – Meter’s address</li>
 *   <li><i>OPTARIF</i> (4) – Chosen tariff option (e.g. “BASE”)</li>
 *   <li><i>ISOUSC</i> (2, A) – Subscribed intensity (PREF in VA / 200 V)</li>
 *   <li><i>BASE</i> (9, Wh) – Base index option (totaliser index)</li>
 *   <li><i>HCHC</i> (9, Wh) – Off-peak hours index (Supplier 1)</li>
 *   <li><i>HCHP</i> (9, Wh) – Peak hours index (Supplier 2)</li>
 *   <li><i>EJPHN</i> (9, Wh) – EJP index, normal times (Supplier 1)</li>
 *   <li><i>EJPHPM</i> (9, Wh) – EJP index, mobile peak times (Supplier 2)</li>
 *   <li><i>BBRHCJB</i> (9, Wh) – Tempo off-peak, blue days (Supplier 1)</li>
 *   <li><i>BBRHPJB</i> (9, Wh) – Tempo peak, blue days (Supplier 2)</li>
 *   <li><i>BBRHCJW</i> (9, Wh) – Tempo off-peak, white days (Supplier 3)</li>
 *   <li><i>BBRHPJW</i> (9, Wh) – Tempo peak, white days (Supplier 4)</li>
 *   <li><i>BBRHCJR</i> (9, Wh) – Tempo off-peak, red days (Supplier 5)</li>
 *   <li><i>BBRHPJR</i> (9, Wh) – Tempo peak, red days (Supplier 6)</li>
 *   <li><i>PEJP</i> (2, min) – EJP start notification (“30” means 30 minutes)</li>
 *   <li><i>PTEC</i> (4) – Current tariff period (e.g. “TH..”)</li>
 *   <li><i>DEMAIN</i> (4) – Tomorrow’s colour (Tempo mode)</li>
 *   <li><i>PAPP</i> (5, VA) – Apparent power, rounded to nearest 10 VA</li>
 *   <li><i>HHPHC</i> (1) – Peak/Off-peak indicator (e.g. “A”)</li>
 *   <li><i>MOTDETAT</i> (6) – Meter status word (e.g. “000000”)</li>
 * </ul>
 *
 * <h3>Single-phase only</h3>
 * <ul>
 *   <li><i>IINST</i> (3, A) – Instantaneous current (effective current)</li>
 *   <li><i>ADPS</i> (3, A) – Subscribed power exceedance notification (triggered if IINST > RI)</li>
 *   <li><i>IMAX</i> (3, A) – Maximum intensity called (e.g. 90 A)</li>
 *   <!-- No PMAX or PPOT in single-phase -->
 * </ul>
 *
 * <h3>Three-phase only</h3>
 * <ul>
 *   <li><i>IINST1</i> (3, A) – Instantaneous current, phase 1</li>
 *   <li><i>IINST2</i> (3, A) – Instantaneous current, phase 2</li>
 *   <li><i>IINST3</i> (3, A) – Instantaneous current, phase 3</li>
 *   <li><i>IMAX1</i> (3, A) – Maximum intensity, phase 1</li>
 *   <li><i>IMAX2</i> (3, A) – Maximum intensity, phase 2</li>
 *   <li><i>IMAX3</i> (3, A) – Maximum intensity, phase 3</li>
 *   <li><i>PMAX</i> (5, W) – Maximum three-phase power reached (Smax of day n-1)</li>
 *   <li><i>PPOT</i> (2) – Presence of potential; “0X” where bit n = 1 means phase n is absent</li>
 * </ul>
 */
public record MicroTeleinfoV3HistoryModeJson(
        @JsonIgnore Map<String, MicroTeleinfoV3DataField> energyData) implements MicroTeleinfoV3Json {

    public MicroTeleinfoV3HistoryModeJson {
        energyData = new HashMap<>();
    }

    @Override
    public void putEnergyData(String key, MicroTeleinfoV3DataField value) {
        energyData.put(key, value);
    }
}