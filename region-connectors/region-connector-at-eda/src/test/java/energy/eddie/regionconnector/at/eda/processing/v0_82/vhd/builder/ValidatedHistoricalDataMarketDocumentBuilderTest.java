package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.*;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static energy.eddie.regionconnector.shared.utils.EsmpDateTime.ESMP_DATE_TIME_MINUTE_FORMATTER;
import static energy.eddie.regionconnector.shared.utils.EsmpDateTime.ESMP_DATE_TIME_SECOND_FORMATTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ValidatedHistoricalDataMarketDocumentBuilderTest {

    @Test
    void build_afterConstruction_setsStaticData() {
        var validatedHistoricalDataMarketDocument = new ValidatedHistoricalDataMarketDocumentBuilder(mock(SeriesPeriodBuilder.class), mock(TimeSeriesBuilder.class)).build();

        assertEquals("0.82", validatedHistoricalDataMarketDocument.getRevisionNumber());
        assertEquals(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT, validatedHistoricalDataMarketDocument.getType());
        assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR, validatedHistoricalDataMarketDocument.getSenderMarketParticipantMarketRoleType());
        assertEquals(RoleTypeList.CONSUMER, validatedHistoricalDataMarketDocument.getReceiverMarketParticipantMarketRoleType());
        assertEquals(ProcessTypeList.REALISED, validatedHistoricalDataMarketDocument.getProcessProcessType());
    }

    @Test
    void withRoutingHeaderData_setsSenderReceiverAndDate() {
        String sender = "sender";
        String receiver = "receiver";
        LocalDateTime date = LocalDateTime.of(2023, 1, 1, 0, 0, 0);


        RoutingHeader routingHeader = new RoutingHeader()
                .withSender(new RoutingAddress().withMessageAddress(sender))
                .withReceiver(new RoutingAddress().withMessageAddress(receiver))
                .withDocumentCreationDateTime(DateTimeConverter.dateTimeToXml(date));

        ValidatedHistoricalDataMarketDocumentBuilder uut = new ValidatedHistoricalDataMarketDocumentBuilder(
                mock(SeriesPeriodBuilder.class), mock(TimeSeriesBuilder.class))
                .withRoutingHeaderData(routingHeader, CodingSchemeTypeList.NORWAY_NATIONAL_CODING_SCHEME);
        var validatedHistoricalDataMarketDocument = uut.build();

        assertEquals(sender, validatedHistoricalDataMarketDocument.getSenderMarketParticipantMRID().getValue());
        assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, validatedHistoricalDataMarketDocument.getSenderMarketParticipantMRID().getCodingScheme());
        assertEquals(receiver, validatedHistoricalDataMarketDocument.getReceiverMarketParticipantMRID().getValue());
        assertEquals(CodingSchemeTypeList.NORWAY_NATIONAL_CODING_SCHEME, validatedHistoricalDataMarketDocument.getReceiverMarketParticipantMRID().getCodingScheme());
        assertEquals(date, LocalDateTime.parse(validatedHistoricalDataMarketDocument.getCreatedDateTime(), ESMP_DATE_TIME_SECOND_FORMATTER));
    }

    @Test
    void withConsumptionRecord_noEnergy_throwsInvalidMappingException() {
        assertThrows(InvalidMappingException.class, () -> new ValidatedHistoricalDataMarketDocumentBuilder(
                mock(SeriesPeriodBuilder.class), mock(TimeSeriesBuilder.class))
                .withConsumptionRecord(new ConsumptionRecord().withProcessDirectory(new ProcessDirectory()))
        );
    }


    @Test
    void withConsumptionRecord_noEnergyData_throwsInvalidMappingException() {
        ConsumptionRecord consumptionRecord = new ConsumptionRecord()
                .withProcessDirectory(new ProcessDirectory()
                        .withEnergy(new Energy()));

        assertThrows(InvalidMappingException.class, () -> new ValidatedHistoricalDataMarketDocumentBuilder(
                mock(SeriesPeriodBuilder.class), mock(TimeSeriesBuilder.class))
                .withConsumptionRecord(consumptionRecord)
        );
    }

    @Test
    void withConsumptionRecord_callsBuildersAndSetsPeriod() throws InvalidMappingException {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = start.plusDays(1);
        ConsumptionRecord consumptionRecord = new ConsumptionRecord()
                .withMarketParticipantDirectory(new MarketParticipantDirectory())
                .withProcessDirectory(new ProcessDirectory()
                        .withEnergy(new Energy()
                                .withMeteringPeriodStart(DateTimeConverter.dateToXml(start))
                                .withMeteringPeriodEnd(DateTimeConverter.dateToXml(end))
                                .withEnergyData(new EnergyData())));

        SeriesPeriodBuilder seriesPeriodBuilder = mock(SeriesPeriodBuilder.class);
        when(seriesPeriodBuilder.withEnergy(any())).thenReturn(seriesPeriodBuilder);
        when(seriesPeriodBuilder.withEnergyData(any())).thenReturn(seriesPeriodBuilder);
        when(seriesPeriodBuilder.build()).thenReturn(new SeriesPeriodComplexType());

        TimeSeriesBuilder timeSeriesBuilder = mock(TimeSeriesBuilder.class);
        when(timeSeriesBuilder.withMarketParticipantDirectory(any())).thenReturn(timeSeriesBuilder);
        when(timeSeriesBuilder.withProcessDirectory(any())).thenReturn(timeSeriesBuilder);
        when(timeSeriesBuilder.withEnergy(any())).thenReturn(timeSeriesBuilder);
        when(timeSeriesBuilder.withEnergyData(any())).thenReturn(timeSeriesBuilder);
        when(timeSeriesBuilder.withSeriesPeriod(any())).thenReturn(timeSeriesBuilder);
        when(timeSeriesBuilder.build()).thenReturn(new TimeSeriesComplexType());


        ValidatedHistoricalDataMarketDocumentBuilder uut = new ValidatedHistoricalDataMarketDocumentBuilder(
                seriesPeriodBuilder, timeSeriesBuilder)
                .withConsumptionRecord(consumptionRecord);
        var validatedHistoricalDataMarketDocument = uut.build();

        var period = validatedHistoricalDataMarketDocument.getPeriodTimeInterval();

        assertEquals(start, LocalDate.parse(period.getStart(), ESMP_DATE_TIME_MINUTE_FORMATTER));
        assertEquals(end, LocalDate.parse(period.getEnd(), ESMP_DATE_TIME_MINUTE_FORMATTER));

        verify(seriesPeriodBuilder).withEnergy(any());
        verify(seriesPeriodBuilder).withEnergyData(any());
        verify(seriesPeriodBuilder).build();

        verify(timeSeriesBuilder).withMarketParticipantDirectory(any());
        verify(timeSeriesBuilder).withProcessDirectory(any());
        verify(timeSeriesBuilder).withEnergy(any());
        verify(timeSeriesBuilder).withEnergyData(any());
        verify(timeSeriesBuilder).withSeriesPeriod(any());
        verify(timeSeriesBuilder).build();
    }
}