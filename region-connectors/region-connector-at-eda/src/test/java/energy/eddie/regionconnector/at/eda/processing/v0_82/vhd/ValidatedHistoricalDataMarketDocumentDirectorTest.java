package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd;

import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder.ValidatedHistoricalDataMarketDocumentBuilder;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ValidatedHistoricalDataMarketDocumentDirectorTest {
    @Test
    void constructor_withNullCommonInformationModelConfiguration_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                     () -> new ValidatedHistoricalDataMarketDocumentDirector(null,
                                                                             mock(ValidatedHistoricalDataMarketDocumentBuilderFactory.class)));
    }

    @Test
    void constructor_withNullValidatedHistoricalDataMarketDocumentBuilderFactory_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                     () -> new ValidatedHistoricalDataMarketDocumentDirector(mock(CommonInformationModelConfiguration.class),
                                                                             null));
    }

    @Test
    void createValidatedHistoricalDataMarketDocument_correctlyCallsTheBuilder() throws InvalidMappingException {
        CodingSchemeTypeList codingSchemeTypeList = CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME;
        EdaConsumptionRecord consumptionRecord = new SimpleEdaConsumptionRecord();

        ValidatedHistoricalDataMarketDocumentBuilder builder = mock(ValidatedHistoricalDataMarketDocumentBuilder.class);
        when(builder.withConsumptionRecord(consumptionRecord)).thenReturn(builder);
        when(builder.withRoutingHeaderData(consumptionRecord, codingSchemeTypeList)).thenReturn(builder);
        when(builder.build()).thenReturn(null);

        ValidatedHistoricalDataMarketDocumentBuilderFactory factory = mock(
                ValidatedHistoricalDataMarketDocumentBuilderFactory.class);
        when(factory.create()).thenReturn(builder);

        ValidatedHistoricalDataMarketDocumentDirector uut = new ValidatedHistoricalDataMarketDocumentDirector(
                new PlainCommonInformationModelConfiguration(codingSchemeTypeList, "fallbackId"),
                factory
        );

        assertNull(uut.createValidatedHistoricalDataMarketDocument(consumptionRecord));

        verify(builder).withConsumptionRecord(consumptionRecord);
        verify(builder).withRoutingHeaderData(consumptionRecord, codingSchemeTypeList);
        verify(builder).build();
    }
}
