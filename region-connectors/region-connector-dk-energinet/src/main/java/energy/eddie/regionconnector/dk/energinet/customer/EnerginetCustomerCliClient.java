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

    public static void main(String[] args) throws IOException {
        final Scanner scanner = new Scanner(System.in, Charset.defaultCharset());
        MeteringPointsRequest meteringPointsRequest = new MeteringPointsRequest();
        MeteringPoints meteringPoints = new MeteringPoints();
        String zoneId = "Europe/Copenhagen";
        TimeSeriesAggregationEnum aggregationEnum = TimeSeriesAggregationEnum.ACTUAL;

        Properties properties = new Properties();
        var in = EnerginetCustomerCliClient.class.getClassLoader().getResourceAsStream("regionconnector-dk-energinet.properties");
        properties.load(in);
        PropertiesEnerginetConfiguration propertiesEnerginetConfiguration = new PropertiesEnerginetConfiguration(properties);

        EnerginetCustomerApiClient apiClient = new EnerginetCustomerApiClient(propertiesEnerginetConfiguration);

        String refreshToken = "";

        while (refreshToken.isBlank()) {
            System.out.println("Please provide your API refresh token [0 = default]");
            refreshToken = scanner.nextLine();
        }

        if (refreshToken.equals("0")) {
            refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlblR5cGUiOiJDdXN0b21lckFQSV9SZWZyZXNoIiwidG9rZW5pZCI6IjIyNmNjYWY5LWQ0NmEtNDhmZC04ZjcyLWM0ZGFjN2FjYjRlMSIsIndlYkFwcCI6IkN1c3RvbWVyQXBwIiwidmVyc2lvbiI6IjIiLCJpZGVudGl0eVRva2VuIjoiWGc1OXUvY3lkOGwrMlp1bEpYZGhlRlVxZGVkV1NBeE8zTVZSOHgzT0tFckRqVnBRY3JWTFhwQVB5SEtCS2JKWnRzMTRDcmpiMzRjbHQyaUh3V3Y5ZnJvbXhSTnpvSENtdGdJRGZuUGZrSm1QQ0piMThPK2wyL3NsaFBzd3VZbkJqbmdQZzVpMzhiN0VLZnRyeGVZaDNkVEpsSEY0ZE5ScU9CTWdBejdrMys1ZVJRWVV1Y05MckhiQ25JZTNIWnJYYTl2UVpuZ0NjVUFNZWtDeGRkQzJ4MkphR1E1VHNKQlltLzc3d212U2RjZFZSZ3FEMlo4b0RFaG94Z1RCTklTbWhVTlV2Z3RLQ2JoeWYxTnY5WkQ3d04xcS9Pd052M1VSSVViWXpsVFZuZHBmcjB4THlieXhMbjNXakNONzJxaDV2V2VDVDgxSjdyK3grTzQ4UGpPbStBTFRSZHYycDE3N1lSM29JZmFiTmtmcGk2aUw0cDRYdEVyeVExVnM3K1A4dWJSREkzZDFRSFVyQ0dESllNdnVoYlhVcjFlRE8wVkpGZHJvbGtJWFNmYTBrUVhJbVJRcURmTWNWQXBLeWd5ZHgwclF0UWszanNYWVlYQU1aWWI3d3BzMGFCZVllcEpkSUJvdjJNczdnNkwvanhwYnllUk0yVUxhMTY2NzFCaGVuOUxoMHlTZjg2cEN0aS9uVUxYcDN3Z1N6M0RJZlQrNEw3VzhZNUFXNnZKclFSMTNaSTFRTzEvcFkvamVmQW45eTkyL045cVhzSHhGZnQzcWdFNmlwMWY0SlNUVC85MkNMNHhyS3dXVnZXTkV5c0V3VU0wOHVoWEJSdHd4ZUdnTHVYbmxXQndFM1FhVStnTk5nK1ZiQ2E3YVhqelBaU05sTlRzZHpJWU5oMTlIeUZVMjZuSVVyaU9keUR0SXpmdWYzNjY0TW5laEszbFhyUnppKytiTlFpbFhKeFJ1OE5XVTVocEdnTEJCek9abzM0Y2I5d3UvSnEzbHBDTy9QTW5ZY2lRQ0tPdUJBZ3RSY0YvNkZPYjI1OTNpVVFaVUxCRVNYeGtoM3c3N3kxVHNWaUEyZzg0TWYzYWYxWllPOVhVTFBnSkkrVTh3Mi9qSk5jaW45aDVsM2NzWUkzYVdpejNpZSsxZERZWmpFK1pRbXlLVkhaVjhsZlhwYk5mVHQ5UWNKQ1ZMa1hoRkZTbTgvNURCcVZSNEk3K1VwS011R1k3aTJaVzFuU3VTNmxaei93Z2RQc2txUnF2ZGdSOHcxUElLbEZTanRaYk9QU3V6SFFrSGNOU2czbDgwRzBJZ2lDWHphaVhmMzhwcmFTMWdiUkFhZlZlNW9oNFgwemk0dkF0OTBZSUdCZ29vQTBOZklzRXBlS3pFdTVkeFpJMzdXWmNRb0Y4c1BJeTZkYmJtVjgxOFltV05wcVEzWUx6ZHlIOHRSZTRJYTdNOXZBSkZKYzhWamgra3NVMkhNbmYyMlBiYzdoYUlMOWdkR3lQSllra0hkcFVJV3hGUHNla205U1lXWmxFbjZ4ZG9xQzA2dkd0SE1zRlNMS21JYlYxb1RTOU1HQ0MvSVZDazZkOFF4MDhCT0hwa29DNWVuNk9wNmIwSzJ4cWYzQ3FtUXI3akFOMVRIZzZqSHFrNUlNbEh6ZGdyWHhpYUVrVVhGNkozKytzd042eHNScXNPRG1rNjd5VUVhLzRrL3VZdFNiTUdkekhWdjJZQ3VZNDNtUzhCd0poaU9tRzJBNUlqRzlEZS9KeVU4ekJmeFc1bnl2K1VZVjI1TzBWc2pXcmlSaGpEVVpQek5xVDRlV1BCQS9MRlFJdzFORWZLVmJyMEFPa25hMUFvNXRtSmdCSThneWVtTjlkMHhScGpTK0N3Q0JkdUJsNktWWURWYkM2VytUcHUwVTE4bnVRd09ZTnZFdmg0cEdkeGVHcm80YUlOR2U1M0tCRVMzbDIwcTFOZTNiajJkR3UzRU0yN3p5QUtEYzhLS0JDcGpGeUhIeVNqK01qUG9Ya1RsSHg5SldFMEF6cUlUSGJGL1BaUXB6YVhvaXFtU0t5OFh5clNEdG55VjE5T0RFNkRIR3FJVldXQ3drMEVLNCtUbmpqVUJzTmdoSjd6SXdNbFhRZEFDa1NibXh5TGlldE5iRkI3Q0R4S25WeTdOeFk4SG1BSlE1K3Y2c1dEZG16WVcybVcyWG5yQWxQemZ0TnJ6Z1VBZUsvTEtXd1V4U2RGZHFubHBNM2hSd2gwME1HVVdCbWRPL296VmlScXJNQWhsWTFjSEw0VVhlb3BBdkdoQjZFYnVZL2d5RjlPUXhBMlZrYUQzKy9ncElsZzhIakNCVjAwSEpqWmdlL0wwMGFuL0xpcFEyVk00SGNWeUVlc2MyeWtOODhiR3RxaHpYampLK2dzV2N5Z3FVN2ZMUEQrd3V3OGpMTmhDc1o4M3VOeUs4MVE4bG13S2s5V0hzTTNLT2hIN0gwV0Y3MGJKSHplWWMxaWpLNHRDTE1lT3RMSG1KT09kQzdNWjI5M3ZvdUhmbEErVCs5ZmpnaU5yL1h0IiwiaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNS8wNS9pZGVudGl0eS9jbGFpbXMvbmFtZWlkZW50aWZpZXIiOiJQSUQ6OTIwOC0yMDAyLTItODE0MzMwMDI3MDA1IiwiaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNS8wNS9pZGVudGl0eS9jbGFpbXMvZ2l2ZW5uYW1lIjoiTWFya3VzIEZhbGx5IiwibG9naW5UeXBlIjoiS2V5Q2FyZCIsInBpZCI6IjkyMDgtMjAwMi0yLTgxNDMzMDAyNzAwNSIsImIzZiI6Ijl4bERSZklBSzY0QnZhcjZ4T0dudjE0QzRNeU55Qzl3Yko0OEgwdldZZUU9IiwidXNlcklkIjoiNTU3NTA5IiwiZXhwIjoxNzIxOTE5ODAwLCJpc3MiOiJFbmVyZ2luZXQiLCJqdGkiOiIyMjZjY2FmOS1kNDZhLTQ4ZmQtOGY3Mi1jNGRhYzdhY2I0ZTEiLCJ0b2tlbk5hbWUiOiJHRzIiLCJhdWQiOiJFbmVyZ2luZXQifQ.9ZNa1ZRDJ-hTweekt6pgiLm0OjZ1WqmRVLhOpDNinKE";
        }

        apiClient.setRefreshToken(refreshToken);

        String meteringPointId = "";
        ZonedDateTime reference = LocalDate.MIN.atStartOfDay(ZoneId.of(zoneId));
        ZonedDateTime start = LocalDate.MIN.atStartOfDay(ZoneId.of(zoneId));
        ZonedDateTime end = LocalDate.MIN.atStartOfDay(ZoneId.of(zoneId));

        try {
            while (meteringPointId.isBlank()) {
                System.out.println("Please enter your metering point id [0 = default]");
                meteringPointId = scanner.nextLine();
            }

            if (meteringPointId.equals("0")) {
                meteringPointId = "571313179100066516";
            }

            meteringPoints.addMeteringPointItem(meteringPointId);
            meteringPointsRequest = new MeteringPointsRequest().meteringPoints(meteringPoints);

            while (start.equals(reference)) {
                System.out.println("Please enter the date from data should be retrieved (YYYY-MM-DD)");
                start = DateTimeConverter.isoDateToZonedDateTime(scanner.nextLine(), zoneId);
            }
            while (end.equals(reference)) {
                System.out.println("Please enter the date until data should be retrieved (YYYY-MM-DD)");
                end = DateTimeConverter.isoDateToZonedDateTime(scanner.nextLine(), zoneId);
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
