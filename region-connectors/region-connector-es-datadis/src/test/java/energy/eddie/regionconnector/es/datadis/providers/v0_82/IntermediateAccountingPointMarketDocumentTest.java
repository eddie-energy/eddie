package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.es.datadis.ContractDetailsProvider;
import energy.eddie.regionconnector.es.datadis.SupplyProvider;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;

class IntermediateAccountingPointMarketDocumentTest {
    // TODO update test with correct mapping assertions GH-1047
    @Test
    void eddieAccountingPointMarketDocument_mapsAsExpected() throws IOException {
        // Given
        PlainDatadisConfiguration datadisConfig = new PlainDatadisConfiguration("clientId", "clientSecret", "basepath");
        PlainCommonInformationModelConfiguration cimConfiguration = new PlainCommonInformationModelConfiguration(
                CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                "fallbackId");

        IdentifiableAccountingPointData identifiableAccountingPointData = identifiableAccountingPointData();
        IntermediateAccountingPointMarketDocument intermediateAccountingPointMarketDocument = new IntermediateAccountingPointMarketDocument(
                identifiableAccountingPointData,
                cimConfiguration,
                datadisConfig
        );

        // When
        var res = intermediateAccountingPointMarketDocument.eddieAccountingPointMarketDocument();

        // Then
        assertAll(
                () -> assertEquals(identifiableAccountingPointData.permissionRequest().permissionId(),
                                   res.permissionId()),
                () -> assertEquals(identifiableAccountingPointData.permissionRequest().dataNeedId(), res.dataNeedId()),
                () -> assertEquals(identifiableAccountingPointData.permissionRequest().connectionId(),
                                   res.connectionId()),
                () -> assertNotNull(res.marketDocument())
        );
    }

    public static IdentifiableAccountingPointData identifiableAccountingPointData() throws IOException {
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest(
                "permissionId",
                "connectionId",
                "dataNeedId",
                null,
                "nif",
                "meteringPointId",
                LocalDate.now(ZONE_ID_SPAIN),
                LocalDate.now(ZONE_ID_SPAIN),
                DistributorCode.ASEME,
                1,
                null,
                PermissionProcessStatus.ACCEPTED,
                null,
                false,
                ZonedDateTime.now(ZoneOffset.UTC),
                null);
        return new IdentifiableAccountingPointData(
                permissionRequest,
                new AccountingPointData(
                        SupplyProvider.loadSupply().getFirst(),
                        ContractDetailsProvider.loadContractDetails().getFirst()
                )
        );
    }
}
