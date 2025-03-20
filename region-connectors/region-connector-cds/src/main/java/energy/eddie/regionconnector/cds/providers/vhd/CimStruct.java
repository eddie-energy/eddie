package energy.eddie.regionconnector.cds.providers.vhd;

import energy.eddie.regionconnector.cds.openapi.model.AccountsEndpoint200ResponseAllOfAccountsInner;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

class CimStruct {
    private final IdentifiableValidatedHistoricalData historicalData;

    public CimStruct(IdentifiableValidatedHistoricalData historicalData) {this.historicalData = historicalData;}

    public List<Account> get() {
        var payload = historicalData.payload();
        var accounts = new ArrayList<Account>();
        for (var account : payload.accounts()) {
            var meters = getMeters(account, payload);
            var acc = new Account(account.getCustomerNumber(), meters);
            accounts.add(acc);
        }
        return accounts;
    }

    private static List<Account.Meter> getMeters(
            AccountsEndpoint200ResponseAllOfAccountsInner account,
            IdentifiableValidatedHistoricalData.Payload payload
    ) {
        var contracts = payload.findByAccountId(account.getCdsAccountId());
        var meters = new ArrayList<Account.Meter>();
        for (var contract : contracts) {
            var points = payload.findServicePointsByServiceContract(contract.getCdsServicecontractId());
            for (var point : points) {
                var meterDevices = payload.findMeterDevicesByServicePoint(point.getCdsServicepointId());
                for (var meterDevice : meterDevices) {
                    var segments = payload.findUsageSegmentsByMeter(meterDevice.getCdsMeterdeviceId());
                    var usageSegments = getUsageSegments(segments);
                    var meter = new Account.Meter(meterDevice.getMeterNumber(), usageSegments);
                    meters.add(meter);
                }
            }
        }
        return meters;
    }

    private static List<Account.UsageSegment> getUsageSegments(List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner> segments) {
        var usageSegments = new ArrayList<Account.UsageSegment>();
        for (var segment : segments) {
            var usageSegmentValues = flattenUsageSegmentValues(segment);
            var usageSegment = new Account.UsageSegment(segment.getSegmentStart().toZonedDateTime(),
                                                        segment.getSegmentEnd().toZonedDateTime(),
                                                        segment.getInterval(),
                                                        usageSegmentValues);
            usageSegments.add(usageSegment);
        }
        return usageSegments;
    }

    private static Map<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum, List<BigDecimal>> flattenUsageSegmentValues(
            UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner segment
    ) {
        Map<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum, List<BigDecimal>> usageSegmentValues = new EnumMap<>(
                UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.class);
        for (var values : segment.getValues()) {
            for (var i = 0; i < segment.getFormat().size(); i++) {
                var format = segment.getFormat().get(i);
                var value = values.get(i).getOrDefault("v", null);
                var oldValues = usageSegmentValues.getOrDefault(format, new ArrayList<>());
                oldValues.addLast(value);
                usageSegmentValues.put(format, oldValues);
            }
        }
        return usageSegmentValues;
    }
}
