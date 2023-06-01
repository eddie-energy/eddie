package energy.eddie.regionconnector.fr.enedis;

import eddie.energy.regionconnector.api.v0.models.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClient;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Scanner;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        final EnedisApiClient enedisApiClient = new EnedisApiClient(EnedisApiClientConfiguration.fromEnvironment());
        final Scanner scanner = new Scanner(System.in, Charset.defaultCharset());
        final File file = new File("region-connectors/region-connector-fr-enedis/bearer.txt");

        String usagePointId = "";
        ZonedDateTime start = null;
        ZonedDateTime end = null;

        try {
            if (!file.exists()) {
                enedisApiClient.postToken();
            }

            while (usagePointId.isBlank()) {
                System.out.println("Please enter your usage point [0 = default]");
                usagePointId = scanner.nextLine();
            }
            if (usagePointId.equals("0")) {
                usagePointId = "11453290002823";
            }

            while (start == null) {
                System.out.println("Please enter the date from data should be retrieved (YYYY-MM-DD)");
                start = DateTimeConverter.isoDateToZonedDateTime(scanner.nextLine());
            }
            while (end == null) {
                System.out.println("Please enter the date until data should be retrieved (YYYY-MM-DD)");
                end = DateTimeConverter.isoDateToZonedDateTime(scanner.nextLine());
            }

            getConsumptionRecords(enedisApiClient, usagePointId, start, end);
        } catch (ApiException e) {
            if (e.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                try {
                    logger.warn("Unauthorised, retry with new token.");

                    if (start == null) {
                        start = ZonedDateTime.now(ZoneId.of("Europe/Paris")).minusDays(2);
                        end = ZonedDateTime.now(ZoneId.of("Europe/Paris")).minusDays(1);
                    }
                    enedisApiClient.postToken();
                    getConsumptionRecords(enedisApiClient, usagePointId, start, end);
                } catch (ApiException e2) {
                    showApiExceptionError(e2);
                } catch (IOException e2) {
                    logger.error("IO Error: " + e2.getMessage(), e2);
                }
            } else {
                showApiExceptionError(e);
            }
        } catch (DateTimeException e) {
            logger.error("Date Time Error Occured: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("IO Error: " + e.getMessage(), e);
        }
    }

    private static void getConsumptionRecords(EnedisApiClient enedisApiClient, String usagePointId, ZonedDateTime start, ZonedDateTime end) throws ApiException, IOException {
        ConsumptionRecord dcRecord = enedisApiClient.getDailyConsumption(usagePointId, start, end);
        logger.info("Daily Consumption Record received.");
        logger.info(dcRecord.toString(), dcRecord);
        ConsumptionRecord clcRecord = enedisApiClient.getConsumptionLoadCurve(usagePointId, start, end);
        logger.info("Consumption Load Curve Record received.");
        logger.info(clcRecord.toString(), clcRecord);
    }

    private static void showApiExceptionError(ApiException e) {
        logger.error("API Exception occured: " + e.getMessage(), e);
    }
}
