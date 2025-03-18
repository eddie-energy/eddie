package energy.eddie.regionconnector.cds.providers;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DataNeedFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataNeedFilter.class);
    private final DataNeedsService dataNeedsService;

    public DataNeedFilter(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService) {this.dataNeedsService = dataNeedsService;}

    public IdentifiableUsageSegments filterUsageSegments(IdentifiableUsageSegments usageSegments) {
        var pr = usageSegments.permissionRequest();
        var dn = dataNeedsService.getById(pr.dataNeedId());
        if (!(dn instanceof ValidatedHistoricalDataDataNeed dataNeed)) {
            LOGGER.warn("Invalid data need {} of type {} for permission request {} for validated historical data",
                        dn.id(),
                        dn.type(),
                        pr.permissionId());
            return usageSegments;
        }
        var energyType = dataNeed.energyType();
        for (var segment : usageSegments.payload()) {
            var formats = segment.getFormat();
            var values = segment.getValues();
            var filteredFormats = new ArrayList<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum>(
                    formats.size());
            var filteredValues = new ArrayList<List<Map<String, BigDecimal>>>(values.size());
            for (int i = 0; i < formats.size(); i++) {
                var format = formats.get(i);
                var value = values.get(i);
                if (formatIs(format, energyType)) {
                    filteredFormats.add(format);
                    filteredValues.add(value);
                }
            }
            segment.format(filteredFormats)
                   .values(filteredValues);
        }
        return usageSegments;
    }

    private static boolean formatIs(
            UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum format,
            EnergyType energyType
    ) {
        return switch (format) {
            case KWH_NET, KWH_FWD, KWH_REV, USAGE_KWH, USAGE_FWD_KWH, USAGE_REV_KWH, USAGE_NET_KWH, AGGREGATED_KWH,
                 DEMAND_KW, SUPPLY_MIX -> energyType == EnergyType.ELECTRICITY;
            case GAS_THERM, GAS_CCF, GAS_MCF, GAS_MMBTU -> energyType == EnergyType.NATURAL_GAS;
            case WATER_M3, WATER_GAL, WATER_FT3, EACS -> false;
        };
    }
}
