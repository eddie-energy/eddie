package energy.eddie.regionconnector.fr.enedis;

import eddie.energy.regionconnector.api.v0.models.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClient;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClientConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Scanner;

public class Main {
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
                start = DateTimeConverter.ISODateToZonedDateTime(scanner.nextLine());
            }
            while (end == null) {
                System.out.println("Please enter the date until data should be retrieved (YYYY-MM-DD)");
                end = DateTimeConverter.ISODateToZonedDateTime(scanner.nextLine());
            }

            ConsumptionRecord dcRecord = enedisApiClient.getDailyConsumption(usagePointId, start, end);
            System.out.println(dcRecord);
            ConsumptionRecord clcRecord = enedisApiClient.getConsumptionLoadCurve(usagePointId, start, end);
            System.out.println(clcRecord);
        } catch (ApiException e) {
            System.out.println(e.getCode());
            System.out.println(e.getResponseBody());
            if (e.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                try {
                    if (start == null) {
                        start = ZonedDateTime.now(ZoneId.of("Europe/Paris")).minusDays(2);
                        end = ZonedDateTime.now(ZoneId.of("Europe/Paris")).minusDays(1);
                    }

                    enedisApiClient.postToken();
                    ConsumptionRecord dcRecord = enedisApiClient.getDailyConsumption(usagePointId, start, end);
                    System.out.println(dcRecord);
                    ConsumptionRecord clcRecord = enedisApiClient.getConsumptionLoadCurve(usagePointId, start, end);
                    System.out.println(clcRecord);
                } catch (ApiException e2) {
                    showApiExceptionError(e2);
                } catch (IOException ex) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            } else {
                showApiExceptionError(e);
            }
        } catch (DateTimeException e) {
            e.printStackTrace();
            System.out.println("Datetime error occured.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static void showApiExceptionError(ApiException e) {
        System.out.println("An API error occurred.");
        System.out.print("Response code: ");
        System.out.println(e.getCode());
        System.out.print("Response Header: ");
        System.out.println(e.getResponseHeaders());
        System.out.println("Response Body: ");
        System.out.print(e.getResponseBody());
        e.printStackTrace();
    }
}
