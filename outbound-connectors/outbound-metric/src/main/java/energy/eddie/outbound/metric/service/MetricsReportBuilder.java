package energy.eddie.outbound.metric.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.metric.generated.*;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MetricsReportBuilder {

    public PermissionRequestMetrics createMetricsReport(List<PermissionRequestMetricsModel> rows, String eddieId) {
        var regionConnectorMetrics = processMetrics(
                rows,
                PermissionRequestMetricsModel::getRegionConnectorId,
                (regionId, paMetrics) -> new RegionConnectorMetric(
                        regionId,
                        paMetrics.getKey(),
                        paMetrics.getValue()
                ),
                regionConnectorList -> processMetrics(
                        regionConnectorList,
                        PermissionRequestMetricsModel::getPermissionAdministratorId,
                        (permissionAdministratorId, dnMetrics) -> new PermissionAdministratorMetric(
                                permissionAdministratorId,
                                dnMetrics.getKey(),
                                dnMetrics.getValue()
                        ),
                        paMetricsList -> processMetrics(
                                paMetricsList,
                                PermissionRequestMetricsModel::getDataNeedType,
                                (dataNeedType, prMetrics) -> new DataNeedTypeMetric(
                                        dataNeedType,
                                        prMetrics.getKey(),
                                        prMetrics.getValue()
                                ),
                                dnTypeMetricsList -> {
                                    var prStateMetricsMap = dnTypeMetricsList.stream().collect(Collectors.groupingBy(
                                            PermissionRequestMetricsModel::getPermissionRequestStatus,
                                            LinkedHashMap::new,
                                            Collectors.toList()
                                    ));
                                    return getPermissionRequestStateMetrics(prStateMetricsMap);
                                }
                        )
                )
        );

        return new PermissionRequestMetrics(eddieId, regionConnectorMetrics.getKey(), regionConnectorMetrics.getValue());
    }

    private <T, K, C, R> AbstractMap.SimpleEntry<Integer, List<R>> processMetrics(
            List<T> input,
            Function<T, K> groupingFunction,
            BiFunction<K, AbstractMap.SimpleEntry<Integer, List<C>>, R> metricsMap,
            Function<List<T>, AbstractMap.SimpleEntry<Integer, List<C>>> resultFunction
    ) {
        var metrics = input.stream().collect(Collectors.groupingBy(
                groupingFunction,
                LinkedHashMap::new, 
                Collectors.toList()
        ));

        List<R> metricsList = new ArrayList<>();
        int count = 0;

        for (var entry : metrics.entrySet()) {
            var resultMetrics = resultFunction.apply(entry.getValue());
            count += resultMetrics.getKey();
            metricsList.add(metricsMap.apply(entry.getKey(), resultMetrics));
        }

        return new AbstractMap.SimpleEntry<>(count, metricsList);
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
                prStateListMetrics.add(new PermissionRequestStateMetric(
                        prStateMetricsEntry.getKey().name(),
                        new Metrics(mean, median, count)
                ));
            }
        }

        return new AbstractMap.SimpleEntry<>(count, prStateListMetrics);
    }
}
