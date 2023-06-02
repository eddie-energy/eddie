package energy.eddie.regionconnector.fr.enedis;

import eddie.energy.regionconnector.api.v0.models.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClient;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClientConfiguration;
import energy.eddie.regionconnector.fr.enedis.contracts.EnedisApiClientContract;

import java.net.HttpURLConnection;
import java.time.ZonedDateTime;

public class EnedisApiClientFacade implements EnedisApiClientContract {
    private final EnedisApiClient enedisApiClient;

    public EnedisApiClientFacade() {
        enedisApiClient = new EnedisApiClient(EnedisApiClientConfiguration.fromEnvironment());
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
