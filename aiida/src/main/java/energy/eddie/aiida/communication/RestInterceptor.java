package energy.eddie.aiida.communication;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RestInterceptor implements ClientHttpRequestInterceptor {
    public static final String SERVICE_NAME_HEADER = "X-Service-Name";

    private static final Logger LOGGER = LoggerFactory.getLogger(RestInterceptor.class);
    private static final String METRIC_RESPONSE_TIME = "http.response.time";
    private static final String METRIC_TOTAL_REQUESTS = "http.requests.total";
    private static final String METRIC_FAILED_REQUESTS = "http.requests.failed";

    private static final String TAG_URI = "uri";
    private static final String TAG_METHOD = "method";
    private static final String TAG_STATUS = "status";
    private static final String TAG_SERVICE = "service";

    private final MeterRegistry meterRegistry;

    @Autowired
    public RestInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        Instant start = Instant.now();
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        String uriPath = request.getURI().getPath();
        String method = request.getMethod().name();
        String service = request.getHeaders().getFirst(SERVICE_NAME_HEADER);

        meterRegistry.counter(METRIC_TOTAL_REQUESTS,
                              TAG_URI, uriPath,
                              TAG_METHOD, method,
                              TAG_SERVICE, service != null ? service : "unknown").increment();

        try {
            ClientHttpResponse response = execution.execute(request, body);
            statusCode = response.getStatusCode().value();
            return response;
        } catch (IOException e) {
            LOGGER.error("Request to {} failed: {}", uriPath, e.getMessage());

            meterRegistry.counter(METRIC_FAILED_REQUESTS,
                                  TAG_URI, uriPath,
                                  TAG_METHOD, method,
                                  TAG_SERVICE, service != null ? service : "unknown").increment();

            throw e;
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            String status = String.valueOf(statusCode);

            Timer.builder(METRIC_RESPONSE_TIME)
                 .tags(List.of(
                         Tag.of(TAG_URI, uriPath),
                         Tag.of(TAG_METHOD, method),
                         Tag.of(TAG_STATUS, status),
                         Tag.of(TAG_SERVICE, service != null ? service : "unknown")
                 ))
                 .publishPercentileHistogram(true)
                 .register(meterRegistry)
                 .record(duration.toMillis(), TimeUnit.MILLISECONDS);

            if (statusCode >= HttpStatus.BAD_REQUEST.value()) {
                LOGGER.error("Request to {} failed with status {}", uriPath, status);

                meterRegistry.counter(METRIC_FAILED_REQUESTS,
                                      TAG_URI, uriPath,
                                      TAG_METHOD, method,
                                      TAG_STATUS, status,
                                      TAG_SERVICE, service != null ? service : "unknown").increment();
            }
        }
    }
}
