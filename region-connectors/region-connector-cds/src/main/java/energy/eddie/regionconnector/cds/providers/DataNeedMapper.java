package energy.eddie.regionconnector.cds.providers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;
import energy.eddie.regionconnector.cds.providers.vhd.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.shared.validation.GranularityChoice;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum;

@Component
public class DataNeedMapper implements UnaryOperator<IdentifiableValidatedHistoricalData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataNeedMapper.class);
    private final DataNeedsService dataNeedsService;

    public DataNeedMapper(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService) {this.dataNeedsService = dataNeedsService;}

    @Override
    public IdentifiableValidatedHistoricalData apply(IdentifiableValidatedHistoricalData data) {
        return filterUsageSegments(data);
    }

    public IdentifiableValidatedHistoricalData filterUsageSegments(IdentifiableValidatedHistoricalData data) {
        var pr = data.permissionRequest();
        var dn = dataNeedsService.getById(pr.dataNeedId());
        if (!(dn instanceof ValidatedHistoricalDataDataNeed dataNeed)) {
            LOGGER.warn("Invalid data need {} of type {} for permission request {} for validated historical data",
                        dn.id(),
                        dn.type(),
                        pr.permissionId());
            return data;
        }
        var oldSegments = data.payload().usageSegments();

        var newSegments = filterSegments(pr.permissionId(), oldSegments, dataNeed);
        return new IdentifiableValidatedHistoricalData(
                pr,
                new IdentifiableValidatedHistoricalData.Payload(
                        data.payload().accounts(),
                        data.payload().serviceContracts(),
                        data.payload().servicePoints(),
                        data.payload().meterDevices(),
                        newSegments
                )
        );
    }

    private static ArrayList<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner> filterSegments(
            String permissionId,
            List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner> oldSegments,
            ValidatedHistoricalDataDataNeed dataNeed
    ) {
        var energyType = dataNeed.energyType();
        var newSegments = new ArrayList<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner>(oldSegments.size());
        for (var segment : oldSegments) {
            if (!isIntervalAllowed(dataNeed, segment)) {
                LOGGER.atInfo()
                      .addArgument(segment::getInterval)
                      .addArgument(permissionId)
                      .log("Unknown granularity for interval {}s of permission request {}");
                continue;
            }
            var result = filterByEnergyType(energyType, segment);
            if (result != null) {
                newSegments.add(result);
            }
        }
        return newSegments;
    }

    private static boolean isIntervalAllowed(
            ValidatedHistoricalDataDataNeed dataNeed,
            UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner segment
    ) {
        var intervalInMinutes = segment.getInterval().divide(BigDecimal.valueOf(60), RoundingMode.HALF_UP);
        try {
            var granularity = Granularity.fromMinutes(intervalInMinutes.intValue());
            var allowed = GranularityChoice.isBetween(granularity,
                                                      dataNeed.minGranularity(),
                                                      dataNeed.maxGranularity());
            if (!allowed) {
                return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    @Nullable
    private static UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner filterByEnergyType(
            EnergyType energyType,
            UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner segment
    ) {
        var formats = segment.getFormat();
        var values = segment.getValues();
        var filteredFormats = new ArrayList<FormatEnum>(formats.size());
        var filteredValues = new ArrayList<List<Map<String, BigDecimal>>>(values.size());
        for (int formatColumn = 0; formatColumn < formats.size(); formatColumn++) {
            var format = formats.get(formatColumn);
            if (!formatIs(format, energyType)) {continue;}
            filteredFormats.add(format);
            for (var valueRow = 0; valueRow < values.size(); valueRow++) {
                var allowedValue = values.get(valueRow).get(formatColumn);
                if (filteredValues.size() <= valueRow) {
                    filteredValues.add(new ArrayList<>(List.of(allowedValue)));
                } else {
                    filteredValues.get(valueRow).add(allowedValue);
                }
            }
        }
        if (filteredFormats.isEmpty()) {
            return null;
        }
        return segment.format(filteredFormats)
                      .values(filteredValues);
    }


    private static boolean formatIs(
            FormatEnum format,
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
