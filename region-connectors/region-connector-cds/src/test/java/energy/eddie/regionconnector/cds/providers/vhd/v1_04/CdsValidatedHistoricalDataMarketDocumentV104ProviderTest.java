// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_04;

import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
        var pairedDocuments = Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(),
                                       provider.getValidatedHistoricalDataV112Stream());
        StepVerifier.create(pairedDocuments)
                    .then(() -> streams.publishValidatedHistoricalData(
                            pr,
                            List.of(account),
                            List.of(serviceContract),
                            List.of(servicePoint),
                            List.of(meterDevice),
                            List.of(usageSegment)
                    ))
                    .then(streams::close)
                    .assertNext(pair -> assertEquivalentHistoricalData(pair.getT1(), pair.getT2()))
                    .verifyComplete();
    }

    private static void assertEquivalentHistoricalData(VHDEnvelope source, Envelope target) {
        assertEquals(source.getMarketDocument().getMRID(), target.getVHDMarketDocument().getMRID());
        assertEquals(
                source.getMessageDocumentHeaderMetaInformationPermissionId(),
                target.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId()
        );
        assertEquals(
                source.getMessageDocumentHeaderMetaInformationDataNeedId(),
                target.getMessageDocumentHeader().getMetaInformation().getDataNeedId()
        );
        var sourceSeries = source.getMarketDocument().getTimeSeries();
        var targetSeries = target.getVHDMarketDocument().getTimeSeries();
        assertEquals(3, sourceSeries.size(), "Fixture time series");
        assertEquals(sourceSeries.size(), targetSeries.size());
        for (int i = 0; i < sourceSeries.size(); i++) {
            var sourceTimeSeries = sourceSeries.get(i);
            var targetTimeSeries = targetSeries.get(i);
            assertEquals(sourceTimeSeries.getMRID(), targetTimeSeries.getMRID());
            assertEquals(
                    sourceTimeSeries.getEnergyMeasurementUnitName(),
                    targetTimeSeries.getEnergyMeasurementUnitName()
            );
            assertEquals(
                    sourceTimeSeries.getEnergyQualityMeasurementUnitName(),
                    targetTimeSeries.getEnergyQualityMeasurementUnitName()
            );
            assertEquals(sourceTimeSeries.getFlowDirectionDirection(), targetTimeSeries.getFlowDirectionDirection());
            assertEquals(1, sourceTimeSeries.getPeriods().size(), "Fixture periods per time series");
            assertEquals(sourceTimeSeries.getPeriods().size(), targetTimeSeries.getSeriesPeriods().size());
            for (int j = 0; j < sourceTimeSeries.getPeriods().size(); j++) {
                var sourcePeriod = sourceTimeSeries.getPeriods().get(j);
                var targetPeriod = targetTimeSeries.getSeriesPeriods().get(j);
                assertEquals(sourcePeriod.getResolution(), targetPeriod.getResolution());
                assertEquals(sourcePeriod.getTimeInterval().getStart(), targetPeriod.getTimeInterval().getStart());
                assertEquals(sourcePeriod.getTimeInterval().getEnd(), targetPeriod.getTimeInterval().getEnd());
                assertEquals(2, sourcePeriod.getPoints().size(), "Fixture points per period");
                assertEquals(sourcePeriod.getPoints().size(), targetPeriod.getPoints().size());
                for (int k = 0; k < sourcePeriod.getPoints().size(); k++) {
                    var sourcePoint = sourcePeriod.getPoints().get(k);
                    var targetPoint = targetPeriod.getPoints().get(k);
                    assertEquals(sourcePoint.getPosition(), targetPoint.getPosition());
                    assertEquals(sourcePoint.getEnergyQuantityQuantity(), targetPoint.getEnergyQuantityQuantity());
                    assertEquals(sourcePoint.getEnergyQuantityQuality(), targetPoint.getEnergyQuantityQuality());
                    assertEquals(
                            sourcePoint.getEnergyQualityQuantityQuantity(),
                            targetPoint.getEnergyQualityQuantityQuantity()
                    );
                    assertEquals(
                            sourcePoint.getEnergyQualityQuantityQuality(),
                            targetPoint.getEnergyQualityQuantityQuality()
                    );
                }
            }
        }
        assertDoesNotThrow(() -> {
            var serialized = new XmlMessageSerde().serialize(target);
            assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(serialized));
        });
    }
}
