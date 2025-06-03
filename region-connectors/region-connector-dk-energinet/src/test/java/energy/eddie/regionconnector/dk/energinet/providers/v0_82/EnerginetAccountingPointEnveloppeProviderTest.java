package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDto;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDtoResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequestBuilder;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableAccountingPointDetails;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EnerginetAccountingPointEnvelopeProviderTest {

    static MeteringPointDetailsCustomerDto meteringPointDetailsCustomerDto;

    static IdentifiableAccountingPointDetails apiResponse;

    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
            "fallbackId"
    );

    @SuppressWarnings("DataFlowIssue")
    @BeforeAll
    static void setUp() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JsonNullableModule());
        try (InputStream is = EnerginetAccountingPointEnvelopeProviderTest.class.getClassLoader()
                                                                                .getResourceAsStream(
                                                                                        "MeteringPointDetailsCustomerDtoResponseListApiResponse.json")) {
            MeteringPointDetailsCustomerDtoResponseListApiResponse response = objectMapper.readValue(is,
                                                                                                     MeteringPointDetailsCustomerDtoResponseListApiResponse.class);
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
    void getEddieValidatedHistoricalDataMarketDocumentStream_producesDocumentWhenTestJsonIsUsed() throws Exception {
        // Given
        TestPublisher<IdentifiableAccountingPointDetails> testPublisher = TestPublisher.create();

        var provider = new EnerginetAccountingPointEnvelopeProvider(testPublisher.flux(), cimConfig);

        // When & Then
        StepVerifier.create(provider.getAccountingPointEnvelopeFlux())
                    .then(() -> {
                        testPublisher.emit(apiResponse);
                        testPublisher.complete();
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

        provider.close();
    }
}
