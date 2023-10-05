package energy.eddie.regionconnector.dk.energinet.customer;

import energy.eddie.regionconnector.dk.energinet.config.PropertiesEnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPoints;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;
import energy.eddie.regionconnector.dk.energinet.utils.DateTimeConverter;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Properties;
import java.util.Scanner;

public class EnerginetCustomerCliClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnerginetCustomerCliClient.class);
    private static final String REFRESH_TOKEN_KEY = "dk.cli.client.refresh.token";
    private static final String METERING_POINT_KEY = "dk.cli.client.metering.point";
    private static final String DK_ZONE_ID = "Europe/Copenhagen";

    public static void main(String[] args) throws IOException {
        final Scanner scanner = new Scanner(System.in, Charset.defaultCharset());
        MeteringPointsRequest meteringPointsRequest = new MeteringPointsRequest();
        MeteringPoints meteringPoints = new MeteringPoints();

        TimeSeriesAggregationEnum aggregationEnum = TimeSeriesAggregationEnum.ACTUAL;

        Properties regionConnectorProperties = new Properties();
        var rcIn = EnerginetCustomerCliClient.class.getClassLoader().getResourceAsStream("regionconnector-dk-energinet.properties");
        regionConnectorProperties.load(rcIn);
        PropertiesEnerginetConfiguration propertiesEnerginetConfiguration = new PropertiesEnerginetConfiguration(regionConnectorProperties);

        EnerginetCustomerApiClient apiClient = new EnerginetCustomerApiClient(propertiesEnerginetConfiguration);

        Properties cliProperties = new Properties();
        var cliIn = EnerginetCustomerCliClient.class.getClassLoader().getResourceAsStream("regionconnector-dk-energinet.properties");
        cliProperties.load(cliIn);

        String refreshToken = cliProperties.getProperty(REFRESH_TOKEN_KEY, "");

        while (refreshToken.isBlank()) {
            System.out.println("Please provide your API refresh token");
            refreshToken = scanner.nextLine();
        }

        apiClient.setRefreshToken(refreshToken);

        String meteringPointId = cliProperties.getProperty(METERING_POINT_KEY, "");

        while (meteringPointId.isBlank()) {
            System.out.println("Please enter your metering point id");
            meteringPointId = scanner.nextLine();
        }

        ZonedDateTime reference = LocalDate.MIN.atStartOfDay(ZoneId.of(DK_ZONE_ID));
        ZonedDateTime start = LocalDate.MIN.atStartOfDay(ZoneId.of(DK_ZONE_ID));
        ZonedDateTime end = LocalDate.MIN.atStartOfDay(ZoneId.of(DK_ZONE_ID));

        try {
            meteringPoints.addMeteringPointItem(meteringPointId);
            meteringPointsRequest = new MeteringPointsRequest().meteringPoints(meteringPoints);

            while (start.equals(reference)) {
                System.out.println("Please enter the date from data should be retrieved (YYYY-MM-DD)");
                start = DateTimeConverter.isoDateToZonedDateTime(scanner.nextLine(), DK_ZONE_ID);
            }
            while (end.equals(reference)) {
                System.out.println("Please enter the date until data should be retrieved (YYYY-MM-DD)");
                end = DateTimeConverter.isoDateToZonedDateTime(scanner.nextLine(), DK_ZONE_ID);
            }

            String aggregation = "";
            while (aggregation.isBlank()) {
                System.out.println("Please enter your preferred aggregation [0 = Actual]");
                aggregation = scanner.nextLine();
            }

            if (aggregation.equals("0")) {
                aggregation = "Actual";
            }

            aggregationEnum = TimeSeriesAggregationEnum.fromString(aggregation);
            var timeSeries = apiClient.getTimeSeries(start, end, aggregationEnum, meteringPointsRequest);
            LOGGER.info("Consumption Record received.");
            LOGGER.info(timeSeries.toString(), timeSeries);

        } catch (FeignException feignException) {
            if (feignException.status() == 401) {
                apiClient.apiToken();
                var timeSeries = apiClient.getTimeSeries(start, end, aggregationEnum, meteringPointsRequest);
                LOGGER.info("Consumption Record received.");
                LOGGER.info(timeSeries.toString(), timeSeries);
            }
        } catch (Exception e) {
            LOGGER.error("Error: " + e.getMessage(), e);
        }
    }
}
