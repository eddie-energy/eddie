package energy.eddie.aiida.services.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import energy.eddie.aiida.models.monitoring.PrometheusResult;
import energy.eddie.aiida.models.monitoring.ServiceSla;
import energy.eddie.aiida.models.monitoring.ServiceSlo;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.*;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.full_compliance.FullComplianceEvaluationResult;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.near_realtime.NearRealtimeEvaluationResult;
import energy.eddie.aiida.models.monitoring.openslo.alert.AlertPolicyDocument;
import energy.eddie.aiida.models.monitoring.openslo.sli.SliDocument;
import energy.eddie.aiida.models.monitoring.openslo.sli.SliIndicator;
import energy.eddie.aiida.models.monitoring.openslo.slo.SloDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OpenSloEvaluationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSloEvaluationService.class);
    private final List<ServiceSla> services = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Instant> firstViolationTimestamps = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 5000)
    void parseSlo() throws IOException {
        if (!services.isEmpty()) {
            LOGGER.info("Services already parsed, evaluating...");
            //evaluateNearRealtime();
            evaluateFullCompliance();
            return;
        }
        LOGGER.debug("Parsing SLO data...");

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        List<String> sloTypes = List.of("response-time", "latency");
        String serviceName = "aiida-ec-service";
        List<ServiceSlo> serviceSlos = new ArrayList<>();

        for (String sloType : sloTypes) {
            String pathPrefix = "http/" + sloType + "/" + serviceName + "-";

            var sloInput = readOpenSloDocument(pathPrefix + sloType + "-slo.yaml");
            var sliInput = readOpenSloDocument(pathPrefix + sloType + "-sli.yaml");
            var alertInput = readOpenSloDocument(pathPrefix + sloType + "-alert.yaml");

            var slo = objectMapper.readValue(sloInput, SloDocument.class);
            var sli = objectMapper.readValue(sliInput, SliDocument.class);
            var alert = objectMapper.readValue(alertInput, AlertPolicyDocument.class);

            serviceSlos.add(new ServiceSlo(slo, sli, alert));
        }

        services.add(new ServiceSla(serviceName, serviceSlos));
    }

    void evaluateNearRealtime() {
        LOGGER.info("Evaluating near real-time compliance...");
        for (ServiceSla service : services) {
            LOGGER.info("Evaluating service: {}", service.getName());
            List<EvaluationResult> evaluationResults = new ArrayList<>();
            String timestamp = Instant.now().toString();

            for (ServiceSlo slo : service.getSlos()) {
                var sloId = slo.getSlo().getMetadata().getName();
                LOGGER.info("Evaluating SLO: {}", sloId);

                var conditionSpec = slo.getAlertPolicy().getSpec().getConditions().getFirst().getSpec();
                var condition = conditionSpec.getCondition();
                var indicator = slo.getSli().getSpec().getIndicator();

                var burnRateQuery = buildPrometheusQuery(
                        indicator,
                        condition.getLookbackWindow()
                );

                Optional<Double> value = queryPrometheus(burnRateQuery);
                double burnRate = value.orElse(0.0);

                boolean isViolated = value.isPresent() && evaluateCondition(condition.getOp(), burnRate, condition.getThreshold());
                Instant now = Instant.now();
                Duration duration = Duration.ZERO;
                long alertAfterMinutes = parseDurationToMinutes(condition.getAlertAfter());

                if (isViolated) {
                    Instant firstSeen = firstViolationTimestamps.computeIfAbsent(sloId, k -> now);
                    duration = Duration.between(firstSeen, now);
                } else {
                    firstViolationTimestamps.remove(sloId);
                }

                EvaluationStatus status;
                if (value.isEmpty()) {
                    status = EvaluationStatus.NO_METRICS;
                } else if (isViolated) {
                    if (duration.toMinutes() >= alertAfterMinutes) {
                        status = EvaluationStatus.VIOLATION;
                    } else {
                        status = EvaluationStatus.PENDING;
                    }
                } else {
                    status = EvaluationStatus.COMPLIANT;
                }

                var result = (NearRealtimeEvaluationResult)
                        buildResult(sloId, status, condition.getLookbackWindow(), burnRate, condition.getThreshold(), EvaluationType.NEAR_REALTIME);

                evaluationResults.add(result);
            }

            List<EvaluationStatus> statuses = evaluationResults.stream()
                                                               .map(EvaluationResult::getStatus)
                                                               .toList();

            var serviceEvaluationResult = new ServiceEvaluationResult(
                    timestamp, service.getName(),
                    EvaluationType.NEAR_REALTIME,
                    ServiceStatus.computeServiceStatus(statuses),
                    evaluationResults);

            try {
                LOGGER.info("Service evaluation result: {}", mapper.writeValueAsString(serviceEvaluationResult));
                evaluationResults.clear();
            }
            catch (IOException e) {
                LOGGER.error("Failed to serialize evaluation result", e);
            }
        }
    }

    void evaluateFullCompliance() {
        LOGGER.info("Evaluating full compliance...");
        for (ServiceSla service : services) {
            LOGGER.info("Evaluating service (full): {}", service.getName());
            List<EvaluationResult> evaluationResults = new ArrayList<>();
            String timestamp = Instant.now().toString();

            for (ServiceSlo slo : service.getSlos()) {
                var sloId = slo.getSlo().getMetadata().getName();
                LOGGER.info("Evaluating SLO (full): {}", sloId);

                var conditionSpec = slo.getAlertPolicy().getSpec().getConditions().getFirst().getSpec();
                var objective = slo.getSlo().getSpec().getObjective();
                var indicator = slo.getSli().getSpec().getIndicator();
                var timeWindow = slo.getSlo().getSpec().getTimeWindow();

                var fullComplianceQuery = buildPrometheusQuery(
                        indicator,
                        timeWindow.getDuration()
                );

                Optional<Double> value = queryPrometheus(fullComplianceQuery);
                double fullComplianceRate = value.orElse(0.0);

                boolean isViolated = value.isPresent()
                                     && evaluateCondition(objective.getOp(), fullComplianceRate, objective.getTarget());

                EvaluationStatus fullComplianceStatus;
                if (value.isEmpty()) {
                    fullComplianceStatus = EvaluationStatus.NO_METRICS;
                } else if (isViolated) {
                    fullComplianceStatus = EvaluationStatus.VIOLATION;
                } else {
                    fullComplianceStatus = EvaluationStatus.COMPLIANT;
                }

                var result = (FullComplianceEvaluationResult)
                        buildResult(sloId, fullComplianceStatus, timeWindow.getDuration(),
                                    fullComplianceRate, objective.getTarget(), EvaluationType.COMPLIANCE);
                evaluationResults.add(result);
            }
            List<EvaluationStatus> statuses = evaluationResults.stream()
                                                               .map(EvaluationResult::getStatus)
                                                               .toList();

            var serviceEvaluationResult = new ServiceEvaluationResult(
                    timestamp, service.getName(),
                    EvaluationType.NEAR_REALTIME,
                    ServiceStatus.computeServiceStatus(statuses),
                    evaluationResults);

            try {
                LOGGER.info("Service evaluation result: {}", mapper.writeValueAsString(serviceEvaluationResult));
                evaluationResults.clear();
            }
            catch (IOException e) {
                LOGGER.error("Failed to serialize evaluation result", e);
            }
        }
    }

    public boolean evaluateCondition(String op, double actual, double target) {
        return switch (op.toLowerCase()) {
            case "gt" -> actual > target;
            case "gte" -> actual >= target;
            case "lt" -> actual < target;
            case "lte" -> actual <= target;
            case "eq" -> actual == target;
            default -> false;
        };
    }

    public long parseDurationToMinutes(String alertAfter) {
        if (alertAfter.endsWith("m")) {
            return Long.parseLong(alertAfter.replace("m", ""));
        }
        if (alertAfter.endsWith("h")) {
            return Long.parseLong(alertAfter.replace("h", "")) * 60;
        }
        return 0;
    }

    EvaluationResult buildResult(String sloId, EvaluationStatus status,
                                 String lookbackWindow, double currentRate, double threshold, EvaluationType evaluationType) {
        var now = Instant.now();
        var lookbackDuration = parseDuration(lookbackWindow);
        var from = now.minus(lookbackDuration);
        var evaluationPeriod = new EvaluationPeriod(
                from.toString(),
                now.toString(),
                lookbackWindow
        );
        var rateEvaluationResult = new RateEvaluationResult(
                Math.round(currentRate * 100.0) / 100.0,
                threshold
        );


        if (evaluationType == EvaluationType.NEAR_REALTIME) {
            return new NearRealtimeEvaluationResult(
                    sloId,
                    status,
                    evaluationPeriod,
                    rateEvaluationResult
            );
        } else {
            return new FullComplianceEvaluationResult(
                    sloId,
                    status,
                    evaluationPeriod,
                    rateEvaluationResult
            );
        }
    }

    Optional<Double> queryPrometheus(String query) {
        LOGGER.trace("Querying Prometheus: {}", query);
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String prometheusUrl = "http://localhost:9090/api/v1/query?query=" + encodedQuery;

            HttpURLConnection connection = (HttpURLConnection) new URL(prometheusUrl).openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder content = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }

                    ObjectMapper mapper = new ObjectMapper();
                    PrometheusResult result = mapper.readValue(content.toString(), PrometheusResult.class);

                    if (result.data != null && !result.data.result.isEmpty()) {
                        List<Object> value = result.data.result.getFirst().value;
                        if (value.size() >= 2) {
                            double parsed = Double.parseDouble(value.get(1).toString());
                            return Optional.of(parsed);
                        }
                    }
                }
            } else {
                LOGGER.warn("Prometheus query failed: HTTP {}", responseCode);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to query Prometheus", e);
        }

        return Optional.empty();
    }

    String buildPrometheusQuery(SliIndicator indicator, String lookbackWindow) {
        String query = "";
        if (indicator.isThresholdMetric()) {
            query = adjustLookbackWindow(indicator.getThresholdMetric().getQuery(), lookbackWindow);
        } else if (indicator.isRatioMetric()) {
            String good = adjustLookbackWindow(indicator.getRatioMetric().getGood().getQuery(), lookbackWindow);
            String total = adjustLookbackWindow(indicator.getRatioMetric().getTotal().getQuery(), lookbackWindow);

            query = String.format("%s / %s", good, total);
        }

        return query;
    }

    String adjustLookbackWindow(String baseQuery, String newWindow) {
        return baseQuery.replaceAll("\\[\\s*\\d+[smhdwy]\\s*\\]", "[" + newWindow + "]");
    }

    Duration parseDuration(String input) {
        if (input.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(input.replace("m", "")));
        }
        if (input.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(input.replace("h", "")));
        }
        if (input.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(input.replace("s", "")));
        }
        if (input.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(input.replace("d", "")));
        }
        throw new IllegalArgumentException("Unsupported duration format: " + input);
    }

    InputStream readOpenSloDocument(String filePath) {
        String classpathFilePath = "openslo/" + filePath;

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(classpathFilePath);
        if (inputStream == null) {
            LOGGER.error("Failed to load OpenSLO file: " + classpathFilePath);
            throw new RuntimeException("Failed to load OpenSLO file: " + classpathFilePath);
        }

        return inputStream;
    }
}