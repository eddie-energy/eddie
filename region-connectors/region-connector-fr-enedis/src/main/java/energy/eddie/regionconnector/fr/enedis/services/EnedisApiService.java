package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
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

    @Retryable(retryFor = ApiException.class)
    public ConsumptionLoadCurveMeterReading getConsumptionLoadCurve(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        return enedisApi.getConsumptionLoadCurve(usagePointId, start, end);
    }
}
