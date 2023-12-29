package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.model.DailyConsumptionMeterReading;

import java.net.HttpURLConnection;
import java.time.ZonedDateTime;

public class EnedisApiClientDecorator implements EnedisApi {
    private final EnedisApi enedisApi;

    public EnedisApiClientDecorator(EnedisApi enedisApi) {
        this.enedisApi = enedisApi;
    }

    @Override
    public void postToken() throws ApiException {
        enedisApi.postToken();
    }

    @Override
    public DailyConsumptionMeterReading getDailyConsumption(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        try {
            return enedisApi.getDailyConsumption(usagePointId, start, end);
        } catch (ApiException e) {
            if (e.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                postToken();
                return enedisApi.getDailyConsumption(usagePointId, start, end);
            }

            throw e;
        }
    }

    @Override
    public ConsumptionLoadCurveMeterReading getConsumptionLoadCurve(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        try {
            return enedisApi.getConsumptionLoadCurve(usagePointId, start, end);
        } catch (ApiException e) {
            if (e.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                postToken();
                return enedisApi.getConsumptionLoadCurve(usagePointId, start, end);
            }

            throw e;
        }
    }
}
