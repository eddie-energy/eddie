package energy.eddie.regionconnector.cds.providers.ap;

import energy.eddie.regionconnector.cds.openapi.model.AccountsEndpoint200ResponseAllOfAccountsInner;
import energy.eddie.regionconnector.cds.openapi.model.MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner;
import energy.eddie.regionconnector.cds.openapi.model.ServiceContractEndpoint200ResponseAllOfServiceContractsInner;
import energy.eddie.regionconnector.cds.openapi.model.ServicePointEndpoint200ResponseAllOfServicePointsInner;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

class CdsAccountingPointDataMarketDocumentProviderTest {

    @Test
    void testAccountingPointDataMarketDocument_returnsDocuments() {
        // Given
        var streams = new IdentifiableDataStreams();
        var provider = new CdsAccountingPointDataMarketDocumentProvider(streams);
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .build();
        var account = new AccountsEndpoint200ResponseAllOfAccountsInner()
                .customerNumber("customer-number")
                .accountType("business")
                .cdsAccountId("account-id")
                .accountNumber("account-number")
                .accountName("EDDIE Ltd");
        var serviceContract = new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()
                .cdsServicecontractId("contract-id")
                .contractAddress("123 Main St - PARKING LOT\nAnytown, TX 11111")
                .serviceType("electric")
                .cdsAccountId("account-number")
                .cdsAccountId("account-id");
        var servicePoint = new ServicePointEndpoint200ResponseAllOfServicePointsInner()
                .cdsServicepointId("service-point-id")
                .addCurrentServicecontractsItem("contract-id")
                .servicepointAddress("123 Main St - PARKING LOT\nAnytown, TX 11111");
        var meterDevice = new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()
                .addCurrentServicepointsItem("service-point-id")
                .cdsMeterdeviceId("meter-id");

        // When
        StepVerifier.create(provider.getAccountingPointEnvelopeFlux())
                    .then(() -> streams.publishAccountingPointData(
                            pr,
                            List.of(account),
                            List.of(serviceContract),
                            List.of(servicePoint),
                            List.of(meterDevice)
                    ))
                    .then(streams::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}