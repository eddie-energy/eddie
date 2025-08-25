package energy.eddie.outbound.metric.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.metric.generated.*;
import energy.eddie.outbound.metric.model.Metrics;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MetricsReportBuilder {

    public PermissionRequestMetrics createMetricsReport(String instance, List<PermissionRequestMetricsModel> rows) {
        PermissionRequestMetrics metricsReport = new PermissionRequestMetrics();
        metricsReport.setInstance(instance);

        Map<String, List<PermissionRequestMetricsModel>> regionMetricsMap = rows.stream()
                .collect(Collectors.groupingBy(PermissionRequestMetricsModel::getRegionConnectorId,
                        LinkedHashMap::new, Collectors.toList()));
        var regionConnectorMetrics = getRegionConnectorMetrics(regionMetricsMap);
        Metrics count = new Metrics(regionConnectorMetrics.getKey());
        metricsReport.setMetrics(count);
        metricsReport.setRegionConnectorMetrics(regionConnectorMetrics.getValue());
        return metricsReport;
    }

    private AbstractMap.SimpleEntry<Integer, List<RegionConnectorMetric>> getRegionConnectorMetrics(Map<String,
            List<PermissionRequestMetricsModel>> map) {
        List<RegionConnectorMetric> regionListMetrics = new ArrayList<>();
        int count = 0;

        for (var regionMetricsEntry : map.entrySet()) {
            RegionConnectorMetric regionMetrics = new RegionConnectorMetric();
            regionMetrics.setId(regionMetricsEntry.getKey());

            Map<String, List<PermissionRequestMetricsModel>> paMetricsMap = regionMetricsEntry.getValue().stream()
                    .collect(Collectors.groupingBy(PermissionRequestMetricsModel::getPermissionAdministratorId,
                            LinkedHashMap::new, Collectors.toList()));

            var paMetrics = getPermissionAdministratorMetrics(paMetricsMap);
            int prPermissionAdministratorCount = paMetrics.getKey();

            Metrics regionCount = new Metrics(prPermissionAdministratorCount);
            regionMetrics.setMetrics(regionCount);
            regionMetrics.setPermissionAdministratorMetrics(paMetrics.getValue());
            regionListMetrics.add(regionMetrics);
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
            PermissionAdministratorMetric paMetrics = new PermissionAdministratorMetric();
            paMetrics.setId(paMetricsEntry.getKey());

            Map<String, List<PermissionRequestMetricsModel>> dnTypeMetricsMap = paMetricsEntry.getValue().stream()
                    .collect(Collectors.groupingBy(PermissionRequestMetricsModel::getDataNeedType,
                            LinkedHashMap::new, Collectors.toList()));

            var dnMetrics = getDataNeedMetrics(dnTypeMetricsMap);
            int prDataNeedCount = dnMetrics.getKey();

            Metrics paCount = new Metrics(prDataNeedCount);
            paMetrics.setMetrics(paCount);
            paMetrics.setDataNeedTypeMetrics(dnMetrics.getValue());
            paListMetrics.add(paMetrics);
            count += prDataNeedCount;
        }

        return new AbstractMap.SimpleEntry<>(count, paListMetrics);
    }

    private AbstractMap.SimpleEntry<Integer, List<DataNeedTypeMetric>> getDataNeedMetrics(Map<String,
            List<PermissionRequestMetricsModel>> map) {
        List<DataNeedTypeMetric> dnTypeListMetrics = new ArrayList<>();
        int count = 0;

        for (var dnTypeMetricsEntry : map.entrySet()) {
            DataNeedTypeMetric dnTypeMetrics = new DataNeedTypeMetric ();
            dnTypeMetrics.setDnType(dnTypeMetricsEntry.getKey());
            Map<PermissionProcessStatus, List<PermissionRequestMetricsModel>> prStateMetricsMap = dnTypeMetricsEntry
                    .getValue().stream().collect(Collectors.groupingBy(PermissionRequestMetricsModel::
                                    getPermissionRequestStatus, LinkedHashMap::new, Collectors.toList()));

            var prStateMetrics = getPermissionRequestStateMetrics(prStateMetricsMap);
            int prStateCount = prStateMetrics.getKey();


            Metrics dnTypeCount = new Metrics(prStateCount);
            dnTypeMetrics.setMetrics(dnTypeCount);
            dnTypeMetrics.setPermissionRequestStateMetrics(prStateMetrics.getValue());
            dnTypeListMetrics.add(dnTypeMetrics);
            count += prStateCount;
        }

        return  new AbstractMap.SimpleEntry<>(count, dnTypeListMetrics);
    }

    private AbstractMap.SimpleEntry<Integer, List<PermissionRequestStateMetric>> getPermissionRequestStateMetrics(
            Map<PermissionProcessStatus, List<PermissionRequestMetricsModel>> map
    ) {
        List<PermissionRequestStateMetric> prStateListMetrics = new ArrayList<>();
        int count = 0;

        for (var prStateMetricsEntry : map.entrySet()) {
            PermissionRequestStateMetric prStateMetrics = new PermissionRequestStateMetric();
            prStateMetrics.setId(prStateMetricsEntry.getKey().name());
            for(var prStateMetricsEntryValue : prStateMetricsEntry.getValue()) {
                double mean = prStateMetricsEntryValue.getMean();
                double median = prStateMetricsEntryValue.getMedian();
                count = prStateMetricsEntryValue.getPermissionRequestCount();
                prStateMetrics.setMetrics(new Metrics(mean, median, count));
            }
            prStateListMetrics.add(prStateMetrics);
        }

        return new AbstractMap.SimpleEntry<>(count, prStateListMetrics);
    }
}
