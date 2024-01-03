package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd;

import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.cim.validated_historical_data.v0_82.ValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class EddieValidatedHistoricalDataMarketDocumentPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(EddieValidatedHistoricalDataMarketDocumentPublisher.class);
    private final PermissionRequestService permissionRequestService;

    public EddieValidatedHistoricalDataMarketDocumentPublisher(PermissionRequestService permissionRequestService) {
        this.permissionRequestService = permissionRequestService;
    }

    private static LocalDate getDataStartDate(ValidatedHistoricalDataMarketDocument marketDocument) {
        return LocalDateTime.parse(
                        marketDocument.getPeriodTimeInterval().getStart(),
                        DateTimeFormatter.ISO_DATE_TIME)
                .toLocalDate();
    }

    private static String getMeteringPointId(ValidatedHistoricalDataMarketDocument marketDocument) {
        return marketDocument.getTimeSeriesList().getTimeSeries().get(0).getMarketEvaluationPointMRID().getValue();
    }

    /**
     * Emits a stream of EddieValidatedHistoricalDataMarketDocuments for each permission request found.
     * If no permission requests are found for the given marketDocument's metering point and date, it emits an empty stream, essentially dropping the marketDocument.
     *
     * @param marketDocument The ValidatedHistoricalDataMarketDocument from which to extract data for emissions.
     * @return A Flux of EddieValidatedHistoricalDataMarketDocument based on existing permission requests.
     */
    public Flux<EddieValidatedHistoricalDataMarketDocument> emitForEachPermissionRequest(ValidatedHistoricalDataMarketDocument marketDocument) {
        String meteringPointId = getMeteringPointId(marketDocument);
        LocalDate date = getDataStartDate(marketDocument);

        var permissionRequests = permissionRequestService.findByMeteringPointIdAndDate(meteringPointId, date);

        if (permissionRequests.isEmpty()) {
            LOGGER.warn("Got ValidatedHistoricalMarketDocument with unknown MeteringPointId for date {}", date);
            return Flux.empty(); // Return an empty Flux if no permission requests are found
        }

        return Flux.fromIterable(permissionRequests).map(permissionRequest -> new EddieValidatedHistoricalDataMarketDocument(
                Optional.ofNullable(permissionRequest.connectionId()),
                Optional.ofNullable(permissionRequest.permissionId()),
                Optional.ofNullable(permissionRequest.dataNeedId()),
                marketDocument)
        );
    }
}