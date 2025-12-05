package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.address.CustomerAddress;
import energy.eddie.regionconnector.fr.enedis.dto.contact.CustomerContact;
import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import energy.eddie.regionconnector.fr.enedis.dto.identity.CustomerIdentity;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisDataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnedisAccountingPointEnvelopeProviderTest {
    @Test
    void testGetEddieValidatedHistoricalDataMarketDocumentStream_publishesDocuments() throws Exception {
        // Given
        var identifiableAccountingPointData = identifiableAccountingPointData();
        PlainEnedisConfiguration enedisConfiguration = new PlainEnedisConfiguration(
                "clientId",
                "clientSecret",
                "/path"
        );
        IntermediateMarketDocumentFactory factory = new IntermediateMarketDocumentFactory(
                enedisConfiguration,
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                             "fallbackId")
        );
        EnergyDataStreams streams = new EnergyDataStreams();
        var provider = new EnedisAccountingPointDataEnvelopeProvider(streams, factory);

        // When
        StepVerifier.create(provider.getAccountingPointEnvelopeFlux())
                    .then(() -> {
                        streams.publish(identifiableAccountingPointData);
                        streams.close();
                    })
                    .assertNext(ap -> assertEquals(identifiableAccountingPointData.permissionRequest().permissionId(),
                                                   ap.getMessageDocumentHeader()
                                                     .getMessageDocumentHeaderMetaInformation()
                                                     .getPermissionid()))
                    .verifyComplete();

        // Clean-Up
        provider.close();
    }

    private IdentifiableAccountingPointData identifiableAccountingPointData() throws IOException {
        var contract = TestResourceProvider.readFromFile(TestResourceProvider.CONTRACT, CustomerContract.class);
        var address = TestResourceProvider.readFromFile(TestResourceProvider.ADDRESS, CustomerAddress.class);
        var identity = TestResourceProvider.readFromFile(TestResourceProvider.IDENTITY, CustomerIdentity.class);
        var contact = TestResourceProvider.readFromFile(TestResourceProvider.CONTACT, CustomerContact.class);
        var permissionRequest = new SimpleFrEnedisPermissionRequest(
                "usagePointId",
                null,
                UsagePointType.CONSUMPTION,
                Optional.empty(),
                "permissionId",
                "connectionId",
                "dataNeedId",
                PermissionProcessStatus.ACCEPTED,
                new EnedisDataSourceInformation(),
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC)
        );
        return new IdentifiableAccountingPointData(
                permissionRequest,
                contract,
                address,
                identity,
                contact
        );
    }
}
