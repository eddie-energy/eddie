// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_12;

import energy.eddie.cim.v1_12.vhd.Envelope;
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

import static org.junit.jupiter.api.Assertions.*;

class CdsValidatedHistoricalDataMarketDocumentV112ProviderTest {
    @Test
    void mapsPopulatedRawFixtureWithoutLegacyProvider() {
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

        var providerV112 = new CdsValidatedHistoricalDataMarketDocumentV112Provider(streams);

        StepVerifier.create(providerV112.getValidatedHistoricalDataV112Stream())
                    .then(() -> streams.publishValidatedHistoricalData(
                            pr,
                            List.of(account),
                            List.of(serviceContract),
                            List.of(servicePoint),
                            List.of(meterDevice),
                            List.of(usageSegment)
                    ))
                    .then(streams::close)
                    .assertNext(CdsValidatedHistoricalDataMarketDocumentV112ProviderTest::assertPopulatedDocument)
                    .verifyComplete();
    }

    private static void assertPopulatedDocument(Envelope envelope) {
        assertEquals("pid", envelope.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
        assertEquals("dnid", envelope.getMessageDocumentHeader().getMetaInformation().getDataNeedId());
        var document = envelope.getVHDMarketDocument();
        assertNotNull(document);
        assertFalse(document.getTimeSeries().isEmpty());
        var pointCount = 0;
        for (var series : document.getTimeSeries()) {
            assertNotNull(series.getDateAndOrTimeDateTime());
            assertFalse(series.getSeriesPeriods().isEmpty());
            for (var period : series.getSeriesPeriods()) {
                assertNotNull(period.getResolution());
                assertNotNull(period.getTimeInterval());
                assertFalse(period.getPoints().isEmpty());
                for (var point : period.getPoints()) {
                    assertTrue(point.getPosition() > 0);
                    assertTrue(point.getEnergyQuantityQuantity() != null || point.getEnergyQualityQuantityQuantity() != null);
                    pointCount++;
                }
            }
        }
        assertTrue(pointCount > 0);
    }
}
