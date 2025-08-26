package energy.eddie.outbound.metric.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.metric.generated.*;
import energy.eddie.outbound.metric.model.ExtendedPermissionRequestMetrics;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MetricsReportBuilder {

    public PermissionRequestMetrics createMetricsReport(List<PermissionRequestMetricsModel> rows) {
        Map<String, List<PermissionRequestMetricsModel>> regionMetricsMap = rows.stream()
                .collect(Collectors.groupingBy(PermissionRequestMetricsModel::getRegionConnectorId,
                        LinkedHashMap::new, Collectors.toList()));
        var regionConnectorMetrics = getRegionConnectorMetrics(regionMetricsMap);
        return new ExtendedPermissionRequestMetrics(regionConnectorMetrics.getKey(), regionConnectorMetrics.getValue());
    }

    private AbstractMap.SimpleEntry<Integer, List<RegionConnectorMetric>> getRegionConnectorMetrics(Map<String,
            List<PermissionRequestMetricsModel>> map) {
        List<RegionConnectorMetric> regionListMetrics = new ArrayList<>();
        int count = 0;

        for (var regionMetricsEntry : map.entrySet()) {
            Map<String, List<PermissionRequestMetricsModel>> paMetricsMap = regionMetricsEntry.getValue().stream()
                    .collect(Collectors.groupingBy(PermissionRequestMetricsModel::getPermissionAdministratorId,
                            LinkedHashMap::new, Collectors.toList()));
            var paMetrics = getPermissionAdministratorMetrics(paMetricsMap);
            int prPermissionAdministratorCount = paMetrics.getKey();
            regionListMetrics.add(new RegionConnectorMetric(regionMetricsEntry.getKey(), prPermissionAdministratorCount, paMetrics.getValue()));
            count += prPermissionAdministratorCount;
        }

        return new AbstractMap.SimpleEntry<>(count, regionListMetrics);
    }

    private AbstractMap.SimpleEntry<Integer, List<PermissionAdministratorMetric>> getPermissionAdministratorMetrics(
            Map<String, List<PermissionRequestMetricsModel>> map
    ) {
        List<PermissionAdministratorMetric> paListMetrics = new ArrayList<>();
        int count = 0;

        for (var paMetricsEntry : map.entrySet()) {
            Map<String, List<PermissionRequestMetricsModel>> dnTypeMetricsMap = paMetricsEntry.getValue().stream()
                    .collect(Collectors.groupingBy(PermissionRequestMetricsModel::getDataNeedType,
                            LinkedHashMap::new, Collectors.toList()));
            var dnMetrics = getDataNeedMetrics(dnTypeMetricsMap);
            int prDataNeedCount = dnMetrics.getKey();
            paListMetrics.add(new PermissionAdministratorMetric(paMetricsEntry.getKey(), prDataNeedCount, dnMetrics.getValue()));
            count += prDataNeedCount;
        }

        return new AbstractMap.SimpleEntry<>(count, paListMetrics);
    }

    private AbstractMap.SimpleEntry<Integer, List<DataNeedTypeMetric>> getDataNeedMetrics(Map<String,
            List<PermissionRequestMetricsModel>> map) {
        List<DataNeedTypeMetric> dnTypeListMetrics = new ArrayList<>();
        int count = 0;

        for (var dnTypeMetricsEntry : map.entrySet()) {
            Map<PermissionProcessStatus, List<PermissionRequestMetricsModel>> prStateMetricsMap = dnTypeMetricsEntry
                    .getValue().stream().collect(Collectors.groupingBy(PermissionRequestMetricsModel::
                                    getPermissionRequestStatus, LinkedHashMap::new, Collectors.toList()));
            var prStateMetrics = getPermissionRequestStateMetrics(prStateMetricsMap);
            int prStateCount = prStateMetrics.getKey();
            dnTypeListMetrics.add(new DataNeedTypeMetric(dnTypeMetricsEntry.getKey(), prStateCount, prStateMetrics.getValue()));
            count += prStateCount;
        }

        return new AbstractMap.SimpleEntry<>(count, dnTypeListMetrics);
    }

    private AbstractMap.SimpleEntry<Integer, List<PermissionRequestStateMetric>> getPermissionRequestStateMetrics(
            Map<PermissionProcessStatus, List<PermissionRequestMetricsModel>> map
    ) {
        List<PermissionRequestStateMetric> prStateListMetrics = new ArrayList<>();
        int count = 0;

        for (var prStateMetricsEntry : map.entrySet()) {
            for(var prStateMetricsEntryValue : prStateMetricsEntry.getValue()) {
                double mean = prStateMetricsEntryValue.getMean();
                double median = prStateMetricsEntryValue.getMedian();
                count = prStateMetricsEntryValue.getPermissionRequestCount();
                prStateListMetrics.add(new PermissionRequestStateMetric(prStateMetricsEntry.getKey().name(),
                        new Metrics(mean, median, count)));
            }
        }

        return new AbstractMap.SimpleEntry<>(count, prStateListMetrics);
    }
}
