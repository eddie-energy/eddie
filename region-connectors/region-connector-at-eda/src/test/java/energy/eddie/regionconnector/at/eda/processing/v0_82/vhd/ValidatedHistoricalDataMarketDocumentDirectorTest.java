package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.MarketParticipantDirectory;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder.ValidatedHistoricalDataMarketDocumentBuilder;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ValidatedHistoricalDataMarketDocumentDirectorTest {
    @Test
    void constructor_withNullCommonInformationModelConfiguration_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ValidatedHistoricalDataMarketDocumentDirector(null, mock(ValidatedHistoricalDataMarketDocumentBuilderFactory.class)));
    }

    @Test
    void constructor_withNullValidatedHistoricalDataMarketDocumentBuilderFactory_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ValidatedHistoricalDataMarketDocumentDirector(mock(CommonInformationModelConfiguration.class), null));
    }

    @Test
    void createValidatedHistoricalDataMarketDocument_correctlyCallsTheBuilder() throws InvalidMappingException {
        CodingSchemeTypeList codingSchemeTypeList = CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME;
        RoutingHeader routingHeader = new RoutingHeader();
        ConsumptionRecord consumptionRecord = new ConsumptionRecord()
                .withMarketParticipantDirectory(new MarketParticipantDirectory()
                        .withRoutingHeader(routingHeader));

        ValidatedHistoricalDataMarketDocumentBuilder builder = mock(ValidatedHistoricalDataMarketDocumentBuilder.class);
        when(builder.withConsumptionRecord(consumptionRecord)).thenReturn(builder);
        when(builder.withRoutingHeaderData(routingHeader, codingSchemeTypeList)).thenReturn(builder);
        when(builder.build()).thenReturn(null);

        ValidatedHistoricalDataMarketDocumentBuilderFactory factory = mock(ValidatedHistoricalDataMarketDocumentBuilderFactory.class);
        when(factory.create()).thenReturn(builder);

        ValidatedHistoricalDataMarketDocumentDirector uut = new ValidatedHistoricalDataMarketDocumentDirector(() -> codingSchemeTypeList, factory);

        assertNull(uut.createValidatedHistoricalDataMarketDocument(consumptionRecord));

        verify(builder).withConsumptionRecord(consumptionRecord);
        verify(builder).withRoutingHeaderData(routingHeader, codingSchemeTypeList);
        verify(builder).build();
    }
}