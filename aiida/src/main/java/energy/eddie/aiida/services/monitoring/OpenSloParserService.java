package energy.eddie.aiida.services.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import energy.eddie.aiida.models.monitoring.PrometheusResult;
import energy.eddie.aiida.models.monitoring.ServiceSla;
import energy.eddie.aiida.models.monitoring.ServiceSlo;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.EvaluationStatus;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.EvaluationType;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.near_realtime.BurnRateEvaluationResult;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.near_realtime.NearRealtimeEvaluationResult;
import energy.eddie.aiida.models.monitoring.openslo.alert.AlertPolicyConditionValue;
import energy.eddie.aiida.models.monitoring.openslo.alert.AlertPolicyDocument;
import energy.eddie.aiida.models.monitoring.openslo.sli.SliDocument;
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
public class OpenSloParserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSloParserService.class);
    private final List<ServiceSla> services = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Instant> firstViolationTimestamps = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 5000)
    void parseSlo() throws IOException {
        if (!services.isEmpty()) {
            LOGGER.info("Services already parsed, evaluating...");
            evaluateNearRealtime();
            return;
        }
        LOGGER.debug("Parsing SLO data...");

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        var sloInput = readOpenSloDocument("aiida-ec-service-response-time-slo.yaml");
        var slo = objectMapper.readValue(sloInput, SloDocument.class);

        var sliInput = readOpenSloDocument("aiida-ec-service-response-time-sli.yaml");
        var sli = objectMapper.readValue(sliInput, SliDocument.class);

        var alertInput = readOpenSloDocument("aiida-ec-service-response-time-alert.yaml");
        var alert = objectMapper.readValue(alertInput, AlertPolicyDocument.class);

        services.add(new ServiceSla("aiida-ec-service", List.of(new ServiceSlo(slo, sli, alert))));
    }

    @Scheduled(fixedRate = 10000)
    void evaluateNearRealtime() {
        for (ServiceSla service : services) {
            LOGGER.info("Evaluating service: {}", service.getName());

            for (ServiceSlo slo : service.getSlos()) {
                var sloId = slo.getSlo().getMetadata().getName();
                LOGGER.info("Evaluating SLO: {}", sloId);

                var conditionSpec = slo.getAlertPolicy().getSpec().getConditions().getFirst().getSpec();
                var condition = conditionSpec.getCondition();
                var queries = slo.getSli().getSpec().getIndicator().getRatioMetric();
                var burnRateQuery = buildBurnRateQuery(
                        queries.getGood().getQuery(),
                        queries.getTotal().getQuery(),
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
                    status = EvaluationStatus.NO_DATA;
                } else if (isViolated) {
                    if (duration.toMinutes() >= alertAfterMinutes) {
                        status = EvaluationStatus.VIOLATION;
                    } else {
                        status = EvaluationStatus.PENDING;
                    }
                } else {
                    status = EvaluationStatus.COMPLIANT;
                }

                var result = buildResult(sloId, service.getName(), status, condition, burnRate);
                try {
                    LOGGER.info("Evaluation result: {}", mapper.writeValueAsString(result));
                } catch (IOException e) {
                    LOGGER.error("Failed to serialize result", e);
                }
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

    NearRealtimeEvaluationResult buildResult(String sloId, String service, EvaluationStatus status,
                                             AlertPolicyConditionValue condition, double currentBurnRate) {
        Instant now = Instant.now();
        Duration lookbackDuration = parseDuration(condition.getLookbackWindow());
        Instant from = now.minus(lookbackDuration);

        return new NearRealtimeEvaluationResult(
                Instant.now().toString(),
                service,
                sloId,
                EvaluationType.NEAR_REALTIME,
                status,
                new BurnRateEvaluationResult(
                        from.toString(),
                        now.toString(),
                        condition.getLookbackWindow(),
                        Math.round(currentBurnRate * 100.0) / 100.0,
                        condition.getThreshold()
                )
        );
    }

    Optional<Double> queryPrometheus(String query) {
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

    String buildBurnRateQuery(String goodQuery, String totalQuery, String lookbackWindow) {
        String good = adjustLookbackWindow(goodQuery, lookbackWindow);
        String total = adjustLookbackWindow(totalQuery, lookbackWindow);

        return String.format("%s / %s", good, total);
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