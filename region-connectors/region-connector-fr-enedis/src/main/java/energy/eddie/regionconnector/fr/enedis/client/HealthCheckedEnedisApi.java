package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.model.DailyConsumptionMeterReading;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class HealthCheckedEnedisApi implements EnedisApi {
    // package-private for testing
    static final String AUTHENTICATION_API = "AuthenticationAPI";
    static final String METERING_POINT_API = "MeteringPointAPI";
    private final EnedisApi enedisApi;
    private final Map<String, HealthState> healthChecks = new HashMap<>();

    public HealthCheckedEnedisApi(EnedisApi enedisApi) {
        this.enedisApi = enedisApi;
    }

    @Override
    public void postToken() throws ApiException {
        try {
            enedisApi.postToken();
            healthChecks.put(AUTHENTICATION_API, HealthState.UP);
        } catch (ApiException e) {
            healthChecks.put(AUTHENTICATION_API, HealthState.DOWN);
            throw e;
        }
    }

    @Override
    public DailyConsumptionMeterReading getDailyConsumption(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        try {
            healthChecks.put(METERING_POINT_API, HealthState.UP);
            return enedisApi.getDailyConsumption(usagePointId, start, end);
        } catch (ApiException e) {
            healthChecks.put(METERING_POINT_API, HealthState.DOWN);
            throw e;
        }
    }

    @Override
    public ConsumptionLoadCurveMeterReading getConsumptionLoadCurve(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        try {
            healthChecks.put(METERING_POINT_API, HealthState.UP);
            return enedisApi.getConsumptionLoadCurve(usagePointId, start, end);
        } catch (ApiException e) {
            healthChecks.put(METERING_POINT_API, HealthState.DOWN);
            throw e;
        }
    }

    @Override
    public Map<String, HealthState> health() {
        return healthChecks;
    }

}
