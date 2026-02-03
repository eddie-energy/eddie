package energy.eddie.regionconnector.de.eta.providers.cim;

import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.math.BigDecimal;
import java.time.ZoneOffset; // Added
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; // Added
import java.util.UUID;

@Component
public class EtaToCimMapper {

    private final DatatypeFactory datatypeFactory;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public EtaToCimMapper() throws Exception {
        this.datatypeFactory = DatatypeFactory.newInstance();
    }

    public Optional<VHDEnvelope> mapToEnvelope(DePermissionRequest request,
                                               List<EtaPlusMeteredData.MeterReading> readings) {
        if (readings == null || readings.isEmpty()) return Optional.empty();

        ZonedDateTime startTime = ZonedDateTime.parse(readings.get(0).timestamp(), ISO_FORMATTER);

        ZonedDateTime secondTime = (readings.size() > 1)
                ? ZonedDateTime.parse(readings.get(1).timestamp(), ISO_FORMATTER)
                : startTime.plusMinutes(15);

        long secondsDiff = ChronoUnit.SECONDS.between(startTime, secondTime);
        long minutes = secondsDiff / 60;

        Duration resolution = datatypeFactory.newDuration(true, 0, 0, 0, 0, (int) minutes, 0);

        List<Point> points = new ArrayList<>();
        int position = 1;
        for (EtaPlusMeteredData.MeterReading r : readings) {
            points.add(new Point()
                    .withPosition(position++)
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(r.value()))
                    .withEnergyQuantityQuality("A04")
            );
        }

        ZonedDateTime endTime = startTime.plus(readings.size() * minutes, ChronoUnit.MINUTES);

        ESMPDateTimeInterval timeInterval = new ESMPDateTimeInterval()
                .withStart(startTime.format(ISO_FORMATTER))
                .withEnd(endTime.format(ISO_FORMATTER));

        SeriesPeriod period = new SeriesPeriod()
                .withResolution(resolution)
                .withTimeInterval(timeInterval)
                .withPoints(points);

        TimeSeries timeSeries = new TimeSeries()
                .withMRID(UUID.randomUUID().toString())
                .withVersion("1")
                .withEnergyMeasurementUnitName("KWH")
                .withFlowDirectionDirection("A01");

        timeSeries.getPeriods().add(period);

        VHDMarketDocument marketDoc = new VHDMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withCreatedDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withType("E66")
                .withTimeSeries(timeSeries);

        return Optional.of(new VHDEnvelope()
                .withMessageDocumentHeaderCreationDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withMessageDocumentHeaderMetaInformationPermissionId(request.permissionId())
                .withMessageDocumentHeaderMetaInformationDataNeedId(request.dataNeedId())
                .withMarketDocument(marketDoc));
    }
}