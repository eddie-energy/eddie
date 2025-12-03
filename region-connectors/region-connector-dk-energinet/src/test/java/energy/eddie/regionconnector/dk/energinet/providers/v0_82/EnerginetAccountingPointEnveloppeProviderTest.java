package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDto;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDtoResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequestBuilder;
import energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableAccountingPointDetails;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EnerginetAccountingPointEnvelopeProviderTest {
    private static IdentifiableAccountingPointDetails apiResponse;

    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
            "fallbackId"
    );

    @BeforeAll
    static void setUp() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JsonNullableModule());
        MeteringPointDetailsCustomerDto meteringPointDetailsCustomerDto;
        try (InputStream is = EnerginetAccountingPointEnvelopeProviderTest.class.getClassLoader()
                                                                                .getResourceAsStream(
                                                                                        "MeteringPointDetailsCustomerDtoResponseListApiResponse.json")) {
            MeteringPointDetailsCustomerDtoResponseListApiResponse response = objectMapper.readValue(is,
                                                                                                     MeteringPointDetailsCustomerDtoResponseListApiResponse.class);
            assert response.getResult() != null;
            meteringPointDetailsCustomerDto = response.getResult().getFirst().getResult();
        }

        var permissionRequest = new EnerginetPermissionRequestBuilder()
                .setPermissionId("permissionId")
                .setConnectionId("connectionId")
                .setDataNeedId("dataNeedId")
                .setMeteringPoint("meteringPointId")
                .setStart(LocalDate.now(ZoneOffset.UTC))
                .setEnd(LocalDate.now(ZoneOffset.UTC))
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                .build();
        apiResponse = new IdentifiableAccountingPointDetails(
                permissionRequest,
                meteringPointDetailsCustomerDto
        );
    }

    @Test
    void getEddieValidatedHistoricalDataMarketDocumentStream_producesDocumentWhenTestJsonIsUsed() {
        // Given
        var streams = new EnergyDataStreams();
        var provider = new EnerginetAccountingPointEnvelopeProvider(streams, cimConfig);

        // When & Then
        StepVerifier.create(provider.getAccountingPointEnvelopeFlux())
                    .then(() -> {
                        streams.publish(apiResponse);
                        streams.close();
                    })
                    .assertNext(document -> assertAll(
                            () -> assertEquals(apiResponse.permissionRequest().permissionId(),
                                               document.getMessageDocumentHeader()
                                                       .getMessageDocumentHeaderMetaInformation()
                                                       .getPermissionid()),
                            () -> assertEquals(apiResponse.permissionRequest().connectionId(),
                                               document.getMessageDocumentHeader()
                                                       .getMessageDocumentHeaderMetaInformation()
                                                       .getConnectionid()),
                            () -> assertEquals(apiResponse.permissionRequest().dataNeedId(),
                                               document.getMessageDocumentHeader()
                                                       .getMessageDocumentHeaderMetaInformation()
                                                       .getDataNeedid()),
                            () -> assertNotNull(document.getAccountingPointMarketDocument())
                    ))
                    .verifyComplete();
    }
}
