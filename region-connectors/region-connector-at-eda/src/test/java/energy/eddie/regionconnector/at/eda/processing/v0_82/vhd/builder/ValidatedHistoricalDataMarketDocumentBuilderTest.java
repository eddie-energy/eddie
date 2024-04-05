package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEnergy;
import energy.eddie.regionconnector.at.eda.dto.SimpleEnergyData;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static energy.eddie.regionconnector.shared.utils.EsmpDateTime.ESMP_DATE_TIME_MINUTE_FORMATTER;
import static energy.eddie.regionconnector.shared.utils.EsmpDateTime.ESMP_DATE_TIME_SECOND_FORMATTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ValidatedHistoricalDataMarketDocumentBuilderTest {

    @Test
    void build_afterConstruction_setsStaticData() {
        var validatedHistoricalDataMarketDocument = new ValidatedHistoricalDataMarketDocumentBuilder(mock(
                SeriesPeriodBuilder.class), mock(TimeSeriesBuilder.class)).build();

        assertEquals("0.82", validatedHistoricalDataMarketDocument.getRevisionNumber());
        assertEquals(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT, validatedHistoricalDataMarketDocument.getType());
        assertEquals(RoleTypeList.METERING_POINT_ADMINISTRATOR,
                     validatedHistoricalDataMarketDocument.getSenderMarketParticipantMarketRoleType());
        assertEquals(RoleTypeList.CONSUMER,
                     validatedHistoricalDataMarketDocument.getReceiverMarketParticipantMarketRoleType());
        assertEquals(ProcessTypeList.REALISED, validatedHistoricalDataMarketDocument.getProcessProcessType());
    }

    @Test
    void withRoutingHeaderData_setsSenderReceiverAndDate() {
        String sender = "sender";
        String receiver = "receiver";
        ZonedDateTime date = ZonedDateTime.of(LocalDate.of(2023, 1, 1), LocalTime.MIN, AT_ZONE_ID);


        EdaConsumptionRecord consumptionRecord = new SimpleEdaConsumptionRecord()
                .setSenderMessageAddress(sender)
                .setReceiverMessageAddress(receiver)
                .setDocumentCreationDateTime(date);

        ValidatedHistoricalDataMarketDocumentBuilder uut = new ValidatedHistoricalDataMarketDocumentBuilder(
                mock(SeriesPeriodBuilder.class), mock(TimeSeriesBuilder.class))
                .withRoutingHeaderData(consumptionRecord, CodingSchemeTypeList.NORWAY_NATIONAL_CODING_SCHEME);
        var validatedHistoricalDataMarketDocument = uut.build();

        assertEquals(sender, validatedHistoricalDataMarketDocument.getSenderMarketParticipantMRID().getValue());
        assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                     validatedHistoricalDataMarketDocument.getSenderMarketParticipantMRID().getCodingScheme());
        assertEquals(receiver, validatedHistoricalDataMarketDocument.getReceiverMarketParticipantMRID().getValue());
        assertEquals(CodingSchemeTypeList.NORWAY_NATIONAL_CODING_SCHEME,
                     validatedHistoricalDataMarketDocument.getReceiverMarketParticipantMRID().getCodingScheme());
        assertEquals(date.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                     LocalDateTime.parse(validatedHistoricalDataMarketDocument.getCreatedDateTime(),
                                         ESMP_DATE_TIME_SECOND_FORMATTER));
    }

    @Test
    void withConsumptionRecord_callsBuildersAndSetsPeriod() throws InvalidMappingException {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = start.plusDays(1);
        EdaConsumptionRecord consumptionRecord = new SimpleEdaConsumptionRecord()
                .setStartDate(start)
                .setEndDate(end)
                .setEnergy(List.of(
                                   new SimpleEnergy()
                                           .setMeterReadingStart(start.atStartOfDay(AT_ZONE_ID))
                                           .setMeterReadingEnd(end.atStartOfDay(AT_ZONE_ID))
                                           .setEnergyData(List.of(
                                                   new SimpleEnergyData()
                                           ))
                           )
                );

        SeriesPeriodBuilder seriesPeriodBuilder = mock(SeriesPeriodBuilder.class);
        when(seriesPeriodBuilder.withEnergy(any())).thenReturn(seriesPeriodBuilder);
        when(seriesPeriodBuilder.withEnergyData(any())).thenReturn(seriesPeriodBuilder);
        when(seriesPeriodBuilder.build()).thenReturn(new SeriesPeriodComplexType());

        TimeSeriesBuilder timeSeriesBuilder = mock(TimeSeriesBuilder.class);
        when(timeSeriesBuilder.withConsumptionRecord(any())).thenReturn(timeSeriesBuilder);
        when(timeSeriesBuilder.withEnergy(any())).thenReturn(timeSeriesBuilder);
        when(timeSeriesBuilder.withEnergyData(any())).thenReturn(timeSeriesBuilder);
        when(timeSeriesBuilder.withSeriesPeriod(any())).thenReturn(timeSeriesBuilder);
        when(timeSeriesBuilder.build()).thenReturn(new TimeSeriesComplexType());


        ValidatedHistoricalDataMarketDocumentBuilder uut = new ValidatedHistoricalDataMarketDocumentBuilder(
                seriesPeriodBuilder, timeSeriesBuilder)
                .withConsumptionRecord(consumptionRecord);
        var validatedHistoricalDataMarketDocument = uut.build();

        var period = validatedHistoricalDataMarketDocument.getPeriodTimeInterval();

        assertEquals(start.atStartOfDay(AT_ZONE_ID)
                          .withZoneSameInstant(ZoneOffset.UTC)
                          .format(ESMP_DATE_TIME_MINUTE_FORMATTER), period.getStart());
        assertEquals(end.atStartOfDay(AT_ZONE_ID)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .format(ESMP_DATE_TIME_MINUTE_FORMATTER), period.getEnd());

        verify(seriesPeriodBuilder).withEnergy(any());
        verify(seriesPeriodBuilder).withEnergyData(any());
        verify(seriesPeriodBuilder).build();

        verify(timeSeriesBuilder).withConsumptionRecord(any());
        verify(timeSeriesBuilder).withEnergy(any());
        verify(timeSeriesBuilder).withEnergyData(any());
        verify(timeSeriesBuilder).withSeriesPeriod(any());
        verify(timeSeriesBuilder).build();
    }
}
