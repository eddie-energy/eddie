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

    @Test
    void testValidatedHistoricalDataMarketDocument_returnsDocuments_alsoEmitsEquivalentV112() {
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
        StepVerifier.create(reactor.core.publisher.Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(), provider.getValidatedHistoricalDataV112Stream()).map(pair -> {
            var source = pair.getT1();
            var target = pair.getT2();
            org.junit.jupiter.api.Assertions.assertEquals(source.getMessageDocumentHeaderMetaInformationPermissionId(), target.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
            org.junit.jupiter.api.Assertions.assertEquals(source.getMessageDocumentHeaderMetaInformationDataNeedId(), target.getMessageDocumentHeader().getMetaInformation().getDataNeedId());
            var sourceSeries = source.getMarketDocument().getTimeSeries();
            var targetSeries = target.getVHDMarketDocument().getTimeSeries();
            org.junit.jupiter.api.Assertions.assertEquals(sourceSeries.size(), targetSeries.size());
            for (int i = 0; i < sourceSeries.size(); i++) {
                var left = sourceSeries.get(i);
                var right = targetSeries.get(i);
                org.junit.jupiter.api.Assertions.assertEquals(left.getEnergyMeasurementUnitName(), right.getEnergyMeasurementUnitName());
                org.junit.jupiter.api.Assertions.assertEquals(left.getEnergyQualityMeasurementUnitName(), right.getEnergyQualityMeasurementUnitName());
                org.junit.jupiter.api.Assertions.assertEquals(left.getFlowDirectionDirection(), right.getFlowDirectionDirection());
                org.junit.jupiter.api.Assertions.assertEquals(left.getPeriods().size(), right.getSeriesPeriods().size());
                for (int j = 0; j < left.getPeriods().size(); j++) {
                    var lp = left.getPeriods().get(j);
                    var rp = right.getSeriesPeriods().get(j);
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getResolution(), rp.getResolution());
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getTimeInterval().getStart(), rp.getTimeInterval().getStart());
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getTimeInterval().getEnd(), rp.getTimeInterval().getEnd());
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getPoints().size(), rp.getPoints().size());
                    for (int k = 0; k < lp.getPoints().size(); k++) {
                        var l = lp.getPoints().get(k);
                        var r = rp.getPoints().get(k);
                        org.junit.jupiter.api.Assertions.assertEquals(l.getPosition(), r.getPosition());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQuantityQuantity(), r.getEnergyQuantityQuantity());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQuantityQuality(), r.getEnergyQuantityQuality());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQualityQuantityQuantity(), r.getEnergyQualityQuantityQuantity());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQualityQuantityQuality(), r.getEnergyQualityQuantityQuality());
                    }
                }
            }
            if (!targetSeries.isEmpty()) {
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                    var serialized = new energy.eddie.cim.serde.XmlMessageSerde().serialize(target);
                    org.junit.jupiter.api.Assertions.assertTrue(energy.eddie.cim.testing.XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(serialized));
                });
            }
            return target;
        }))
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