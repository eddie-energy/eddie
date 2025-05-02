package energy.eddie.aiida.adapters.datasource.fr.transformer.standard;

import com.fasterxml.jackson.annotation.JsonIgnore;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3DataField;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3Json;

import java.util.HashMap;
import java.util.Map;

/**
 * Meaning of each value:
 *
 * <p>              Meter's secondary address                             (None) <br>
 * <i>adsc</i>      TIC version                                           (None) <br>
 * <i>vtic</i>      Current date and time                                 (None) <br>
 * <i>date</i>      Name of supplier's tariff calendar                    (None) <br>
 * <i>ngtf</i>      Current supplier's tariff label                       (None) <br>
 * <i>ltarf</i>     Total extracted active energy                         (Wh)   <br>
 * <i>east</i>      Supplier extracted active energy, index 01            (Wh)   <br>
 * <i>easf01</i>    Supplier extracted active energy, index 02            (Wh)   <br>
 * <i>easf02</i>    Supplier extracted active energy, index 03            (Wh)   <br>
 * <i>easf03</i>    Supplier extracted active energy, index 04            (Wh)   <br>
 * <i>easf04</i>    Supplier extracted active energy, index 05            (Wh)   <br>
 * <i>easf05</i>    Supplier extracted active energy, index 06            (Wh)   <br>
 * <i>easf06</i>    Supplier extracted active energy, index 07            (Wh)   <br>
 * <i>easf07</i>    Supplier extracted active energy, index 08            (Wh)   <br>
 * <i>easf08</i>    Supplier extracted active energy, index 09            (Wh)   <br>
 * <i>easf09</i>    Supplier extracted active energy, index 10            (Wh)   <br>
 * <i>easf10</i>    Distributor extracted active energy, index 01         (Wh)   <br>
 * <i>easd01</i>    Distributor extracted active energy, index 02         (Wh)   <br>
 * <i>easd02</i>    Distributor extracted active energy, index 03         (Wh)   <br>
 * <i>easd03</i>    Distributor extracted active energy, index 04         (Wh)   <br>
 * <i>easd04</i>    Total injected active energy                          (Wh)   <br>
 * <i>eait</i>      Total Q1 reactive energy                              (VArh) <br>
 * <i>erq1</i>      Total Q2 reactive energy                              (VArh) <br>
 * <i>erq2</i>      Total Q3 reactive energy                              (VArh) <br>
 * <i>erq3</i>      Total Q4 reactive energy                              (VArh) <br>
 * <i>erq4</i>      Effective current, phase 1                            (A)    <br>
 * <i>irms1</i>     Effective current, phase 2                            (A)    <br>
 * <i>irms2</i>     Effective current, phase 3                            (A)    <br>
 * <i>irms3</i>     Effective voltage, phase 1                            (V)    <br>
 * <i>urms1</i>     Effective voltage, phase 2                            (V)    <br>
 * <i>urms2</i>     Effective voltage, phase 3                            (V)    <br>
 * <i>urms3</i>     App. reference power (PREF)                           (kVA)  <br>
 * <i>pref</i>      App. breaking capacity (PCOUP)                        (kVA)  <br>
 * <i>pcoup</i>     Extracted instantaneous app. power                    (VA)   <br>
 * <i>sinsts</i>    Extracted instantaneous app. power phase 1            (VA)   <br>
 * <i>sinsts1</i>   Extracted instantaneous app. power phase 2            (VA)   <br>
 * <i>sinsts2</i>   Extracted instantaneous app. power phase 3            (VA)   <br>
 * <i>sinsts3</i>   Extracted max. app. power n                           (VA)   <br>
 * <i>smaxsn</i>    Extracted max. app. power n phase 1                   (VA)   <br>
 * <i>smaxsn1</i>   Extracted max. app. power n phase 2                   (VA)   <br>
 * <i>smaxsn2</i>   Extracted max. app. power n phase 3                   (VA)   <br>
 * <i>smaxsn3</i>   Extracted max. app. power n-1                         (VA)   <br>
 * <i>smaxsn_1</i>  Extracted max. app. power n-1 phase 1                 (VA)   <br>
 * <i>smaxsn1_1</i> Extracted max. app. power n-1 phase 2                 (VA)   <br>
 * <i>smaxsn2_1</i> Extracted max. app. power n-1 phase 3                 (VA)   <br>
 * <i>smaxsn3_1</i> Injected instantaneous app. power                     (VA)   <br>
 * <i>sinsti</i>    Injected max. app. power n                            (VA)   <br>
 * <i>smaxin</i>    Injected max. app. power n-1                          (VA)   <br>
 * <i>smaxin_1</i>  Point n of the extracted active load curve            (W)    <br>
 * <i>ccasn</i>     Point n-1 of the extracted active load curve          (W)    <br>
 * <i>ccasn_1</i>   Point n of the injected active load curve             (W)    <br>
 * <i>ccain</i>     Point n-1 of the injected active load curve           (W)    <br>
 * <i>ccain_1</i>   Mean voltage ph. 1                                    (V)    <br>
 * <i>umoy1</i>     Mean voltage ph. 2                                    (V)    <br>
 * <i>umoy2</i>     Mean voltage ph. 3                                    (V)    <br>
 * <i>umoy3</i>     Status Register                                       (None) <br>
 * <i>stge</i>      Start of Mobile 1 Peak Time                           (None) <br>
 * <i>dpm1</i>      End of Mobile 1 Peak Time                             (None) <br>
 * <i>fpm1</i>      Start of Mobile 2 Peak Time                           (None) <br>
 * <i>dpm2</i>      End of Mobile 2 Peak Time                             (None) <br>
 * <i>fpm2</i>      Start of Mobile 3 Peak Time                           (None) <br>
 * <i>dpm3</i>      End of Mobile 3 Peak Time                             (None) <br>
 * <i>fpm3</i>      Short message                                         (None) <br>
 * <i>msg1</i>      Ultra-short message                                   (None) <br>
 * <i>msg2</i>      PRM                                                   (None) <br>
 * <i>prm</i>       Relay                                                 (None) <br>
 * <i>relais</i>    Current tariff index number                           (None) <br>
 * <i>ntarf</i>     Number of current day in supplier's calendar          (None) <br>
 * <i>njourf</i>    Number of next day in supplier's calendar             (None) <br>
 * <i>njourf_1</i>  Profile of the next day in the supplier's calendar    (None) <br>
 * </p>
 */
public record MicroTeleinfoV3StandardModeJson(
        @JsonIgnore Map<String, MicroTeleinfoV3DataField> energyData) implements MicroTeleinfoV3Json {

    public MicroTeleinfoV3StandardModeJson {
        energyData = new HashMap<>();
    }

    @Override
    public void putEnergyData(String key, MicroTeleinfoV3DataField value) {
        this.energyData.put(key.replaceAll("[-+]", "_"), value);
    }
}