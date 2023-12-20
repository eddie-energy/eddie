package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Retryable
@Service
public class EnedisApiService {
    private final EnedisApi enedisApi;

    public EnedisApiService(EnedisApi enedisApi) {
        this.enedisApi = enedisApi;
    }

    @Retryable(retryFor = ApiException.class, backoff = @Backoff(delay = 3000))
    public ConsumptionRecord getDailyConsumption(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        return enedisApi.getDailyConsumption(usagePointId, start, end);
    }

    @Retryable(retryFor = ApiException.class)
    public ConsumptionRecord getConsumptionLoadCurve(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        return enedisApi.getConsumptionLoadCurve(usagePointId, start, end);
    }
}
