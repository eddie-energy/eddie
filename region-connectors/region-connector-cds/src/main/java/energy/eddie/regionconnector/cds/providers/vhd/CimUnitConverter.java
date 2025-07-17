package energy.eddie.regionconnector.cds.providers.vhd;

import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;

import static energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum;

class CimUnitConverter {
    public static final BigDecimal GAL_TO_CUBIC_METRE = BigDecimal.valueOf(0.00378541);
    public static final BigDecimal FT3_TO_CUBIC_METRE = BigDecimal.valueOf(0.0283168);
    public static final BigDecimal THERM_TO_KWH = BigDecimal.valueOf(29.3001);
    public static final BigDecimal CCF_TO_KWH = THERM_TO_KWH;
    public static final BigDecimal MCF_TO_KWH = THERM_TO_KWH.scaleByPowerOfTen(1);
    public static final BigDecimal MMBTU_TO_KWH = BigDecimal.valueOf(293);
    private final FormatEnum format;

    CimUnitConverter(FormatEnum format) {this.format = format;}

    @Nullable
    public UnitOfMeasureTypeList unit() {
        return convert().unit();
    }

    public BigDecimal convert(BigDecimal old) {
        return convert().conversionFactor().multiply(old);
    }

    private ConversionFactor convert() {
        return switch (format) {
            case KWH_NET, KWH_FWD, KWH_REV, USAGE_KWH, USAGE_FWD_KWH, USAGE_REV_KWH, USAGE_NET_KWH, AGGREGATED_KWH ->
                    new ConversionFactor(UnitOfMeasureTypeList.KILOWATT_HOUR, BigDecimal.ONE);
            case DEMAND_KW -> new ConversionFactor(UnitOfMeasureTypeList.KILOWATT, BigDecimal.ONE);
            case GAS_MMBTU -> new ConversionFactor(UnitOfMeasureTypeList.KILOWATT_HOUR, MMBTU_TO_KWH);
            case GAS_MCF -> new ConversionFactor(UnitOfMeasureTypeList.KILOWATT_HOUR, MCF_TO_KWH);
            case GAS_CCF -> new ConversionFactor(UnitOfMeasureTypeList.KILOWATT_HOUR, CCF_TO_KWH);
            case GAS_THERM -> new ConversionFactor(UnitOfMeasureTypeList.KILOWATT_HOUR, THERM_TO_KWH);
            case WATER_FT3 -> new ConversionFactor(UnitOfMeasureTypeList.CUBIC_METRE, FT3_TO_CUBIC_METRE);
            case WATER_GAL -> new ConversionFactor(UnitOfMeasureTypeList.CUBIC_METRE, GAL_TO_CUBIC_METRE);
            case WATER_M3 -> new ConversionFactor(UnitOfMeasureTypeList.CUBIC_METRE, BigDecimal.ONE);
            case SUPPLY_MIX, EACS -> new ConversionFactor(null, BigDecimal.ONE);
        };
    }

    private record ConversionFactor(UnitOfMeasureTypeList unit, BigDecimal conversionFactor) {}
}
