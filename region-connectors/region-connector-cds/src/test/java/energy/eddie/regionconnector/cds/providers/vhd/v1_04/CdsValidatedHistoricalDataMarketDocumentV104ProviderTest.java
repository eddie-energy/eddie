// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_04;

import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

class CdsValidatedHistoricalDataMarketDocumentV104ProviderTest {

    @Test
    void testValidatedHistoricalDataMarketDocument_returnsDocuments() {
        // Given
        var streams = new IdentifiableDataStreams();
        var provider = new CdsValidatedHistoricalDataMarketDocumentV104Provider(streams);
        var account = new AccountsEndpoint200ResponseAllOfAccountsInner()
                .customerNumber("customer-number")
                .cdsAccountId("cds-account-id");
        var serviceContract = new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()
                .cdsAccountId("cds-account-id")
                .cdsServicecontractId("cds-servicecontract-id");
        var servicePoint = new ServicePointEndpoint200ResponseAllOfServicePointsInner()
                .cdsServicepointId("cds-servicepoint-id")
                .currentServicecontracts(List.of("cds-servicecontract-id"));
        var meterDevice = new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()
                .cdsMeterdeviceId("cds-meterdevice-id")
                .meterNumber("meter-number")
                .addCurrentServicepointsItem("cds-servicepoint-id");
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var usageSegment = new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()
                .addRelatedMeterdevicesItem("cds-meterdevice-id")
                .segmentStart(now)
                .segmentEnd(now)
                .interval(BigDecimal.valueOf(900))
                .format(List.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_FWD,
                                UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_REV,
                                UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_NET))
                .values(List.of(
                        List.of(Map.of("v", BigDecimal.ONE), Map.of("v", BigDecimal.ONE), Map.of("v", BigDecimal.ONE)),
                        List.of(Map.of("v", BigDecimal.ONE), Map.of("v", BigDecimal.ONE), Map.of("v", BigDecimal.ONE))
                ));
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .setDataStart(now.toLocalDate())
                .setDataEnd(now.toLocalDate())
                .build();

        // When
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> streams.publishValidatedHistoricalData(
                            pr,
                            List.of(account),
                            List.of(serviceContract),
                            List.of(servicePoint),
                            List.of(meterDevice),
                            List.of(usageSegment)
                    ))
                    .then(streams::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}