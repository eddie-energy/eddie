package energy.eddie.aiida.adapters.datasource.fr.mode.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.adapters.datasource.fr.mode.MicroTeleinfoV3DataField;
import energy.eddie.aiida.adapters.datasource.fr.mode.MicroTeleinfoV3Json;
import jakarta.annotation.Nullable;

/**
 * Meaning of each value:
 *
 * @param adsc      Meter's secondary address                             (None)
 * @param vtic      TIC version                                           (None)
 * @param date      Current date and time                                 (None)
 * @param ngtf      Name of supplier's tariff calendar                    (None)
 * @param ltarf     Current supplier's tariff label                       (None)
 * @param east      Total extracted active energy                         (Wh)
 * @param easf01    Supplier extracted active energy, index 01            (Wh)
 * @param easf02    Supplier extracted active energy, index 02            (Wh)
 * @param easf03    Supplier extracted active energy, index 03            (Wh)
 * @param easf04    Supplier extracted active energy, index 04            (Wh)
 * @param easf05    Supplier extracted active energy, index 05            (Wh)
 * @param easf06    Supplier extracted active energy, index 06            (Wh)
 * @param easf07    Supplier extracted active energy, index 07            (Wh)
 * @param easf08    Supplier extracted active energy, index 08            (Wh)
 * @param easf09    Supplier extracted active energy, index 09            (Wh)
 * @param easf10    Supplier extracted active energy, index 10            (Wh)
 * @param easd01    Distributor extracted active energy, index 01         (Wh)
 * @param easd02    Distributor extracted active energy, index 02         (Wh)
 * @param easd03    Distributor extracted active energy, index 03         (Wh)
 * @param easd04    Distributor extracted active energy, index 04         (Wh)
 * @param eait      Total injected active energy                          (Wh)
 * @param erq1      Total Q1 reactive energy                              (VArh)
 * @param erq2      Total Q2 reactive energy                              (VArh)
 * @param erq3      Total Q3 reactive energy                              (VArh)
 * @param erq4      Total Q4 reactive energy                              (VArh)
 * @param irms1     Effective current, phase 1                            (A)
 * @param irms2     Effective current, phase 2                            (A)
 * @param irms3     Effective current, phase 3                            (A)
 * @param urms1     Effective voltage, phase 1                            (V)
 * @param urms2     Effective voltage, phase 2                            (V)
 * @param urms3     Effective voltage, phase 3                            (V)
 * @param pref      App. reference power (PREF)                           (kVA)
 * @param pcoup     App. breaking capacity (PCOUP)                        (kVA)
 * @param sinsts    Extracted instantaneous app. power                    (VA)
 * @param sinsts1   Extracted instantaneous app. power phase 1            (VA)
 * @param sinsts2   Extracted instantaneous app. power phase 2            (VA)
 * @param sinsts3   Extracted instantaneous app. power phase 3            (VA)
 * @param smaxsn    Extracted max. app. power n                           (VA)
 * @param smaxsn1   Extracted max. app. power n phase 1                   (VA)
 * @param smaxsn2   Extracted max. app. power n phase 2                   (VA)
 * @param smaxsn3   Extracted max. app. power n phase 3                   (VA)
 * @param smaxsn_1  Extracted max. app. power n-1                         (VA)
 * @param smaxsn1_1 Extracted max. app. power n-1 phase 1                 (VA)
 * @param smaxsn2_1 Extracted max. app. power n-1 phase 2                 (VA)
 * @param smaxsn3_1 Extracted max. app. power n-1 phase 3                 (VA)
 * @param sinsti    Injected instantaneous app. power                     (VA)
 * @param smaxin    Injected max. app. power n                            (VA)
 * @param smaxin_1  Injected max. app. power n-1                          (VA)
 * @param ccasn     Point n of the extracted active load curve            (W)
 * @param ccasn_1   Point n-1 of the extracted active load curve          (W)
 * @param ccain     Point n of the injected active load curve             (W)
 * @param ccain_1   Point n-1 of the injected active load curve           (W)
 * @param umoy1     Mean voltage ph. 1                                    (V)
 * @param umoy2     Mean voltage ph. 2                                    (V)
 * @param umoy3     Mean voltage ph. 3                                    (V)
 * @param stge      Status Register                                       (None)
 * @param dpm1      Start of Mobile 1 Peak Time                           (None)
 * @param fpm1      End of Mobile 1 Peak Time                             (None)
 * @param dpm2      Start of Mobile 2 Peak Time                           (None)
 * @param fpm2      End of Mobile 2 Peak Time                             (None)
 * @param dpm3      Start of Mobile 3 Peak Time                           (None)
 * @param fpm3      End of Mobile 3 Peak Time                             (None)
 * @param msg1      Short message                                         (None)
 * @param msg2      Ultra-short message                                   (None)
 * @param prm       PRM                                                   (None)
 * @param relais    Relay                                                 (None)
 * @param ntarf     Current tariff index number                           (None)
 * @param njourf    Number of current day in supplier's calendar          (None)
 * @param njourf_1  Number of next day in supplier's calendar             (None)
 * @param pjourf_1  Profile of the next day in the supplier's calendar    (None)
 */
