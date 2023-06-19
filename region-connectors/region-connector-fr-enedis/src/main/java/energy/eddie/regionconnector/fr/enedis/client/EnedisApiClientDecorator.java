package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;

import java.net.HttpURLConnection;
import java.time.ZonedDateTime;

public class EnedisApiClientDecorator implements EnedisApi {
    private final EnedisApiClient enedisApiClient;

    public EnedisApiClientDecorator(EnedisConfiguration enedisConfiguration) {
        enedisApiClient = new EnedisApiClient(enedisConfiguration);
    }

    @Override
    public void postToken() throws ApiException {
        enedisApiClient.postToken();
    }

    @Override
    public ConsumptionRecord getDailyConsumption(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        try {
            return enedisApiClient.getDailyConsumption(usagePointId, start, end);
        } catch (ApiException e) {
            if (e.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                postToken();
                return enedisApiClient.getDailyConsumption(usagePointId, start, end);
            }

            throw e;
        }
    }

    @Override
    public ConsumptionRecord getConsumptionLoadCurve(String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException {
        try {
            return enedisApiClient.getConsumptionLoadCurve(usagePointId, start, end);
        } catch (ApiException e) {
            if (e.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                postToken();
                return enedisApiClient.getConsumptionLoadCurve(usagePointId, start, end);
            }

            throw e;
        }
    }
}