public record MicroTeleinfoV3StandardModeJson(@JsonProperty("ADSC") MicroTeleinfoV3DataField adsc,
                                              @JsonProperty("VTIC") MicroTeleinfoV3DataField vtic,
                                              @JsonProperty("DATE") MicroTeleinfoV3DataField date,
                                              @JsonProperty("NGTF") MicroTeleinfoV3DataField ngtf,
                                              @JsonProperty("LTARF") MicroTeleinfoV3DataField ltarf,
                                              @JsonProperty("EAST") MicroTeleinfoV3DataField east,
                                              @JsonProperty("EASF01") MicroTeleinfoV3DataField easf01,
                                              @JsonProperty("EASF02") MicroTeleinfoV3DataField easf02,
                                              @JsonProperty("EASF03") MicroTeleinfoV3DataField easf03,
                                              @JsonProperty("EASF04") MicroTeleinfoV3DataField easf04,
                                              @JsonProperty("EASF05") MicroTeleinfoV3DataField easf05,
                                              @JsonProperty("EASF06") MicroTeleinfoV3DataField easf06,
                                              @JsonProperty("EASF07") MicroTeleinfoV3DataField easf07,
                                              @JsonProperty("EASF08") MicroTeleinfoV3DataField easf08,
                                              @JsonProperty("EASF09") MicroTeleinfoV3DataField easf09,
                                              @JsonProperty("EASF10") MicroTeleinfoV3DataField easf10,
                                              @JsonProperty("EASD01") MicroTeleinfoV3DataField easd01,
                                              @JsonProperty("EASD02") MicroTeleinfoV3DataField easd02,
                                              @JsonProperty("EASD03") MicroTeleinfoV3DataField easd03,
                                              @JsonProperty("EASD04") MicroTeleinfoV3DataField easd04,
                                              @JsonProperty("EAIT") @Nullable MicroTeleinfoV3DataField eait,
                                              @JsonProperty("ERQ1") @Nullable MicroTeleinfoV3DataField erq1,
                                              @JsonProperty("ERQ2") @Nullable MicroTeleinfoV3DataField erq2,
                                              @JsonProperty("ERQ3") @Nullable MicroTeleinfoV3DataField erq3,
                                              @JsonProperty("ERQ4") @Nullable MicroTeleinfoV3DataField erq4,
                                              @JsonProperty("IRMS1") MicroTeleinfoV3DataField irms1,
                                              @JsonProperty("IRMS2") @Nullable MicroTeleinfoV3DataField irms2,
                                              @JsonProperty("IRMS3") @Nullable MicroTeleinfoV3DataField irms3,
                                              @JsonProperty("URMS1") MicroTeleinfoV3DataField urms1,
                                              @JsonProperty("URMS2") @Nullable MicroTeleinfoV3DataField urms2,
                                              @JsonProperty("URMS3") @Nullable MicroTeleinfoV3DataField urms3,
                                              @JsonProperty("PREF") MicroTeleinfoV3DataField pref,
                                              @JsonProperty("PCOUP") MicroTeleinfoV3DataField pcoup,
                                              @JsonProperty("SINSTS") MicroTeleinfoV3DataField sinsts,
                                              @JsonProperty("SINSTS1") @Nullable MicroTeleinfoV3DataField sinsts1,
                                              @JsonProperty("SINSTS2") @Nullable MicroTeleinfoV3DataField sinsts2,
                                              @JsonProperty("SINSTS3") @Nullable MicroTeleinfoV3DataField sinsts3,
                                              @JsonProperty("SMAXSN") MicroTeleinfoV3DataField smaxsn,
                                              @JsonProperty("SMAXSN1") @Nullable MicroTeleinfoV3DataField smaxsn1,
                                              @JsonProperty("SMAXSN2") @Nullable MicroTeleinfoV3DataField smaxsn2,
                                              @JsonProperty("SMAXSN3") @Nullable MicroTeleinfoV3DataField smaxsn3,
                                              @JsonProperty("SMAXSN-1") MicroTeleinfoV3DataField smaxsn_1,
                                              @JsonProperty("SMAXSN1-1") @Nullable MicroTeleinfoV3DataField smaxsn1_1,
                                              @JsonProperty("SMAXSN2-1") @Nullable MicroTeleinfoV3DataField smaxsn2_1,
                                              @JsonProperty("SMAXSN3-1") @Nullable MicroTeleinfoV3DataField smaxsn3_1,
                                              @JsonProperty("SINSTI") @Nullable MicroTeleinfoV3DataField sinsti,
                                              @JsonProperty("SMAXIN") @Nullable MicroTeleinfoV3DataField smaxin,
                                              @JsonProperty("SMAXIN-1") @Nullable MicroTeleinfoV3DataField smaxin_1,
                                              @JsonProperty("CCASN") MicroTeleinfoV3DataField ccasn,
                                              @JsonProperty("CCASN-1") MicroTeleinfoV3DataField ccasn_1,
                                              @JsonProperty("CCAIN") @Nullable MicroTeleinfoV3DataField ccain,
                                              @JsonProperty("CCAIN-1") @Nullable MicroTeleinfoV3DataField ccain_1,
                                              @JsonProperty("UMOY1") MicroTeleinfoV3DataField umoy1,
                                              @JsonProperty("UMOY2") @Nullable MicroTeleinfoV3DataField umoy2,
                                              @JsonProperty("UMOY3") @Nullable MicroTeleinfoV3DataField umoy3,
                                              @JsonProperty("STGE") MicroTeleinfoV3DataField stge,
                                              @JsonProperty("DPM1") @Nullable MicroTeleinfoV3DataField dpm1,
                                              @JsonProperty("FPM1") @Nullable MicroTeleinfoV3DataField fpm1,
                                              @JsonProperty("DPM2") @Nullable MicroTeleinfoV3DataField dpm2,
                                              @JsonProperty("FPM2") @Nullable MicroTeleinfoV3DataField fpm2,
                                              @JsonProperty("DPM3") @Nullable MicroTeleinfoV3DataField dpm3,
                                              @JsonProperty("FPM3") @Nullable MicroTeleinfoV3DataField fpm3,
                                              @JsonProperty("MSG1") MicroTeleinfoV3DataField msg1,
                                              @JsonProperty("MSG2") @Nullable MicroTeleinfoV3DataField msg2,
                                              @JsonProperty("PRM") MicroTeleinfoV3DataField prm,
                                              @JsonProperty("RELAIS") MicroTeleinfoV3DataField relais,
                                              @JsonProperty("NTARF") MicroTeleinfoV3DataField ntarf,
                                              @JsonProperty("NJOURF") MicroTeleinfoV3DataField njourf,
                                              @JsonProperty("NJOURF+1") MicroTeleinfoV3DataField njourf_1,
                                              @JsonProperty("PJOURF+1") MicroTeleinfoV3DataField pjourf_1,
                                              @JsonProperty("PPOINTE") MicroTeleinfoV3DataField ppointe) implements MicroTeleinfoV3Json {
}